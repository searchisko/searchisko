/*
 * JBoss, Home of Professional Open Source
 * Copyright 2015 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 */
package org.searchisko.ftest.rest;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashSet;
import java.util.Set;

import static org.hamcrest.Matchers.is;
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
import static org.searchisko.ftest.rest.RestTestHelpers.givenJsonAndLogIfFailsAndAuthDefaultProvider;

/**
 * Integration test for /metrics REST API.
 *
 * @author Libor Krzyzanek
 */
@RunWith(Arquillian.class)
public class MetricsRestServiceTest {

	public static final Set<String> ALLOWED_ROLES = new HashSet<>();

	static {
		ALLOWED_ROLES.add(Role.ADMIN);
	}

	public static final String METRICS_REST_API_BASE = DeploymentHelpers.CURRENT_REST_VERSION + "sys/metrics/";

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
		// GET /
		givenJsonAndLogIfFailsAndAuthPreemptive(username, password).expect().statusCode(expStatus).when()
				.get(new URL(context, METRICS_REST_API_BASE).toExternalForm());
	}

	@Test
	@InSequence(10)
	public void assertGetVersion() throws MalformedURLException {
		givenJsonAndLogIfFailsAndAuthDefaultProvider()
				.expect().statusCode(200).body("status", is(200)).body("request.type", is("version"))
				.when().get(new URL(context, METRICS_REST_API_BASE).toExternalForm());
	}

}
