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
import static org.hamcrest.Matchers.*;
import static org.hamcrest.Matchers.not;

/**
 * Integration test for /rating REST API.
 * <p/>
 * http://docs.jbossorg.apiary.io/#personalizedcontentratingapi
 *
 * @author Libor Krzyzanek
 * @see org.searchisko.api.rest.RatingRestService
 */
@RunWith(Arquillian.class)
public class RatingRestServiceTest {

	public static final String RATING_REST_API = DeploymentHelpers.DEFAULT_REST_VERSION + "rating/{id}";

	@Deployment(testable = false)
	public static WebArchive createDeployment() throws IOException {
		return DeploymentHelpers.createDeployment();
	}

	@ArquillianResource
	URL context;


	@Test
	@InSequence(0)
	public void assertNotAuthenticated() throws MalformedURLException {
		int expStatus = 403;

		// GET /rating
		given().contentType(ContentType.JSON)
				.pathParam("id", "")
				.expect()
				.log().ifStatusCodeMatches(is(not(expStatus)))
				.statusCode(expStatus)
				.header("WWW-Authenticate", nullValue())
				.body(is("Required authorization {0}."))
				.when().get(new URL(context, RATING_REST_API).toExternalForm());

		// GET /rating/bad-id
		given().contentType(ContentType.JSON)
				.pathParam("id", "bad-id")
				.expect().statusCode(expStatus)
				.header("WWW-Authenticate", nullValue())
				.log().ifStatusCodeMatches(is(not(expStatus)))
				.when().get(new URL(context, RATING_REST_API).toExternalForm());

		// POST /rating/bad-id
		given().contentType(ContentType.JSON)
				.pathParam("id", "bad-id")
				.expect().statusCode(expStatus)
				.header("WWW-Authenticate", nullValue())
				.log().ifStatusCodeMatches(is(not(expStatus)))
				.when().get(new URL(context, RATING_REST_API).toExternalForm());

	}

	public static final String TYPE1 = "provider1_blog";

	static ProviderModel provider1 = new ProviderModel("provider1", "password");

	static final String contentId = "test-id";

	@Test
	@InSequence(1)
	public void assertCreateProvider1BlogPost() throws MalformedURLException {
		provider1.addContentType(TYPE1, "blogpost", true);

		ProviderRestServiceTest.createNewProvider(context, provider1);
	}

	@Test
	@InSequence(2)
	public void assertPushContentWithId() throws MalformedURLException {
		Map<String, Object> content = new HashMap<>();
		content.put("data", "test");
		ContentRestServiceTest.createOrUpdateContent(context, provider1, TYPE1, contentId, content);
	}

	String contribUsername = "contributor1";

	String contribPassword = "contributor1Password";


	@Test
	@InSequence(10) @Ignore("No HTTP Basic Auth for Contributor")
	public void assertRate() throws MalformedURLException {
		given().contentType(ContentType.JSON)
				.auth().preemptive().basic(contribUsername, contribPassword)
				.pathParam("id", contentId)
				.expect().statusCode(200)
				.log().ifError()
				.when().get(new URL(context, RATING_REST_API).toExternalForm());

		given().contentType(ContentType.JSON)
				.pathParam("id", contentId)
				.auth().basic(contribUsername, contribPassword)
				.expect()
				.log().ifError()
				.statusCode(200)
				.contentType(ContentType.JSON)
				.body("rating", is(1))
				.when().get(new URL(context, RATING_REST_API).toExternalForm());
	}

	@Test
	@InSequence(20) @Ignore("No HTTP Basic Auth for Contributor")
	public void assertGetAll() throws MalformedURLException {
		given().contentType(ContentType.JSON)
				.pathParam("id", "")
				.queryParam("id", contentId)
				.auth().preemptive().basic(contribUsername, contribPassword)
				.expect()
				.log().ifError()
				.statusCode(200)
				.contentType(ContentType.JSON)
				.body(contentId + ".rating", is("1"))
				.when().get(new URL(context, RATING_REST_API).toExternalForm());
	}

	//TODO: FTEST: RatingRestServiceTest: Fake CAS JAAS module by additional HTTP Basic Login Module which authenticate contributor

}
