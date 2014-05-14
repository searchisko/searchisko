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
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

/**
 * Integration test for /normalization REST API.
 * <p/>
 * http://docs.jbossorg.apiary.io/#normalizationapi
 *
 * @author Libor Krzyzanek
 * @see org.searchisko.api.rest.NormalizationRestService
 */
@RunWith(Arquillian.class)
public class NormalizationRestServiceTest {

	public static final String NORMALIZATION_REST_API = DeploymentHelpers.DEFAULT_REST_VERSION + "normalization/{normalizationName}/{id}";

	@Deployment(testable = false)
	public static WebArchive createDeployment() throws IOException {
		return DeploymentHelpers.createDeployment();
	}

	@ArquillianResource
	URL context;


	@Test
	@InSequence(0)
	public void assertNotAuthenticated() throws MalformedURLException {
		int expStatus = 401;

		// GET /indexer/{normalizationName}/
		given().contentType(ContentType.JSON)
				.pathParam("normalizationName", "bad-name")
				.pathParam("id", "")
				.expect().statusCode(expStatus)
				.log().ifStatusCodeMatches(is(not(expStatus)))
				.when().get(new URL(context, NORMALIZATION_REST_API).toExternalForm());

		// GET /indexer/{normalizationName}/{id}
		given().contentType(ContentType.JSON)
				.pathParam("normalizationName", "bad-name")
				.pathParam("id", "bad-id")
				.expect().statusCode(expStatus)
				.log().ifStatusCodeMatches(is(not(expStatus)))
				.when().get(new URL(context, NORMALIZATION_REST_API).toExternalForm());
	}

	//TODO: FTEST: NormalizationRestServiceTest: Test distinct normalizations
}
