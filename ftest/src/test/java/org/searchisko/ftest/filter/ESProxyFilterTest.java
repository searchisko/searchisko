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

import com.jayway.restassured.http.ContentType;

import static com.jayway.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;

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

	public static final String SEARCH_REST_API = DeploymentHelpers.CURRENT_REST_VERSION + "sys/es/search";

	public static final String STATS_REST_API = DeploymentHelpers.CURRENT_REST_VERSION + "sys/es/stats";

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
				.body(new HashMap<>()).expect().log().ifError().statusCode(200).contentType(ContentType.JSON).when()
				.post(new URL(context, SEARCH_REST_API + "/unknownindex").toExternalForm());

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

	/**
	 * Helper method to create index in ES 'search' cluster. Call it from other tests when you need this.
	 * 
	 * @param context to be used to call REST API
	 * @param indexName name of index to create
	 * @param indexDef JSON with definition of index
	 * @throws MalformedURLException
	 */
	public static final void createSearchESIndex(URL context, String indexName, String indexDef)
			throws MalformedURLException {
		given().contentType(ContentType.JSON).content(indexDef).auth().preemptive()
				.basic(DeploymentHelpers.DEFAULT_PROVIDER_NAME, DeploymentHelpers.DEFAULT_PROVIDER_PASSWORD).expect().log()
				.ifError().statusCode(200).contentType(ContentType.JSON).when()
				.put(new URL(context, SEARCH_REST_API + "/" + indexName + "/").toExternalForm());
	}

	/**
	 * Helper method to create mapping in ES 'search' cluster. Call it from other tests when you need this. You have to
	 * create index first!
	 * 
	 * @param context to be used to call REST API
	 * @param indexName name of index mapping is for
	 * @param indexType name of ES type mapping is for
	 * @param mapping JSON with definition of mapping
	 * @throws MalformedURLException
	 */
	public static final void createSearchESIndexMapping(URL context, String indexName, String indexType, String mapping)
			throws MalformedURLException {
		given().contentType(ContentType.JSON).content(mapping).auth().preemptive()
				.basic(DeploymentHelpers.DEFAULT_PROVIDER_NAME, DeploymentHelpers.DEFAULT_PROVIDER_PASSWORD).expect().log()
				.ifError().statusCode(200).contentType(ContentType.JSON).when()
				.put(new URL(context, SEARCH_REST_API + "/" + indexName + "/" + indexType + "/_mapping").toExternalForm());
	}

}
