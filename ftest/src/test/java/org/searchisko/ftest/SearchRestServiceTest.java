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
import java.util.HashMap;
import java.util.Map;

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

	@Test
	@InSequence(10)
	public void assertSearchAllNoResult() throws MalformedURLException {
		given().contentType(ContentType.JSON)
				.expect()
				.log().ifError()
				.statusCode(200)
				.contentType(ContentType.JSON)
				.body("hits.total", is(0))
				.when().get(new URL(context, SEARCH_REST_API).toExternalForm());
	}


	public static final String TYPE1 = "provider1_blog";

	static ProviderModel provider1 = new ProviderModel("provider1", "password");

	static final String contentId = "test-id";

	static final String contentId2 = "test-id2";

	@Test
	@InSequence(30)
	public void assertCreateProvider1BlogPost() throws MalformedURLException {
		provider1.addContentType(TYPE1, "blogpost", true);

		ProviderRestServiceTest.createNewProvider(context, provider1);
	}

	@Test
	@InSequence(31)
	public void assertPushContentWithId() throws MalformedURLException {
		Map<String, Object> content = new HashMap<>();
		content.put("data", "test");
		ContentRestServiceTest.createOrUpdateContent(context, provider1, TYPE1, contentId, content);
	}

	@Test
	@InSequence(32)
	public void assertPushContentWithId2() throws MalformedURLException {
		Map<String, Object> content = new HashMap<>();
		content.put("data2", "test2");
		ContentRestServiceTest.createOrUpdateContent(context, provider1, TYPE1, contentId2, content);
	}


	@Test
	@InSequence(40)
	public void assertRefreshES() throws MalformedURLException {
		DeploymentHelpers.refreshES();
	}

	protected static String uuid;

	@Test
	@InSequence(41)
	public void assertSearchAllInsertedContent() throws MalformedURLException {
		uuid = given().contentType(ContentType.JSON)
				.expect()
				.log().all()
				.statusCode(200)
				.contentType(ContentType.JSON)
				.body("hits.total", is(2))
				.body("hits.hits[0]._type", is(TYPE1))
				.body("hits.hits[1]._type", is(TYPE1))
				.when().get(new URL(context, SEARCH_REST_API).toExternalForm()).andReturn().getBody().jsonPath().getString("uuid");
	}

	@Test
	@InSequence(42)
	public void assertRefreshES2() throws MalformedURLException {
		DeploymentHelpers.refreshES();
	}


	@Test
	@InSequence(50)
	public void assertPutSearchInvalid() throws MalformedURLException {
		given().contentType(ContentType.JSON)
				.pathParam("search_result_uuid", "invalid-uuid")
				.pathParam("hit_id", "invalid-hit-id")
				.expect()
				.statusCode(200)
				.log().ifError()
				.body(is("statistics record ignored"))
				.when().put(new URL(context, SEARCH_REST_API + "/{search_result_uuid}/{hit_id}").toExternalForm());

	}

	@Test
	@InSequence(51) @Ignore("REST Search API behaves differently")
	public void assertPutSearch() throws MalformedURLException {
		// TODO: FTEST: SearchRestAPI: Investigate Put Search
		given().contentType(ContentType.JSON)
				.pathParam("search_result_uuid", uuid)
				.pathParam("hit_id", TYPE1 + "-" + contentId)
				.log().all()
				.expect()
				.log().all()
				.statusCode(200)
				.body(is("statistics record accepted"))
				.when().put(new URL(context, SEARCH_REST_API + "/{search_result_uuid}/{hit_id}").toExternalForm());
	}

}
