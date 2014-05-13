/*
 * JBoss, Home of Professional Open Source
 * Copyright 2012 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 */
package org.searchisko.ftest;


import com.jayway.restassured.http.ContentType;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.junit.InSequence;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import static com.jayway.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;


/**
 * Integration tests for {@link org.searchisko.api.rest.ContentRestService} REST API
 * <p/>
 * see http://docs.jbossorg.apiary.io/#contentmanipulationapi
 *
 * @author Libor Krzyzanek
 * @see org.searchisko.api.rest.ContentRestService
 */
@RunWith(Arquillian.class)
public class ContentRestServiceTest {

	public static final String CONTENT_REST_API = DeploymentHelpers.DEFAULT_REST_VERSION + "content/{type}/{contentId}";

	public static final String TYPE1 = "provider1_blog";

	public static final String TYPE2 = "provider2_issue";


	@ArquillianResource
	protected URL context;

	@Deployment(testable = false)
	public static WebArchive createDeployment() throws IOException {
		return DeploymentHelpers.createDeployment();
	}

	@Test
	@InSequence(0)
	public void assertBasicAuthHeader() throws MalformedURLException {
		int expStatus = 401;
		given().pathParam("type", TYPE1).pathParam("contentId", "test").contentType(ContentType.JSON)
				.body("{}")
				.expect().statusCode(expStatus)
				.header("WWW-Authenticate", "Basic realm=\"Insert Provider's username and password\"")
				.log().ifStatusCodeMatches(is(not(expStatus)))
				.when().post(new URL(context, CONTENT_REST_API).toExternalForm());
	}

	@Test
	@InSequence(1)
	public void assertNotAuthenticated() throws MalformedURLException {
		int expStatus = 401;
		// POST /content/type/id
		given().pathParam("type", TYPE1).pathParam("contentId", "id").contentType(ContentType.JSON)
				.body("{}")
				.expect().statusCode(expStatus).log().ifStatusCodeMatches(is(not(expStatus)))
				.when().post(new URL(context, CONTENT_REST_API).toExternalForm());

		// POST /content/type/
		given().pathParam("type", TYPE1).pathParam("contentId", "").contentType(ContentType.JSON)
				.body("{}")
				.expect().statusCode(expStatus).log().ifStatusCodeMatches(is(not(expStatus)))
				.when().post(new URL(context, CONTENT_REST_API).toExternalForm());

		// DELETE /content/type/
		given().pathParam("type", TYPE1).pathParam("contentId", "").contentType(ContentType.JSON)
				.body("{}")
				.expect().statusCode(expStatus).log().ifStatusCodeMatches(is(not(expStatus)))
				.when().delete(new URL(context, CONTENT_REST_API).toExternalForm());

		// DELETE /content/type/id
		given().pathParam("type", TYPE1).pathParam("contentId", "id").contentType(ContentType.JSON)
				.body("{}")
				.expect().statusCode(expStatus).log().ifStatusCodeMatches(is(not(expStatus)))
				.when().delete(new URL(context, CONTENT_REST_API).toExternalForm());

	}

	static ProviderModel provider1 = new ProviderModel("provider1", "password");

	static ProviderModel provider2 = new ProviderModel("provider2", "password2");


	@Test
	@InSequence(1)
	public void assertCreateProvider1BlogPost() throws MalformedURLException {
		provider1.addContentType(TYPE1, "blogpost", true);

		ProviderRestServiceTest.createNewProvider(context, provider1);
	}

	@Test
	@InSequence(2)
	public void assertCreateProvider2JiraIssue() throws MalformedURLException {
		provider2.addContentType(TYPE2, "issue", true);

		ProviderRestServiceTest.createNewProvider(context, provider2);
	}

	static final String contentId = "test-id";

	public void createOrUpdateContent(ProviderModel provider, String contentType, String contentId, Map<String, Object> content) throws MalformedURLException {
		given().pathParam("type", contentType).pathParam("contentId", contentId).contentType(ContentType.JSON)
				.auth().basic(provider.name, provider.password)
				.body(content)
				.expect()
				.statusCode(200)
				.contentType(ContentType.JSON)
				.log().ifError()
				.body("status", isOneOf("insert", "update"))
				.body("message", isOneOf("Content inserted successfully.", "Content updated successfully."))
				.when().post(new URL(context, CONTENT_REST_API).toExternalForm());

		given().pathParam("type", contentType).pathParam("contentId", contentId).contentType(ContentType.JSON)
				.expect()
				.statusCode(200)
				.contentType(ContentType.JSON)
				.log().ifError()
				.body("sys_id", is(TYPE1 + "-" + contentId))
				.body("sys_content_id", is(contentId))
				.when().get(new URL(context, CONTENT_REST_API).toExternalForm());
	}

	@Test
	@InSequence(10)
	public void assertPushContentWithId() throws MalformedURLException {
		Map<String, Object> content = new HashMap<>();
		content.put("data", "test");
		createOrUpdateContent(provider1, TYPE1, contentId, content);
	}

	@Test
	@InSequence(11)
	public void assertRefreshES() throws MalformedURLException {
		DeploymentHelpers.refreshES();
	}

	@Test
	@InSequence(12)
	public void assertGetAll() throws MalformedURLException {
		given().pathParam("type", TYPE1).pathParam("contentId", "").contentType(ContentType.JSON)
				.expect()
				.statusCode(200)
				.contentType(ContentType.JSON)
				.log().ifError()
				.body("total", is(1))
				.body("hits[0].id", is(contentId))
				.when().get(new URL(context, CONTENT_REST_API).toExternalForm());
	}

	@Test
	@InSequence(13)
	public void assertUpdateContentWithId() throws MalformedURLException {
		Map<String, Object> content = new HashMap<>();
		content.put("data", "test");
		createOrUpdateContent(provider1, TYPE1, contentId, content);
	}

	@Test
	@InSequence(20)
	public void assertDeleteSecurity() throws MalformedURLException {
		// provider 2 cannot delete content from provider 1
		given().pathParam("type", TYPE1).pathParam("contentId", "").contentType(ContentType.JSON)
				.auth().basic(provider2.name, provider2.password)
				.body("{\"id\":[\"" + contentId + "\"]}")
				.expect()
				.statusCode(403)
				.contentType(ContentType.JSON)
				.log().ifStatusCodeMatches(is(not(403)))
				.when().delete(new URL(context, CONTENT_REST_API).toExternalForm());
	}

	public void deleteContent(ProviderModel provider, String contentType, String contentId) throws MalformedURLException {
		given().pathParam("type", contentType).pathParam("contentId", "").contentType(ContentType.JSON)
				.auth().basic(provider.name, provider.password)
				.body("{\"id\":[\"" + contentId + "\"]}")
				.expect()
				.statusCode(200)
				.contentType(ContentType.JSON)
				.body(contentId, is("ok"))
				.log().ifError()
				.when().delete(new URL(context, CONTENT_REST_API).toExternalForm());

		given().pathParam("type", contentType).pathParam("contentId", contentId).contentType(ContentType.JSON)
				.expect()
				.statusCode(404)
				.log().ifStatusCodeMatches(is(not(404)))
				.when().get(new URL(context, CONTENT_REST_API).toExternalForm());
	}


	@Test
	@InSequence(21)
	public void assertDelete() throws MalformedURLException {
		deleteContent(provider1, TYPE1, contentId);
	}

	@Test
	@InSequence(22)
	public void assertRefreshES2() throws MalformedURLException {
		DeploymentHelpers.refreshES();
	}

	@Test
	@InSequence(30)
	public void assertPushContentWithId2() throws MalformedURLException {
		Map<String, Object> content = new HashMap<>();
		content.put("data2", "test2");
		createOrUpdateContent(provider1, TYPE1, contentId, content);
	}

	@Test
	@InSequence(31)
	public void assertDeleteAsSuperProvider() throws MalformedURLException {
		deleteContent(DeploymentHelpers.DEFAULT_PROVIDER, TYPE1, contentId);
	}

}
