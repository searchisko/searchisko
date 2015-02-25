/*
 * JBoss, Home of Professional Open Source
 * Copyright 2012 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 */
package org.searchisko.ftest.rest;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashSet;
import java.util.Set;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.junit.InSequence;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.searchisko.api.security.Role;
import org.searchisko.ftest.DeploymentHelpers;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.searchisko.ftest.rest.RestTestHelpers.givenJsonAndLogIfFailsAndAuthDefaultProvider;
import static org.searchisko.ftest.rest.RestTestHelpers.givenJsonAndLogIfFailsAndAuthPreemptive;

/**
 * Integration test for /config REST API.
 * <p/>
 * http://docs.jbossorg.apiary.io/#managementapiconfiguration
 * 
 * @author Libor Krzyzanek
 * @see org.searchisko.api.rest.ConfigRestService
 */
@RunWith(Arquillian.class)
public class ConfigRestServiceTest {

	public static final Set<String> ALLOWED_ROLES = new HashSet<>();
	static {
		ALLOWED_ROLES.add(Role.ADMIN);
	}

	public static final String CONFIG_REST_API_BASE = DeploymentHelpers.CURRENT_REST_VERSION + "config/";

	public static final String CONFIG_REST_API = CONFIG_REST_API_BASE + "{id}";

	@Deployment(testable = false)
	public static WebArchive createDeployment() throws IOException {
		return DeploymentHelpers.createDeployment();
	}

	@ArquillianResource
	URL context;

	/**
	 * Helper method which allows to upload config file even from other tests.
	 * 
	 * @param context to be used for upload
	 * @param id of the config file
	 * @param data JSON content of the config file
	 * @throws MalformedURLException
	 */
	public static void uploadConfigFile(URL context, String id, String data) throws MalformedURLException {
		givenJsonAndLogIfFailsAndAuthDefaultProvider().pathParam("id", id).body(data).expect().statusCode(200)
				.body("id", is(id)).when().post(new URL(context, CONFIG_REST_API).toExternalForm());
	}

	/**
	 * Helper method which allows to delete config file even from other tests.
	 * 
	 * @param context to be used for upload
	 * @param id of the config file
	 * @throws MalformedURLException
	 */
	public static void removeConfigFile(URL context, String id) throws MalformedURLException {
		givenJsonAndLogIfFailsAndAuthDefaultProvider().pathParam("id", id).expect().statusCode(200).when()
				.delete(new URL(context, CONFIG_REST_API).toExternalForm());
	}

	@Test
	@InSequence(0)
	public void assertNotAuthenticated() throws MalformedURLException {
		assertAccess(401, null, null);
	}

	@Test
	@InSequence(1)
	public void assertForbidden() throws MalformedURLException {
		for (String role : Role.ALL_ROLES) {
			if (!ALLOWED_ROLES.contains(role)) {
				assertAccess(403, role, role);
			}
		}
	}

	public void assertAccess(int expStatus, String username, String password) throws MalformedURLException {
		// GET /config
		givenJsonAndLogIfFailsAndAuthPreemptive(username, password).expect().statusCode(expStatus).when()
				.get(new URL(context, CONFIG_REST_API_BASE).toExternalForm());

		// GET /config/some-id
		givenJsonAndLogIfFailsAndAuthPreemptive(username, password).pathParam("id", "some-id").expect()
				.statusCode(expStatus).when().get(new URL(context, CONFIG_REST_API).toExternalForm());

		// POST /config/some-id
		givenJsonAndLogIfFailsAndAuthPreemptive(username, password).pathParam("id", "some-id").body("{}").expect()
				.statusCode(expStatus).when().post(new URL(context, CONFIG_REST_API).toExternalForm());

		// DELETE /config/some-id
		givenJsonAndLogIfFailsAndAuthPreemptive(username, password).pathParam("id", "some-id").expect()
				.statusCode(expStatus).when().delete(new URL(context, CONFIG_REST_API).toExternalForm());
	}

	@Test
	@InSequence(10)
	public void assertGetDefaultAll() throws MalformedURLException {
		givenJsonAndLogIfFailsAndAuthDefaultProvider().expect().statusCode(200).body("total", is(0)).when()
				.get(new URL(context, CONFIG_REST_API_BASE).toExternalForm());
	}

	protected static final String configId = "search_fulltext_query_fields";

	@Test
	@InSequence(20)
	public void assertCreateNew() throws MalformedURLException {
		// see
		// https://github.com/searchisko/searchisko/blob/master/documentation/rest-api/management/config_search_fulltext_query_fields.md
		final String data = "{\n" + "  \"sys_title\": \"2.5\",\n" + "  \"sys_description\": \"\",\n"
				+ "  \"sys_project_name\": \"2\",\n" + "  \"sys_tags\":\"1.5\",\n" + "  \"sys_contributors.fulltext\": \"\"\n"
				+ "}";
		uploadConfigFile(context, configId, data);
	}

	@Test
	@InSequence(21)
	public void assertRefreshES() throws MalformedURLException {
		DeploymentHelpers.refreshES();
	}

	@Test
	@InSequence(25)
	public void assertGetCreated() throws MalformedURLException {
		givenJsonAndLogIfFailsAndAuthDefaultProvider().pathParam("id", configId).expect().statusCode(200)
				.body("sys_description", notNullValue()).when().get(new URL(context, CONFIG_REST_API).toExternalForm());
	}

	@Test
	@InSequence(26)
	public void assertGetCreatedAll() throws MalformedURLException {
		givenJsonAndLogIfFailsAndAuthDefaultProvider().expect().statusCode(200).body("total", is(1))
				.body("hits[0].id", is(configId)).when().get(new URL(context, CONFIG_REST_API_BASE).toExternalForm());
	}

	@Test
	@InSequence(30)
	public void assertDeleteCreated() throws MalformedURLException {
		givenJsonAndLogIfFailsAndAuthDefaultProvider().pathParam("id", configId).expect().contentType("").statusCode(200)
				.when().delete(new URL(context, CONFIG_REST_API).toExternalForm());
	}
}
