/*
 * JBoss, Home of Professional Open Source
 * Copyright 2012 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 */
package org.searchisko.ftest;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.net.URL;

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

	public static final String INDEXER_REST_API = DeploymentHelpers.DEFAULT_REST_VERSION + "indexer";

	@Deployment(testable = false)
	public static WebArchive createDeployment() throws IOException {
		return DeploymentHelpers.createDeployment();
	}

	@ArquillianResource
	URL context;



}
