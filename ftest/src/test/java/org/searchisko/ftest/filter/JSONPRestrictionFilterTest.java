/*
 * JBoss, Home of Professional Open Source
 * Copyright 2014 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 */
package org.searchisko.ftest.filter;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.junit.InSequence;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.searchisko.api.filter.JSONPRestrictionFilter;
import org.searchisko.ftest.DeploymentHelpers;
import org.searchisko.ftest.ProviderModel;
import org.searchisko.ftest.rest.ProviderRestServiceTest;

import static com.jayway.restassured.RestAssured.given;

/**
 * Functional test for JSONP handling, see {@link JSONPRestrictionFilter}.
 * 
 * @author Vlastimil Elias (velias at redhat dot com)
 */
@RunWith(Arquillian.class)
public class JSONPRestrictionFilterTest {

	public static final String AUTH_STATUS_REST_API = DeploymentHelpers.CURRENT_REST_VERSION + "auth/status";

	public static final String COMMON_REST_API = DeploymentHelpers.CURRENT_REST_VERSION + "search";

	@ArquillianResource
	protected URL context;

	@Deployment(testable = false)
	public static WebArchive createDeployment() throws IOException {
		return DeploymentHelpers.createDeployment();
	}

	public static final String TYPE1 = "provider1_blog";

	public static ProviderModel provider1 = new ProviderModel("provider1", "password");

	@Test
	@InSequence(0)
	public void setupEverythingForSearchAPI() throws MalformedURLException {
		String idx1 = provider1.addContentType(TYPE1, "blogpost", true);

		ProviderRestServiceTest.createNewProvider(context, provider1);
		ESProxyFilterTest.createSearchESIndex(context, idx1, "{}");
		ESProxyFilterTest.createSearchESIndexMapping(context, idx1, TYPE1, "{\"" + TYPE1 + "\":{}}");
	}

	@Test
	@InSequence(1)
	public void assertNoJSONPResponseForAuthStatus(@ArquillianResource URL context) throws MalformedURLException {
		given()
				.expect()
				.statusCode(403)
				.log()
				.ifValidationFails()
				.when()
				.get(
						new URL(context, AUTH_STATUS_REST_API + "?" + JSONPRestrictionFilter.PARAM_CALLBACK + "=func12")
								.toExternalForm());

	}

	@Test
	@InSequence(2)
	public void assertNoJSONPResponseForAuthenticatedUser(@ArquillianResource URL context) throws MalformedURLException {
		given()
				.auth()
				.preemptive()
				.basic(DeploymentHelpers.DEFAULT_PROVIDER_NAME, DeploymentHelpers.DEFAULT_PROVIDER_PASSWORD)
				.expect()
				.statusCode(403)
				.log()
				.ifValidationFails()
				.when()
				.get(
						new URL(context, COMMON_REST_API + "?" + JSONPRestrictionFilter.PARAM_CALLBACK + "=func12")
								.toExternalForm());

	}

	@Test
	@InSequence(3)
	public void assertJSONPResponseForAnonymousUser(@ArquillianResource URL context) throws MalformedURLException {
		String response = given()
				.expect()
				.statusCode(200)
				.log()
				.ifValidationFails()
				.when()
				.get(
						new URL(context, COMMON_REST_API + "?" + JSONPRestrictionFilter.PARAM_CALLBACK + "=func12")
								.toExternalForm()).asString();

		Assert.assertTrue(response.startsWith("func12({\"uuid\":"));
	}

	@Test
	@InSequence(4)
	public void assertCommonResponseForAnonymousUser(@ArquillianResource URL context) throws MalformedURLException {
		String response = given().expect().statusCode(200).log().ifValidationFails().when()
				.get(new URL(context, COMMON_REST_API).toExternalForm()).asString();
		Assert.assertTrue(response.startsWith("{\"uuid\":"));
	}

	@Test
	@InSequence(5)
	public void assertCommonResponseForAuthenticatedUser(@ArquillianResource URL context) throws MalformedURLException {
		String response = given().auth().preemptive()
				.basic(DeploymentHelpers.DEFAULT_PROVIDER_NAME, DeploymentHelpers.DEFAULT_PROVIDER_PASSWORD).expect()
				.statusCode(200).log().ifValidationFails().when().get(new URL(context, COMMON_REST_API).toExternalForm())
				.asString();
		Assert.assertTrue(response.startsWith("{\"uuid\":"));
	}

}
