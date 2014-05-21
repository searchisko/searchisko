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
import org.junit.AfterClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.searchisko.ftest.DeploymentHelpers;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

import static com.jayway.restassured.RestAssured.given;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;

/**
 * Integration test for /config REST API.
 * <p/>
 * http://docs.jbossorg.apiary.io/#managementapiconfiguration
 *
 * @author Libor Krzyzanek
 * @see org.searchisko.api.rest.ConfigRestService
 */
@RunWith(Arquillian.class)
public class ConfigRestServiceTest {

	public static final String CONFIG_REST_API = DeploymentHelpers.DEFAULT_REST_VERSION + "config/{id}";

	@Deployment(testable = false)
	public static WebArchive createDeployment() throws IOException {
		return DeploymentHelpers.createDeployment();
	}

	@AfterClass
	public static void cleanAfterTest() throws IOException {
		DeploymentHelpers.removeSearchiskoDataDir();
	}

	@ArquillianResource
	URL context;


	@Test
	@InSequence(0)
	public void assertNotAuthenticated() throws MalformedURLException {
		int expStatus = 401;

		// GET /config
		given().contentType(ContentType.JSON)
				.pathParam("id", "")
				.expect().statusCode(expStatus)
				.log().ifStatusCodeMatches(is(not(expStatus)))
				.when().get(new URL(context, CONFIG_REST_API).toExternalForm());

		// GET /config/some-id
		given().contentType(ContentType.JSON)
				.pathParam("id", "some-id")
				.expect().statusCode(expStatus)
				.log().ifStatusCodeMatches(is(not(expStatus)))
				.when().get(new URL(context, CONFIG_REST_API).toExternalForm());

		// POST /config/some-id
		given().contentType(ContentType.JSON)
				.pathParam("id", "some-id")
				.body("{}")
				.expect().statusCode(expStatus)
				.log().ifStatusCodeMatches(is(not(expStatus)))
				.when().post(new URL(context, CONFIG_REST_API).toExternalForm());

		// DELETE /config/some-id
		given().contentType(ContentType.JSON)
				.pathParam("id", "some-id")
				.expect().statusCode(expStatus)
				.log().ifStatusCodeMatches(is(not(expStatus)))
				.when().delete(new URL(context, CONFIG_REST_API).toExternalForm());
	}

	@Test
	@InSequence(10)
	public void assertGetDefaultAll() throws MalformedURLException {
		given().contentType(ContentType.JSON)
				.auth().basic(DeploymentHelpers.DEFAULT_PROVIDER_NAME, DeploymentHelpers.DEFAULT_PROVIDER_PASSWORD)
				.pathParam("id", "")
				.expect()
				.log().ifError()
				.statusCode(200)
				.contentType(ContentType.JSON)
				.body("total", is(0))
				.when().get(new URL(context, CONFIG_REST_API).toExternalForm());
	}

	protected static final String configId = "search_fulltext_query_fields";

	@Test
	@InSequence(20)
	public void assertCreateNew() throws MalformedURLException {
		// see https://github.com/searchisko/searchisko/blob/master/documentation/rest-api/management/config_search_fulltext_query_fields.md
		final String data = "{\n" +
				"  \"sys_title\": \"2.5\",\n" +
				"  \"sys_description\": \"\",\n" +
				"  \"sys_project_name\": \"2\",\n" +
				"  \"sys_tags\":\"1.5\",\n" +
				"  \"sys_contributors.fulltext\": \"\"\n" +
				"}";
		given().contentType(ContentType.JSON)
				.auth().basic(DeploymentHelpers.DEFAULT_PROVIDER_NAME, DeploymentHelpers.DEFAULT_PROVIDER_PASSWORD)
				.pathParam("id", configId)
				.body(data)
				.expect()
				.log().ifError()
				.statusCode(200)
				.contentType(ContentType.JSON)
				.body("id", is(configId))
				.when().post(new URL(context, CONFIG_REST_API).toExternalForm());
	}

	@Test
	@InSequence(21)
	public void assertRefreshES() throws MalformedURLException {
		DeploymentHelpers.refreshES();
	}

	@Test
	@InSequence(25)
	public void assertGetCreated() throws MalformedURLException {
		given().contentType(ContentType.JSON)
				.auth().basic(DeploymentHelpers.DEFAULT_PROVIDER_NAME, DeploymentHelpers.DEFAULT_PROVIDER_PASSWORD)
				.pathParam("id", configId)
				.expect()
				.log().ifError()
				.statusCode(200)
				.contentType(ContentType.JSON)
				.body("sys_description", notNullValue())
				.when().get(new URL(context, CONFIG_REST_API).toExternalForm());
	}

	@Test
	@InSequence(26)
	public void assertGetCreatedAll() throws MalformedURLException {
		given().contentType(ContentType.JSON)
				.auth().basic(DeploymentHelpers.DEFAULT_PROVIDER_NAME, DeploymentHelpers.DEFAULT_PROVIDER_PASSWORD)
				.pathParam("id", "")
				.expect()
				.log().ifError()
				.statusCode(200)
				.contentType(ContentType.JSON)
				.body("total", is(1))
				.body("hits[0].id", is(configId))
				.when().get(new URL(context, CONFIG_REST_API).toExternalForm());
	}

	@Test
	@InSequence(30)
	public void assertDeleteCreated() throws MalformedURLException {
		given().contentType(ContentType.JSON)
				.auth().basic(DeploymentHelpers.DEFAULT_PROVIDER_NAME, DeploymentHelpers.DEFAULT_PROVIDER_PASSWORD)
				.pathParam("id", configId)
				.expect()
				.log().ifError()
				.statusCode(200)
				.when().delete(new URL(context, CONFIG_REST_API).toExternalForm());
	}
}
