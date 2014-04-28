/*
 * JBoss, Home of Professional Open Source
 * Copyright 2012 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 */
package org.searchisko.ftest;

//import org.searchisko.api.testtools.ESRealClientTestBase;

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
 * Integration tests for {@link org.searchisko.api.rest.ContentRestService} REST API
 * <p/>
 * see http://docs.jbossorg.apiary.io/#contentpushapi
 *
 * @author Libor Krzyzanek
 * @see org.searchisko.ftest.ContentRestServiceTest
 */
@RunWith(Arquillian.class)
public class ContentRestServiceTest {

	public static final String CONTENT_REST_API = DeploymentHelpers.DEFAULT_REST_VERSION + "content/{type}/{contentId}";

	public static final String TYPE = "testtype";

	@ArquillianResource
	protected URL context;

	@Deployment(testable = false)
	public static WebArchive createDeployment() throws IOException {
		return DeploymentHelpers.createDeployment();
	}

	@Test
	@InSequence(0)
	public void assertNotAuthenticated() throws MalformedURLException {
		// TEST: POST
		int expStatus = 401;
		given().pathParam("type", TYPE).pathParam("contentId", "test").contentType(ContentType.JSON)
				.body("{}")
				.expect().statusCode(expStatus).log().ifStatusCodeMatches(is(not(expStatus)))
				.when().post(new URL(context, CONTENT_REST_API).toExternalForm());

		// TEST: DELETE
		expStatus = 401;
		given().pathParam("type", TYPE).pathParam("contentId", "test").contentType(ContentType.JSON)
				.body("{}")
				.expect().statusCode(expStatus).log().ifStatusCodeMatches(is(not(expStatus)))
				.when().delete(new URL(context, CONTENT_REST_API).toExternalForm());
	}

}
