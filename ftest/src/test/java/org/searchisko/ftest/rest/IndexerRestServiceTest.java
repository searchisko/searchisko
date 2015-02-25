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
import static org.searchisko.ftest.rest.RestTestHelpers.givenJsonAndLogIfFailsAndAuthPreemptive;

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

	public static final Set<String> ALLOWED_ROLES = new HashSet<>();

	static {
		ALLOWED_ROLES.add(Role.ADMIN);
		ALLOWED_ROLES.add(Role.PROVIDER);
	}

	public static final String INDEXER_REST_API = DeploymentHelpers.CURRENT_REST_VERSION + "indexer/{type}/{operation}";

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
		for (String type : types) {
			// GET /indexer/{type}/_status
			givenJsonAndLogIfFailsAndAuthPreemptive(username, password)
					.pathParam("type", type)
					.pathParam("operation", "_status")
					.expect().statusCode(expStatus)
					.when().get(new URL(context, INDEXER_REST_API).toExternalForm());

			// POST /indexer/{type}/_stop
			givenJsonAndLogIfFailsAndAuthPreemptive(username, password)
					.pathParam("type", type)
					.pathParam("operation", "_stop")
					.expect().statusCode(expStatus)
					.when().post(new URL(context, INDEXER_REST_API).toExternalForm());


			// POST /indexer/{type}/_restart
			givenJsonAndLogIfFailsAndAuthPreemptive(username, password)
					.pathParam("type", type)
					.pathParam("operation", "_restart")
					.expect().statusCode(expStatus)
					.when().post(new URL(context, INDEXER_REST_API).toExternalForm());


			if (!TYPE_ALL.equals(type)) {
				// POST /indexer/{type}/_force_reindex
				givenJsonAndLogIfFailsAndAuthPreemptive(username, password)
						.pathParam("type", type)
						.pathParam("operation", "_force_reindex")
						.expect().statusCode(expStatus)
						.when().post(new URL(context, INDEXER_REST_API).toExternalForm());
			}
		}

	}

	//TODO: FTEST: IndexerRestServiceTest: Test start reindex
}
