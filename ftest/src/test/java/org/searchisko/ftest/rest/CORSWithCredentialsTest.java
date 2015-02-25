/*
 * JBoss, Home of Professional Open Source
 * Copyright 2014 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 */

package org.searchisko.ftest.rest;

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
import org.searchisko.ftest.DeploymentHelpers;
import org.searchisko.ftest.ProviderModel;
import org.searchisko.ftest.filter.ESProxyFilterTest;

import com.jayway.restassured.builder.ResponseSpecBuilder;
import com.jayway.restassured.http.ContentType;
import com.jayway.restassured.response.Response;
import com.jayway.restassured.specification.ResponseSpecification;

import static com.jayway.restassured.RestAssured.given;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.isEmptyOrNullString;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

/**
 * Functional test for {@link org.searchisko.api.filter.CORSWithCredentialsFilter}
 * 
 * @author Libor Krzyzanek
 */
@RunWith(Arquillian.class)
public class CORSWithCredentialsTest {

	@Deployment(testable = false)
	public static WebArchive createDeployment() throws IOException {
		return DeploymentHelpers.createDeployment();
	}

	@ArquillianResource
	URL context;

	private static final String ORIGIN_VALUE = "http://mydomain.org";

	/**
	 * CORS headers
	 */
	private static final String HEADER_ORIGIN = "Origin";
	private static final String HEADER_USER_AGENT = "User-Agent";

	private static final String ACCESS_CONTROL_ALLOW_ORIGIN = "Access-Control-Allow-Origin";
	private static final String ACCESS_CONTROL_ALLOW_CREDENTIALS = "Access-Control-Allow-Credentials";

	private static final String ACCESS_CONTROL_ALLOW_HEADERS = "Access-Control-Allow-Headers";
	private static final String ACCESS_CONTROL_ALLOW_METHODS = "Access-Control-Allow-Methods";
	private static final String ACCESS_CONTROL_MAX_AGE = "Access-Control-Max-Age";

	public static final String TYPE1 = "provider1_blog";
	static ProviderModel provider1 = new ProviderModel("provider1", "password");

	@Test
	@InSequence(0)
	public void assertCreateContentType() throws MalformedURLException {
		// we have to create at least one type not to get error from search service
		String idx1 = provider1.addContentType(TYPE1, "blogpost", true);

		ProviderRestServiceTest.createNewProvider(context, provider1);
		ESProxyFilterTest.createSearchESIndex(context, idx1, "{}");
		ESProxyFilterTest.createSearchESIndexMapping(context, idx1, TYPE1, "{\"" + TYPE1 + "\":{}}");
	}

	@Test
	@InSequence(1)
	public void assertNoCORSNoOptions() throws MalformedURLException {
		ResponseSpecBuilder response = new ResponseSpecBuilder();
		response.expectStatusCode(200);
		response.expectHeader(ACCESS_CONTROL_ALLOW_ORIGIN, isEmptyOrNullString());
		response.expectHeader(ACCESS_CONTROL_ALLOW_CREDENTIALS, isEmptyOrNullString());
		response.expectContentType(ContentType.JSON);
		ResponseSpecification responseSpec = response.build();

		given().contentType(ContentType.JSON).expect().spec(responseSpec).log().ifError().when()
				.get(new URL(context, SearchRestServiceTest.SEARCH_REST_API).toExternalForm());
	}

	@Test
	@InSequence(2)
	public void assertOptions() throws MalformedURLException {
		given().contentType(ContentType.JSON).header(HEADER_ORIGIN, ORIGIN_VALUE).expect().log().ifError().statusCode(200)
				.header(ACCESS_CONTROL_ALLOW_ORIGIN, is(ORIGIN_VALUE)).header(ACCESS_CONTROL_ALLOW_CREDENTIALS, is("true"))
				.header(ACCESS_CONTROL_ALLOW_HEADERS, notNullValue()).header(ACCESS_CONTROL_ALLOW_METHODS, notNullValue())
				.header(ACCESS_CONTROL_MAX_AGE, notNullValue()).when()
				.options(new URL(context, SearchRestServiceTest.SEARCH_REST_API).toExternalForm());
	}

	@Test
	@InSequence(10)
	public void assertCORS() throws MalformedURLException {
		Response response = given().contentType(ContentType.JSON).header(HEADER_ORIGIN, ORIGIN_VALUE).expect().log()
				.ifError().statusCode(200).header(ACCESS_CONTROL_ALLOW_CREDENTIALS, is("true"))
				.header(ACCESS_CONTROL_ALLOW_HEADERS, nullValue()).header(ACCESS_CONTROL_ALLOW_METHODS, nullValue())
				.header(ACCESS_CONTROL_MAX_AGE, nullValue()).when()
				.get(new URL(context, SearchRestServiceTest.SEARCH_REST_API).toExternalForm()).thenReturn();

		testCORSHeader(response);

	}

	protected void testCORSHeader(Response response) {
		final String[] validOrigin = new String[] { ORIGIN_VALUE };
		Assert.assertArrayEquals(validOrigin, response.getHeaders().getValues(ACCESS_CONTROL_ALLOW_ORIGIN).toArray());
	}

	@Test
	@InSequence(11)
	public void assertCORSOnESProxy() throws MalformedURLException {
		Response response = given().contentType(ContentType.JSON).header(HEADER_ORIGIN, ORIGIN_VALUE)
				.header(HEADER_USER_AGENT, "Mozilla").auth()
				.basic(DeploymentHelpers.DEFAULT_PROVIDER_NAME, DeploymentHelpers.DEFAULT_PROVIDER_PASSWORD).expect().log()
				.ifError().statusCode(200).header(ACCESS_CONTROL_ALLOW_CREDENTIALS, is("true"))
				.header(ACCESS_CONTROL_ALLOW_HEADERS, nullValue()).header(ACCESS_CONTROL_ALLOW_METHODS, nullValue())
				.header(ACCESS_CONTROL_MAX_AGE, nullValue()).when()
				.get(new URL(context, DeploymentHelpers.CURRENT_REST_VERSION + "sys/es/search/").toExternalForm()).thenReturn();
		testCORSHeader(response);
	}

}
