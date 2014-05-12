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
import org.searchisko.api.service.ProviderService;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.jayway.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

/**
 * Integration test for /provider REST API.
 *
 * @author Libor Krzyzanek
 * @see org.searchisko.api.rest.ProviderRestService
 */
@RunWith(Arquillian.class)
public class ProviderRestServiceTest {

	protected static Logger log = Logger.getLogger(ProviderRestServiceTest.class.getName());

	public static final String PROVIDER_REST_API = DeploymentHelpers.DEFAULT_REST_VERSION + "provider/{id}";

	public static final ProviderModel provider1 = new ProviderModel("provider1", "Password1");

	public static final ProviderModel provider2 = new ProviderModel("provider2", "Password2");

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

	/**
	 * Helper method to create new provider
	 *
	 * @param context
	 * @param provider
	 * @throws MalformedURLException
	 */
	public static void createNewProvider(URL context, ProviderModel provider) throws MalformedURLException {
		log.log(Level.INFO, "Create new provider, data: {0}", provider);
		// TEST: Ensure that provider doesn't exist
		given().pathParam("id", provider.name).contentType(ContentType.JSON)
				.auth().basic(DeploymentHelpers.DEFAULT_PROVIDER_NAME, DeploymentHelpers.DEFAULT_PROVIDER_PASSWORD)
				.expect()
				.log().ifStatusCodeMatches(is(not(404)))
				.statusCode(404)
				.when().get(new URL(context, PROVIDER_REST_API).toExternalForm());


		// TEST: New Provider
		given().pathParam("id", provider.name).contentType(ContentType.JSON)
				.auth().basic(DeploymentHelpers.DEFAULT_PROVIDER_NAME, DeploymentHelpers.DEFAULT_PROVIDER_PASSWORD)
				.body(provider.getProviderJSONModel())
				.expect()
				.log().ifError()
				.statusCode(200)
				.body("id", is(provider.name))
				.when().post(new URL(context, PROVIDER_REST_API).toExternalForm());

		// TEST: Get provider back
		given().pathParam("id", provider.name).contentType(ContentType.JSON)
				.auth().basic(provider.name, provider.password)
				.expect()
				.log().ifError()
				.statusCode(200)
				.body("name", is(provider.name))
				.body("super_provider", nullValue())
				.body(ProviderService.PASSWORD_HASH, nullValue())
				.when().get(new URL(context, PROVIDER_REST_API).toExternalForm());

	}

	@Test
	@InSequence(21)
	public void assertNewProvider1(@ArquillianResource URL context) throws MalformedURLException {
		createNewProvider(context, provider1);
	}

	@Test
	@InSequence(22)
	public void assertNewProvider2(@ArquillianResource URL context) throws MalformedURLException {
		createNewProvider(context, provider2);
	}


	@Test
	@InSequence(23)
	public void assertProvidersRoles(@ArquillianResource URL context) throws MalformedURLException {
		// TEST: Get provider2 via provider1 authentication
		given().pathParam("id", provider2.name).contentType(ContentType.JSON)
				.auth().basic(provider1.name, provider1.password)
				.expect()
				.log().ifStatusCodeMatches(is(not(403)))
				.statusCode(403)
				.when().get(new URL(context, PROVIDER_REST_API).toExternalForm());


		// TEST: Get provider1 via Super Provider
		given().pathParam("id", provider1.name).contentType(ContentType.JSON)
				.auth().basic(DeploymentHelpers.DEFAULT_PROVIDER_NAME, DeploymentHelpers.DEFAULT_PROVIDER_PASSWORD)
				.expect()
				.log().ifError()
				.statusCode(200)
				.body("name", is(provider1.name))
				.body("super_provider", nullValue())
				.body(ProviderService.PASSWORD_HASH, nullValue())
				.when().get(new URL(context, PROVIDER_REST_API).toExternalForm());

		// TEST: Create a new Provider via standard (not super) provider
		given().pathParam("id", provider2.name).contentType(ContentType.JSON)
				.auth().basic(provider1.name, provider1.password)
				.body(provider2.getProviderJSONModel())
				.expect()
				.log().ifStatusCodeMatches(is(not(401)))
				.statusCode(401)
				.when().post(new URL(context, PROVIDER_REST_API).toExternalForm());
	}


	@Test
	@InSequence(30)
	public void assertChangePassword(@ArquillianResource URL context) throws MalformedURLException {
		final String newPwd = "password-new1";

		// TEST: New Password
		given().pathParam("id", provider1.name).contentType(ContentType.JSON)
				.auth().basic(provider1.name, provider1.password)
				.body(newPwd)
				.expect()
				.log().ifError()
				.statusCode(200)
				.when().post(new URL(context, PROVIDER_REST_API + "/password").toExternalForm());

		// TEST: Get Provider Back via new password
		given().pathParam("id", provider1.name).contentType(ContentType.JSON)
				.auth().basic(provider1.name, newPwd)
				.expect()
				.log().ifError()
				.statusCode(200)
				.body("name", is(provider1.name))
				.body("super_provider", nullValue())
				.body(ProviderService.PASSWORD_HASH, nullValue())
				.when().get(new URL(context, PROVIDER_REST_API).toExternalForm());
	}


	@Test
	@InSequence(31)
	public void assertChangePasswordViaAdmin(@ArquillianResource URL context) throws MalformedURLException {
		// TEST: Change password via Super Provider
		given().pathParam("id", provider1.name).contentType(ContentType.JSON)
				.auth().basic(DeploymentHelpers.DEFAULT_PROVIDER_NAME, DeploymentHelpers.DEFAULT_PROVIDER_PASSWORD)
				.body(provider1.password)
				.expect()
				.log().ifError()
				.statusCode(200)
				.when().post(new URL(context, PROVIDER_REST_API + "/password").toExternalForm());

		// TEST: Get Provider Back via new password
		given().pathParam("id", provider1.name).contentType(ContentType.JSON)
				.auth().basic(provider1.name, provider1.password)
				.expect()
				.log().ifError()
				.statusCode(200)
				.body("name", is(provider1.name))
				.body("super_provider", nullValue())
				.body(ProviderService.PASSWORD_HASH, nullValue())
				.when().get(new URL(context, PROVIDER_REST_API).toExternalForm());
	}

	@Test
	@InSequence(32)
	public void assertChangePasswordSecurity(@ArquillianResource URL context) throws MalformedURLException {
		// Providers cannot Change password each other
		given().pathParam("id", provider2.name).contentType(ContentType.JSON)
				.auth().basic(provider1.name, provider1.password)
				.body(provider1.password)
				.expect()
				.log().ifStatusCodeMatches(is(not(403)))
				.statusCode(403)
				.when().post(new URL(context, PROVIDER_REST_API + "/password").toExternalForm());
	}

	@Test
	@InSequence(40)
	public void assertRefreshES() throws MalformedURLException {
		DeploymentHelpers.refreshES();
	}

	@Test
	@InSequence(41)
	public void assertGetAllProvidersWithNewlyCreated(@ArquillianResource URL context) throws MalformedURLException {
		given().pathParam("id", "").contentType(ContentType.JSON)
				.auth().basic(DeploymentHelpers.DEFAULT_PROVIDER_NAME, DeploymentHelpers.DEFAULT_PROVIDER_PASSWORD)
				.expect()
				.log().ifError()
				.statusCode(200)
				.body("total", is(3))
				.when().get(new URL(context, PROVIDER_REST_API).toExternalForm());
	}


	/**
	 * Helper method to delete provider
	 *
	 * @param context
	 * @param provider
	 * @throws MalformedURLException
	 */
	public static void deleteProvider(URL context, ProviderModel provider) throws MalformedURLException {
		given().pathParam("id", provider.name).contentType(ContentType.JSON)
				.auth().basic(DeploymentHelpers.DEFAULT_PROVIDER_NAME, DeploymentHelpers.DEFAULT_PROVIDER_PASSWORD)
				.expect()
				.log().ifError()
				.statusCode(200)
				.when().delete(new URL(context, PROVIDER_REST_API).toExternalForm());

		given().pathParam("id", provider.name).contentType(ContentType.JSON)
				.auth().basic(DeploymentHelpers.DEFAULT_PROVIDER_NAME, DeploymentHelpers.DEFAULT_PROVIDER_PASSWORD)
				.expect().statusCode(404).log().ifStatusCodeMatches(is(not(404)))
				.when().get(new URL(context, PROVIDER_REST_API).toExternalForm());
	}

	@Test
	@InSequence(50)
	public void assertDeleteProviderSecurity(@ArquillianResource URL context) throws MalformedURLException {
		// Provider1 cannot delete itself
		given().pathParam("id", provider1.name).contentType(ContentType.JSON)
				.auth().basic(provider1.name, provider1.password)
				.expect()
				.log().ifStatusCodeMatches(is(not(401)))
				.statusCode(401)
				.when().delete(new URL(context, PROVIDER_REST_API).toExternalForm());

		// Provider1 cannot delete Provider2
		given().pathParam("id", provider2.name).contentType(ContentType.JSON)
				.auth().basic(provider1.name, provider1.password)
				.expect()
				.log().ifStatusCodeMatches(is(not(401)))
				.statusCode(401)
				.when().delete(new URL(context, PROVIDER_REST_API).toExternalForm());

	}

	@Test
	@InSequence(51)
	public void assertDeleteProvider1(@ArquillianResource URL context) throws MalformedURLException {
		deleteProvider(context, provider1);
	}

	@Test
	@InSequence(52)
	public void assertDeleteProvider2(@ArquillianResource URL context) throws MalformedURLException {
		deleteProvider(context, provider2);
	}

}
