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
 * see http://docs.jbossorg.apiary.io/#contentmanipulationapi
 *
 * @author Libor Krzyzanek
 * @see org.searchisko.ftest.ContentRestServiceTest
 */
@RunWith(Arquillian.class)
public class ContentRestServiceTest {

	public static final String CONTENT_REST_API = DeploymentHelpers.DEFAULT_REST_VERSION + "content/{type}/{contentId}";

	public static final String TYPE = "provider_blog";

	@ArquillianResource
	protected URL context;

	@Deployment(testable = false)
	public static WebArchive createDeployment() throws IOException {
		return DeploymentHelpers.createDeployment();
	}

	@Test
	@InSequence(0)
	public void assertBasicAuthHeader() throws MalformedURLException {
		int expStatus = 401;
		given().pathParam("type", TYPE).pathParam("contentId", "test").contentType(ContentType.JSON)
				.body("{}")
				.expect().statusCode(expStatus)
				.header("WWW-Authenticate", "Basic realm=\"Insert Provider's username and password\"")
				.log().ifStatusCodeMatches(is(not(expStatus)))
				.when().post(new URL(context, CONTENT_REST_API).toExternalForm());
	}

	@Test
	@InSequence(1)
	public void assertNotAuthenticated() throws MalformedURLException {
		int expStatus = 401;
		// POST /content/type/id
		given().pathParam("type", TYPE).pathParam("contentId", "id").contentType(ContentType.JSON)
				.body("{}")
				.expect().statusCode(expStatus).log().ifStatusCodeMatches(is(not(expStatus)))
				.when().post(new URL(context, CONTENT_REST_API).toExternalForm());

		// POST /content/type/
		given().pathParam("type", TYPE).pathParam("contentId", "").contentType(ContentType.JSON)
				.body("{}")
				.expect().statusCode(expStatus).log().ifStatusCodeMatches(is(not(expStatus)))
				.when().post(new URL(context, CONTENT_REST_API).toExternalForm());

		// DELETE /content/type/
		given().pathParam("type", TYPE).pathParam("contentId", "").contentType(ContentType.JSON)
				.body("{}")
				.expect().statusCode(expStatus).log().ifStatusCodeMatches(is(not(expStatus)))
				.when().delete(new URL(context, CONTENT_REST_API).toExternalForm());

		// DELETE /content/type/id
		given().pathParam("type", TYPE).pathParam("contentId", "id").contentType(ContentType.JSON)
				.body("{}")
				.expect().statusCode(expStatus).log().ifStatusCodeMatches(is(not(expStatus)))
				.when().delete(new URL(context, CONTENT_REST_API).toExternalForm());

	}

	static ProviderModel provider = new ProviderModel("provider", "password");

	@Test
	@InSequence(1)
	public void assertCreateProviderBlogPost() throws MalformedURLException {
		provider.addContentType(TYPE, "blogpost", true);

		ProviderRestServiceTest.createNewProvider(context, provider);
	}

	@Test
	@InSequence(10)
	public void assertPushContent() throws MalformedURLException {
		String contentId = "test-id";
		given().pathParam("type", TYPE).pathParam("contentId", contentId).contentType(ContentType.JSON)
				.auth().basic(provider.name, provider.password)
				.body("{\"data\": \"test\"}")
				.expect()
				.statusCode(200)
				.contentType(ContentType.JSON)
				.log().ifError()
				.body("status", is("insert"))
				.body("message", is("Content inserted successfully."))
				.when().post(new URL(context, CONTENT_REST_API).toExternalForm());
	}


	//TODO: FTEST: ContentRestServiceTest: Test adding/removing data via testing provider

}
