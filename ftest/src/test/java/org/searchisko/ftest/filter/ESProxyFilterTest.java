/*
 * JBoss, Home of Professional Open Source
 * Copyright 2014 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 */
package org.searchisko.ftest.filter;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;

import com.jayway.restassured.http.ContentType;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.junit.InSequence;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.searchisko.api.filter.ESProxyFilter;
import org.searchisko.ftest.DeploymentHelpers;
import org.searchisko.ftest.ProviderModel;
import org.searchisko.ftest.rest.ProviderRestServiceTest;

import static com.jayway.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

/**
 * Integration test for /sys/es/search and /sys/es/stats access to full Elasticsearch REST API.
 * <p/>
 * http://docs.jbossorg.apiary.io/#managementapisystem
 * 
 * @author Vlastimil Elias (velias at redhat dot com)
 * @see ESProxyFilter
 */
@RunWith(Arquillian.class)
public class ESProxyFilterTest {

	public static final String SEARCH_REST_API = DeploymentHelpers.DEFAULT_REST_VERSION + "sys/es/search";

	public static final String STATS_REST_API = DeploymentHelpers.DEFAULT_REST_VERSION + "sys/es/stats";

	@ArquillianResource
	protected URL context;

	@Deployment(testable = false)
	public static WebArchive createDeployment() throws IOException {
		return DeploymentHelpers.createDeployment();
	}

	@Test
	@InSequence(0)
	public void assertSearchNotAuthenticated(@ArquillianResource URL context) throws MalformedURLException {
		int expStatus = 401;
		given().contentType(ContentType.JSON).expect().statusCode(expStatus).log().ifStatusCodeMatches(is(not(expStatus)))
				.when().get(new URL(context, SEARCH_REST_API).toExternalForm());

		given().contentType(ContentType.JSON).expect().statusCode(expStatus).log().ifStatusCodeMatches(is(not(expStatus)))
				.when().post(new URL(context, SEARCH_REST_API).toExternalForm());

		given().contentType(ContentType.JSON).expect().statusCode(expStatus).log().ifStatusCodeMatches(is(not(expStatus)))
				.when().put(new URL(context, SEARCH_REST_API).toExternalForm());

		given().contentType(ContentType.JSON).expect().statusCode(expStatus).log().ifStatusCodeMatches(is(not(expStatus)))
				.when().delete(new URL(context, SEARCH_REST_API).toExternalForm());

		given().contentType(ContentType.JSON).expect().statusCode(expStatus).log().ifStatusCodeMatches(is(not(expStatus)))
				.when().delete(new URL(context, SEARCH_REST_API + "/_all").toExternalForm());
	}

	@Test
	@InSequence(2)
	public void assertStatsNotAuthenticated(@ArquillianResource URL context) throws MalformedURLException {
		int expStatus = 401;
		given().contentType(ContentType.JSON).expect().statusCode(expStatus).log().ifStatusCodeMatches(is(not(expStatus)))
				.when().get(new URL(context, STATS_REST_API).toExternalForm());

		given().contentType(ContentType.JSON).expect().statusCode(expStatus).log().ifStatusCodeMatches(is(not(expStatus)))
				.when().post(new URL(context, STATS_REST_API).toExternalForm());

		given().contentType(ContentType.JSON).expect().statusCode(expStatus).log().ifStatusCodeMatches(is(not(expStatus)))
				.when().put(new URL(context, STATS_REST_API).toExternalForm());

		given().contentType(ContentType.JSON).expect().statusCode(expStatus).log().ifStatusCodeMatches(is(not(expStatus)))
				.when().delete(new URL(context, STATS_REST_API).toExternalForm());

		given().contentType(ContentType.JSON).expect().statusCode(expStatus).log().ifStatusCodeMatches(is(not(expStatus)))
				.when().delete(new URL(context, STATS_REST_API + "/_all").toExternalForm());
	}

	public static final ProviderModel PROVIDER_NO_ADMIN = new ProviderModel("provider1", "Password1");

	@Test
	@InSequence(5)
	public void assertSearchNotAuthorized(@ArquillianResource URL context) throws MalformedURLException {

		// create New Provider without permissions
		given().pathParam("id", PROVIDER_NO_ADMIN.name).contentType(ContentType.JSON).auth()
				.basic(DeploymentHelpers.DEFAULT_PROVIDER_NAME, DeploymentHelpers.DEFAULT_PROVIDER_PASSWORD)
				.body(PROVIDER_NO_ADMIN.getProviderJSONModel()).expect().log().ifError().statusCode(200)
				.body("id", is(PROVIDER_NO_ADMIN.name)).when()
				.post(new URL(context, ProviderRestServiceTest.PROVIDER_REST_API).toExternalForm());

		// test
		int expStatus = 403;

		given().contentType(ContentType.JSON).auth().preemptive().basic(PROVIDER_NO_ADMIN.name, PROVIDER_NO_ADMIN.password)
				.expect().statusCode(expStatus).log().ifStatusCodeMatches(is(not(expStatus))).when()
				.get(new URL(context, SEARCH_REST_API).toExternalForm());

		given().contentType(ContentType.JSON).auth().preemptive().basic(PROVIDER_NO_ADMIN.name, PROVIDER_NO_ADMIN.password)
				.expect().statusCode(expStatus).log().ifStatusCodeMatches(is(not(expStatus))).when()
				.post(new URL(context, SEARCH_REST_API).toExternalForm());

		given().contentType(ContentType.JSON).auth().preemptive().basic(PROVIDER_NO_ADMIN.name, PROVIDER_NO_ADMIN.password)
				.expect().statusCode(expStatus).log().ifStatusCodeMatches(is(not(expStatus))).when()
				.put(new URL(context, SEARCH_REST_API).toExternalForm());

		given().contentType(ContentType.JSON).auth().preemptive().basic(PROVIDER_NO_ADMIN.name, PROVIDER_NO_ADMIN.password)
				.expect().statusCode(expStatus).log().ifStatusCodeMatches(is(not(expStatus))).when()
				.delete(new URL(context, SEARCH_REST_API).toExternalForm());

		given().contentType(ContentType.JSON).auth().preemptive().basic(PROVIDER_NO_ADMIN.name, PROVIDER_NO_ADMIN.password)
				.expect().statusCode(expStatus).log().ifStatusCodeMatches(is(not(expStatus))).when()
				.delete(new URL(context, SEARCH_REST_API + "/_all").toExternalForm());
	}

	@Test
	@InSequence(6)
	public void assertStatsNotAuthorized(@ArquillianResource URL context) throws MalformedURLException {

		// create New Provider without permissions
		given().pathParam("id", PROVIDER_NO_ADMIN.name).contentType(ContentType.JSON).auth()
				.basic(DeploymentHelpers.DEFAULT_PROVIDER_NAME, DeploymentHelpers.DEFAULT_PROVIDER_PASSWORD)
				.body(PROVIDER_NO_ADMIN.getProviderJSONModel()).expect().log().ifError().statusCode(200)
				.body("id", is(PROVIDER_NO_ADMIN.name)).when()
				.post(new URL(context, ProviderRestServiceTest.PROVIDER_REST_API).toExternalForm());

		// test
		int expStatus = 403;

		given().contentType(ContentType.JSON).auth().preemptive().basic(PROVIDER_NO_ADMIN.name, PROVIDER_NO_ADMIN.password)
				.expect().statusCode(expStatus).log().ifStatusCodeMatches(is(not(expStatus))).when()
				.get(new URL(context, STATS_REST_API).toExternalForm());

		given().contentType(ContentType.JSON).auth().preemptive().basic(PROVIDER_NO_ADMIN.name, PROVIDER_NO_ADMIN.password)
				.expect().statusCode(expStatus).log().ifStatusCodeMatches(is(not(expStatus))).when()
				.post(new URL(context, STATS_REST_API).toExternalForm());

		given().contentType(ContentType.JSON).auth().preemptive().basic(PROVIDER_NO_ADMIN.name, PROVIDER_NO_ADMIN.password)
				.expect().statusCode(expStatus).log().ifStatusCodeMatches(is(not(expStatus))).when()
				.put(new URL(context, STATS_REST_API).toExternalForm());

		given().contentType(ContentType.JSON).auth().preemptive().basic(PROVIDER_NO_ADMIN.name, PROVIDER_NO_ADMIN.password)
				.expect().statusCode(expStatus).log().ifStatusCodeMatches(is(not(expStatus))).when()
				.delete(new URL(context, STATS_REST_API).toExternalForm());

		given().contentType(ContentType.JSON).auth().preemptive().basic(PROVIDER_NO_ADMIN.name, PROVIDER_NO_ADMIN.password)
				.expect().statusCode(expStatus).log().ifStatusCodeMatches(is(not(expStatus))).when()
				.delete(new URL(context, STATS_REST_API + "/_all").toExternalForm());
	}

	@Test
	@InSequence(11)
	public void assertSearchAuthenticated() throws MalformedURLException {
		given().contentType(ContentType.JSON).auth().preemptive()
				.basic(DeploymentHelpers.DEFAULT_PROVIDER_NAME, DeploymentHelpers.DEFAULT_PROVIDER_PASSWORD).expect().log()
				.ifError().statusCode(200).contentType(ContentType.JSON).body("name", notNullValue())
				.body("status", equalTo(200)).when().get(new URL(context, SEARCH_REST_API).toExternalForm());

		given().auth().preemptive()
				.basic(DeploymentHelpers.DEFAULT_PROVIDER_NAME, DeploymentHelpers.DEFAULT_PROVIDER_PASSWORD).expect().log()
				.ifError().statusCode(404).when()
				.head(new URL(context, SEARCH_REST_API + "/unknownindex/unknowntype").toExternalForm());

		// create index
		given().contentType(ContentType.JSON).auth().preemptive()
				.basic(DeploymentHelpers.DEFAULT_PROVIDER_NAME, DeploymentHelpers.DEFAULT_PROVIDER_PASSWORD)
				.body(new HashMap<>()).expect().log().ifError().statusCode(200).contentType(ContentType.JSON)
				.when().post(new URL(context, SEARCH_REST_API + "/unknownindex").toExternalForm());

		// and check it exists
		given().auth().preemptive()
				.basic(DeploymentHelpers.DEFAULT_PROVIDER_NAME, DeploymentHelpers.DEFAULT_PROVIDER_PASSWORD).expect().log()
				.ifError().statusCode(200).when().head(new URL(context, SEARCH_REST_API + "/unknownindex").toExternalForm());

	}

	@Test
	@InSequence(12)
	public void assertStatsAuthenticated() throws MalformedURLException {
		given().contentType(ContentType.JSON).auth().preemptive()
				.basic(DeploymentHelpers.DEFAULT_PROVIDER_NAME, DeploymentHelpers.DEFAULT_PROVIDER_PASSWORD).expect().log()
				.ifError().statusCode(200).contentType(ContentType.JSON).body("name", notNullValue())
				.body("status", equalTo(200)).when().get(new URL(context, STATS_REST_API).toExternalForm());

		given().contentType(ContentType.JSON).auth().preemptive()
				.basic(DeploymentHelpers.DEFAULT_PROVIDER_NAME, DeploymentHelpers.DEFAULT_PROVIDER_PASSWORD).expect().log()
				.all().statusCode(400).when()
				.get(new URL(context, STATS_REST_API + "/unknownindex/unknowntype").toExternalForm());
	}

}
