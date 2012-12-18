/*
 * JBoss, Home of Professional Open Source
 * Copyright 2012 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 */
package org.jboss.dcp.api.rest;

import javax.ws.rs.core.UriInfo;

import org.jboss.dcp.api.testtools.TestUtils;
import org.junit.Test;

/**
 * TODO Unit test for {@link SearchRestService}
 * 
 * @author Vlastimil Elias (velias at redhat dot com)
 */
public class SearchRestServiceTest {

	@Test
	public void search_permissions() {
		TestUtils.assertPermissionGuest(SearchRestService.class, "search", UriInfo.class);
	}

}
