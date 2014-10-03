/*
 * JBoss, Home of Professional Open Source
 * Copyright 2014 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 */
package org.searchisko.ftest.filter;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.hamcrest.Matchers;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.junit.InSequence;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.searchisko.api.filter.CORSWithCredentialsFilter;
import org.searchisko.api.service.ConfigService;
import org.searchisko.ftest.DeploymentHelpers;
import org.searchisko.ftest.ProviderModel;
import org.searchisko.ftest.rest.ConfigRestServiceTest;
import org.searchisko.ftest.rest.ProviderRestServiceTest;

import com.jayway.restassured.http.ContentType;
import com.jayway.restassured.response.Header;
import com.jayway.restassured.specification.RequestSpecification;

import static com.jayway.restassured.RestAssured.given;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

/**
 * Unit test for CORS handling implemented in {@link CORSWithCredentialsFilter}.
 * 
 * @author Vlastimil Elias (velias at redhat dot com)
 */
@RunWith(Arquillian.class)
public class CORSWithCredentialsFilterTest {

	public static final String ORIGIN_1 = "http://www.jboss.org";
	public static final String ORIGIN_2 = "null";

	public static final String PROVIDER_REST_API = ProviderRestServiceTest.PROVIDER_REST_API_BASE + "provider1";

	@ArquillianResource
	protected URL context;

	@Deployment(testable = false)
	public static WebArchive createDeployment() throws IOException {
		return DeploymentHelpers.createDeployment();
	}

	public static final String TYPE1 = "provider1_blog";

	public static ProviderModel provider = new ProviderModel("provider1", "password");
	static {
		provider.addContentType(TYPE1, "blogpost", true);
	}

	@Test
	@InSequence(1)
	public void assertNoCORSWithoutOrigin() throws MalformedURLException {
		// no origin header send so no CORS response
		assertCORSHeadersNotPresent(null);
	}

	@Test
	@InSequence(2)
	public void assertCORSWithAuthIfNoConfig() throws MalformedURLException {
		// no config is present so everything is allowed
		assertCORSHeadersPresent(ORIGIN_1, "true");
	}

	@Test
	@InSequence(9)
	public void setupCORSConfig() throws MalformedURLException {
		String data = "{ \"origins\":[ \"" + ORIGIN_1 + "\", \"" + ORIGIN_2 + "\"], \"origins_with_credentials\":[ \""
				+ ORIGIN_1 + "\"]}";
		ConfigRestServiceTest.uploadConfigFile(context, ConfigService.CFGNAME_SECURITY_RESTAPI_CORS, data);
	}

	@Test
	@InSequence(10)
	public void assertNoCORSBecauseNotInConfig() throws MalformedURLException {
		// origin is not configured as allowed
		assertCORSHeadersNotPresent("http://test.org");
	}

	@Test
	@InSequence(11)
	public void assertCORSWithoutAuth() throws MalformedURLException {

		// ORIGIN is not configured as trusted so credentials header is not returned
		assertCORSHeadersPresent(ORIGIN_2, null);

	}

	@Test
	@InSequence(12)
	public void assertCORSWithAuth() throws MalformedURLException {
		// ORIGIN is configured as trusted so auth is allowed
		assertCORSHeadersPresent(ORIGIN_1, "true");

	}

	private RequestSpecification prepareRequestSpecWithOriginHeader(String requestOrigin) {
		RequestSpecification rs = given().contentType(ContentType.JSON);
		if (requestOrigin != null) {
			rs.header(CORSWithCredentialsFilter.HEADER_ORIGIN, requestOrigin);
		}
		return rs;
	}

	private void assertCORSHeadersNotPresent(String requestOrigin) throws MalformedURLException {
		// test for all types of request
		prepareRequestSpecWithOriginHeader(requestOrigin).auth()
				.basic(DeploymentHelpers.DEFAULT_PROVIDER_NAME, DeploymentHelpers.DEFAULT_PROVIDER_PASSWORD).expect().log()
				.ifValidationFails().statusCode(200).header(CORSWithCredentialsFilter.ACCESS_CONTROL_ALLOW_ORIGIN, nullValue())
				.header(CORSWithCredentialsFilter.ACCESS_CONTROL_ALLOW_CREDENTIALS, nullValue()).when()
				.get(new URL(context, ProviderRestServiceTest.PROVIDER_REST_API_BASE).toExternalForm());

		prepareRequestSpecWithOriginHeader(requestOrigin).auth()
				.basic(DeploymentHelpers.DEFAULT_PROVIDER_NAME, DeploymentHelpers.DEFAULT_PROVIDER_PASSWORD)
				.body(provider.getProviderJSONModel()).expect().log().ifValidationFails().statusCode(200)
				.header(CORSWithCredentialsFilter.ACCESS_CONTROL_ALLOW_ORIGIN, nullValue())
				.header(CORSWithCredentialsFilter.ACCESS_CONTROL_ALLOW_CREDENTIALS, nullValue()).when()
				.post(new URL(context, PROVIDER_REST_API).toExternalForm());

		prepareRequestSpecWithOriginHeader(requestOrigin).auth()
				.basic(DeploymentHelpers.DEFAULT_PROVIDER_NAME, DeploymentHelpers.DEFAULT_PROVIDER_PASSWORD).expect().log()
				.ifValidationFails().statusCode(200).header(CORSWithCredentialsFilter.ACCESS_CONTROL_ALLOW_ORIGIN, nullValue())
				.header(CORSWithCredentialsFilter.ACCESS_CONTROL_ALLOW_CREDENTIALS, nullValue()).when()
				.delete(new URL(context, PROVIDER_REST_API).toExternalForm());

		// even for OPTION which means CORS Preflight Request handling
		prepareRequestSpecWithOriginHeader(requestOrigin).auth()
				.basic(DeploymentHelpers.DEFAULT_PROVIDER_NAME, DeploymentHelpers.DEFAULT_PROVIDER_PASSWORD).expect().log()
				.ifValidationFails().statusCode(200).header(CORSWithCredentialsFilter.ACCESS_CONTROL_ALLOW_ORIGIN, nullValue())
				.header(CORSWithCredentialsFilter.ACCESS_CONTROL_ALLOW_CREDENTIALS, nullValue())
				.header(CORSWithCredentialsFilter.ACCESS_CONTROL_ALLOW_METHODS, nullValue())
				.header(CORSWithCredentialsFilter.ACCESS_CONTROL_ALLOW_HEADERS, nullValue())
				.header(CORSWithCredentialsFilter.ACCESS_CONTROL_MAX_AGE, nullValue()).when()
				.options(new URL(context, ProviderRestServiceTest.PROVIDER_REST_API_BASE).toExternalForm());
	}

	private void assertCORSHeadersPresent(String origin, String credentialsAllowed) throws MalformedURLException {

		// test for all types of request
		given().contentType(ContentType.JSON).headers(CORSWithCredentialsFilter.HEADER_ORIGIN, origin).auth()
				.basic(DeploymentHelpers.DEFAULT_PROVIDER_NAME, DeploymentHelpers.DEFAULT_PROVIDER_PASSWORD).expect().log()
				.ifValidationFails().statusCode(200).header(CORSWithCredentialsFilter.ACCESS_CONTROL_ALLOW_ORIGIN, origin)
				.header(CORSWithCredentialsFilter.ACCESS_CONTROL_ALLOW_CREDENTIALS, is(credentialsAllowed)).when()
				.get(new URL(context, ProviderRestServiceTest.PROVIDER_REST_API_BASE).toExternalForm());

		given().contentType(ContentType.JSON).headers(CORSWithCredentialsFilter.HEADER_ORIGIN, origin).auth()
				.basic(DeploymentHelpers.DEFAULT_PROVIDER_NAME, DeploymentHelpers.DEFAULT_PROVIDER_PASSWORD)
				.body(provider.getProviderJSONModel()).expect().log().ifValidationFails().statusCode(200)
				.header(CORSWithCredentialsFilter.ACCESS_CONTROL_ALLOW_ORIGIN, origin)
				.header(CORSWithCredentialsFilter.ACCESS_CONTROL_ALLOW_CREDENTIALS, is(credentialsAllowed)).when()
				.post(new URL(context, PROVIDER_REST_API).toExternalForm());

		given().contentType(ContentType.JSON).headers(CORSWithCredentialsFilter.HEADER_ORIGIN, origin).auth()
				.basic(DeploymentHelpers.DEFAULT_PROVIDER_NAME, DeploymentHelpers.DEFAULT_PROVIDER_PASSWORD).expect().log()
				.ifValidationFails().statusCode(200).header(CORSWithCredentialsFilter.ACCESS_CONTROL_ALLOW_ORIGIN, origin)
				.header(CORSWithCredentialsFilter.ACCESS_CONTROL_ALLOW_CREDENTIALS, is(credentialsAllowed)).when()
				.delete(new URL(context, PROVIDER_REST_API).toExternalForm());

		// even for OPTION which means CORS Preflight Request handling
		List<Header> listHeaders = given()
				.contentType(ContentType.JSON)
				.headers(CORSWithCredentialsFilter.HEADER_ORIGIN, origin)
				.auth()
				.basic(DeploymentHelpers.DEFAULT_PROVIDER_NAME, DeploymentHelpers.DEFAULT_PROVIDER_PASSWORD)
				.expect()
				.log()
				.ifValidationFails()
				.statusCode(200)
				.header(CORSWithCredentialsFilter.ACCESS_CONTROL_ALLOW_ORIGIN, origin)
				.header(CORSWithCredentialsFilter.ACCESS_CONTROL_ALLOW_CREDENTIALS, credentialsAllowed)
				.header(CORSWithCredentialsFilter.ACCESS_CONTROL_ALLOW_HEADERS,
						CORSWithCredentialsFilter.ACCESS_CONTROL_ALLOW_HEADERS_VALUE)
				.header(CORSWithCredentialsFilter.ACCESS_CONTROL_MAX_AGE,
						CORSWithCredentialsFilter.ACCESS_CONTROL_MAX_AGE_VALUE).when()
				.options(new URL(context, ProviderRestServiceTest.PROVIDER_REST_API_BASE).toExternalForm()).headers()
				.getList(CORSWithCredentialsFilter.ACCESS_CONTROL_ALLOW_METHODS);

		Assert.assertEquals(4, listHeaders.size());
		Assert.assertThat(extractHeaderValues(listHeaders), Matchers.contains(CORSWithCredentialsFilter.GET,
				CORSWithCredentialsFilter.POST, CORSWithCredentialsFilter.PUT, CORSWithCredentialsFilter.DELETE));

	}

	private List<String> extractHeaderValues(List<Header> listHeaders) {
		List<String> ret = new ArrayList<String>();
		if (listHeaders != null) {
			for (Header h : listHeaders) {
				ret.add(h.getValue());
			}
		}
		return ret;
	}
}
