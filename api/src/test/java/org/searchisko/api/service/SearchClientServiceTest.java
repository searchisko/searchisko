/*
 * JBoss, Home of Professional Open Source
 * Copyright 2012 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 */
package org.searchisko.api.service;

import org.junit.Assert;
import org.junit.Test;
import org.searchisko.api.model.AppConfiguration;
import org.searchisko.api.model.AppConfiguration.ClientType;

import java.util.logging.Logger;

/**
 * Unit test for {@link SearchClientService}
 *
 * @author Vlastimil Elias (velias at redhat dot com)
 */
public class SearchClientServiceTest {

	@Test
	public void init_embedded() throws Exception {
		AppConfigurationService acs = new AppConfigurationService();
		AppConfiguration ac = new AppConfiguration(System.getProperty("java.io.tmpdir"));
		acs.appConfiguration = ac;
		ac.setClientType(ClientType.EMBEDDED);
		ac.setProviderCreateInitData(false);

		SearchClientService tested = new SearchClientService();
		tested.appConfigurationService = acs;
		tested.log = Logger.getLogger("testlogger");

		try {
			tested.init();
			Assert.assertNotNull(tested.node);
			Assert.assertNotNull(tested.client);
		} finally {
			tested.destroy();
			Assert.assertNull(tested.node);
			Assert.assertNull(tested.client);
		}
	}

	@Test
	public void init_transport() {
		// untestable :-(
	}

}
