/*
 * JBoss, Home of Professional Open Source
 * Copyright 2012 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 */
package org.searchisko.api.service;

import java.io.IOException;
import java.util.logging.Logger;

import org.junit.Assert;
import org.junit.Test;
import org.searchisko.api.model.AppConfiguration.ClientType;

/**
 * Unit test for {@link AppConfigurationService}
 * 
 * @author Vlastimil Elias (velias at redhat dot com)
 */
public class AppConfigurationServiceTest {

	@Test
	public void loadConfig() throws IOException {

		AppConfigurationService tested = new AppConfigurationService();
		tested.log = Logger.getLogger("testlogger");

		Assert.assertNull(tested.getAppConfiguration());

		// case - successful load
		tested.loadConfig();
		Assert.assertNotNull(tested.getAppConfiguration());
		Assert.assertEquals(ClientType.EMBEDDED, tested.getAppConfiguration().getClientType());
		Assert.assertEquals("/test/path", tested.getAppConfiguration().getAppDataPath());
		Assert.assertEquals(true, tested.getAppConfiguration().isProviderCreateInitData());

		// case - successful load 2 with overlay applied
		tested.appConfiguration = null;
		tested.loadConfig("/app2.properties");
		Assert.assertNotNull(tested.getAppConfiguration());
		Assert.assertEquals(ClientType.TRANSPORT, tested.getAppConfiguration().getClientType());
		Assert.assertEquals("/test/path/2", tested.getAppConfiguration().getAppDataPath());
		Assert.assertEquals(false, tested.getAppConfiguration().isProviderCreateInitData());

		// case - exception if file not found
		tested.appConfiguration = null;
		try {
			tested.loadConfig("/app-unknown.properties");
			Assert.fail("IOException expected");
		} catch (IOException e) {
			// OK
		}

	}

}
