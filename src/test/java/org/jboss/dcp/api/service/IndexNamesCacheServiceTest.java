/*
 * JBoss, Home of Professional Open Source
 * Copyright 2013 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 */
package org.jboss.dcp.api.service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
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
	public void prepareKey() {

		Assert.assertEquals("_all||", IndexNamesCacheService.prepareKey(null));
		List<String> dcpTypesRequested = new ArrayList<String>();
		Assert.assertEquals("_all||", IndexNamesCacheService.prepareKey(dcpTypesRequested));

		dcpTypesRequested.add("aaaa");
		Assert.assertEquals("aaaa", IndexNamesCacheService.prepareKey(dcpTypesRequested));

		dcpTypesRequested.add("bb");
		Assert.assertEquals("aaaa|bb|", IndexNamesCacheService.prepareKey(dcpTypesRequested));

		// check ordering
		dcpTypesRequested = new ArrayList<String>();
		dcpTypesRequested.add("bb");
		dcpTypesRequested.add("zzzzz");
		dcpTypesRequested.add("aaaa");
		Assert.assertEquals("aaaa|bb|zzzzz|", IndexNamesCacheService.prepareKey(dcpTypesRequested));

	}

	@Test
	public void cacheWorks() throws InterruptedException {

		IndexNamesCacheService tested = new IndexNamesCacheService();
		tested.ttl = 500;

		Set<String> indexNames = new HashSet<String>();
		Set<String> indexNames2 = new HashSet<String>();
		Set<String> indexNames3 = new HashSet<String>();
		List<String> dcpTypesRequested = null;
		List<String> dcpTypesRequested2_1 = new ArrayList<String>();
		dcpTypesRequested2_1.add("aaa");
		List<String> dcpTypesRequested2_2 = new ArrayList<String>();
		dcpTypesRequested2_2.add("aaa");

		List<String> dcpTypesRequested3_1 = new ArrayList<String>();
		dcpTypesRequested3_1.add("aaa");
		dcpTypesRequested3_1.add("zzzzzzz");
		List<String> dcpTypesRequested3_2 = new ArrayList<String>();
		dcpTypesRequested3_2.add("aaa");
		dcpTypesRequested3_2.add("zzzzzzz");

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
