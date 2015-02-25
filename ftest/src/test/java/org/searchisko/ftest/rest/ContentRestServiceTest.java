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
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.junit.InSequence;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.searchisko.api.security.Role;
import org.searchisko.api.service.ContentManipulationLockService;
import org.searchisko.ftest.DeploymentHelpers;
import org.searchisko.ftest.ProviderModel;

import com.jayway.restassured.http.ContentType;

import static com.jayway.restassured.RestAssured.given;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.isOneOf;
import static org.hamcrest.Matchers.not;
import static org.searchisko.ftest.rest.RestTestHelpers.givenJsonAndLogIfFailsAndAuthPreemptive;

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

	public static final Set<String> ALLOWED_ROLES = new HashSet<>();
	static {
		ALLOWED_ROLES.add(Role.ADMIN);
		ALLOWED_ROLES.add(Role.PROVIDER);
	}

	public static final String CONTENT_REST_API_BASE = DeploymentHelpers.CURRENT_REST_VERSION + "content/{type}/";

	public static final String CONTENT_REST_API = CONTENT_REST_API_BASE + "{contentId}";

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
		given().pathParam("type", TYPE1).pathParam("contentId", "test").contentType(ContentType.JSON).body("{}").expect()
				.statusCode(expStatus).header("WWW-Authenticate", "Basic realm=\"Insert Provider's username and password\"")
				.log().ifStatusCodeMatches(is(not(expStatus))).when().post(new URL(context, CONTENT_REST_API).toExternalForm());
	}

	@Test
	@InSequence(1)
	public void assertNotAuthenticated() throws MalformedURLException {
		assertAccess(401, null, null);
	}

	@Test
	@InSequence(2)
	public void assertForbidden() throws MalformedURLException {
		for (String role : Role.ALL_ROLES) {
			if (!ALLOWED_ROLES.contains(role)) {
				assertAccess(403, role, role);
			}
		}
	}

	public void assertAccess(int expStatus, String username, String password) throws MalformedURLException {
		// POST /content/type/id
		givenJsonAndLogIfFailsAndAuthPreemptive(username, password)
				.pathParam("type", TYPE1).pathParam("contentId", "id").contentType(ContentType.JSON).body("{}")
				.expect().statusCode(expStatus)
				.when().post(new URL(context, CONTENT_REST_API).toExternalForm());

		// POST /content/type/
		givenJsonAndLogIfFailsAndAuthPreemptive(username, password)
				.pathParam("type", TYPE1).contentType(ContentType.JSON).body("{}").expect().statusCode(expStatus)
				.when().post(new URL(context, CONTENT_REST_API_BASE).toExternalForm());

		// DELETE /content/type/
		givenJsonAndLogIfFailsAndAuthPreemptive(username, password)
				.pathParam("type", TYPE1).contentType(ContentType.JSON).body("{}")
				.expect().statusCode(expStatus)
				.when().delete(new URL(context, CONTENT_REST_API_BASE).toExternalForm());

		// DELETE /content/type/id
		givenJsonAndLogIfFailsAndAuthPreemptive(username, password)
				.pathParam("type", TYPE1).pathParam("contentId", "id").contentType(ContentType.JSON).body("{}")
				.expect().statusCode(expStatus)
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

	public static void createOrUpdateContent(URL context, ProviderModel provider, String contentType, String contentId,
			Map<String, Object> content) throws MalformedURLException {
		given().pathParam("type", contentType).pathParam("contentId", contentId).contentType(ContentType.JSON).auth()
				.basic(provider.name, provider.password).body(content).expect().statusCode(200).contentType(ContentType.JSON)
				.log().ifValidationFails().body("status", isOneOf("insert", "update"))
				.body("message", isOneOf("Content inserted successfully.", "Content updated successfully.")).when()
				.post(new URL(context, CONTENT_REST_API).toExternalForm());

		given().pathParam("type", contentType).pathParam("contentId", contentId).contentType(ContentType.JSON).expect()
				.statusCode(200).contentType(ContentType.JSON).log().ifValidationFails()
				.body("sys_id", is(contentType + "-" + contentId)).body("sys_content_id", is(contentId)).when()
				.get(new URL(context, CONTENT_REST_API).toExternalForm());
	}

	@Test
	@InSequence(10)
	public void assertPushContentWithId() throws MalformedURLException {
		Map<String, Object> content = new HashMap<>();
		content.put("data", "test");
		createOrUpdateContent(context, provider1, TYPE1, contentId, content);
	}

	@Test
	@InSequence(11)
	public void assertRefreshES() throws MalformedURLException {
		DeploymentHelpers.refreshES();
	}

	@Test
	@InSequence(12)
	public void assertGetAll() throws MalformedURLException {
		given().pathParam("type", TYPE1).contentType(ContentType.JSON).expect().statusCode(200)
				.contentType(ContentType.JSON).log().ifValidationFails().body("total", is(1)).body("hits[0].id", is(contentId))
				.when().get(new URL(context, CONTENT_REST_API_BASE).toExternalForm());
	}

	@Test
	@InSequence(13)
	public void assertUpdateContentWithId() throws MalformedURLException {
		Map<String, Object> content = new HashMap<>();
		content.put("data", "test");
		createOrUpdateContent(context, provider1, TYPE1, contentId, content);
	}

	@Test
	@InSequence(20)
	public void assertDeleteSecurity() throws MalformedURLException {
		// provider 2 cannot delete content from provider 1
		given().pathParam("type", TYPE1).contentType(ContentType.JSON).auth().basic(provider2.name, provider2.password)
				.body("{\"id\":[\"" + contentId + "\"]}").expect().statusCode(403).contentType(ContentType.JSON).log()
				.ifStatusCodeMatches(is(not(403))).when().delete(new URL(context, CONTENT_REST_API_BASE).toExternalForm());
	}

	public static void deleteContent(URL context, ProviderModel provider, String contentType, String contentId)
			throws MalformedURLException {
		given().pathParam("type", contentType).contentType(ContentType.JSON).auth().basic(provider.name, provider.password)
				.body("{\"id\":[\"" + contentId + "\"]}").expect().statusCode(200).contentType(ContentType.JSON)
				.body(contentId, is("ok")).log().ifValidationFails().when()
				.delete(new URL(context, CONTENT_REST_API_BASE).toExternalForm());

		given().pathParam("type", contentType).pathParam("contentId", contentId).contentType(ContentType.JSON).expect()
				.statusCode(404).log().ifStatusCodeMatches(is(not(404))).when()
				.get(new URL(context, CONTENT_REST_API).toExternalForm());
	}

	@Test
	@InSequence(21)
	public void assertDelete() throws MalformedURLException {
		deleteContent(context, provider1, TYPE1, contentId);
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
		createOrUpdateContent(context, provider1, TYPE1, contentId, content);
	}

	@Test
	@InSequence(31)
	public void assertDeleteAsSuperProvider() throws MalformedURLException {
		deleteContent(context, DeploymentHelpers.DEFAULT_PROVIDER, TYPE1, contentId);
	}

	// ///////////////////////////// issue #109 - Content manipulation lock handling

	@Test
	@InSequence(40)
	public void assertContentAPILockForProvider() throws MalformedURLException {

		ProviderRestServiceTest.cmLockCreate(context, provider1.name, DeploymentHelpers.DEFAULT_PROVIDER_NAME,
				DeploymentHelpers.DEFAULT_PROVIDER_PASSWORD);

		Map<String, Object> content = new HashMap<>();
		content.put("data", "test");

		// API blocked for provider 1
		given().pathParam("type", TYPE1).pathParam("contentId", contentId).contentType(ContentType.JSON).auth()
				.basic(provider1.name, provider1.password).body(content).expect().statusCode(503).log().ifValidationFails()
				.when().post(new URL(context, CONTENT_REST_API).toExternalForm());

		// API works for provider 2
		createOrUpdateContent(context, provider2, TYPE2, contentId, content);
	}

	@Test
	@InSequence(41)
	public void assertContentAPILockForAll() throws MalformedURLException {

		ProviderRestServiceTest.cmLockCreate(context, ContentManipulationLockService.API_ID_ALL,
				DeploymentHelpers.DEFAULT_PROVIDER_NAME, DeploymentHelpers.DEFAULT_PROVIDER_PASSWORD);

		Map<String, Object> content = new HashMap<>();
		content.put("data", "test");

		// API blocked for provider 1
		given().pathParam("type", TYPE1).pathParam("contentId", contentId).contentType(ContentType.JSON).auth()
				.basic(provider1.name, provider1.password).body(content).expect().statusCode(503).log().ifValidationFails()
				.when().post(new URL(context, CONTENT_REST_API).toExternalForm());

		// API blocked for provider 2
		given().pathParam("type", TYPE2).pathParam("contentId", contentId).contentType(ContentType.JSON).auth()
				.basic(provider2.name, provider2.password).body(content).expect().statusCode(503).log().ifValidationFails()
				.when().post(new URL(context, CONTENT_REST_API).toExternalForm());
	}

}
