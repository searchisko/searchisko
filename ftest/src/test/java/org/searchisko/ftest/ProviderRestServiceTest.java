/*
 * JBoss, Home of Professional Open Source
 * Copyright 2012 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 */
package org.searchisko.ftest;

import com.jayway.restassured.http.ContentType;
import org.apache.commons.codec.digest.DigestUtils;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.junit.InSequence;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.searchisko.api.service.ProviderService;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import static com.jayway.restassured.RestAssured.given;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;

/**
 * Integration test for /provider REST API.
 *
 * @author Libor Krzyzanek
 * @see org.searchisko.api.rest.ProviderRestService
 */
@RunWith(Arquillian.class)
public class ProviderRestServiceTest {

	public static final String PROVIDER_REST_API = DeploymentHelpers.DEFAULT_REST_VERSION + "provider/{id}";

	@Deployment(testable = false)
	public static WebArchive createDeployment() throws IOException {
		return DeploymentHelpers.createDeployment();
	}

	@Test
	@InSequence(0)
	public void assertNotAuthenticated(@ArquillianResource URL context) throws MalformedURLException {
		int expStatus = 401;
		// TEST: GET /provider
		given().pathParam("id", "").contentType(ContentType.JSON)
				.expect().statusCode(expStatus).log().ifStatusCodeMatches(is(not(expStatus)))
				.when().get(new URL(context, PROVIDER_REST_API).toExternalForm());

		// TEST: GET /provider/jbossorg
		given().pathParam("id", DeploymentHelpers.DEFAULT_PROVIDER_NAME).contentType(ContentType.JSON)
				.expect().statusCode(expStatus).log().ifStatusCodeMatches(is(not(expStatus)))
				.when().get(new URL(context, PROVIDER_REST_API).toExternalForm());

		// TEST: POST /provider
		given().contentType(ContentType.JSON)
				.body("")
				.expect().statusCode(expStatus).log().ifStatusCodeMatches(is(not(expStatus)))
				.when().post(new URL(context, PROVIDER_REST_API).toExternalForm());

		// TEST: POST /provider/test
		given().pathParam("id", "test").contentType(ContentType.JSON)
				.body("")
				.expect().statusCode(expStatus).log().ifStatusCodeMatches(is(not(expStatus)))
				.when().post(new URL(context, PROVIDER_REST_API).toExternalForm());

		// TEST: DELETE /provider/test
		given().pathParam("id", DeploymentHelpers.DEFAULT_PROVIDER_NAME).contentType(ContentType.JSON)
				.body("")
				.expect().statusCode(expStatus).log().ifStatusCodeMatches(is(not(expStatus)))
				.when().delete(new URL(context, PROVIDER_REST_API).toExternalForm());
	}

	@Test
	@InSequence(10)
	public void assertGetAllProviders(@ArquillianResource URL context) throws MalformedURLException {
		given().pathParam("id", "").contentType(ContentType.JSON)
				.auth().basic(DeploymentHelpers.DEFAULT_PROVIDER_NAME, DeploymentHelpers.DEFAULT_PROVIDER_PASSWORD)
				.expect()
				.log().ifError()
				.statusCode(200)
				.body("hits[0].id", is("jbossorg"))
				.body("hits[0].data.name", is("jbossorg"))
				.body("hits[0].data.super_provider", is(true))
				.body("hits[0].data." + ProviderService.PASSWORD_HASH, nullValue())
				.when().get(new URL(context, PROVIDER_REST_API).toExternalForm());
	}

	@Test
	@InSequence(11)
	public void assertGetDefaultProvider(@ArquillianResource URL context) throws MalformedURLException {
		// TEST: Correct ID
		given().pathParam("id", DeploymentHelpers.DEFAULT_PROVIDER_NAME).contentType(ContentType.JSON)
				.auth().basic(DeploymentHelpers.DEFAULT_PROVIDER_NAME, DeploymentHelpers.DEFAULT_PROVIDER_PASSWORD)
				.expect()
				.log().ifError()
				.statusCode(200)
				.body("name", is("jbossorg"))
				.body("super_provider", is(true))
				.body(ProviderService.PASSWORD_HASH, nullValue())
				.when().get(new URL(context, PROVIDER_REST_API).toExternalForm());

		// TEST: Invalid ID
		given().pathParam("id", "invalid-id").contentType(ContentType.JSON).auth().basic(DeploymentHelpers.DEFAULT_PROVIDER_NAME, DeploymentHelpers.DEFAULT_PROVIDER_PASSWORD)
				.expect()
				.statusCode(404)
				.when().get(new URL(context, PROVIDER_REST_API).toExternalForm());
	}


	final String name = "provider-test";
	final String pwd = "pwd1";

	@Test
	@InSequence(20)
	public void assertValidateNewProvider(@ArquillianResource URL context) throws MalformedURLException {
		// TEST: Missing params in body
		given().pathParam("id", "").contentType(ContentType.JSON)
				.body("")
				.auth().basic(DeploymentHelpers.DEFAULT_PROVIDER_NAME, DeploymentHelpers.DEFAULT_PROVIDER_PASSWORD)
				.expect()
				.statusCode(400)
				.when().post(new URL(context, PROVIDER_REST_API).toExternalForm());

	}

	@Test
	@InSequence(21)
	public void assertNewProvider(@ArquillianResource URL context) throws MalformedURLException {
		final Map<String, Object> params = new HashMap<>();
		params.put(ProviderService.NAME, name);
		// See SecurityService#createPwdHash
		params.put(ProviderService.PASSWORD_HASH, DigestUtils.shaHex(pwd + name));

		// TEST: Ensure that provider doesn't exist
		given().pathParam("id", name).contentType(ContentType.JSON)
				.auth().basic(DeploymentHelpers.DEFAULT_PROVIDER_NAME, DeploymentHelpers.DEFAULT_PROVIDER_PASSWORD)
				.expect()
				.statusCode(404)
				.when().get(new URL(context, PROVIDER_REST_API).toExternalForm());


		// TEST: New Provider
		given().pathParam("id", name).contentType(ContentType.JSON)
				.auth().basic(DeploymentHelpers.DEFAULT_PROVIDER_NAME, DeploymentHelpers.DEFAULT_PROVIDER_PASSWORD)
				.body(params)
				.expect()
				.log().ifError()
				.statusCode(200)
				.body("id", is(name))
				.when().post(new URL(context, PROVIDER_REST_API).toExternalForm());

		// TEST: Get provider back
		given().pathParam("id", name).contentType(ContentType.JSON)
				.auth().basic(name, pwd)
				.expect()
				.log().ifError()
				.statusCode(200)
				.body("name", is(name))
				.body("super_provider", nullValue())
				.body(ProviderService.PASSWORD_HASH, nullValue())
				.when().get(new URL(context, PROVIDER_REST_API).toExternalForm());

		// TEST: Get different provider
		given().pathParam("id", DeploymentHelpers.DEFAULT_PROVIDER_NAME).contentType(ContentType.JSON)
				.auth().basic(name, pwd)
				.expect()
				.statusCode(403)
				.when().get(new URL(context, PROVIDER_REST_API).toExternalForm());
	}

	final String newPwd = "password-new1";

	@Test
	@InSequence(22)
	public void assertChangePassword(@ArquillianResource URL context) throws MalformedURLException {
		// TEST: New Provider
		given().pathParam("id", name).contentType(ContentType.JSON)
				.auth().basic(name, pwd)
				.body(newPwd)
				.expect()
				.log().ifError()
				.statusCode(200)
				.when().post(new URL(context, PROVIDER_REST_API + "/password").toExternalForm());

		// TEST: New Password
		given().pathParam("id", name).contentType(ContentType.JSON)
				.auth().basic(name, newPwd)
				.expect()
				.log().ifError()
				.statusCode(200)
				.body("name", is(name))
				.body("super_provider", nullValue())
				.body(ProviderService.PASSWORD_HASH, nullValue())
				.when().get(new URL(context, PROVIDER_REST_API).toExternalForm());

		// TEST: old Password
		given().pathParam("id", name).contentType(ContentType.JSON)
				.auth().basic(name, pwd)
				.expect()
				.expect().statusCode(403).log().ifStatusCodeMatches(is(not(403)))
				.when().get(new URL(context, PROVIDER_REST_API).toExternalForm());

	}

	@Test
	@InSequence(30)
	public void assertDelete(@ArquillianResource URL context) throws MalformedURLException {
		given().pathParam("id", name).contentType(ContentType.JSON)
				.auth().basic(DeploymentHelpers.DEFAULT_PROVIDER_NAME, DeploymentHelpers.DEFAULT_PROVIDER_PASSWORD)
				.expect()
				.log().ifError()
				.statusCode(200)
				.when().delete(new URL(context, PROVIDER_REST_API).toExternalForm());

		given().pathParam("id", name).contentType(ContentType.JSON)
				.auth().basic(DeploymentHelpers.DEFAULT_PROVIDER_NAME, DeploymentHelpers.DEFAULT_PROVIDER_PASSWORD)
				.expect().statusCode(404).log().ifStatusCodeMatches(is(not(404)))
				.when().get(new URL(context, PROVIDER_REST_API).toExternalForm());
	}
}
