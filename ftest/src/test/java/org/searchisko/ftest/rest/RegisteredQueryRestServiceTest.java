/*
 * JBoss, Home of Professional Open Source
 * Copyright 2012 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 */
package org.searchisko.ftest.rest;

import com.jayway.restassured.http.ContentType;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.junit.InSequence;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.searchisko.api.security.Role;
import org.searchisko.ftest.DeploymentHelpers;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashSet;
import java.util.Set;

import static com.jayway.restassured.RestAssured.given;
import static org.hamcrest.Matchers.is;
import static org.searchisko.ftest.rest.RestTestHelpers.givenJsonAndLogIfFailsAndAuthPreemptive;

/**
 * Integration test for {@link org.searchisko.api.rest.RegisteredQueryRestService}.
 *
 * @author Lukas Vlcek
 * @see org.searchisko.api.rest.RegisteredQueryRestService
 */
@RunWith(Arquillian.class)
public class RegisteredQueryRestServiceTest {

	public static final Set<String> ALLOWED_ROLES = new HashSet<>();
	static {
		ALLOWED_ROLES.add(Role.ADMIN);
	}

	public static final String QUERY_REST_API = DeploymentHelpers.CURRENT_REST_VERSION + "query";

	@ArquillianResource
	protected URL context;

	@Deployment(testable = false)
	public static WebArchive createDeployment() throws IOException {
		return DeploymentHelpers.createDeployment();
	}

	@Test
	@InSequence(0)
	public void assertNotAuthenticated() throws MalformedURLException {
		assertAccess(401, null, null);
	}

	@Test
	@InSequence(1)
	public void assertForbidden() throws MalformedURLException {
		for (String role : Role.ALL_ROLES) {
			if (!ALLOWED_ROLES.contains(role)) {
				assertAccess(403, role, role);
			}
		}
	}

	public void assertAccess(int expStatus, String username, String password) throws MalformedURLException {
		// POST /query
		givenJsonAndLogIfFailsAndAuthPreemptive(username, password)
				.body("{}")
				.expect().statusCode(expStatus)
				.log().ifValidationFails()
				.when().post(new URL(context, QUERY_REST_API).toExternalForm());

		// DELETE /query/queryID
		givenJsonAndLogIfFailsAndAuthPreemptive(username, password)
				.expect().statusCode(expStatus)
				.log().ifValidationFails()
				.when().delete(new URL(context, QUERY_REST_API + "/queryID").toExternalForm());
	}

	@Test
	@InSequence(10)
	public void assertNoRegisteredQueries() throws MalformedURLException {
		given().contentType(ContentType.JSON)
				.auth().basic(DeploymentHelpers.DEFAULT_PROVIDER_NAME, DeploymentHelpers.DEFAULT_PROVIDER_PASSWORD)
				.expect().statusCode(200)
				.log().ifValidationFails()
				.body("total", is(0))
				.when().get(new URL(context, QUERY_REST_API).toExternalForm());
	}

	@Test
	@InSequence(20)
	public void assertCreateQuery() throws MalformedURLException {
		String data = "{\n" +
				"  \"id\": \"matchAll\",\n" +
				"  \"template\": {" +
				"      \"query\": {" +
				"         \"match_all\": {}" +
				"       }" +
				"   }\n" +
				"}";

		given().contentType(ContentType.JSON)
				.auth().basic(DeploymentHelpers.DEFAULT_PROVIDER_NAME, DeploymentHelpers.DEFAULT_PROVIDER_PASSWORD)
				.body(data)
				.expect().statusCode(200)
				.log().ifValidationFails()
				.body("id", is("matchAll"))
				.when().post(new URL(context, QUERY_REST_API).toExternalForm());
	}

	@Test
	@InSequence(21)
	public void assertRefreshES() throws MalformedURLException {
		DeploymentHelpers.refreshES();
	}

	@Test
	@InSequence(30)
	public void assertGetCreatedQuery() throws MalformedURLException {
		given().contentType(ContentType.JSON)
				.auth().basic(DeploymentHelpers.DEFAULT_PROVIDER_NAME, DeploymentHelpers.DEFAULT_PROVIDER_PASSWORD)
				.expect().statusCode(200)
				.log().ifValidationFails()
				.body("total", is(1))
				.when().get(new URL(context, QUERY_REST_API).toExternalForm());

		given().contentType(ContentType.JSON)
				.auth().basic(DeploymentHelpers.DEFAULT_PROVIDER_NAME, DeploymentHelpers.DEFAULT_PROVIDER_PASSWORD)
				.expect().statusCode(200)
				.log().ifValidationFails()
				.body("id", is("matchAll"))
				.when().get(new URL(context, QUERY_REST_API + "/matchAll").toExternalForm());
	}

	@Test
	@InSequence(40)
	public void assertQueryDelete() throws MalformedURLException {
		given().contentType(ContentType.JSON)
				.auth().basic(DeploymentHelpers.DEFAULT_PROVIDER_NAME, DeploymentHelpers.DEFAULT_PROVIDER_PASSWORD)
				.expect().statusCode(200)
				.log().ifValidationFails()
				.when().delete(new URL(context, QUERY_REST_API + "/matchAll").toExternalForm());
	}

	@Test
	@InSequence(41)
	public void assertRefreshESAfterDelete() throws MalformedURLException {
		DeploymentHelpers.refreshES();
	}

	@Test
	@InSequence(50)
	public void assertNoRegisteredQueriesAfterDelete() throws MalformedURLException {
		given().contentType(ContentType.JSON)
				.auth().basic(DeploymentHelpers.DEFAULT_PROVIDER_NAME, DeploymentHelpers.DEFAULT_PROVIDER_PASSWORD)
				.expect().statusCode(200)
				.log().ifValidationFails()
				.body("total", is(0))
				.when().get(new URL(context, QUERY_REST_API).toExternalForm());
	}

	@Test
	@InSequence(60)
	public void assertNonMatchingKeyShouldFail() throws MalformedURLException {
		String data = "{\n" +
				"  \"id\": \"key_1\",\n" +
				"  \"template\": {" +
				"      \"query\": {" +
				"         \"match_all\": {}" +
				"       }" +
				"   }\n" +
				"}";

		given().contentType(ContentType.JSON)
				.auth().basic(DeploymentHelpers.DEFAULT_PROVIDER_NAME, DeploymentHelpers.DEFAULT_PROVIDER_PASSWORD)
				.body(data)
				.expect().statusCode(400)
				.log().ifValidationFails()
				.when().post(new URL(context, QUERY_REST_API + "/key_2").toExternalForm());
	}

	@Test
	@InSequence(70)
	public void assertCreateEmptyTemplateShouldFail() throws MalformedURLException {
		String data = "{\n" +
				"  \"id\": \"emptyTemplate\",\n" +
				"  \"template\": {}\n" +
				"}";

		given().contentType(ContentType.JSON)
				.auth().basic(DeploymentHelpers.DEFAULT_PROVIDER_NAME, DeploymentHelpers.DEFAULT_PROVIDER_PASSWORD)
				.body(data)
				.expect().statusCode(400)
				.log().ifValidationFails()
				.when().post(new URL(context, QUERY_REST_API + "/emptyTemplate").toExternalForm());
	}

	/**
	 * Note: As we can see it is possible to push query template that does not contain valid
	 * Elasticsearch query.
	 *
	 * @throws MalformedURLException
	 */
	@Test
	@InSequence(80)
	public void assertCreateInvalidQueryTemplatePass() throws MalformedURLException {
		String data = "{\n" +
				"  \"id\": \"invalidTemplate\",\n" +
				"  \"template\": { \"bumpy\": \"dumpy\" }\n" +
				"}";

		given().contentType(ContentType.JSON)
				.auth().basic(DeploymentHelpers.DEFAULT_PROVIDER_NAME, DeploymentHelpers.DEFAULT_PROVIDER_PASSWORD)
				.body(data)
				.expect().statusCode(200)
				.log().ifValidationFails()
				.body("id", is("invalidTemplate"))
				.when().post(new URL(context, QUERY_REST_API + "/invalidTemplate").toExternalForm());
	}

	@Test
	@InSequence(90)
	public void assertCreateTemplateWithInvalidRolesShouldFail() throws MalformedURLException {
		String data = "{\n" +
				"  \"id\": \"someTemplate\",\n" +
				"  \"roles\": \"MUST BE AN ARRAY!\",\n" +
				"  \"template\": {" +
				"      \"query\": {" +
				"         \"match_all\": {}" +
				"       }" +
				"   }\n" +
				"}";

		given().contentType(ContentType.JSON)
				.auth().basic(DeploymentHelpers.DEFAULT_PROVIDER_NAME, DeploymentHelpers.DEFAULT_PROVIDER_PASSWORD)
				.body(data)
				.expect().statusCode(400)
				.log().ifValidationFails()
				.when().post(new URL(context, QUERY_REST_API + "/someTemplate").toExternalForm());
	}

	@Test
	@InSequence(100)
	public void assertCreateTemplateWithRoles() throws MalformedURLException {
		String data = "{\n" +
				"  \"id\": \"someTemplate\",\n" +
				"  \"roles\": [ \"provider\" ],\n" +
				"  \"template\": {" +
				"      \"query\": {" +
				"         \"match_all\": {}" +
				"       }" +
				"   }\n" +
				"}";

		given().contentType(ContentType.JSON)
				.auth().basic(DeploymentHelpers.DEFAULT_PROVIDER_NAME, DeploymentHelpers.DEFAULT_PROVIDER_PASSWORD)
				.body(data)
				.expect().statusCode(200)
				.log().ifValidationFails()
				.when().post(new URL(context, QUERY_REST_API + "/someTemplate").toExternalForm());
	}
}
