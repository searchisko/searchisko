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
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

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

	//TODO: FTEST: RatingRestServiceTest: Add contributor, content and try rating

}
