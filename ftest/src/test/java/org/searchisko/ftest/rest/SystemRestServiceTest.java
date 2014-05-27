/*
 * JBoss, Home of Professional Open Source
 * Copyright 2012 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 */
package org.searchisko.ftest.rest;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.logging.Logger;

import com.jayway.restassured.builder.ResponseSpecBuilder;
import com.jayway.restassured.http.ContentType;
import com.jayway.restassured.specification.ResponseSpecification;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.junit.InSequence;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.searchisko.ftest.DeploymentHelpers;
import org.searchisko.ftest.ProviderModel;

import static com.jayway.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

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

	public static final String OPERATION_AUDITLOG = "auditlog";

	protected static Logger log = Logger.getLogger(SystemRestServiceTest.class.getName());


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
		// TEST: GET /auditlog
		given().pathParam("operation", OPERATION_AUDITLOG).contentType(ContentType.JSON)
				.expect().statusCode(expStatus).log().ifStatusCodeMatches(is(not(expStatus)))
				.when().get(new URL(context, SYSTEM_REST_API).toExternalForm());
	}

	// TODO: Create index and push mapping

	@Test
	@InSequence(5)
	public void assertAuditLogIndexIsMissing() throws MalformedURLException {
		given().pathParam("operation", OPERATION_AUDITLOG).contentType(ContentType.JSON)
				.auth().basic(DeploymentHelpers.DEFAULT_PROVIDER_NAME, DeploymentHelpers.DEFAULT_PROVIDER_PASSWORD)
				.expect().statusCode(204)
				.when().get(new URL(context, SYSTEM_REST_API).toExternalForm());
	}


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
				.body("system", is(nullValue()))
				.body("servlet-container", is(nullValue()))
				.when().get(new URL(context, SYSTEM_REST_API).toExternalForm());
	}

	@Test
	@InSequence(11)
	public void assertGetInfoAuthenticated() throws MalformedURLException {
		// Authentication needs to be preemptive because guest is allowed as well.
		given().contentType(ContentType.JSON)
				.pathParam("operation", "info")
				.auth().preemptive().basic(DeploymentHelpers.DEFAULT_PROVIDER_NAME, DeploymentHelpers.DEFAULT_PROVIDER_PASSWORD)
				.expect()
				.log().ifError()
				.statusCode(200)
				.contentType(ContentType.JSON)
				.body("build.version", notNullValue())
				.body("build.build-timestamp", notNullValue())
				.body("system", notNullValue())
				.body("servlet-container", notNullValue())
				.when().get(new URL(context, SYSTEM_REST_API).toExternalForm());
	}

	@Test
	@InSequence(100)
	public void assertRefreshES() throws MalformedURLException {
		DeploymentHelpers.refreshES();
	}


	@Test
	@InSequence(110)
	public void assertAuditAfterGetInfo() throws MalformedURLException {
		given().pathParam("operation", OPERATION_AUDITLOG).contentType(ContentType.JSON)
				.auth().basic(DeploymentHelpers.DEFAULT_PROVIDER_NAME, DeploymentHelpers.DEFAULT_PROVIDER_PASSWORD)
				.expect().statusCode(200)
				.log().ifError()
				.contentType(ContentType.JSON)
				.body("total", is(2))
				.body("hits[0].data.id", nullValue())
				.body("hits[0].data.content", nullValue())
				.body("hits[0].data.operation", is("GET"))
				.body("hits[0].data.type", is("audit"))
				.body("hits[0].data.date", notNullValue())
				.when().get(new URL(context, SYSTEM_REST_API).toExternalForm());
	}

	public static final ProviderModel provider1 = new ProviderModel("provider1", "Password1");

	@Test
	@InSequence(120)
	public void assertNewProvider1() throws MalformedURLException {
		ProviderRestServiceTest.createNewProvider(context, provider1);
	}

	@Test
	@InSequence(121)
	public void assertRefreshES2() throws MalformedURLException {
		DeploymentHelpers.refreshES();
	}

	static ResponseSpecification auditProvider1ResponseSpec = new ResponseSpecBuilder()
			.expectStatusCode(200)
			.expectContentType(ContentType.JSON)
			.expectBody("x.y.size()", is(2))
			.expectBody("total", is(1))
			.expectBody("hits[0].data.id", is(provider1.name))
			.expectBody("hits[0].data.content.name", is(provider1.name))
			.expectBody("hits[0].data.operation", is("POST"))
			.expectBody("hits[0].data.user_type", is("PROVIDER"))
			.expectBody("hits[0].data.type", is("audit"))
			.expectBody("hits[0].data.date", notNullValue())
			.build();

	@Test
	@InSequence(122)
	@Ignore("Filtering doesn't work because it needs ES mapping")
	public void assertAuditFilterByOperation() throws MalformedURLException {
		given().pathParam("operation", OPERATION_AUDITLOG).contentType(ContentType.JSON)
				.queryParam("operation", "POST")
				.auth().basic(DeploymentHelpers.DEFAULT_PROVIDER_NAME, DeploymentHelpers.DEFAULT_PROVIDER_PASSWORD)
				.log().all()
				.expect()
				.log().all()
				.spec(auditProvider1ResponseSpec)
				.when().get(new URL(context, SYSTEM_REST_API).toExternalForm());
	}

}
