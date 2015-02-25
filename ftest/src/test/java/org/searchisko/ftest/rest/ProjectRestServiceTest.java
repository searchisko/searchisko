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

import com.jayway.restassured.http.ContentType;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.junit.InSequence;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.searchisko.api.security.Role;
import org.searchisko.ftest.DeploymentHelpers;

import static com.jayway.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;
import static org.searchisko.ftest.rest.RestTestHelpers.givenJsonAndLogIfFailsAndAuthPreemptive;

/**
 * Integration tests for {@link org.searchisko.api.rest.ProjectRestService} REST API
 * <p/>
 * see http://docs.jbossorg.apiary.io/#managementapiprojects
 *
 * @author Libor Krzyzanek
 * @see org.searchisko.api.rest.ProjectRestService
 */
@RunWith(Arquillian.class)
public class ProjectRestServiceTest {

	public static final Set<String> ALLOWED_ROLES = new HashSet<>();
	static {
		ALLOWED_ROLES.add(Role.ADMIN);
		ALLOWED_ROLES.add(Role.PROJECTS_MANAGER);
	}

	public static final String PROJECT_REST_API = DeploymentHelpers.CURRENT_REST_VERSION + "project";

	@ArquillianResource
	protected URL context;

	@Deployment(testable = false)
	public static WebArchive createDeployment() throws IOException {
		return DeploymentHelpers.createDeployment();
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
		// GET /project
		givenJsonAndLogIfFailsAndAuthPreemptive(username, password)
				.expect().statusCode(expStatus)
				.log().ifValidationFails()
				.when().get(new URL(context, PROJECT_REST_API + "/search").toExternalForm());

		// POST /project
		givenJsonAndLogIfFailsAndAuthPreemptive(username, password)
				.body("{}")
				.expect().statusCode(expStatus)
				.log().ifValidationFails()
				.when().post(new URL(context, PROJECT_REST_API).toExternalForm());

		// DELETE /project/projectcode
		givenJsonAndLogIfFailsAndAuthPreemptive(username, password)
				.expect().statusCode(expStatus)
				.log().ifValidationFails()
				.when().delete(new URL(context, PROJECT_REST_API + "/projectcode").toExternalForm());
	}

	@Test
	@InSequence(10)
	public void assertGetAllDefault() throws MalformedURLException {
		// GET /project
		given().contentType(ContentType.JSON)
				.expect().statusCode(200)
				.log().ifValidationFails()
				.body("total", is(0))
				.when().get(new URL(context, PROJECT_REST_API).toExternalForm());
	}

	@Test
	@InSequence(20)
	public void assertCreate() throws MalformedURLException {
		String data = "{\n" +
				"  \"code\": \"jbosstools\",\n" +
				"  \"name\": \"JBoss Tools\",\n" +
				"  \"description\" : \"\",\n" +
				"  \"type_specific_code\" : {\n" +
				"    \"jbossorg_blog\": [\"jbosstools\"],\n" +
				"    \"jbossorg_jira\": [\"JBIDE\"],\n" +
				"    \"jbossorg_mailing_list\": \"\",\n" +
				"    \"jbossorg_project_info\": \"/jbosstools\"\n" +
				"  }\n" +
				"}";

		given().contentType(ContentType.JSON)
				.auth().basic(DeploymentHelpers.DEFAULT_PROVIDER_NAME, DeploymentHelpers.DEFAULT_PROVIDER_PASSWORD)
				.body(data)
				.expect().statusCode(200)
				.log().ifValidationFails()
				.body("id", is("jbosstools"))
				.when().post(new URL(context, PROJECT_REST_API).toExternalForm());

	}

	@Test
	@InSequence(21)
	public void assertRefreshES() throws MalformedURLException {
		DeploymentHelpers.refreshES();
	}

	@Test
	@InSequence(25)
	public void assertGetCreatedProject() throws MalformedURLException {
		given().contentType(ContentType.JSON)
				.expect().statusCode(200)
				.log().ifValidationFails()
				.body("code", is("jbosstools"))
				.body("name", is("JBoss Tools"))
				.body("type_specific_code", nullValue())
				.when().get(new URL(context, PROJECT_REST_API + "/jbosstools").toExternalForm());
	}

	@Test
	@InSequence(26)
	public void assertGetCreatedProjectAuthenticated() throws MalformedURLException {
		given().contentType(ContentType.JSON)
				.auth().preemptive().basic(DeploymentHelpers.DEFAULT_PROVIDER_NAME, DeploymentHelpers.DEFAULT_PROVIDER_PASSWORD)
				.expect().statusCode(200)
				.log().ifValidationFails()
				.body("code", is("jbosstools"))
				.body("name", is("JBoss Tools"))
				.body("type_specific_code.jbossorg_blog[0]", is("jbosstools"))
				.when().get(new URL(context, PROJECT_REST_API + "/jbosstools").toExternalForm());
	}


	@Test
	@InSequence(30)
	public void assertSearchCreatedProject() throws MalformedURLException {
		given().contentType(ContentType.JSON)
				.auth().basic(DeploymentHelpers.DEFAULT_PROVIDER_NAME, DeploymentHelpers.DEFAULT_PROVIDER_PASSWORD)
				.param("jbossorg_jira", "JBIDE")
				.expect().statusCode(200)
				.log().ifValidationFails()
				.body("total", is(1))
				.body("hits[0].id", is("jbosstools"))
				.when().get(new URL(context, PROJECT_REST_API + "/search").toExternalForm());

		given().contentType(ContentType.JSON)
				.auth().basic(DeploymentHelpers.DEFAULT_PROVIDER_NAME, DeploymentHelpers.DEFAULT_PROVIDER_PASSWORD)
				.param("code", "jbosstools")
				.expect().statusCode(200)
				.log().ifValidationFails()
				.body("total", is(1))
				.body("hits[0].id", is("jbosstools"))
				.when().get(new URL(context, PROJECT_REST_API + "/search").toExternalForm());
	}


	@Test
	@InSequence(40)
	public void assertDeleteProject() throws MalformedURLException {
		given().contentType(ContentType.JSON)
				.auth().basic(DeploymentHelpers.DEFAULT_PROVIDER_NAME, DeploymentHelpers.DEFAULT_PROVIDER_PASSWORD)
				.expect().statusCode(200)
				.log().ifValidationFails()
				.when().delete(new URL(context, PROJECT_REST_API + "/jbosstools").toExternalForm());
	}
}