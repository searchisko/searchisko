/*
 * JBoss, Home of Professional Open Source
 * Copyright 2012 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 */
package org.searchisko.ftest.rest;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import com.jayway.restassured.http.ContentType;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.junit.InSequence;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.searchisko.ftest.DeploymentHelpers;
import org.searchisko.ftest.ProviderModel;

import static com.jayway.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;
import org.searchisko.persistence.jpa.model.Tag;

/**
 * Integration test for /tagging REST API.
 * <p/>
 * http://docs.jbossorg.apiary.io/#customtags *TODO*
 *
 * @author Jiri Mauritz
 * @see org.searchisko.api.rest.CustomTagRestService
 */
@RunWith(Arquillian.class)
public class CustomTagRestServiceTest {

	public static final String TAGGING_REST_API_BASE = DeploymentHelpers.DEFAULT_REST_VERSION + "tagging/";

	public static final String TAGGING_REST_API = TAGGING_REST_API_BASE + "{id}";
	public static final String TAGGING_REST_API_TYPE = TAGGING_REST_API_BASE + "type/{id}";

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

		// GET /tagging
		given().contentType(ContentType.JSON)
				.expect()
				.log().ifValidationFails()
				.statusCode(expStatus)
				.header("WWW-Authenticate", nullValue())
				.body(is("Required authorization {0}."))
				.when().get(new URL(context, TAGGING_REST_API_BASE).toExternalForm());

		// GET /rating/bad-id
		given().contentType(ContentType.JSON)
				.pathParam("id", "bad-id")
				.expect().statusCode(expStatus)
				.header("WWW-Authenticate", nullValue())
				.log().ifValidationFails()
				.when().get(new URL(context, TAGGING_REST_API).toExternalForm());

		// POST /rating/bad-id
		given().contentType(ContentType.JSON)
				.pathParam("id", "bad-id")
				.expect().statusCode(expStatus)
				.header("WWW-Authenticate", nullValue())
				.log().ifValidationFails()
				.when().post(new URL(context, TAGGING_REST_API).toExternalForm());

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

	@Test
	@InSequence(3)
	public void assertRefreshES() throws MalformedURLException {
		DeploymentHelpers.refreshES();
	}

	@Test
	@InSequence(5)
	public void assertCreateContributor() throws MalformedURLException {
		final Map<String, Object> typeSpecificCode = new HashMap<>();
		typeSpecificCode.put("jbossorg_username", contribUsername);

		String contributorCode = "TEST <test@test.com>";

		final Map<String, Object> params = new HashMap<>();
		params.put("code", contributorCode);
		params.put("email", "test@test.com");
		params.put("type_specific_code", typeSpecificCode);
		ContributorRestServiceTest.createContributor(context, params);
	}


	String contribUsername = "contributor1";

	String contribPassword = "password1";

	final String contentIdToTag = TYPE1 + "-" + contentId;

	@Test
	@InSequence(11)
	public void assertRate() throws MalformedURLException {
		Map<String, Object> tagging = new HashMap<>();
		tagging.put("tag", "label");

		given().contentType(ContentType.JSON)
				.auth().preemptive().basic(contribUsername, contribPassword)
				.pathParam("id", contentIdToTag)
				.body(tagging)
				.expect().statusCode(201)
				.log().ifValidationFails()
				.when().post(new URL(context, TAGGING_REST_API).toExternalForm());

		given().contentType(ContentType.JSON)
				.pathParam("id", contentIdToTag)
				.auth().preemptive().basic(contribUsername, contribPassword)
				.expect()
				.log().ifValidationFails()
				.statusCode(200)
				.contentType(ContentType.JSON)
				.body("tag", is("{\"tag\":[\"label\"]}"))
				.when().get(new URL(context, TAGGING_REST_API).toExternalForm());
	}

	@Test
	@InSequence(20)
	public void assertGetAllForType() throws MalformedURLException {
		given().contentType(ContentType.JSON)
				.queryParam("id", TYPE1)
				.auth().preemptive().basic(contribUsername, contribPassword)
				.expect()
				.log().ifValidationFails()
				.statusCode(200)
				.contentType(ContentType.JSON)
				.body("tag", is("{\"tag\":[\"label\"]}"))
				.when().get(new URL(context, TAGGING_REST_API_TYPE).toExternalForm());
	}

}
