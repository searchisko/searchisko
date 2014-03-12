/*
 * JBoss, Home of Professional Open Source
 * Copyright 2013 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 */
package org.searchisko.api.service;

import org.junit.Assert;
import org.junit.Test;

/**
 * Unit test for {@link StatsRecordType}.
 * 
 * @author Vlastimil Elias (velias at redhat dot com)
 */
public class StatsRecordTypeTest {

	@Test
	public void values() {
		for (StatsRecordType t : StatsRecordType.values()) {
			Assert.assertEquals(t.name().toLowerCase(), t.getSearchIndexedValue());
			Assert.assertEquals(t.name().toLowerCase(), t.getSearchIndexType());
			Assert.assertEquals("stats_api_" + t.name().toLowerCase(), t.getSearchIndexName());
		}
	}

}
