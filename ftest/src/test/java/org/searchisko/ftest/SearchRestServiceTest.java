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
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

import static com.jayway.restassured.RestAssured.given;
import static org.hamcrest.Matchers.is;

/**
 * Integration test for /search REST API.
 *
 * @author Libor Krzyzanek
 * @see org.searchisko.api.rest.SearchRestService
 */
@RunWith(Arquillian.class)
public class SearchRestServiceTest {

	public static final String SEARCH_REST_API = DeploymentHelpers.DEFAULT_REST_VERSION + "search";

	@Deployment(testable = false)
	public static WebArchive createDeployment() throws IOException {
		return DeploymentHelpers.createDeployment();
	}

	@ArquillianResource
	URL context;


	protected static String uuid;

	@Test
	@InSequence(10)
	public void assertSearchAll() throws MalformedURLException {
		uuid = given().contentType(ContentType.JSON)
				.expect()
				.log().ifError()
				.statusCode(200)
				.contentType(ContentType.JSON)
				.body("hits.total", is(0))
				.when().get(new URL(context, SEARCH_REST_API).toExternalForm()).andReturn().getBody().jsonPath().getString("uuid");
	}


	@Test
	@InSequence(20)
	@Ignore("No data to search")
	public void assertPutSearch() throws MalformedURLException {
		given().contentType(ContentType.JSON)
				.pathParam("search_result_uuid", uuid)
				.pathParam("hit_id", "hit-id")
				.expect()
				.log().ifError()
				.statusCode(200)
				.body(is("statistics record ignored"))
				.when().put(new URL(context, SEARCH_REST_API + "/{search_result_uuid}/{hit_id}").toExternalForm());

	}

	@Test
	@InSequence(21)
	public void assertPutSearchInvalid() throws MalformedURLException {
		//TODO: Invalid POST /search should return BAD_REQUEST not 200
		given().contentType(ContentType.JSON)
				.pathParam("search_result_uuid", "invalid-uuid")
				.pathParam("hit_id", "invalid-hit-id")
				.expect()
				.statusCode(200)
				.body(is("statistics record ignored"))
				.when().put(new URL(context, SEARCH_REST_API + "/{search_result_uuid}/{hit_id}").toExternalForm());

	}


}
