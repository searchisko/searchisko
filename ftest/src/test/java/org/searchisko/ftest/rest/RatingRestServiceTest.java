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

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.junit.InSequence;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.searchisko.api.ContentObjectFields;
import org.searchisko.api.service.ProviderService;
import org.searchisko.ftest.DeploymentHelpers;
import org.searchisko.ftest.ProviderModel;

import com.jayway.restassured.http.ContentType;

import static com.jayway.restassured.RestAssured.given;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

/**
 * Integration test for /rating REST API.
 * <p/>
 * http://docs.jbossorg.apiary.io/#personalizedcontentratingapi
 * 
 * @author Libor Krzyzanek
 * @author Vlastimil Elias (velias at redhat dot com)
 * @see org.searchisko.api.rest.RatingRestService
 */
@RunWith(Arquillian.class)
public class RatingRestServiceTest {

	public static final String RATING_REST_API_BASE = DeploymentHelpers.CURRENT_REST_VERSION + "rating/";

	public static final String RATING_REST_API = RATING_REST_API_BASE + "{id}";

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
		given().contentType(ContentType.JSON).expect().log().ifValidationFails().statusCode(expStatus)
				.header("WWW-Authenticate", nullValue()).body(is("Required authorization {0}.")).when()
				.get(new URL(context, RATING_REST_API_BASE).toExternalForm());

		// GET /rating/bad-id
		given().contentType(ContentType.JSON).pathParam("id", "bad-id").expect().statusCode(expStatus)
				.header("WWW-Authenticate", nullValue()).log().ifValidationFails().when()
				.get(new URL(context, RATING_REST_API).toExternalForm());

		// POST /rating/bad-id
		given().contentType(ContentType.JSON).pathParam("id", "bad-id").expect().statusCode(expStatus)
				.header("WWW-Authenticate", nullValue()).log().ifValidationFails().when()
				.get(new URL(context, RATING_REST_API).toExternalForm());

	}

	public static final String TYPE1 = "provider1_blog";

	static ProviderModel provider1 = new ProviderModel("provider1", "password");

	static final String contentId = "test-id";

	@Test
	@InSequence(1)
	public void setupCreateProvider1BlogPost() throws MalformedURLException {
		provider1.addContentType(TYPE1, "blogpost", true);

		ProviderRestServiceTest.createNewProvider(context, provider1);
	}

	@Test
	@InSequence(2)
	public void setupPushContentWithId() throws MalformedURLException {
		Map<String, Object> content = new HashMap<>();
		content.put("data", "test");
		ContentRestServiceTest.createOrUpdateContent(context, provider1, TYPE1, contentId, content);

		DeploymentHelpers.refreshES();
	}

	@Test
	@InSequence(5)
	public void setupCreateContributor() throws MalformedURLException {
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

	final String idToRate = TYPE1 + "-" + contentId;

	@Test
	@InSequence(11)
	public void assertRate_Get() throws MalformedURLException {
		Map<String, Object> rating = new HashMap<>();
		rating.put("rating", 4);

		given().contentType(ContentType.JSON).auth().preemptive().basic(contribUsername, contribPassword)
				.pathParam("id", idToRate).body(rating).expect().statusCode(200).log().ifValidationFails()
				.contentType(ContentType.JSON).body("sys_rating_avg", is(new Float(4.0))).body("sys_rating_num", is(1)).when()
				.post(new URL(context, RATING_REST_API).toExternalForm());

		given().contentType(ContentType.JSON).pathParam("id", idToRate).auth().preemptive()
				.basic(contribUsername, contribPassword).expect().log().ifValidationFails().statusCode(200)
				.contentType(ContentType.JSON).body("rating", is(4)).when()
				.get(new URL(context, RATING_REST_API).toExternalForm());
	}

	@Test
	@InSequence(20)
	public void assertGetAll() throws MalformedURLException {
		given().contentType(ContentType.JSON).queryParam("id", idToRate).auth().preemptive()
				.basic(contribUsername, contribPassword).expect().log().ifValidationFails().statusCode(200)
				.contentType(ContentType.JSON).body(idToRate + ".rating", is(4)).when()
				.get(new URL(context, RATING_REST_API_BASE).toExternalForm());
	}

	// #191 - reflect document level security
	@Test
	@InSequence(30)
	public void setup_dls_fail() throws MalformedURLException {
		Map<String, Object> content = new HashMap<>();
		content.put("data", "test");
		content.put(ContentObjectFields.SYS_VISIBLE_FOR_ROLES, "unknownrole");
		ContentRestServiceTest.createOrUpdateContent(context, provider1, TYPE1, contentId, content);
		DeploymentHelpers.refreshES();
	}

	@Test
	@InSequence(31)
	public void assertRate_dls_fail() throws MalformedURLException {
		Map<String, Object> rating = new HashMap<>();
		rating.put("rating", 4);

		given().contentType(ContentType.JSON).auth().preemptive().basic(contribUsername, contribPassword)
				.pathParam("id", idToRate).body(rating).expect().statusCode(403).log().ifValidationFails().when()
				.post(new URL(context, RATING_REST_API).toExternalForm());
	}

	@Test
	@InSequence(32)
	public void setup_dls_ok() throws MalformedURLException {
		Map<String, Object> content = new HashMap<>();
		content.put("data", "test");
		content.put(ContentObjectFields.SYS_VISIBLE_FOR_ROLES, "contributor");
		ContentRestServiceTest.createOrUpdateContent(context, provider1, TYPE1, contentId, content);
		DeploymentHelpers.refreshES();
	}

	@Test
	@InSequence(33)
	public void assertRate_dls_ok() throws MalformedURLException {
		Map<String, Object> rating = new HashMap<>();
		rating.put("rating", 4);

		given().contentType(ContentType.JSON).auth().preemptive().basic(contribUsername, contribPassword)
				.pathParam("id", idToRate).body(rating).expect().statusCode(200).log().ifValidationFails()
				.contentType(ContentType.JSON).body("sys_rating_avg", is(new Float(4.0))).body("sys_rating_num", is(1)).when()
				.post(new URL(context, RATING_REST_API).toExternalForm());

	}

	// #191 - reflect type level security
	@Test
	@InSequence(40)
	public void setup_tls_fail() throws MalformedURLException {
		Map<String, Object> content = new HashMap<>();
		content.put("data", "test");
		ContentRestServiceTest.createOrUpdateContent(context, provider1, TYPE1, contentId, content);
		DeploymentHelpers.refreshES();

		Map<String, Object> ct = provider1.getContentType(TYPE1);
		ct.put(ProviderService.SYS_VISIBLE_FOR_ROLES, "unknownrole");
		ProviderRestServiceTest.createOrUpdateProvider(context, provider1);
	}

	@Test
	@InSequence(41)
	public void assertRate_tls_fail() throws MalformedURLException {
		Map<String, Object> rating = new HashMap<>();
		rating.put("rating", 4);

		given().contentType(ContentType.JSON).auth().preemptive().basic(contribUsername, contribPassword)
				.pathParam("id", idToRate).body(rating).expect().statusCode(403).log().ifValidationFails().when()
				.post(new URL(context, RATING_REST_API).toExternalForm());
	}

	@Test
	@InSequence(42)
	public void setup_tls_ok() throws MalformedURLException {
		Map<String, Object> ct = provider1.getContentType(TYPE1);
		ct.put(ProviderService.SYS_VISIBLE_FOR_ROLES, "contributor");
		ProviderRestServiceTest.createOrUpdateProvider(context, provider1);
	}

	@Test
	@InSequence(43)
	public void assertRate_tls_ok() throws MalformedURLException {
		Map<String, Object> rating = new HashMap<>();
		rating.put("rating", 4);

		given().contentType(ContentType.JSON).auth().preemptive().basic(contribUsername, contribPassword)
				.pathParam("id", idToRate).body(rating).expect().statusCode(200).log().ifValidationFails()
				.contentType(ContentType.JSON).body("sys_rating_avg", is(new Float(4.0))).body("sys_rating_num", is(1)).when()
				.post(new URL(context, RATING_REST_API).toExternalForm());

	}

}
