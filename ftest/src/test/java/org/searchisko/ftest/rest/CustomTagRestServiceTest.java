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
import org.searchisko.ftest.DeploymentHelpers;
import org.searchisko.ftest.ProviderModel;

import com.jayway.restassured.http.ContentType;

import static com.jayway.restassured.RestAssured.given;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

/**
 * Integration test for /tagging REST API.
 * <p/>
 * http://docs.jbossorg.apiary.io/#customtags
 * 
 * @author Jiri Mauritz
 * @see org.searchisko.api.rest.CustomTagRestService
 */
@RunWith(Arquillian.class)
public class CustomTagRestServiceTest {

	public static final String TAGGING_REST_API_BASE = DeploymentHelpers.CURRENT_REST_VERSION + "tagging/";

	public static final String TAGGING_REST_API = TAGGING_REST_API_BASE + "{id}";
	public static final String TAGGING_REST_API_DELETE_ALL = TAGGING_REST_API_BASE + "{id}/_all";
	public static final String TAGGING_REST_API_TYPE = TAGGING_REST_API_BASE + "type/{id}";

	@Deployment(testable = false)
	public static WebArchive createDeployment() throws IOException {
		return DeploymentHelpers.createDeployment();
	}

	@ArquillianResource
	URL context;

	public static final String TYPE1 = "provider1_blog";

	static ProviderModel provider1 = new ProviderModel("provider1", "password");

	static final String contentId = "test-id";
	static final String contentId2 = "test-id2";

	@Test
	@InSequence(1)
	public void setupContent() throws MalformedURLException {
		provider1.addContentType(TYPE1, "blogpost", true);
		ProviderRestServiceTest.createNewProvider(context, provider1);

		Map<String, Object> content = new HashMap<>();
		content.put("data", "test");
		ContentRestServiceTest.createOrUpdateContent(context, provider1, TYPE1, contentId, content);

		ContentRestServiceTest.createOrUpdateContent(context, provider1, TYPE1, contentId2, content);

		DeploymentHelpers.refreshES();
	}

	@Test
	@InSequence(2)
	public void setupContributor() throws MalformedURLException {
		final Map<String, Object> typeSpecificCode = new HashMap<>();
		typeSpecificCode.put("jbossorg_username", contribUsername);

		String contributorCode = "TEST <test@test.com>";

		final Map<String, Object> params = new HashMap<>();
		params.put("code", contributorCode);
		params.put("email", "test@test.com");
		params.put("type_specific_code", typeSpecificCode);
		ContributorRestServiceTest.createContributor(context, params);

	}

	@Test
	@InSequence(3)
	public void setupContributor2() throws MalformedURLException {
		final Map<String, Object> typeSpecificCode = new HashMap<>();
		typeSpecificCode.put("jbossorg_username", contrib2Username);

		String contributorCode = "TEST22 <test22@test.com>";

		final Map<String, Object> params = new HashMap<>();
		params.put("code", contributorCode);
		params.put("email", "test22@test.com");
		params.put("type_specific_code", typeSpecificCode);
		ContributorRestServiceTest.createContributor(context, params);

	}

	@Test
	@InSequence(4)
	public void setupContributorUnauthorized() throws MalformedURLException {
		final Map<String, Object> typeSpecificCode = new HashMap<>();
		typeSpecificCode.put("jbossorg_username", contribUnauthorizedUsername);

		String contributorCode = "TEST2 <test2@test.com>";

		final Map<String, Object> params = new HashMap<>();
		params.put("code", contributorCode);
		params.put("email", "test2@test.com");
		params.put("type_specific_code", typeSpecificCode);
		ContributorRestServiceTest.createContributor(context, params);
	}

	final String contribUsername = "contributor1";
	final String contribPassword = "password1";

	final String contrib2Username = "contributor2";
	final String contrib2Password = "password2";

	final String contribUnauthorizedUsername = "contributor";
	final String contribUnauthorizedPassword = "password";

	final String contentIdToTag = TYPE1 + "-" + contentId;
	final String contentId2ToTag = TYPE1 + "-" + contentId2;

	@Test
	@InSequence(5)
	public void assertNotAuthenticated() throws MalformedURLException {
		int expStatus = 403;

		// GET /tagging/existing-id
		given().contentType(ContentType.JSON).pathParam("id", contentIdToTag).expect().log().ifValidationFails()
				.statusCode(expStatus).header("WWW-Authenticate", nullValue()).body(is("Required authorization {0}.")).when()
				.get(new URL(context, TAGGING_REST_API).toExternalForm());

		// POST /rating/existing-id
		given().contentType(ContentType.JSON).pathParam("id", contentIdToTag).content("{ \"tag\" : \"tag1\"}").expect()
				.statusCode(expStatus).header("WWW-Authenticate", nullValue()).log().ifValidationFails().when()
				.post(new URL(context, TAGGING_REST_API).toExternalForm());

		// DELETE concrete label
		given().contentType(ContentType.JSON).pathParam("id", contentIdToTag).content("{ \"tag\" : \"tag1\"}").expect()
				.log().ifValidationFails().statusCode(expStatus).header("WWW-Authenticate", nullValue())
				.body(is("Required authorization {0}.")).when()
				.delete(new URL(context, TAGGING_REST_API_DELETE_ALL).toExternalForm());

		// DELETE _all
		given().contentType(ContentType.JSON).pathParam("id", contentIdToTag).expect().log().ifValidationFails()
				.statusCode(expStatus).header("WWW-Authenticate", nullValue()).body(is("Required authorization {0}.")).when()
				.delete(new URL(context, TAGGING_REST_API_DELETE_ALL).toExternalForm());
	}

	@Test
	@InSequence(5)
	public void assertNotAuthorized() throws MalformedURLException {
		int expStatus = 403;

		// GET /tagging/existing-id
		given().contentType(ContentType.JSON).auth().preemptive()
				.basic(contribUnauthorizedUsername, contribUnauthorizedPassword).pathParam("id", contentIdToTag).expect().log()
				.ifValidationFails().statusCode(expStatus).when().get(new URL(context, TAGGING_REST_API).toExternalForm());

		// POST /rating/existing-id
		given().contentType(ContentType.JSON).auth().preemptive()
				.basic(contribUnauthorizedUsername, contribUnauthorizedPassword).pathParam("id", contentIdToTag)
				.content("{ \"tag\" : \"tag1\"}").expect().statusCode(expStatus).log().ifValidationFails().when()
				.post(new URL(context, TAGGING_REST_API).toExternalForm());

		// DELETE concrete label
		given().contentType(ContentType.JSON).auth().preemptive()
				.basic(contribUnauthorizedUsername, contribUnauthorizedPassword).pathParam("id", contentIdToTag)
				.content("{ \"tag\" : \"tag1\"}").expect().log().ifValidationFails().statusCode(expStatus).when()
				.delete(new URL(context, TAGGING_REST_API_DELETE_ALL).toExternalForm());

		// DELETE _all
		given().contentType(ContentType.JSON).auth().preemptive()
				.basic(contribUnauthorizedUsername, contribUnauthorizedPassword).pathParam("id", contentIdToTag).expect().log()
				.ifValidationFails().statusCode(expStatus).when()
				.delete(new URL(context, TAGGING_REST_API_DELETE_ALL).toExternalForm());

	}

	@Test
	@InSequence(7)
	public void assertReadCreateTag_UnknownContentId() throws MalformedURLException {
		int expStatus = 404;

		// GET /rating/bad-id
		given().contentType(ContentType.JSON).auth().preemptive().basic(contribUsername, contribPassword)
				.pathParam("id", "bad-id").expect().statusCode(expStatus).header("WWW-Authenticate", nullValue()).log()
				.ifValidationFails().when().get(new URL(context, TAGGING_REST_API).toExternalForm());

		// POST /rating/bad-id
		given().contentType(ContentType.JSON).auth().preemptive().basic(contribUsername, contribPassword)
				.pathParam("id", TYPE1 + "-45345gf").content("{ \"tag\" : \"tag1\"}").expect().statusCode(expStatus)
				.header("WWW-Authenticate", nullValue()).log().ifValidationFails().when()
				.post(new URL(context, TAGGING_REST_API).toExternalForm());

	}

	@Test
	@InSequence(11)
	public void assertCreateReadTag_authenticatedContributor_tags_admin() throws MalformedURLException {
		Map<String, Object> tagging = new HashMap<>();
		tagging.put("tag", "label");

		given().contentType(ContentType.JSON).auth().preemptive().basic(contribUsername, contribPassword)
				.pathParam("id", contentIdToTag).body(tagging).expect().statusCode(201).log().ifValidationFails().when()
				.post(new URL(context, TAGGING_REST_API).toExternalForm());

		// code is 200 if tag exists already
		given().contentType(ContentType.JSON).auth().preemptive().basic(contribUsername, contribPassword)
				.pathParam("id", contentIdToTag).body(tagging).expect().statusCode(200).log().ifValidationFails().when()
				.post(new URL(context, TAGGING_REST_API).toExternalForm());

		// insert second label
		tagging.put("tag", "label_2");
		given().contentType(ContentType.JSON).auth().preemptive().basic(contribUsername, contribPassword)
				.pathParam("id", contentIdToTag).body(tagging).expect().statusCode(201).log().ifValidationFails().when()
				.post(new URL(context, TAGGING_REST_API).toExternalForm());

		// assert read
		given().contentType(ContentType.JSON).pathParam("id", contentIdToTag).auth().preemptive()
				.basic(contribUsername, contribPassword).expect().log().ifValidationFails().statusCode(200)
				.contentType(ContentType.JSON).body("tag[0]", is("label")).body("tag[1]", is("label_2")).when()
				.get(new URL(context, TAGGING_REST_API).toExternalForm());

		// TODO test that sys_tags of content is updated

	}

	@Test
	@InSequence(12)
	public void assertCreateReadTag_authenticatedContributor_admin() throws MalformedURLException {
		Map<String, Object> tagging = new HashMap<>();
		tagging.put("tag", "label_id2");

		given().contentType(ContentType.JSON).auth().preemptive().basic(contrib2Username, contrib2Password)
				.pathParam("id", contentId2ToTag).body(tagging).expect().statusCode(201).log().ifValidationFails().when()
				.post(new URL(context, TAGGING_REST_API).toExternalForm());

		// insert second label
		tagging.put("tag", "label_id2_2");
		given().contentType(ContentType.JSON).auth().preemptive().basic(contrib2Username, contrib2Password)
				.pathParam("id", contentId2ToTag).body(tagging).expect().statusCode(201).log().ifValidationFails().when()
				.post(new URL(context, TAGGING_REST_API).toExternalForm());

		// assert read by other user
		given().contentType(ContentType.JSON).pathParam("id", contentId2ToTag).auth().preemptive()
				.basic(contribUsername, contribPassword).expect().log().ifValidationFails().statusCode(200)
				.contentType(ContentType.JSON).body("tag[0]", is("label_id2")).body("tag[1]", is("label_id2_2")).when()
				.get(new URL(context, TAGGING_REST_API).toExternalForm());
	}

	@Test
	@InSequence(15)
	public void assertGetAllForType() throws MalformedURLException {
		given().contentType(ContentType.JSON).pathParam("id", TYPE1).auth().preemptive()
				.basic(contribUsername, contribPassword).expect().log().ifValidationFails().statusCode(200)
				.contentType(ContentType.JSON).body("tag[0]", is("label")).body("tag[1]", is("label_2"))
				.body("tag[2]", is("label_id2")).body("tag[3]", is("label_id2_2")).when()
				.get(new URL(context, TAGGING_REST_API_TYPE).toExternalForm());
	}

	@Test
	@InSequence(20)
	public void assertDeleteTag() throws MalformedURLException {
		Map<String, Object> tagging = new HashMap<>();
		tagging.put("tag", "label");

		given().contentType(ContentType.JSON).auth().preemptive().basic(contribUsername, contribPassword)
				.pathParam("id", contentIdToTag).body(tagging).expect().statusCode(200).log().ifValidationFails().when()
				.delete(new URL(context, TAGGING_REST_API).toExternalForm());

		// assert read
		given().contentType(ContentType.JSON).pathParam("id", contentIdToTag).auth().preemptive()
				.basic(contribUsername, contribPassword).expect().log().ifValidationFails().statusCode(200)
				.contentType(ContentType.JSON).body("tag[0]", is("label_2")).when()
				.get(new URL(context, TAGGING_REST_API).toExternalForm());

		// TODO test that sys_tags of content is updated

	}

	@Test
	@InSequence(21)
	public void assertDeleteall_authenticatedContributor_admin() throws MalformedURLException {

		given().contentType(ContentType.JSON).auth().preemptive().basic(contrib2Username, contrib2Password)
				.pathParam("id", contentId2ToTag).expect().statusCode(200).log().ifValidationFails().when()
				.delete(new URL(context, TAGGING_REST_API_DELETE_ALL).toExternalForm());

		// assert read by other user
		given().contentType(ContentType.JSON).pathParam("id", contentId2ToTag).auth().preemptive()
				.basic(contribUsername, contribPassword).expect().log().ifValidationFails().statusCode(404).when()
				.get(new URL(context, TAGGING_REST_API).toExternalForm());

		// TODO test that sys_tags of content is updated
	}

}
