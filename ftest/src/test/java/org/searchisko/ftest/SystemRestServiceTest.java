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
import static org.hamcrest.Matchers.notNullValue;

/**
 * Integration test for /sys REST API.
 * <p/>
 * http://docs.jbossorg.apiary.io/#managementapisystem
 *
 * @author Libor Krzyzanek
 * @see org.searchisko.api.rest.SystemRestService
 */
@RunWith(Arquillian.class)
public class SystemRestServiceTest {

	public static final String SYSTEM_REST_API = DeploymentHelpers.DEFAULT_REST_VERSION + "sys/{operation}";

	@Deployment(testable = false)
	public static WebArchive createDeployment() throws IOException {
		return DeploymentHelpers.createDeployment();
	}

	@ArquillianResource
	URL context;

	@Test
	@InSequence(10)
	public void assertGetInfoNotAuthenticated() throws MalformedURLException {
		given().contentType(ContentType.JSON)
				.pathParam("operation", "info")
				.expect()
				.log().ifError()
				.statusCode(200)
				.contentType(ContentType.JSON)
				.body("build.version", notNullValue())
				.body("build.build-timestamp", notNullValue())
				.when().get(new URL(context, SYSTEM_REST_API).toExternalForm());
	}

	@Test
	@InSequence(11)
	@Ignore("get system info returns same data as un authenticated.")
	public void assertGetInfoAuthenticated() throws MalformedURLException {
		//TODO: Check why /sys/info returns same values for authenticated provider like for unauthenticated
		given().contentType(ContentType.JSON)
				.pathParam("operation", "info")
				.auth().basic(DeploymentHelpers.DEFAULT_PROVIDER_NAME, DeploymentHelpers.DEFAULT_PROVIDER_PASSWORD)
				.expect()
				.log().all()
				.statusCode(200)
				.contentType(ContentType.JSON)
				.body("build.version", notNullValue())
				.body("build.build-timestamp", notNullValue())
				.body("system", notNullValue())
				.when().get(new URL(context, SYSTEM_REST_API).toExternalForm());
	}


}
