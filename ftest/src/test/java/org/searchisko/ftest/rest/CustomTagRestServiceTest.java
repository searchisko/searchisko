/*
 * JBoss, Home of Professional Open Source
 * Copyright 2012 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 */
package org.searchisko.ftest.rest;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hamcrest.Matchers;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.junit.InSequence;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.searchisko.api.ContentObjectFields;
import org.searchisko.ftest.DeploymentHelpers;
import org.searchisko.ftest.ProviderModel;

import com.jayway.restassured.http.ContentType;
import com.jayway.restassured.path.json.JsonPath;

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
	public static final String TYPE2 = "provider1_issue";

	static ProviderModel provider1 = new ProviderModel("provider1", "password");

	static final String contentId1 = "test-id";
	static final String contentId2 = "test-id2";
	static final String contentId3 = "test-id3";

	final String contribTagsAdminUsername = "contributor1";
	final String contribTagsAdminPassword = "password1";

	final String contribAdminUsername = "contributor2";
	final String contribAdminPassword = "password2";

	final String contribTagsAdminTypeOtherUsername = "contributor3";
	final String contribTagsAdminTypeOtherPassword = "password3";

	final String contribTagsAdminTypeCorrectUsername = "contributor4";
	final String contribTagsAdminTypeCorrectPassword = "password4";

	final String contribUnauthorizedUsername = "contributor";
	final String contribUnauthorizedPassword = "password";

	final String contentId1ToTag = TYPE1 + "-" + contentId1;
	final String contentId2ToTag = TYPE1 + "-" + contentId2;
	final String contentId3ToTag = TYPE1 + "-" + contentId3;

	final String contentId1ToTag_type2 = TYPE2 + "-" + contentId1;

	final String contentIdToTagNonexistingDoc = TYPE1 + "-unknown23";
	final String contentIdToTagNonexistingType = "unknowntype" + "-unknown23";

	static final String TAG_PROVIDER_1 = "labe_orig_1";
	static final String TAG_PROVIDER_2 = "labe_orig_2";

	@Test
	@InSequence(1)
	public void setupContent() throws MalformedURLException {
		provider1.addContentType(TYPE1, "blogpost", true);
		provider1.addContentType(TYPE2, "issue", false);
		ProviderRestServiceTest.createNewProvider(context, provider1);

		Map<String, Object> content = new HashMap<>();
		content.put("data", "test1");
		List<String> tags = new ArrayList<>();
		tags.add(TAG_PROVIDER_1);
		tags.add(TAG_PROVIDER_2);
		content.put(ContentObjectFields.TAGS, tags);
		ContentRestServiceTest.createOrUpdateContent(context, provider1, TYPE1, contentId1, content);

		// second document is without tags
		content.put("data", "test2");
		content.remove(ContentObjectFields.TAGS);
		ContentRestServiceTest.createOrUpdateContent(context, provider1, TYPE1, contentId2, content);

		// third document is without tags
		content.put("data", "test3");
		content.remove(ContentObjectFields.TAGS);
		ContentRestServiceTest.createOrUpdateContent(context, provider1, TYPE1, contentId3, content);

		// one documnt for other type
		ContentRestServiceTest.createOrUpdateContent(context, provider1, TYPE2, contentId1, content);

		DeploymentHelpers.refreshES();
	}

	@Test
	@InSequence(2)
	public void setupContributors() throws MalformedURLException {
		setupContributor(contribAdminUsername);
		setupContributor(contribTagsAdminUsername);
		setupContributor(contribTagsAdminTypeCorrectUsername);
		setupContributor(contribTagsAdminTypeOtherUsername);
		setupContributor(contribUnauthorizedUsername);

	}

	private String setupContributor(String username) throws MalformedURLException {
		final Map<String, Object> typeSpecificCode = new HashMap<>();
		typeSpecificCode.put("jbossorg_username", username);

		String contributorCode = username + " <" + username + "@test.com>";

		final Map<String, Object> params = new HashMap<>();
		params.put("code", contributorCode);
		params.put("email", username + "@test.com");
		params.put("type_specific_code", typeSpecificCode);
		ContributorRestServiceTest.createContributor(context, params);
		return contributorCode;
	}

	@Test
	@InSequence(5)
	public void assertNotAuthenticated() throws MalformedURLException {
		int expStatus = 403;

		// GET /tagging/existing-id
		given().contentType(ContentType.JSON).pathParam("id", contentId1ToTag).expect().log().ifValidationFails()
				.statusCode(expStatus).header("WWW-Authenticate", nullValue()).body(is("Required authorization {0}.")).when()
				.get(new URL(context, TAGGING_REST_API).toExternalForm());

		// GET /tagging/type/type1
		given().contentType(ContentType.JSON).pathParam("id", TYPE1).expect().log().ifValidationFails()
				.statusCode(expStatus).header("WWW-Authenticate", nullValue()).body(is("Required authorization {0}.")).when()
				.get(new URL(context, TAGGING_REST_API_TYPE).toExternalForm());

		// POST /rating/existing-id
		given().contentType(ContentType.JSON).pathParam("id", contentId1ToTag).content("{ \"tag\" : \"tag1\"}").expect()
				.statusCode(expStatus).header("WWW-Authenticate", nullValue()).log().ifValidationFails().when()
				.post(new URL(context, TAGGING_REST_API).toExternalForm());

		// DELETE concrete label
		given().contentType(ContentType.JSON).pathParam("id", contentId1ToTag).content("{ \"tag\" : \"tag1\"}").expect()
				.log().ifValidationFails().statusCode(expStatus).header("WWW-Authenticate", nullValue())
				.body(is("Required authorization {0}.")).when()
				.delete(new URL(context, TAGGING_REST_API_DELETE_ALL).toExternalForm());

		// DELETE _all
		given().contentType(ContentType.JSON).pathParam("id", contentId1ToTag).expect().log().ifValidationFails()
				.statusCode(expStatus).header("WWW-Authenticate", nullValue()).body(is("Required authorization {0}.")).when()
				.delete(new URL(context, TAGGING_REST_API_DELETE_ALL).toExternalForm());
	}

	@Test
	@InSequence(5)
	public void assertNotAuthorized() throws MalformedURLException {
		int expStatus = 403;

		// GET /tagging/existing-id
		given().contentType(ContentType.JSON).auth().preemptive()
				.basic(contribUnauthorizedUsername, contribUnauthorizedPassword).pathParam("id", contentId1ToTag).expect()
				.log().ifValidationFails().statusCode(expStatus).when()
				.get(new URL(context, TAGGING_REST_API).toExternalForm());

		// GET /tagging/type/type1
		given().contentType(ContentType.JSON).auth().preemptive()
				.basic(contribUnauthorizedUsername, contribUnauthorizedPassword).pathParam("id", TYPE1).expect().log()
				.ifValidationFails().statusCode(expStatus).when().get(new URL(context, TAGGING_REST_API_TYPE).toExternalForm());

		// POST /rating/existing-id
		given().contentType(ContentType.JSON).auth().preemptive()
				.basic(contribUnauthorizedUsername, contribUnauthorizedPassword).pathParam("id", contentId1ToTag)
				.content("{ \"tag\" : \"tag1\"}").expect().statusCode(expStatus).log().ifValidationFails().when()
				.post(new URL(context, TAGGING_REST_API).toExternalForm());

		// DELETE concrete label
		given().contentType(ContentType.JSON).auth().preemptive()
				.basic(contribUnauthorizedUsername, contribUnauthorizedPassword).pathParam("id", contentId1ToTag)
				.content("{ \"tag\" : \"tag1\"}").expect().log().ifValidationFails().statusCode(expStatus).when()
				.delete(new URL(context, TAGGING_REST_API_DELETE_ALL).toExternalForm());

		// DELETE _all
		given().contentType(ContentType.JSON).auth().preemptive()
				.basic(contribUnauthorizedUsername, contribUnauthorizedPassword).pathParam("id", contentId1ToTag).expect()
				.log().ifValidationFails().statusCode(expStatus).when()
				.delete(new URL(context, TAGGING_REST_API_DELETE_ALL).toExternalForm());
	}

	@Test
	@InSequence(6)
	public void assertAuthorizedToOtherTypeOnly() throws MalformedURLException {
		int expStatus = 403;

		// this call is here to be sure the user is setup correctly
		given().contentType(ContentType.JSON).auth().preemptive()
				.basic(contribTagsAdminTypeOtherUsername, contribTagsAdminTypeOtherPassword).pathParam("id", TYPE2).expect()
				.log().ifValidationFails().statusCode(404).when().get(new URL(context, TAGGING_REST_API_TYPE).toExternalForm());

		// GET /tagging/existing-id
		given().contentType(ContentType.JSON).auth().preemptive()
				.basic(contribTagsAdminTypeOtherUsername, contribTagsAdminTypeOtherPassword).pathParam("id", contentId1ToTag)
				.expect().log().ifValidationFails().statusCode(expStatus).when()
				.get(new URL(context, TAGGING_REST_API).toExternalForm());

		// GET /tagging/type/type1
		given().contentType(ContentType.JSON).auth().preemptive()
				.basic(contribTagsAdminTypeOtherUsername, contribTagsAdminTypeOtherPassword).pathParam("id", TYPE1).expect()
				.log().ifValidationFails().statusCode(expStatus).when()
				.get(new URL(context, TAGGING_REST_API_TYPE).toExternalForm());

		// POST /rating/existing-id
		given().contentType(ContentType.JSON).auth().preemptive()
				.basic(contribTagsAdminTypeOtherUsername, contribTagsAdminTypeOtherPassword).pathParam("id", contentId1ToTag)
				.content("{ \"tag\" : \"tag1\"}").expect().statusCode(expStatus).log().ifValidationFails().when()
				.post(new URL(context, TAGGING_REST_API).toExternalForm());

		// DELETE concrete label
		given().contentType(ContentType.JSON).auth().preemptive()
				.basic(contribTagsAdminTypeOtherUsername, contribTagsAdminTypeOtherPassword).pathParam("id", contentId1ToTag)
				.content("{ \"tag\" : \"tag1\"}").expect().log().ifValidationFails().statusCode(expStatus).when()
				.delete(new URL(context, TAGGING_REST_API_DELETE_ALL).toExternalForm());

		// DELETE _all
		given().contentType(ContentType.JSON).auth().preemptive()
				.basic(contribTagsAdminTypeOtherUsername, contribTagsAdminTypeOtherPassword).pathParam("id", contentId1ToTag)
				.expect().log().ifValidationFails().statusCode(expStatus).when()
				.delete(new URL(context, TAGGING_REST_API_DELETE_ALL).toExternalForm());
	}

	@Test
	@InSequence(7)
	public void assertCreateReadTag_UnknownContent() throws MalformedURLException {
		int expStatus = 404;

		// GET /rating/bad-id
		given().contentType(ContentType.JSON).auth().preemptive().basic(contribTagsAdminUsername, contribTagsAdminPassword)
				.pathParam("id", contentIdToTagNonexistingDoc).expect().statusCode(expStatus).log().ifValidationFails().when()
				.get(new URL(context, TAGGING_REST_API).toExternalForm());

		given().contentType(ContentType.JSON).auth().preemptive().basic(contribTagsAdminUsername, contribTagsAdminPassword)
				.pathParam("id", contentIdToTagNonexistingType).expect().statusCode(expStatus).log().ifValidationFails().when()
				.get(new URL(context, TAGGING_REST_API).toExternalForm());

		// POST /rating/bad-id
		given().contentType(ContentType.JSON).auth().preemptive().basic(contribTagsAdminUsername, contribTagsAdminPassword)
				.pathParam("id", contentIdToTagNonexistingDoc).content("{ \"tag\" : \"tag1\"}").expect().statusCode(expStatus)
				.log().ifValidationFails().when().post(new URL(context, TAGGING_REST_API).toExternalForm());

		given().contentType(ContentType.JSON).auth().preemptive().basic(contribTagsAdminUsername, contribTagsAdminPassword)
				.pathParam("id", contentIdToTagNonexistingType).content("{ \"tag\" : \"tag1\"}").expect().statusCode(expStatus)
				.log().ifValidationFails().when().post(new URL(context, TAGGING_REST_API).toExternalForm());

	}

	@Test
	@InSequence(11)
	public void assertCreateReadTag_authenticatedContributor_tags_admin() throws MalformedURLException {
		Map<String, Object> tagging = new HashMap<>();
		tagging.put("tag", "label");

		given().contentType(ContentType.JSON).auth().preemptive().basic(contribTagsAdminUsername, contribTagsAdminPassword)
				.pathParam("id", contentId1ToTag).body(tagging).expect().statusCode(201).log().ifValidationFails().when()
				.post(new URL(context, TAGGING_REST_API).toExternalForm());

		// code is 200 if tag exists already
		given().contentType(ContentType.JSON).auth().preemptive().basic(contribTagsAdminUsername, contribTagsAdminPassword)
				.pathParam("id", contentId1ToTag).body(tagging).expect().statusCode(200).log().ifValidationFails().when()
				.post(new URL(context, TAGGING_REST_API).toExternalForm());

		// insert second label
		tagging.put("tag", "label_2");
		given().contentType(ContentType.JSON).auth().preemptive().basic(contribTagsAdminUsername, contribTagsAdminPassword)
				.pathParam("id", contentId1ToTag).body(tagging).expect().statusCode(201).log().ifValidationFails().when()
				.post(new URL(context, TAGGING_REST_API).toExternalForm());

		// assert read
		given().contentType(ContentType.JSON).pathParam("id", contentId1ToTag).auth().preemptive()
				.basic(contribTagsAdminUsername, contribTagsAdminPassword).expect().log().ifValidationFails().statusCode(200)
				.contentType(ContentType.JSON).body("tag[0]", is("label")).body("tag[1]", is("label_2")).when()
				.get(new URL(context, TAGGING_REST_API).toExternalForm());

		// test that sys_tags of content is updated (two provider tags should be here)
		DeploymentHelpers.refreshES();
		List<String> st = getSysTagsForContent(contentId1ToTag);
		Assert.assertNotNull(st);
		Assert.assertEquals(4, st.size());
		Assert.assertTrue(st.contains(TAG_PROVIDER_1));
		Assert.assertTrue(st.contains(TAG_PROVIDER_2));
		Assert.assertTrue(st.contains("label"));
		Assert.assertTrue(st.contains("label_2"));
	}

	@Test
	@InSequence(12)
	public void assertCreateReadTag_authenticatedContributor_tags_admin_type() throws MalformedURLException {
		Map<String, Object> tagging = new HashMap<>();
		tagging.put("tag", "label");

		given().contentType(ContentType.JSON).auth().preemptive()
				.basic(contribTagsAdminTypeCorrectUsername, contribTagsAdminTypeCorrectPassword)
				.pathParam("id", contentId3ToTag).body(tagging).expect().statusCode(201).log().ifValidationFails().when()
				.post(new URL(context, TAGGING_REST_API).toExternalForm());

		// assert read
		given().contentType(ContentType.JSON).pathParam("id", contentId3ToTag).auth().preemptive()
				.basic(contribTagsAdminTypeCorrectUsername, contribTagsAdminTypeCorrectPassword).expect().log()
				.ifValidationFails().statusCode(200).contentType(ContentType.JSON).body("tag[0]", is("label")).when()
				.get(new URL(context, TAGGING_REST_API).toExternalForm());

		// test that sys_tags of content is updated
		DeploymentHelpers.refreshES();
		List<String> st = getSysTagsForContent(contentId3ToTag);
		Assert.assertNotNull(st);
		Assert.assertEquals(1, st.size());
		Assert.assertTrue(st.contains("label"));
	}

	@Test
	@InSequence(13)
	public void assertCreateReadTag_authenticatedContributor_admin() throws MalformedURLException {
		Map<String, Object> tagging = new HashMap<>();
		tagging.put("tag", "label_id2");

		given().contentType(ContentType.JSON).auth().preemptive().basic(contribAdminUsername, contribAdminPassword)
				.pathParam("id", contentId2ToTag).body(tagging).expect().statusCode(201).log().ifValidationFails().when()
				.post(new URL(context, TAGGING_REST_API).toExternalForm());

		// insert second label
		tagging.put("tag", "label_id2_2");
		given().contentType(ContentType.JSON).auth().preemptive().basic(contribAdminUsername, contribAdminPassword)
				.pathParam("id", contentId2ToTag).body(tagging).expect().statusCode(201).log().ifValidationFails().when()
				.post(new URL(context, TAGGING_REST_API).toExternalForm());

		// assert read by other user
		given().contentType(ContentType.JSON).pathParam("id", contentId2ToTag).auth().preemptive()
				.basic(contribTagsAdminUsername, contribTagsAdminPassword).expect().log().ifValidationFails().statusCode(200)
				.contentType(ContentType.JSON).body("tag[0]", is("label_id2")).body("tag[1]", is("label_id2_2")).when()
				.get(new URL(context, TAGGING_REST_API).toExternalForm());

		// test that sys_tags of content is updated (two provider tags should be here)
		DeploymentHelpers.refreshES();
		List<String> st = getSysTagsForContent(contentId2ToTag);
		Assert.assertNotNull(st);
		Assert.assertEquals(2, st.size());
		Assert.assertTrue(st.contains("label_id2"));
		Assert.assertTrue(st.contains("label_id2_2"));
	}

	@Test
	@InSequence(15)
	public void assertGetAllForType_UnknownType() throws MalformedURLException {

		given().contentType(ContentType.JSON).pathParam("id", "unknowntype").auth().preemptive()
				.basic(contribTagsAdminUsername, contribTagsAdminPassword).expect().log().ifValidationFails().statusCode(404)
				.when().get(new URL(context, TAGGING_REST_API_TYPE).toExternalForm());
	}

	@Test
	@InSequence(16)
	public void assertGetAllForType_Empty() throws MalformedURLException {

		// get everything from type 2 where no any label is added
		given().contentType(ContentType.JSON).pathParam("id", TYPE2).auth().preemptive()
				.basic(contribTagsAdminUsername, contribTagsAdminPassword).expect().log().ifValidationFails().statusCode(404)
				.when().get(new URL(context, TAGGING_REST_API_TYPE).toExternalForm());

	}

	@Test
	@InSequence(17)
	public void assertGetAllForType() throws MalformedURLException {

		// insert same tag to second content to be sure duplicities are removed from response
		Map<String, Object> tagging = new HashMap<>();
		tagging.put("tag", "label");
		given().contentType(ContentType.JSON).auth().preemptive().basic(contribAdminUsername, contribAdminPassword)
				.pathParam("id", contentId2ToTag).body(tagging).expect().statusCode(201).log().ifValidationFails().when()
				.post(new URL(context, TAGGING_REST_API).toExternalForm());

		// insert something to other type to be sure it is filtered out
		tagging.put("tag", "label_other_type");
		given().contentType(ContentType.JSON).auth().preemptive().basic(contribAdminUsername, contribAdminPassword)
				.pathParam("id", contentId1ToTag_type2).body(tagging).expect().statusCode(201).log().ifValidationFails().when()
				.post(new URL(context, TAGGING_REST_API).toExternalForm());

		// get everything from type 1
		given().contentType(ContentType.JSON).pathParam("id", TYPE1).auth().preemptive()
				.basic(contribTagsAdminUsername, contribTagsAdminPassword).expect().log().ifValidationFails().statusCode(200)
				.contentType(ContentType.JSON).body("tag[0]", is("label")).body("tag[1]", is("label_2"))
				.body("tag[2]", is("label_id2")).body("tag[3]", is("label_id2_2")).body("tag[4]", Matchers.nullValue()).when()
				.get(new URL(context, TAGGING_REST_API_TYPE).toExternalForm());

		// get everything from type 2
		given().contentType(ContentType.JSON).pathParam("id", TYPE2).auth().preemptive()
				.basic(contribTagsAdminUsername, contribTagsAdminPassword).expect().log().ifValidationFails().statusCode(200)
				.contentType(ContentType.JSON).body("tag[0]", is("label_other_type")).body("tag[1]", Matchers.nullValue())
				.when().get(new URL(context, TAGGING_REST_API_TYPE).toExternalForm());

	}

	@Test
	@InSequence(20)
	public void assertDocumentChangePreservesTags() throws MalformedURLException {

		// delete one provider tag
		Map<String, Object> content = new HashMap<>();
		content.put("data", "test1");
		List<String> tags = new ArrayList<>();
		tags.add(TAG_PROVIDER_1);
		content.put(ContentObjectFields.TAGS, tags);
		ContentRestServiceTest.createOrUpdateContent(context, provider1, TYPE1, contentId1, content);

		DeploymentHelpers.refreshES();
		List<String> st = getSysTagsForContent(contentId1ToTag);
		Assert.assertNotNull(st);
		Assert.assertEquals(3, st.size());
		Assert.assertTrue(st.contains(TAG_PROVIDER_1));
		Assert.assertTrue(st.contains("label"));
		Assert.assertTrue(st.contains("label_2"));

		// delete all provider tags
		content = new HashMap<>();
		content.put("data", "test1");
		ContentRestServiceTest.createOrUpdateContent(context, provider1, TYPE1, contentId1, content);

		DeploymentHelpers.refreshES();
		st = getSysTagsForContent(contentId1ToTag);
		Assert.assertNotNull(st);
		Assert.assertEquals(2, st.size());
		Assert.assertTrue(st.contains("label"));
		Assert.assertTrue(st.contains("label_2"));

		// set back two provider tags for further tests
		content = new HashMap<>();
		content.put("data", "test1");
		tags = new ArrayList<>();
		tags.add(TAG_PROVIDER_1);
		tags.add(TAG_PROVIDER_2);
		content.put(ContentObjectFields.TAGS, tags);
		ContentRestServiceTest.createOrUpdateContent(context, provider1, TYPE1, contentId1, content);

	}

	@Test
	@InSequence(30)
	public void assertDeleteTag_UnknownContent() throws MalformedURLException {
		Map<String, Object> tagging = new HashMap<>();
		tagging.put("tag", "label");

		given().contentType(ContentType.JSON).auth().preemptive().basic(contribTagsAdminUsername, contribTagsAdminPassword)
				.pathParam("id", contentIdToTagNonexistingDoc).body(tagging).expect().statusCode(404).log().ifValidationFails()
				.when().delete(new URL(context, TAGGING_REST_API).toExternalForm());

		given().contentType(ContentType.JSON).auth().preemptive().basic(contribTagsAdminUsername, contribTagsAdminPassword)
				.pathParam("id", contentIdToTagNonexistingType).body(tagging).expect().statusCode(404).log()
				.ifValidationFails().when().delete(new URL(context, TAGGING_REST_API).toExternalForm());

	}

	@Test
	@InSequence(31)
	public void assertDeleteTag() throws MalformedURLException {
		Map<String, Object> tagging = new HashMap<>();
		tagging.put("tag", "label");

		// delete by tags_admin user
		given().contentType(ContentType.JSON).auth().preemptive().basic(contribTagsAdminUsername, contribTagsAdminPassword)
				.pathParam("id", contentId1ToTag).body(tagging).expect().statusCode(200).log().ifValidationFails().when()
				.delete(new URL(context, TAGGING_REST_API).toExternalForm());

		// attempt to delete nonexisting tag, admin user
		tagging.put("tag", "label_non_existing");
		given().contentType(ContentType.JSON).auth().preemptive().basic(contribAdminUsername, contribAdminPassword)
				.pathParam("id", contentId1ToTag).body(tagging).expect().statusCode(200).log().ifValidationFails().when()
				.delete(new URL(context, TAGGING_REST_API).toExternalForm());

		// attempt to delete nonexisting tag, tag_admin user for type
		tagging.put("tag", "label_non_existing");
		given().contentType(ContentType.JSON).auth().preemptive()
				.basic(contribTagsAdminTypeCorrectUsername, contribTagsAdminTypeCorrectPassword)
				.pathParam("id", contentId1ToTag).body(tagging).expect().statusCode(200).log().ifValidationFails().when()
				.delete(new URL(context, TAGGING_REST_API).toExternalForm());

		// assert read
		given().contentType(ContentType.JSON).pathParam("id", contentId1ToTag).auth().preemptive()
				.basic(contribTagsAdminUsername, contribTagsAdminPassword).expect().log().ifValidationFails().statusCode(200)
				.contentType(ContentType.JSON).body("tag[0]", is("label_2")).when()
				.get(new URL(context, TAGGING_REST_API).toExternalForm());

		// test that sys_tags of content is updated (two provider tags should be here)
		DeploymentHelpers.refreshES();
		List<String> st = getSysTagsForContent(contentId1ToTag);
		Assert.assertNotNull(st);
		Assert.assertEquals(3, st.size());
		Assert.assertTrue(st.contains(TAG_PROVIDER_1));
		Assert.assertTrue(st.contains(TAG_PROVIDER_2));
		Assert.assertTrue(st.contains("label_2"));

	}

	@Test
	@InSequence(40)
	public void assertDeleteAll_UnknownContent() throws MalformedURLException {

		given().contentType(ContentType.JSON).auth().preemptive().basic(contribAdminUsername, contribAdminPassword)
				.pathParam("id", contentIdToTagNonexistingDoc).expect().statusCode(404).log().ifValidationFails().when()
				.delete(new URL(context, TAGGING_REST_API_DELETE_ALL).toExternalForm());

		given().contentType(ContentType.JSON).auth().preemptive().basic(contribAdminUsername, contribAdminPassword)
				.pathParam("id", contentIdToTagNonexistingType).expect().statusCode(404).log().ifValidationFails().when()
				.delete(new URL(context, TAGGING_REST_API_DELETE_ALL).toExternalForm());

	}

	@Test
	@InSequence(41)
	public void assertDeleteAll_authenticatedContributor_admin() throws MalformedURLException {

		given().contentType(ContentType.JSON).auth().preemptive().basic(contribAdminUsername, contribAdminPassword)
				.pathParam("id", contentId2ToTag).expect().statusCode(200).log().ifValidationFails().when()
				.delete(new URL(context, TAGGING_REST_API_DELETE_ALL).toExternalForm());

		// assert read by other user
		given().contentType(ContentType.JSON).pathParam("id", contentId2ToTag).auth().preemptive()
				.basic(contribTagsAdminUsername, contribTagsAdminPassword).expect().log().ifValidationFails().statusCode(404)
				.when().get(new URL(context, TAGGING_REST_API).toExternalForm());

		// test that sys_tags of content is updated
		DeploymentHelpers.refreshES();
		List<String> st = getSysTagsForContent(contentId2ToTag);
		Assert.assertTrue(st == null || st.isEmpty());
	}

	@Test
	@InSequence(42)
	public void assertDeleteAll_authenticatedContributor_tags_admin() throws MalformedURLException {

		given().contentType(ContentType.JSON).auth().preemptive().basic(contribTagsAdminUsername, contribTagsAdminPassword)
				.pathParam("id", contentId1ToTag).expect().statusCode(200).log().ifValidationFails().when()
				.delete(new URL(context, TAGGING_REST_API_DELETE_ALL).toExternalForm());

		// assert read by
		given().contentType(ContentType.JSON).pathParam("id", contentId1ToTag).auth().preemptive()
				.basic(contribTagsAdminUsername, contribTagsAdminPassword).expect().log().ifValidationFails().statusCode(404)
				.when().get(new URL(context, TAGGING_REST_API).toExternalForm());

		// test that sys_tags of content is updated (two provider tags should be here)
		DeploymentHelpers.refreshES();
		List<String> st = getSysTagsForContent(contentId1ToTag);
		Assert.assertNotNull(st);
		Assert.assertEquals(2, st.size());
		Assert.assertTrue(st.contains(TAG_PROVIDER_1));
		Assert.assertTrue(st.contains(TAG_PROVIDER_2));
	}

	@Test
	@InSequence(43)
	public void assertDeleteAll_authenticatedContributor_tags_admin_type() throws MalformedURLException {

		given().contentType(ContentType.JSON).auth().preemptive()
				.basic(contribTagsAdminTypeCorrectUsername, contribTagsAdminTypeCorrectPassword)
				.pathParam("id", contentId3ToTag).expect().statusCode(200).log().ifValidationFails().when()
				.delete(new URL(context, TAGGING_REST_API_DELETE_ALL).toExternalForm());

		// assert read by
		given().contentType(ContentType.JSON).pathParam("id", contentId3ToTag).auth().preemptive()
				.basic(contribTagsAdminTypeCorrectUsername, contribTagsAdminTypeCorrectPassword).expect().log()
				.ifValidationFails().statusCode(404).when().get(new URL(context, TAGGING_REST_API).toExternalForm());

		// test that sys_tags of content is updated (two provider tags should be here)
		DeploymentHelpers.refreshES();
		List<String> st = getSysTagsForContent(contentId3ToTag);
		Assert.assertNull(st);
	}

	@SuppressWarnings("unchecked")
	private List<String> getSysTagsForContent(String id) throws MalformedURLException {
		JsonPath ret = given().contentType(ContentType.JSON).queryParam("field", "sys_tags").expect().log().all()
				.statusCode(200).contentType(ContentType.JSON).when()
				.get(new URL(context, SearchRestServiceTest.SEARCH_REST_API).toExternalForm()).andReturn().getBody().jsonPath();
		List<Map<String, Object>> hits = ret.getList("hits.hits");

		for (Map<String, Object> hit : hits) {
			if (id.equals(hit.get("_id")) && hit.get("fields") != null) {
				return (List<String>) ((Map<String, Object>) hit.get("fields")).get(ContentObjectFields.SYS_TAGS);
			}
		}
		return null;
	}

}
