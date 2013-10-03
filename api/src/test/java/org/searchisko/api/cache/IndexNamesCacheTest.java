/*
 * JBoss, Home of Professional Open Source
 * Copyright 2013 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 */
package org.searchisko.api.cache;

import java.util.HashSet;
import java.util.Set;

import org.junit.Assert;
import org.junit.Test;

/**
 * Unit test for {@link IndexNamesCache}.
 *
 * @author Vlastimil Elias (velias at redhat dot com)
 */
public class IndexNamesCacheTest {

	@Test
	public void cacheWorks() throws InterruptedException {

		IndexNamesCache tested = new IndexNamesCache();
		tested.ttl = 500;

		Set<String> indexNames = new HashSet<String>();
		Set<String> indexNames2 = new HashSet<String>();
		Set<String> indexNames3 = new HashSet<String>();
		String sysTypesRequested = "a";
		String sysTypesRequested2_1 = "aaa";
		String sysTypesRequested2_2 = "aaa";

		String sysTypesRequested3_1 = "aaa|zzzzz";
		String sysTypesRequested3_2 = "aaa|zzzzz";

		tested.put(sysTypesRequested, indexNames);
		tested.put(sysTypesRequested2_1, indexNames2);
		tested.put(sysTypesRequested3_1, indexNames3);
		Assert.assertEquals(indexNames, tested.get(sysTypesRequested));
		Assert.assertEquals(indexNames2, tested.get(sysTypesRequested2_1));
		Assert.assertEquals(indexNames2, tested.get(sysTypesRequested2_2));
		Assert.assertEquals(indexNames3, tested.get(sysTypesRequested3_1));
		Assert.assertEquals(indexNames3, tested.get(sysTypesRequested3_2));
		Thread.sleep(600);
		Assert.assertNull(tested.get(sysTypesRequested));
		Assert.assertNull(tested.get(sysTypesRequested2_1));
		Assert.assertNull(tested.get(sysTypesRequested2_2));
		Assert.assertNull(tested.get(sysTypesRequested3_1));
		Assert.assertNull(tested.get(sysTypesRequested3_2));
	}

}
