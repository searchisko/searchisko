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
 * Integration test for /normalization REST API.
 * <p/>
 * http://docs.jbossorg.apiary.io/#normalizationapi
 *
 * @author Libor Krzyzanek
 * @see org.searchisko.api.rest.NormalizationRestService
 */
@RunWith(Arquillian.class)
public class NormalizationRestServiceTest {

	public static final Set<String> ALLOWED_ROLES = new HashSet<>();
	static {
		ALLOWED_ROLES.add(Role.ADMIN);
		ALLOWED_ROLES.add(Role.PROVIDER);
	}

	public static final String NORMALIZATION_REST_API_BASE = DeploymentHelpers.CURRENT_REST_VERSION + "normalization/{normalizationName}/";

	public static final String NORMALIZATION_REST_API = NORMALIZATION_REST_API_BASE + "{id}";

	@Deployment(testable = false)
	public static WebArchive createDeployment() throws IOException {
		return DeploymentHelpers.createDeployment();
	}

	@ArquillianResource
	URL context;

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
		// GET /indexer/{normalizationName}/
		givenJsonAndLogIfFailsAndAuthPreemptive(username, password)
				.pathParam("normalizationName", "bad-name")
				.expect().statusCode(expStatus)
				.when().get(new URL(context, NORMALIZATION_REST_API_BASE).toExternalForm());

		// GET /indexer/{normalizationName}/{id}
		givenJsonAndLogIfFailsAndAuthPreemptive(username, password)
				.pathParam("normalizationName", "bad-name")
				.pathParam("id", "bad-id")
				.expect().statusCode(expStatus)
				.when().get(new URL(context, NORMALIZATION_REST_API).toExternalForm());
	}

	//TODO: FTEST: NormalizationRestServiceTest: Test distinct normalizations
}
