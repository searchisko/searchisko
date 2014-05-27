/*
 * JBoss, Home of Professional Open Source
 * Copyright 2012 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 */
package org.searchisko.ftest.rest;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

import com.jayway.restassured.http.ContentType;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.junit.InSequence;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.searchisko.ftest.DeploymentHelpers;

import static com.jayway.restassured.RestAssured.given;

/**
 * Integration test for /feed REST API.
 * <p/>
 * http://docs.jbossorg.apiary.io/#feedapi
 *
 * @author Libor Krzyzanek
 * @see org.searchisko.api.rest.FeedRestService
 */
@RunWith(Arquillian.class)
public class FeedRestServiceTest {
	public static final String FEED_REST_API = DeploymentHelpers.DEFAULT_REST_VERSION + "feed";

	@Deployment(testable = false)
	public static WebArchive createDeployment() throws IOException {
		return DeploymentHelpers.createDeployment();
	}

	@ArquillianResource
	URL context;

	@Test
	@InSequence(10)
	public void assertGetAll() throws MalformedURLException {
		given().contentType(ContentType.JSON)
				.expect()
				.log().ifError()
				.statusCode(200)
				.contentType("application/atom+xml")
				.when().get(new URL(context, FEED_REST_API).toExternalForm());
	}


}
