/*
 * JBoss, Home of Professional Open Source
 * Copyright 2012 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 */
package org.searchisko.ftest;

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
 * Integration test for /indexer REST API.
 * <p/>
 * http://docs.jbossorg.apiary.io/#contentindexersapi
 *
 * @author Libor Krzyzanek
 * @see org.searchisko.api.rest.IndexerRestService
 */
@RunWith(Arquillian.class)
public class IndexerRestServiceTest {

	public static final String INDEXER_REST_API = DeploymentHelpers.DEFAULT_REST_VERSION + "indexer/{type}/{operation}";

	@Deployment(testable = false)
	public static WebArchive createDeployment() throws IOException {
		return DeploymentHelpers.createDeployment();
	}

	@ArquillianResource
	URL context;

	public static final String TYPE_ALL = "_all";

	public final String[] types = {"elasticsearch-river-remote", "elasticsearch-river-jira", TYPE_ALL};

	@Test
	@InSequence(0)
	public void assertNotAuthenticated() throws MalformedURLException {
		int expStatus = 401;

		for (String type : types) {
			// GET /indexer/{type}/_status
			given().contentType(ContentType.JSON)
					.pathParam("type", type)
					.pathParam("operation", "_status")
					.expect().statusCode(expStatus)
					.log().ifStatusCodeMatches(is(not(expStatus)))
					.when().get(new URL(context, INDEXER_REST_API).toExternalForm());

			// POST /indexer/{type}/_stop
			given().contentType(ContentType.JSON)
					.pathParam("type", type)
					.pathParam("operation", "_stop")
					.expect().statusCode(expStatus)
					.log().ifStatusCodeMatches(is(not(expStatus)))
					.when().post(new URL(context, INDEXER_REST_API).toExternalForm());


			// POST /indexer/{type}/_restart
			given().contentType(ContentType.JSON)
					.pathParam("type", type)
					.pathParam("operation", "_restart")
					.expect().statusCode(expStatus)
					.log().ifStatusCodeMatches(is(not(expStatus)))
					.when().post(new URL(context, INDEXER_REST_API).toExternalForm());


			if (!TYPE_ALL.equals(type)) {
				// POST /indexer/{type}/_force_reindex
				given().contentType(ContentType.JSON)
						.pathParam("type", type)
						.pathParam("operation", "_force_reindex")
						.expect().statusCode(expStatus)
						.log().ifStatusCodeMatches(is(not(expStatus)))
						.when().post(new URL(context, INDEXER_REST_API).toExternalForm());
			}
		}
	}

	//TODO: FTEST: IndexerRestServiceTest: Test start reindex
}
