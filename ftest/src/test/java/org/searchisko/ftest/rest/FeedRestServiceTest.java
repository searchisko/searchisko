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
 * @see org.searchisko.api.rest.FeedRestService
 */
@RunWith(Arquillian.class)
public class FeedRestServiceTest {
	public static final String FEED_REST_API = DeploymentHelpers.DEFAULT_REST_VERSION + "feed";

	@Deployment(testable = false)
	public static WebArchive createDeployment() throws IOException {
		return DeploymentHelpers.createDeployment();
	}

	@ArquillianResource
	URL context;

	@Test
	@InSequence(10)
	public void assertNoProviderConfigured() throws MalformedURLException {
		given().contentType(ContentType.JSON).expect().log().ifValidationFails().statusCode(500).when()
				.get(new URL(context, FEED_REST_API).toExternalForm());
	}

	static ProviderModel provider1 = new ProviderModel("provider1", "password");
	public static final String TYPE1 = "provider1_blog";
	public static final String TYPE2 = "provider1_issue";
	public static final String CONTENT_TYPE = "text/html";
	public static final String FEED_CONTENT_TYPE = "html";

	@Test
	@InSequence(19)
	public void assertCreateProvider() throws MalformedURLException {
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
		given().contentType(ContentType.JSON).expect().log().ifValidationFails().statusCode(200)
				.contentType("application/atom+xml").when().get(new URL(context, FEED_REST_API).toExternalForm());
	}

	static final String contentId = "test-id";
	static final String contentId2 = "test-id2";

	@Test
	@InSequence(21)
	public void assertPushContentWithId() throws MalformedURLException {
		Map<String, Object> content = new HashMap<>();
		content.put("sys_description", "desc");
		content.put("sys_content", "content");
		ContentRestServiceTest.createOrUpdateContent(context, provider1, TYPE1, contentId, content);
	}

	@Test
	@InSequence(22)
	public void assertPushContentWithId2() throws MalformedURLException {
		Map<String, Object> content = new HashMap<>();
		content.put("sys_description", "desc 2");
		content.put("sys_content", "content 2");
		ContentRestServiceTest.createOrUpdateContent(context, provider1, TYPE2, contentId2, content);
	}

	@Test
	@InSequence(30)
	public void assertRefreshES() throws MalformedURLException {
		DeploymentHelpers.refreshES();
	}

	@Test
	@InSequence(31)
	public void assertGetDataFeed_noPermissionForAnonym() throws MalformedURLException {
		given().contentType(ContentType.XML).expect().log().all().statusCode(200).contentType("application/atom+xml")
				.body("feed.entry.author.name.text()", is("unknown")).body("feed.entry.content.@type", is(FEED_CONTENT_TYPE))
				.body("feed.entry.content.text()", is("content")).body("feed.entry.summary.text()", is("desc")).when()
				.get(new URL(context, FEED_REST_API).toExternalForm());
	}

	@Test
	@InSequence(32)
	public void assertGetDataFeed_providerHasPermission() throws MalformedURLException {
		// authenticated provider has right to type 2 as it allows role "provider"
		given().contentType(ContentType.XML).auth().preemptive().basic(provider1.name, provider1.password).expect().log()
				.all().statusCode(200).contentType("application/atom+xml")
				.body("feed.entry[0].author.name.text()", is("unknown"))
				.body("feed.entry[1].author.name.text()", is("unknown")).when()
				.get(new URL(context, FEED_REST_API).toExternalForm());
	}

	@Test
	@InSequence(40)
	public void assertChangeRoleInProvider1() throws MalformedURLException {
		provider1.addContentType(TYPE2, "issue", true, "", "otherrole");
		ProviderRestServiceTest.deleteProvider(context, provider1);
		ProviderRestServiceTest.createNewProvider(context, provider1);
	}

	@Test
	@InSequence(41)
	public void assertGetDataFeed_providerHasNoPermission() throws MalformedURLException {
		// authenticated provider has no right to type 2 as it allows another role now
		given().contentType(ContentType.XML).auth().preemptive().basic(provider1.name, provider1.password).expect().log()
				.all().statusCode(200).contentType("application/atom+xml").body("feed.entry.author.name.text()", is("unknown"))
				.body("feed.entry.content.@type", is(FEED_CONTENT_TYPE)).body("feed.entry.content.text()", is("content"))
				.body("feed.entry.summary.text()", is("desc")).when().get(new URL(context, FEED_REST_API).toExternalForm());
	}

	@Test
	@InSequence(42)
	public void assertGetDataFeed_adminHasPermission() throws MalformedURLException {
		// authenticated admin has always right to type 2
		given().contentType(ContentType.XML).auth().preemptive()
				.basic(DeploymentHelpers.DEFAULT_PROVIDER_NAME, DeploymentHelpers.DEFAULT_PROVIDER_PASSWORD).expect().log()
				.all().statusCode(200).contentType("application/atom+xml")
				.body("feed.entry[0].author.name.text()", is("unknown"))
				.body("feed.entry[1].author.name.text()", is("unknown")).when()
				.get(new URL(context, FEED_REST_API).toExternalForm());
	}

}
