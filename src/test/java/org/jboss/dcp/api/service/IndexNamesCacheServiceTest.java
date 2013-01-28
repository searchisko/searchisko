/*
 * JBoss, Home of Professional Open Source
 * Copyright 2013 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 */
package org.jboss.dcp.api.service;

import java.util.HashSet;
import java.util.Set;

import org.junit.Assert;
import org.junit.Test;

/**
 * Unit test for {@link IndexNamesCacheService}.
 * 
 * @author Vlastimil Elias (velias at redhat dot com)
 */
public class IndexNamesCacheServiceTest {

	@Test
	public void cacheWorks() throws InterruptedException {

		IndexNamesCacheService tested = new IndexNamesCacheService();
		tested.ttl = 500;

		Set<String> indexNames = new HashSet<String>();
		Set<String> indexNames2 = new HashSet<String>();
		Set<String> indexNames3 = new HashSet<String>();
		String dcpTypesRequested = "a";
		String dcpTypesRequested2_1 = "aaa";
		String dcpTypesRequested2_2 = "aaa";

		String dcpTypesRequested3_1 = "aaa|zzzzz";
		String dcpTypesRequested3_2 = "aaa|zzzzz";

		tested.put(dcpTypesRequested, indexNames);
		tested.put(dcpTypesRequested2_1, indexNames2);
		tested.put(dcpTypesRequested3_1, indexNames3);
		Assert.assertEquals(indexNames, tested.get(dcpTypesRequested));
		Assert.assertEquals(indexNames2, tested.get(dcpTypesRequested2_1));
		Assert.assertEquals(indexNames2, tested.get(dcpTypesRequested2_2));
		Assert.assertEquals(indexNames3, tested.get(dcpTypesRequested3_1));
		Assert.assertEquals(indexNames3, tested.get(dcpTypesRequested3_2));
		Thread.sleep(600);
		Assert.assertNull(tested.get(dcpTypesRequested));
		Assert.assertNull(tested.get(dcpTypesRequested2_1));
		Assert.assertNull(tested.get(dcpTypesRequested2_2));
		Assert.assertNull(tested.get(dcpTypesRequested3_1));
		Assert.assertNull(tested.get(dcpTypesRequested3_2));
	}

}
