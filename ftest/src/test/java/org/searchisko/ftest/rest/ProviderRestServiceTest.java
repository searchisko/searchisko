/*
 * JBoss, Home of Professional Open Source
 * Copyright 2012 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 */
package org.searchisko.ftest.rest;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.junit.InSequence;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.searchisko.api.security.Role;
import org.searchisko.api.service.ContentManipulationLockService;
import org.searchisko.api.service.ProviderService;
import org.searchisko.ftest.DeploymentHelpers;
import org.searchisko.ftest.ProviderModel;

import com.jayway.restassured.http.ContentType;

import static com.jayway.restassured.RestAssured.given;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.searchisko.ftest.rest.RestTestHelpers.givenJsonAndLogIfFailsAndAuthPreemptive;

/**
 * Integration test for /provider REST API.
 * 
 * @author Libor Krzyzanek
 * @see org.searchisko.api.rest.ProviderRestService
 */
@RunWith(Arquillian.class)
public class ProviderRestServiceTest {

	public static final Set<String> ALLOWED_ROLES = new HashSet<>();
	static {
		ALLOWED_ROLES.add(Role.ADMIN);
	}

	protected static Logger log = Logger.getLogger(ProviderRestServiceTest.class.getName());

	public static final String PROVIDER_REST_API_BASE = DeploymentHelpers.CURRENT_REST_VERSION + "provider/";

	public static final String PROVIDER_REST_API = PROVIDER_REST_API_BASE + "{id}";

	public static final String PROVIDER_CML_REST_API = PROVIDER_REST_API_BASE + "{id}/content_manipulation_lock";

	public static final ProviderModel provider1 = new ProviderModel("provider1", "Password1");

	public static final ProviderModel provider2 = new ProviderModel("provider2", "Password2");

	@ArquillianResource
	URL context;

	@Deployment(testable = false)
	public static WebArchive createDeployment() throws IOException {
		return DeploymentHelpers.createDeployment();
	}

	@Test
	@InSequence(0)
	public void assertNotAuthenticated() throws MalformedURLException {
		assertAccess(401, null, null, null);
	}

	@Test
	@InSequence(1)
	public void assertForbidden() throws MalformedURLException {
		for (String role : Role.ALL_ROLES) {
			if (!ALLOWED_ROLES.contains(role)) {
				assertAccess(403, role, role, role);
			}
		}
	}

	public void assertAccess(int expStatus, String username, String password, String role) throws MalformedURLException {
		// TEST: GET /provider
		givenJsonAndLogIfFailsAndAuthPreemptive(username, password).expect().statusCode(expStatus).when()
				.get(new URL(context, PROVIDER_REST_API_BASE).toExternalForm());

		// TEST: POST /provider
		givenJsonAndLogIfFailsAndAuthPreemptive(username, password).body("").expect().statusCode(expStatus).when()
				.post(new URL(context, PROVIDER_REST_API_BASE).toExternalForm());

		// TEST: POST /provider/test
		givenJsonAndLogIfFailsAndAuthPreemptive(username, password).pathParam("id", "test").body("").expect()
				.statusCode(expStatus).when().post(new URL(context, PROVIDER_REST_API).toExternalForm());

		// Overridden default role access
		// PROVIDER has access to this as well so skip it
		if (expStatus == 401 || (expStatus == 403 && !Role.PROVIDER.equals(role))) {
			// TEST: GET /provider/jbossorg
			givenJsonAndLogIfFailsAndAuthPreemptive(username, password)
					.pathParam("id", DeploymentHelpers.DEFAULT_PROVIDER_NAME).expect().statusCode(expStatus).when()
					.get(new URL(context, PROVIDER_REST_API).toExternalForm());

			// TEST: DELETE /provider/test
			givenJsonAndLogIfFailsAndAuthPreemptive(username, password)
					.pathParam("id", DeploymentHelpers.DEFAULT_PROVIDER_NAME).body("").expect().statusCode(expStatus).when()
					.delete(new URL(context, PROVIDER_REST_API).toExternalForm());

			// / Content manipulation lock part of API

			// TEST: GET /provider/jbossorg/content_manipulation_lock
			givenJsonAndLogIfFailsAndAuthPreemptive(username, password)
					.pathParam("id", DeploymentHelpers.DEFAULT_PROVIDER_NAME).expect().statusCode(expStatus).when()
					.get(new URL(context, PROVIDER_CML_REST_API).toExternalForm());

			// TEST: POST /provider/test/content_manipulation_lock
			givenJsonAndLogIfFailsAndAuthPreemptive(username, password).pathParam("id", "test").body("").expect()
					.statusCode(expStatus).when().post(new URL(context, PROVIDER_CML_REST_API).toExternalForm());

			// TEST: DELETE /provider/jbossorg/content_manipulation_lock
			givenJsonAndLogIfFailsAndAuthPreemptive(username, password)
					.pathParam("id", DeploymentHelpers.DEFAULT_PROVIDER_NAME).body("").expect().statusCode(expStatus).when()
					.delete(new URL(context, PROVIDER_CML_REST_API).toExternalForm());
		}
	}

	@Test
	@InSequence(10)
	public void assertGetAllProviders(@ArquillianResource URL context) throws MalformedURLException {
		given().contentType(ContentType.JSON).auth()
				.basic(DeploymentHelpers.DEFAULT_PROVIDER_NAME, DeploymentHelpers.DEFAULT_PROVIDER_PASSWORD).expect().log()
				.ifError().statusCode(200).body("hits[0].id", is("jbossorg")).body("hits[0].data.name", is("jbossorg"))
				.body("hits[0].data.super_provider", is(true))
				.body("hits[0].data." + ProviderService.PASSWORD_HASH, nullValue()).when()
				.get(new URL(context, PROVIDER_REST_API_BASE).toExternalForm());
	}

	@Test
	@InSequence(11)
	public void assertGetDefaultProvider(@ArquillianResource URL context) throws MalformedURLException {
		// TEST: Correct ID
		given().pathParam("id", DeploymentHelpers.DEFAULT_PROVIDER_NAME).contentType(ContentType.JSON).auth()
				.basic(DeploymentHelpers.DEFAULT_PROVIDER_NAME, DeploymentHelpers.DEFAULT_PROVIDER_PASSWORD).expect().log()
				.ifError().statusCode(200).body("name", is("jbossorg")).body("super_provider", is(true))
				.body(ProviderService.PASSWORD_HASH, nullValue()).when()
				.get(new URL(context, PROVIDER_REST_API).toExternalForm());

		// TEST: Invalid ID
		given().pathParam("id", "invalid-id").contentType(ContentType.JSON).auth()
				.basic(DeploymentHelpers.DEFAULT_PROVIDER_NAME, DeploymentHelpers.DEFAULT_PROVIDER_PASSWORD).expect()
				.statusCode(404).when().get(new URL(context, PROVIDER_REST_API).toExternalForm());
	}

	@Test
	@InSequence(20)
	public void assertValidateNewProvider(@ArquillianResource URL context) throws MalformedURLException {
		// TEST: Missing params in body
		given().contentType(ContentType.JSON).body("").auth()
				.basic(DeploymentHelpers.DEFAULT_PROVIDER_NAME, DeploymentHelpers.DEFAULT_PROVIDER_PASSWORD).expect()
				.statusCode(400).when().post(new URL(context, PROVIDER_REST_API_BASE).toExternalForm());
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
		given().pathParam("id", provider2.name).contentType(ContentType.JSON).auth()
				.basic(provider1.name, provider1.password).expect().log().ifStatusCodeMatches(is(not(403))).statusCode(403)
				.when().get(new URL(context, PROVIDER_REST_API).toExternalForm());

		// TEST: Get provider1 via Super Provider
		given().pathParam("id", provider1.name).contentType(ContentType.JSON).auth()
				.basic(DeploymentHelpers.DEFAULT_PROVIDER_NAME, DeploymentHelpers.DEFAULT_PROVIDER_PASSWORD).expect().log()
				.ifError().statusCode(200).body("name", is(provider1.name)).body("super_provider", nullValue())
				.body(ProviderService.PASSWORD_HASH, nullValue()).when()
				.get(new URL(context, PROVIDER_REST_API).toExternalForm());

		// TEST: Create a new Provider via standard (not super) provider
		given().pathParam("id", provider2.name).contentType(ContentType.JSON).auth()
				.basic(provider1.name, provider1.password).body(provider2.getProviderJSONModel()).expect().log()
				.ifStatusCodeMatches(is(not(401))).statusCode(403).when()
				.post(new URL(context, PROVIDER_REST_API).toExternalForm());
	}

	@Test
	@InSequence(30)
	public void assertChangePassword(@ArquillianResource URL context) throws MalformedURLException {
		final String newPwd = "password-new1";

		// TEST: New Password
		given().pathParam("id", provider1.name).contentType(ContentType.JSON).auth()
				.basic(provider1.name, provider1.password).body(newPwd).expect().log().ifError().statusCode(200).when()
				.post(new URL(context, PROVIDER_REST_API + "/password").toExternalForm());

		// TEST: Get Provider Back via new password
		given().pathParam("id", provider1.name).contentType(ContentType.JSON).auth().basic(provider1.name, newPwd).expect()
				.log().ifError().statusCode(200).body("name", is(provider1.name)).body("super_provider", nullValue())
				.body(ProviderService.PASSWORD_HASH, nullValue()).when()
				.get(new URL(context, PROVIDER_REST_API).toExternalForm());
	}

	@Test
	@InSequence(31)
	public void assertChangePasswordViaAdmin(@ArquillianResource URL context) throws MalformedURLException {
		// TEST: Change password via Super Provider
		given().pathParam("id", provider1.name).contentType(ContentType.JSON).auth()
				.basic(DeploymentHelpers.DEFAULT_PROVIDER_NAME, DeploymentHelpers.DEFAULT_PROVIDER_PASSWORD)
				.body(provider1.password).expect().log().ifError().statusCode(200).when()
				.post(new URL(context, PROVIDER_REST_API + "/password").toExternalForm());

		// TEST: Get Provider Back via new password
		given().pathParam("id", provider1.name).contentType(ContentType.JSON).auth()
				.basic(provider1.name, provider1.password).expect().log().ifError().statusCode(200)
				.body("name", is(provider1.name)).body("super_provider", nullValue())
				.body(ProviderService.PASSWORD_HASH, nullValue()).when()
				.get(new URL(context, PROVIDER_REST_API).toExternalForm());
	}

	@Test
	@InSequence(32)
	public void assertChangePasswordSecurity(@ArquillianResource URL context) throws MalformedURLException {
		// Providers cannot Change password each other
		given().pathParam("id", provider2.name).contentType(ContentType.JSON).auth()
				.basic(provider1.name, provider1.password).body(provider1.password).expect().log()
				.ifStatusCodeMatches(is(not(403))).statusCode(403).when()
				.post(new URL(context, PROVIDER_REST_API + "/password").toExternalForm());
	}

	@Test
	@InSequence(40)
	public void assertRefreshES() throws MalformedURLException {
		DeploymentHelpers.refreshES();
	}

	@Test
	@InSequence(41)
	public void assertGetAllProvidersWithNewlyCreated(@ArquillianResource URL context) throws MalformedURLException {
		given().contentType(ContentType.JSON).auth()
				.basic(DeploymentHelpers.DEFAULT_PROVIDER_NAME, DeploymentHelpers.DEFAULT_PROVIDER_PASSWORD).expect().log()
				.ifError().statusCode(200).body("total", is(3)).when()
				.get(new URL(context, PROVIDER_REST_API_BASE).toExternalForm());
	}

	@Test
	@InSequence(50)
	public void assertDeleteProviderSecurity(@ArquillianResource URL context) throws MalformedURLException {
		// Provider1 cannot delete itself
		given().pathParam("id", provider1.name).contentType(ContentType.JSON).auth()
				.basic(provider1.name, provider1.password).expect().log().ifValidationFails().statusCode(403).when()
				.delete(new URL(context, PROVIDER_REST_API).toExternalForm());

		// Provider1 cannot delete Provider2
		given().pathParam("id", provider2.name).contentType(ContentType.JSON).auth()
				.basic(provider1.name, provider1.password).expect().log().ifValidationFails().statusCode(403).when()
				.delete(new URL(context, PROVIDER_REST_API).toExternalForm());

	}

	// ///////////////////////////// issue #109 - Content manipulation lock handling

	@Test
	@InSequence(58)
	public void assertCMLGetByAdminNoAnyLockDefined(@ArquillianResource URL context) throws MalformedURLException {

		// get for unknown provider
		given().pathParam("id", "unknown").contentType(ContentType.JSON).auth()
				.basic(DeploymentHelpers.DEFAULT_PROVIDER_NAME, DeploymentHelpers.DEFAULT_PROVIDER_PASSWORD).expect()
				.statusCode(404).log().ifValidationFails().when().get(new URL(context, PROVIDER_CML_REST_API).toExternalForm());

		// get for known provider
		given().pathParam("id", provider1.name).contentType(ContentType.JSON).auth()
				.basic(DeploymentHelpers.DEFAULT_PROVIDER_NAME, DeploymentHelpers.DEFAULT_PROVIDER_PASSWORD).expect()
				.statusCode(200).log().ifValidationFails().body("content_manipulation_lock", nullValue()).when()
				.get(new URL(context, PROVIDER_CML_REST_API).toExternalForm());

		// get all
		given().pathParam("id", ContentManipulationLockService.API_ID_ALL).contentType(ContentType.JSON).auth()
				.basic(DeploymentHelpers.DEFAULT_PROVIDER_NAME, DeploymentHelpers.DEFAULT_PROVIDER_PASSWORD).expect()
				.statusCode(200).log().ifValidationFails().body("content_manipulation_lock", nullValue()).when()
				.get(new URL(context, PROVIDER_CML_REST_API).toExternalForm());
	}

	@Test
	@InSequence(59)
	public void assertCMLGetByProviderNoAnyLockDefined(@ArquillianResource URL context) throws MalformedURLException {

		// get for me
		given().pathParam("id", provider1.name).contentType(ContentType.JSON).auth()
				.basic(provider1.name, provider1.password).expect().statusCode(200).log().ifValidationFails()
				.body("content_manipulation_lock", nullValue()).when()
				.get(new URL(context, PROVIDER_CML_REST_API).toExternalForm());

		// get from other provider is denied
		given().pathParam("id", provider2.name).contentType(ContentType.JSON).auth()
				.basic(provider1.name, provider1.password).expect().statusCode(403).log().ifValidationFails()
				.get(new URL(context, PROVIDER_CML_REST_API).toExternalForm());

		// get all is denied
		given().pathParam("id", ContentManipulationLockService.API_ID_ALL).contentType(ContentType.JSON).auth()
				.basic(provider1.name, provider1.password).expect().statusCode(403).log().ifValidationFails()
				.get(new URL(context, PROVIDER_CML_REST_API).toExternalForm());
	}

	@Test
	@InSequence(60)
	public void assertCMLCreateForProviderByAdmin(@ArquillianResource URL context) throws MalformedURLException {
		cmLockCreate(context, provider1.name, DeploymentHelpers.DEFAULT_PROVIDER_NAME,
				DeploymentHelpers.DEFAULT_PROVIDER_PASSWORD);
	}

	@Test
	@InSequence(61)
	public void assertCMLCreateForProviderByProvider(@ArquillianResource URL context) throws MalformedURLException {
		cmLockCreate(context, provider2.name, provider2.name, provider2.password);

		// not possible to create lock for another provider
		given().pathParam("id", provider1.name).contentType(ContentType.JSON).body("").auth()
				.basic(provider2.name, provider2.password).expect().statusCode(403).log().ifValidationFails().when()
				.post(new URL(context, PROVIDER_CML_REST_API).toExternalForm());
	}

	@Test
	@InSequence(62)
	public void assertCMLGetByProviderSomeLocksDefined(@ArquillianResource URL context) throws MalformedURLException {

		given().pathParam("id", provider1.name).contentType(ContentType.JSON).auth()
				.basic(provider1.name, provider1.password).expect().statusCode(200).log().ifValidationFails()
				.body("content_manipulation_lock[0]", is(provider1.name)).when()
				.get(new URL(context, PROVIDER_CML_REST_API).toExternalForm());

		given().pathParam("id", provider2.name).contentType(ContentType.JSON).auth()
				.basic(provider2.name, provider2.password).expect().statusCode(200).log().ifValidationFails()
				.body("content_manipulation_lock[0]", is(provider2.name)).when()
				.get(new URL(context, PROVIDER_CML_REST_API).toExternalForm());
	}

	@Test
	@InSequence(62)
	public void assertCMLGetByAdminSomeLocksDefined(@ArquillianResource URL context) throws MalformedURLException {

		// get for named provider
		given().pathParam("id", provider1.name).contentType(ContentType.JSON).auth()
				.basic(DeploymentHelpers.DEFAULT_PROVIDER_NAME, DeploymentHelpers.DEFAULT_PROVIDER_PASSWORD).expect()
				.statusCode(200).log().ifValidationFails().body("content_manipulation_lock[0]", is(provider1.name)).when()
				.get(new URL(context, PROVIDER_CML_REST_API).toExternalForm());

		given().pathParam("id", provider2.name).contentType(ContentType.JSON).auth()
				.basic(DeploymentHelpers.DEFAULT_PROVIDER_NAME, DeploymentHelpers.DEFAULT_PROVIDER_PASSWORD).expect()
				.statusCode(200).log().ifValidationFails().body("content_manipulation_lock[0]", is(provider2.name)).when()
				.get(new URL(context, PROVIDER_CML_REST_API).toExternalForm());

		// get all
		given().pathParam("id", ContentManipulationLockService.API_ID_ALL).contentType(ContentType.JSON).auth()
				.basic(DeploymentHelpers.DEFAULT_PROVIDER_NAME, DeploymentHelpers.DEFAULT_PROVIDER_PASSWORD).expect()
				.statusCode(200).log().ifValidationFails().body("content_manipulation_lock[0]", is(provider1.name))
				.body("content_manipulation_lock[1]", is(provider2.name)).when()
				.get(new URL(context, PROVIDER_CML_REST_API).toExternalForm());
	}

	@Test
	@InSequence(64)
	public void assertCMLDeleteForProviderByAdmin(@ArquillianResource URL context) throws MalformedURLException {

		// delete for one provider
		cmLockDelete(context, provider1.name, DeploymentHelpers.DEFAULT_PROVIDER_NAME,
				DeploymentHelpers.DEFAULT_PROVIDER_PASSWORD);

		// check it is deleted
		given().pathParam("id", ContentManipulationLockService.API_ID_ALL).contentType(ContentType.JSON).auth()
				.basic(DeploymentHelpers.DEFAULT_PROVIDER_NAME, DeploymentHelpers.DEFAULT_PROVIDER_PASSWORD).expect()
				.statusCode(200).log().ifValidationFails().body("content_manipulation_lock[0]", is(provider2.name)).when()
				.get(new URL(context, PROVIDER_CML_REST_API).toExternalForm());

		// delete for all must remove individual locks
		cmLockDelete(context, ContentManipulationLockService.API_ID_ALL, DeploymentHelpers.DEFAULT_PROVIDER_NAME,
				DeploymentHelpers.DEFAULT_PROVIDER_PASSWORD);

		// check it is deleted
		given().pathParam("id", ContentManipulationLockService.API_ID_ALL).contentType(ContentType.JSON).auth()
				.basic(DeploymentHelpers.DEFAULT_PROVIDER_NAME, DeploymentHelpers.DEFAULT_PROVIDER_PASSWORD).expect()
				.statusCode(200).log().ifValidationFails().body("content_manipulation_lock", nullValue()).when()
				.get(new URL(context, PROVIDER_CML_REST_API).toExternalForm());
	}

	@Test
	@InSequence(65)
	public void assertCMLDeleteForProviderByProvider(@ArquillianResource URL context) throws MalformedURLException {

		// create test lock
		cmLockCreate(context, provider2.name, provider2.name, provider2.password);

		// not possible to delete for another provider
		given().pathParam("id", provider2.name).contentType(ContentType.JSON).auth()
				.basic(provider1.name, provider1.password).expect().statusCode(403).log().ifValidationFails().when()
				.delete(new URL(context, PROVIDER_CML_REST_API).toExternalForm());

		// delete for me
		cmLockDelete(context, provider2.name, provider2.name, provider2.password);

		// check lock is deleted
		given().pathParam("id", ContentManipulationLockService.API_ID_ALL).contentType(ContentType.JSON).auth()
				.basic(DeploymentHelpers.DEFAULT_PROVIDER_NAME, DeploymentHelpers.DEFAULT_PROVIDER_PASSWORD).expect()
				.statusCode(200).log().ifValidationFails().body("content_manipulation_lock", nullValue()).when()
				.get(new URL(context, PROVIDER_CML_REST_API).toExternalForm());

	}

	@Test
	@InSequence(66)
	public void assertCMLCreateForAllByProviderNoPerm(@ArquillianResource URL context) throws MalformedURLException {
		given().pathParam("id", ContentManipulationLockService.API_ID_ALL).contentType(ContentType.JSON).body("").auth()
				.basic(provider2.name, provider2.password).expect().statusCode(403).log().ifValidationFails().when()
				.post(new URL(context, PROVIDER_CML_REST_API).toExternalForm());
	}

	@Test
	@InSequence(67)
	public void assertCMLCreateForAllByAdmin(@ArquillianResource URL context) throws MalformedURLException {
		// test that _all lock replaces individual locks if any
		cmLockCreate(context, provider1.name, DeploymentHelpers.DEFAULT_PROVIDER_NAME,
				DeploymentHelpers.DEFAULT_PROVIDER_PASSWORD);

		cmLockCreate(context, ContentManipulationLockService.API_ID_ALL, DeploymentHelpers.DEFAULT_PROVIDER_NAME,
				DeploymentHelpers.DEFAULT_PROVIDER_PASSWORD);

		// check it is created
		given().pathParam("id", ContentManipulationLockService.API_ID_ALL).contentType(ContentType.JSON).auth()
				.basic(DeploymentHelpers.DEFAULT_PROVIDER_NAME, DeploymentHelpers.DEFAULT_PROVIDER_PASSWORD).expect()
				.statusCode(200).log().ifValidationFails()
				.body("content_manipulation_lock[0]", is(ContentManipulationLockService.API_ID_ALL)).when()
				.get(new URL(context, PROVIDER_CML_REST_API).toExternalForm());

		// check how GEt works for _all value if only one provider is requested
		given().pathParam("id", provider1.name).contentType(ContentType.JSON).auth()
				.basic(DeploymentHelpers.DEFAULT_PROVIDER_NAME, DeploymentHelpers.DEFAULT_PROVIDER_PASSWORD).expect()
				.statusCode(200).log().ifValidationFails()
				.body("content_manipulation_lock[0]", is(ContentManipulationLockService.API_ID_ALL)).when()
				.get(new URL(context, PROVIDER_CML_REST_API).toExternalForm());
		given().pathParam("id", provider1.name).contentType(ContentType.JSON).auth()
				.basic(provider1.name, provider1.password).expect().statusCode(200).log().ifValidationFails()
				.body("content_manipulation_lock[0]", is(ContentManipulationLockService.API_ID_ALL)).when()
				.get(new URL(context, PROVIDER_CML_REST_API).toExternalForm());

	}

	@Test
	@InSequence(68)
	public void assertCMLDeleteForProviderByProviderNotPermIfLockAllExists(@ArquillianResource URL context)
			throws MalformedURLException {
		given().pathParam("id", provider2.name).contentType(ContentType.JSON).auth()
				.basic(provider2.name, provider2.password).expect().statusCode(403).log().ifValidationFails().when()
				.delete(new URL(context, PROVIDER_CML_REST_API).toExternalForm());
	}

	@Test
	@InSequence(69)
	public void assertCMLDeleteForAllByAdmin(@ArquillianResource URL context) throws MalformedURLException {
		cmLockDelete(context, ContentManipulationLockService.API_ID_ALL, DeploymentHelpers.DEFAULT_PROVIDER_NAME,
				DeploymentHelpers.DEFAULT_PROVIDER_PASSWORD);

		// check it is deleted
		given().pathParam("id", ContentManipulationLockService.API_ID_ALL).contentType(ContentType.JSON).auth()
				.basic(DeploymentHelpers.DEFAULT_PROVIDER_NAME, DeploymentHelpers.DEFAULT_PROVIDER_PASSWORD).expect()
				.statusCode(200).log().ifValidationFails().body("content_manipulation_lock", nullValue()).when()
				.get(new URL(context, PROVIDER_CML_REST_API).toExternalForm());
	}

	/**
	 * Helper method to delete Content Manipulation API lock for given provider.
	 * 
	 * @param context to be used for call REST API
	 * @param providerNameForLock name of provider to unlock
	 * @param username to authenticate on REST API
	 * @param password to authenticate on REST API
	 * @throws MalformedURLException
	 */
	public static final void cmLockDelete(URL context, String providerNameForLock, String username, String password)
			throws MalformedURLException {
		given().pathParam("id", providerNameForLock).contentType(ContentType.JSON).auth().basic(username, password)
				.expect().statusCode(200).log().ifValidationFails().when()
				.delete(new URL(context, PROVIDER_CML_REST_API).toExternalForm());
	}

	/**
	 * Helper method to create Content Manipulation API lock for given provider.
	 * 
	 * @param context to be used for call REST API
	 * @param providerNameForLock name of provider to lock
	 * @param username to authenticate on REST API
	 * @param password to authenticate on REST API
	 * @throws MalformedURLException
	 */
	public static final void cmLockCreate(URL context, String providerNameForLock, String username, String password)
			throws MalformedURLException {
		given().pathParam("id", providerNameForLock).contentType(ContentType.JSON).auth().basic(username, password)
				.expect().statusCode(200).log().ifValidationFails().when()
				.post(new URL(context, PROVIDER_CML_REST_API).toExternalForm());
	}

	@Test
	@InSequence(100)
	public void assertDeleteProvider1(@ArquillianResource URL context) throws MalformedURLException {
		deleteProvider(context, provider1);
	}

	@Test
	@InSequence(101)
	public void assertDeleteProvider2(@ArquillianResource URL context) throws MalformedURLException {
		deleteProvider(context, provider2);
	}

	/**
	 * Helper method to create new provider - non existence is tested
	 * 
	 * @param context
	 * @param provider
	 * @throws MalformedURLException
	 */
	public static void createNewProvider(URL context, ProviderModel provider) throws MalformedURLException {
		log.log(Level.INFO, "Create new provider, data: {0}", provider);
		// TEST: Ensure that provider doesn't exist
		given().pathParam("id", provider.name).contentType(ContentType.JSON).auth()
				.basic(DeploymentHelpers.DEFAULT_PROVIDER_NAME, DeploymentHelpers.DEFAULT_PROVIDER_PASSWORD).expect().log()
				.ifStatusCodeMatches(is(not(404))).statusCode(404).when()
				.get(new URL(context, PROVIDER_REST_API).toExternalForm());

		// TEST: New Provider
		given().pathParam("id", provider.name).contentType(ContentType.JSON).auth()
				.basic(DeploymentHelpers.DEFAULT_PROVIDER_NAME, DeploymentHelpers.DEFAULT_PROVIDER_PASSWORD)
				.body(provider.getProviderJSONModel()).expect().log().ifError().statusCode(200).body("id", is(provider.name))
				.when().post(new URL(context, PROVIDER_REST_API).toExternalForm());

		// TEST: Get provider back
		given().pathParam("id", provider.name).contentType(ContentType.JSON).auth().basic(provider.name, provider.password)
				.expect().log().ifError().statusCode(200).body("name", is(provider.name)).body("super_provider", nullValue())
				.body(ProviderService.PASSWORD_HASH, nullValue()).when()
				.get(new URL(context, PROVIDER_REST_API).toExternalForm());

	}

	/**
	 * Helper method to create or update provider
	 * 
	 * @param context
	 * @param provider
	 * @throws MalformedURLException
	 */
	public static void createOrUpdateProvider(URL context, ProviderModel provider) throws MalformedURLException {
		log.log(Level.INFO, "Create or update provider, data: {0}", provider);

		// TEST: Post Provider
		given().pathParam("id", provider.name).contentType(ContentType.JSON).auth()
				.basic(DeploymentHelpers.DEFAULT_PROVIDER_NAME, DeploymentHelpers.DEFAULT_PROVIDER_PASSWORD)
				.body(provider.getProviderJSONModel()).expect().log().ifError().statusCode(200).body("id", is(provider.name))
				.when().post(new URL(context, PROVIDER_REST_API).toExternalForm());

		// TEST: Get provider back
		given().pathParam("id", provider.name).contentType(ContentType.JSON).auth().basic(provider.name, provider.password)
				.expect().log().ifError().statusCode(200).body("name", is(provider.name)).body("super_provider", nullValue())
				.body(ProviderService.PASSWORD_HASH, nullValue()).when()
				.get(new URL(context, PROVIDER_REST_API).toExternalForm());

	}

	/**
	 * Helper method to update provider
	 * 
	 * @param context
	 * @param provider
	 * @throws MalformedURLException
	 */
	public static void updateProvider(URL context, ProviderModel provider) throws MalformedURLException {
		log.log(Level.INFO, "Update provider, data: {0}", provider);

		given().pathParam("id", provider.name).contentType(ContentType.JSON).auth()
				.basic(DeploymentHelpers.DEFAULT_PROVIDER_NAME, DeploymentHelpers.DEFAULT_PROVIDER_PASSWORD)
				.body(provider.getProviderJSONModel()).expect().log().ifError().statusCode(200).body("id", is(provider.name))
				.when().post(new URL(context, PROVIDER_REST_API).toExternalForm());

	}

	/**
	 * Helper method to delete provider
	 * 
	 * @param context
	 * @param provider
	 * @throws MalformedURLException
	 */
	public static void deleteProvider(URL context, ProviderModel provider) throws MalformedURLException {
		given().pathParam("id", provider.name).contentType(ContentType.JSON).auth()
				.basic(DeploymentHelpers.DEFAULT_PROVIDER_NAME, DeploymentHelpers.DEFAULT_PROVIDER_PASSWORD).expect().log()
				.ifError().statusCode(200).when().delete(new URL(context, PROVIDER_REST_API).toExternalForm());

		given().pathParam("id", provider.name).contentType(ContentType.JSON).auth()
				.basic(DeploymentHelpers.DEFAULT_PROVIDER_NAME, DeploymentHelpers.DEFAULT_PROVIDER_PASSWORD).expect()
				.statusCode(404).log().ifStatusCodeMatches(is(not(404))).when()
				.get(new URL(context, PROVIDER_REST_API).toExternalForm());
	}

}
