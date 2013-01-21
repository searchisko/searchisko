/*
 * JBoss, Home of Professional Open Source
 * Copyright 2012 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 */
package org.jboss.dcp.api.rest;

import java.util.Map;

import org.jboss.dcp.api.service.ConfigService;
import org.jboss.dcp.api.testtools.TestUtils;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

/**
 * Unit test for {@link ConfigRestService}.
 * 
 * @author Vlastimil Elias (velias at redhat dot com)
 */
public class ConfigRestServiceTest {

	@Test
	public void init() {
		ConfigRestService tested = new ConfigRestService();
		Assert.assertNull(tested.entityService);
		tested.configService = Mockito.mock(ConfigService.class);
		Assert.assertNull(tested.entityService);
		tested.init();
		Assert.assertEquals(tested.configService, tested.entityService);
	}

	@Test
	public void getAll_permissions() {
		TestUtils.assertPermissionSuperProvider(ConfigRestService.class, "getAll", Integer.class, Integer.class);
	}

	@Test
	public void get_permissions() {
		TestUtils.assertPermissionSuperProvider(ConfigRestService.class, "get", String.class);
	}

	@Test
	public void create_permissions() {
		TestUtils.assertPermissionSuperProvider(ConfigRestService.class, "create", String.class, Map.class);
		TestUtils.assertPermissionSuperProvider(ConfigRestService.class, "create", Map.class);
	}

	@Test
	public void delete_permissions() {
		TestUtils.assertPermissionSuperProvider(ConfigRestService.class, "delete", String.class);
	}

}
