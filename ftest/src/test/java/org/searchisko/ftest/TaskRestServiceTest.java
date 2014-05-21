/*
 * JBoss, Home of Professional Open Source
 * Copyright 2013 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 */
package org.searchisko.ftest;

import com.jayway.restassured.http.ContentType;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.junit.InSequence;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

import static com.jayway.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

/**
 * Integration test for Tasks REST API {@link org.searchisko.api.rest.TaskRestService}
 * <p/>
 * http://docs.jbossorg.apiary.io/#managementapicontentreindexationtasks
 *
 * @author Libor Krzyzanek
 */
@RunWith(Arquillian.class)
public class TaskRestServiceTest {

	public static final String TASKS_REST_API = DeploymentHelpers.DEFAULT_REST_VERSION + "tasks";

	@Deployment(testable = false)
	public static WebArchive createDeployment() throws IOException {
		return DeploymentHelpers.createDeployment();
	}

	@AfterClass
	public static void cleanAfterTest() throws IOException {
		DeploymentHelpers.removeSearchiskoDataDir();
	}

	@ArquillianResource
	protected URL context;

	@Test
	@InSequence(0)
	public void assertNotAuthenticated() throws MalformedURLException {
		int expStatus = 401;

		// GET /tasks/type
		given().contentType(ContentType.JSON)
				.expect().statusCode(expStatus)
				.log().ifStatusCodeMatches(is(not(expStatus)))
				.when().get(new URL(context, TASKS_REST_API + "/type").toExternalForm());

		// GET /tasks/task
		given().contentType(ContentType.JSON)
				.expect().statusCode(expStatus)
				.log().ifStatusCodeMatches(is(not(expStatus)))
				.when().get(new URL(context, TASKS_REST_API + "/task").toExternalForm());

		// GET /tasks/taskId
		given().contentType(ContentType.JSON)
				.expect().statusCode(expStatus)
				.log().ifStatusCodeMatches(is(not(expStatus)))
				.when().get(new URL(context, TASKS_REST_API + "/task/id").toExternalForm());

		// POST /tasks/taskType e.g. reindex_from_persistence
		given().contentType(ContentType.JSON)
				.body("{\"sys_content_type\" : \"jbossorg_blog\"}")
				.expect().statusCode(expStatus)
				.log().ifStatusCodeMatches(is(not(expStatus)))
				.when().post(new URL(context, TASKS_REST_API + "/task/reindex_from_persistence").toExternalForm());

		// DELETE /tasks/task/id e.g. reindex_from_persistence
		given().contentType(ContentType.JSON)
				.expect().statusCode(expStatus)
				.log().ifStatusCodeMatches(is(not(expStatus)))
				.when().delete(new URL(context, TASKS_REST_API + "/task/id").toExternalForm());
	}

	@Test
	@InSequence(10)
	public void assertGetTasksType() throws MalformedURLException {
		// GET /tasks/type
		given().contentType(ContentType.JSON)
				.auth().basic(DeploymentHelpers.DEFAULT_PROVIDER_NAME, DeploymentHelpers.DEFAULT_PROVIDER_PASSWORD)
				.expect().statusCode(200)
				.log().ifError()
				.body("", containsInAnyOrder(
						"reindex_from_persistence",
						"renormalize_by_content_type",
						"renormalize_by_project_code",
						"renormalize_by_contributor_code",
						"renormalize_by_contributor_lookup_id",
						"renormalize_by_project_lookup_id",
						"update_contributor_profile",
						"reindex_contributor",
						"reindex_project"
				))
				.when().get(new URL(context, TASKS_REST_API + "/type").toExternalForm());
	}

	@Test
	@InSequence(11)
	public void assertGetTasksTask() throws MalformedURLException {
		// GET /tasks/task
		given().contentType(ContentType.JSON)
				.auth().basic(DeploymentHelpers.DEFAULT_PROVIDER_NAME, DeploymentHelpers.DEFAULT_PROVIDER_PASSWORD)
				.expect().statusCode(200)
				.log().ifError()
				.body("size()", equalTo(0))
				.when().get(new URL(context, TASKS_REST_API + "/task").toExternalForm());
	}

	static String taskID;

	@Test
	@InSequence(20)
	public void assertReindexContributor() throws MalformedURLException {
		// POST /tasks/task/reindex_contributor
		taskID = given().contentType(ContentType.JSON)
				.auth().basic(DeploymentHelpers.DEFAULT_PROVIDER_NAME, DeploymentHelpers.DEFAULT_PROVIDER_PASSWORD)
				.body("{}")
				.expect().statusCode(200)
				.log().ifError()
				.contentType(ContentType.JSON)
				.body("id", is(not(empty())))
				.when().post(new URL(context, TASKS_REST_API + "/task/reindex_contributor").toExternalForm())
				.andReturn().body().jsonPath().get("id");
		System.out.println("id:" + taskID);
	}

	@Test
	@InSequence(21)
	public void assertGetCreatedReindexContributorTask() throws MalformedURLException {
		given().contentType(ContentType.JSON)
				.pathParam("id", taskID)
				.auth().basic(DeploymentHelpers.DEFAULT_PROVIDER_NAME, DeploymentHelpers.DEFAULT_PROVIDER_PASSWORD)
				.expect().statusCode(200)
				.log().ifError()
				.body("id", is(taskID))
				.body("taskType", is("reindex_contributor"))
				.body("runCount", isA(Integer.class))
				.body("taskStatus", notNullValue())
				.body("cancelRequested", is(false))
				.when().get(new URL(context, TASKS_REST_API + "/task/{id}").toExternalForm());
	}


	@Test
	@InSequence(30)
	public void assertDeleteCreatedReindexContributorTask() throws MalformedURLException {
		given().contentType(ContentType.JSON)
				.pathParam("id", taskID)
				.auth().basic(DeploymentHelpers.DEFAULT_PROVIDER_NAME, DeploymentHelpers.DEFAULT_PROVIDER_PASSWORD)
				.expect().statusCode(200)
				.log().ifError()
				.when().delete(new URL(context, TASKS_REST_API + "/task/{id}").toExternalForm());
	}

}
