/*
 * JBoss, Home of Professional Open Source
 * Copyright 2012 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 */
package org.searchisko.api.model;

import java.io.IOException;

import org.junit.Assert;
import org.junit.Test;

/**
 * Unit test for {@link StatsConfiguration}.
 *
 * @author Vlastimil Elias (velias at redhat dot com)
 */
public class StatsConfiguationTest {

	@Test
	public void init() throws IOException {
		StatsConfiguration tested = new StatsConfiguration();
		Assert.assertFalse(tested.enabled());

		tested.init();

		Assert.assertFalse(tested.enabled());
		Assert.assertTrue(tested.isUseSearchCluster());
	}

}
