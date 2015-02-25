/*
 * JBoss, Home of Professional Open Source
 * Copyright 2012 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 */
package org.searchisko.ftest.rest;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.junit.InSequence;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.searchisko.api.ContentObjectFields;
import org.searchisko.api.security.Role;
import org.searchisko.ftest.DeploymentHelpers;
import org.searchisko.ftest.ProviderModel;
import org.searchisko.ftest.filter.ESProxyFilterTest;

import com.jayway.restassured.http.ContentType;

import static com.jayway.restassured.RestAssured.given;
import static org.hamcrest.Matchers.is;

/**
 * Integration test for /feed REST API.
 * <p/>
 * http://docs.jbossorg.apiary.io/#feedapi
 * 
 * @author Libor Krzyzanek
 * @author Lukas Vlcek
 * @see org.searchisko.api.rest.FeedRestService
 */
@RunWith(Arquillian.class)
public class FeedRestServiceTest {

	public static final String FEED_REST_API_V1 = DeploymentHelpers.V1_REST_VERSION + "feed";
	public static final String FEED_REST_API_CURRENT = DeploymentHelpers.CURRENT_REST_VERSION + "feed";

    // We want to test several versions of FEED API. See https://github.com/searchisko/searchisko/issues/211
    public static final String[] FEED_API_VERSIONS = new String[]{ FEED_REST_API_V1, FEED_REST_API_CURRENT };

	@Deployment(testable = false)
	public static WebArchive createDeployment() throws IOException {
		return DeploymentHelpers.createDeployment();
	}

	@ArquillianResource
	URL context;

	@Test
	@InSequence(10)
	public void assertNoProviderConfigured() throws MalformedURLException {
        for (String feedAPI: FEED_API_VERSIONS) {
            given().contentType(ContentType.JSON).expect().log().ifValidationFails().statusCode(500).when()
                    .get(new URL(context, feedAPI).toExternalForm());
        }
	}

	static ProviderModel provider1 = new ProviderModel("provider1", "password");
	public static final String TYPE1 = "provider1_blog";
	public static final String TYPE2 = "provider1_issue";
	public static final String CONTENT_TYPE = "text/html";
	public static final String FEED_CONTENT_TYPE = "html";

	@Test
	@InSequence(19)
	public void setupCreateProviderAndContentTypes() throws MalformedURLException {
		String idx1 = provider1.addContentType(TYPE1, "blogpost", true, CONTENT_TYPE);
		String idx2 = provider1.addContentType(TYPE2, "issue", true, CONTENT_TYPE, Role.PROVIDER);

		ProviderRestServiceTest.createNewProvider(context, provider1);
		ESProxyFilterTest.createSearchESIndex(context, idx1, "{}");
		ESProxyFilterTest.createSearchESIndex(context, idx2, "{}");
		ESProxyFilterTest.createSearchESIndexMapping(context, idx1, TYPE1, "{\"" + TYPE1 + "\":{}}");
		ESProxyFilterTest.createSearchESIndexMapping(context, idx2, TYPE2, "{\"" + TYPE2 + "\":{}}");
	}

	@Test
	@InSequence(20)
	public void assertGetAllEmpty() throws MalformedURLException {
        for (String feedAPI: FEED_API_VERSIONS) {
            given().contentType(ContentType.JSON).expect().log().ifValidationFails().statusCode(200)
                    .contentType("application/atom+xml").when().get(new URL(context, feedAPI).toExternalForm());
        }
	}

	static final String contentId = "test-id";
	static final String contentId2 = "test-id2";
	static final String contentId3 = "test-id3";
	static final String contentId4 = "test-id4";
	static final String contentId5 = "test-id5";

	@Test
	@InSequence(21)
	public void setupPushContentWithType1Id1() throws MalformedURLException {
		Map<String, Object> content = new HashMap<>();
		content.put("sys_description", "desc");
		content.put("sys_content", "content");
		ContentRestServiceTest.createOrUpdateContent(context, provider1, TYPE1, contentId, content);
	}

	@Test
	@InSequence(22)
	public void setupPushContentWithType2Id2() throws MalformedURLException {
		Map<String, Object> content = new HashMap<>();
		content.put("sys_description", "desc 2");
		content.put("sys_content", "content 2");
		ContentRestServiceTest.createOrUpdateContent(context, provider1, TYPE2, contentId2, content);
	}

	@Test
	@InSequence(30)
	public void setupRefreshES() throws MalformedURLException {
		DeploymentHelpers.refreshES();
	}

	@Test
	@InSequence(31)
	public void assertGetDataFeed_noPermissionForAnonym() throws MalformedURLException {
        for (String feedAPI: FEED_API_VERSIONS) {
            given().contentType(ContentType.XML).expect().log().ifValidationFails().statusCode(200)
                    .contentType("application/atom+xml").body("feed.entry.author.name.text()", is("unknown"))
                    .body("feed.entry.content.@type", is(FEED_CONTENT_TYPE)).body("feed.entry.content.text()", is("content"))
                    .body("feed.entry.summary.text()", is("desc")).when().get(new URL(context, feedAPI).toExternalForm());
        }
	}

	@Test
	@InSequence(32)
	public void assertGetDataFeed_providerHasPermission() throws MalformedURLException {
		// authenticated provider has right to type 2 as it allows role "provider"
        for (String feedAPI: FEED_API_VERSIONS) {
            given().contentType(ContentType.XML).auth().preemptive().basic(provider1.name, provider1.password).expect().log()
                    .ifValidationFails().statusCode(200).contentType("application/atom+xml")
                    .body("feed.entry[0].author.name.text()", is("unknown"))
                    .body("feed.entry[1].author.name.text()", is("unknown")).when()
                    .get(new URL(context, feedAPI).toExternalForm());
        }
	}

	@Test
	@InSequence(40)
	public void setupChangeRoleInProvider1() throws MalformedURLException {
		provider1.addContentType(TYPE2, "issue", true, "", "otherrole");
		ProviderRestServiceTest.updateProvider(context, provider1);
	}

	@Test
	@InSequence(41)
	public void assertGetDataFeed_providerHasNoPermission() throws MalformedURLException {
		// authenticated provider has no right to type 2 as it allows another role now
        for (String feedAPI: FEED_API_VERSIONS) {
            given().contentType(ContentType.XML).auth().preemptive().basic(provider1.name, provider1.password).expect().log()
                    .ifValidationFails().statusCode(200).contentType("application/atom+xml")
                    .body("feed.entry.author.name.text()", is("unknown")).body("feed.entry.content.@type", is(FEED_CONTENT_TYPE))
                    .body("feed.entry.content.text()", is("content")).body("feed.entry.summary.text()", is("desc")).when()
                    .get(new URL(context, feedAPI).toExternalForm());
        }
	}

	@Test
	@InSequence(42)
	public void assertGetDataFeed_adminHasPermission() throws MalformedURLException {
		// authenticated admin has always right to type 2
        for (String feedAPI: FEED_API_VERSIONS) {
            given().contentType(ContentType.XML).auth().preemptive()
                    .basic(DeploymentHelpers.DEFAULT_PROVIDER_NAME, DeploymentHelpers.DEFAULT_PROVIDER_PASSWORD).expect().log()
                    .ifValidationFails().statusCode(200).contentType("application/atom+xml")
                    .body("feed.entry[0].author.name.text()", is("unknown"))
                    .body("feed.entry[1].author.name.text()", is("unknown")).when()
                    .get(new URL(context, feedAPI).toExternalForm());
        }
	}

	@Test
	@InSequence(50)
	public void setupPushContentWithType1Id2() throws MalformedURLException {
		Map<String, Object> content = new HashMap<>();
		content.put("sys_description", "desc 2");
		content.put("sys_content", "content 2");
		ContentRestServiceTest.createOrUpdateContent(context, provider1, TYPE1, contentId2, content);

		DeploymentHelpers.refreshES();
	}

	@Test
	@InSequence(51)
	public void assertGetDataFeed_paginationSupport() throws MalformedURLException {
        for (String feedAPI: FEED_API_VERSIONS) {
            given().contentType(ContentType.XML).expect().log().ifValidationFails().statusCode(200)
                    .contentType("application/atom+xml").body("feed.entry[0].content.text()", is("content"))
                    .body("feed.entry[1].content.text()", is("content 2")).when()
                    .get(new URL(context, feedAPI).toExternalForm());

            given().contentType(ContentType.XML).expect().log().ifValidationFails().statusCode(200)
                    .contentType("application/atom+xml").body("feed.entry.content.@type", is(FEED_CONTENT_TYPE))
                    .body("feed.entry.content.text()", is("content")).when()
                    .get(new URL(context, feedAPI + "?size=1&from=0").toExternalForm());

            given().contentType(ContentType.XML).expect().log().ifValidationFails().statusCode(200)
                    .contentType("application/atom+xml").body("feed.entry.content.@type", is(FEED_CONTENT_TYPE))
                    .body("feed.entry.content.text()", is("content 2")).when()
                    .get(new URL(context, feedAPI + "?size=1&from=1").toExternalForm());
        }
	}

	// /////////////////////////////// Document level security - #143 ////////////////////////////////

	@Test
	@InSequence(60)
	public void setupDlsTests() throws MalformedURLException {

		provider1.addContentType(TYPE2, "issue", true, "");
		ProviderRestServiceTest.updateProvider(context, provider1);

		ContentRestServiceTest.deleteContent(context, provider1, TYPE2, contentId2);

		// this one will be visible only for admin in our case
		{
			Map<String, Object> content = new HashMap<>();
			content.put(ContentObjectFields.SYS_VISIBLE_FOR_ROLES, "role1");
			content.put("sys_description", "desc 3");
			content.put("sys_content", "content 3");
			ContentRestServiceTest.createOrUpdateContent(context, provider1, TYPE1, contentId3, content);
		}

		// this one will be visible for admin and auth provider with role in our case
		{
			Map<String, Object> content = new HashMap<>();
			content.put(ContentObjectFields.SYS_VISIBLE_FOR_ROLES, "provider");
			content.put("sys_description", "desc 4");
			content.put("sys_content", "content 4");
			ContentRestServiceTest.createOrUpdateContent(context, provider1, TYPE1, contentId4, content);
		}

		// this one will be visible for all again
		{
			Map<String, Object> content = new HashMap<>();
			content.put("sys_description", "desc 5");
			content.put("sys_content", "content 5");
			ContentRestServiceTest.createOrUpdateContent(context, provider1, TYPE1, contentId5, content);
		}

		DeploymentHelpers.refreshES();
	}

	@Test
	@InSequence(61)
	public void assertDls_adminHasPermissionToAllDocuments() throws MalformedURLException {
        for (String feedAPI: FEED_API_VERSIONS) {
            given().contentType(ContentType.XML).auth().preemptive()
                    .basic(DeploymentHelpers.DEFAULT_PROVIDER_NAME, DeploymentHelpers.DEFAULT_PROVIDER_PASSWORD).expect().log()
                    .ifValidationFails().statusCode(200).contentType("application/atom+xml")
                    .body("feed.entry[0].id.text()", is("searchisko:content:id:provider1_blog-test-id"))
                    .body("feed.entry[1].id.text()", is("searchisko:content:id:provider1_blog-test-id2"))
                    .body("feed.entry[2].id.text()", is("searchisko:content:id:provider1_blog-test-id3"))
                    .body("feed.entry[3].id.text()", is("searchisko:content:id:provider1_blog-test-id4"))
                    .body("feed.entry[4].id.text()", is("searchisko:content:id:provider1_blog-test-id5")).when()
                    .get(new URL(context, feedAPI).toExternalForm());
        }
	}

	@Test
	@InSequence(62)
	public void assertDls_anonym() throws MalformedURLException {
        for (String feedAPI: FEED_API_VERSIONS) {
            given().contentType(ContentType.XML).expect().log().ifValidationFails().statusCode(200)
                    .contentType("application/atom+xml")
                    .body("feed.entry[0].id.text()", is("searchisko:content:id:provider1_blog-test-id"))
                    .body("feed.entry[1].id.text()", is("searchisko:content:id:provider1_blog-test-id2"))
                    .body("feed.entry[2].id.text()", is("searchisko:content:id:provider1_blog-test-id5")).when()
                    .get(new URL(context, feedAPI).toExternalForm());
        }
	}

	@Test
	@InSequence(63)
	public void assertDls_userwithrole() throws MalformedURLException {
        for (String feedAPI: FEED_API_VERSIONS) {
            given().contentType(ContentType.XML).auth().preemptive().basic(provider1.name, provider1.password).expect().log()
                    .ifValidationFails().statusCode(200).contentType("application/atom+xml")
                    .body("feed.entry[0].id.text()", is("searchisko:content:id:provider1_blog-test-id"))
                    .body("feed.entry[1].id.text()", is("searchisko:content:id:provider1_blog-test-id2"))
                    .body("feed.entry[2].id.text()", is("searchisko:content:id:provider1_blog-test-id4"))
                    .body("feed.entry[3].id.text()", is("searchisko:content:id:provider1_blog-test-id5")).when()
                    .get(new URL(context, feedAPI).toExternalForm());
        }
	}

}
