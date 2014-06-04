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
import static org.hamcrest.Matchers.containsString;


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
	public void assertGetAllEmpty() throws MalformedURLException {
		given().contentType(ContentType.JSON)
				.expect()
				.log().ifValidationFails()
				.statusCode(200)
				.contentType("application/atom+xml")
				.when().get(new URL(context, FEED_REST_API).toExternalForm());
	}

	static ProviderModel provider1 = new ProviderModel("provider1", "password");
	public static final String TYPE1 = "provider1_blog";
	public static final String CONTENT_TYPE = "text/html";
	public static final String FEED_CONTENT_TYPE = "html";

	@Test
	@InSequence(20)
	public void assertCreateProvider1BlogPost() throws MalformedURLException {
		provider1.addContentType(TYPE1, "blogpost", true, CONTENT_TYPE);
		ProviderRestServiceTest.createNewProvider(context, provider1);
	}

	static final String contentId = "test-id";

	@Test
	@InSequence(21)
	public void assertPushContentWithId() throws MalformedURLException {
		Map<String, Object> content = new HashMap<>();
		content.put("sys_description", "desc");
		content.put("sys_content", "content");
		ContentRestServiceTest.createOrUpdateContent(context, provider1, TYPE1, contentId, content);
	}

	@Test
	@InSequence(30)
	public void assertRefreshES() throws MalformedURLException {
		DeploymentHelpers.refreshES();
	}

	@Test
	@InSequence(31)
	public void assertGetDataFeed() throws MalformedURLException {
		// After library upgrade use https://code.google.com/p/rest-assured/wiki/Usage#Example_2_-_XML
		given().contentType(ContentType.XML)
				.expect()
				.log().ifValidationFails()
				.statusCode(200)
				.contentType("application/atom+xml")
				.body(containsString("<atom:content type=\"" + FEED_CONTENT_TYPE + "\">"))
				.when().get(new URL(context, FEED_REST_API).toExternalForm());
	}

}
