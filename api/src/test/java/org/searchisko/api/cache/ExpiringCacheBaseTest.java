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
 * Unit test for {@link ExpiringCacheBase}.
 *
 * @author Vlastimil Elias (velias at redhat dot com)
 */
public class ExpiringCacheBaseTest {

	@Test
	public void cacheWorks() throws InterruptedException {

		ExpiringCacheBase<Set<String>> tested = new ExpiringCacheBase<Set<String>>() {
		};
		tested.ttl = 500;

		Set<String> value = new HashSet<String>();
		Set<String> value2 = new HashSet<String>();
		Set<String> value3 = new HashSet<String>();
		String key = "a";
		String key2_1 = "aaa";
		String key2_2 = "aaa";

		String key3_1 = "aaa|zzzzz";
		String key3_2 = "aaa|zzzzz";

		tested.put(key, value);
		tested.put(key2_1, value2);
		tested.put(key3_1, value3);
		Assert.assertEquals(value, tested.get(key));
		Assert.assertEquals(value2, tested.get(key2_1));
		Assert.assertEquals(value2, tested.get(key2_2));
		Assert.assertEquals(value3, tested.get(key3_1));
		Assert.assertEquals(value3, tested.get(key3_2));
		Thread.sleep(600);
		Assert.assertNull(tested.get(key));
		Assert.assertNull(tested.get(key2_1));
		Assert.assertNull(tested.get(key2_2));
		Assert.assertNull(tested.get(key3_1));
		Assert.assertNull(tested.get(key3_2));
	}

	@Test
	public void flush() {
		ExpiringCacheBase<Set<String>> tested = new ExpiringCacheBase<Set<String>>() {
		};
		tested.ttl = 50000;

		Set<String> value = new HashSet<String>();
		Set<String> value2 = new HashSet<String>();
		Set<String> value3 = new HashSet<String>();
		String key = "a";
		String key2_1 = "aaa";
		String key2_2 = "aaa";

		String key3_1 = "aaa|zzzzz";
		String key3_2 = "aaa|zzzzz";

		tested.put(key, value);
		tested.put(key2_1, value2);
		tested.put(key3_1, value3);
		Assert.assertEquals(value, tested.get(key));
		Assert.assertEquals(value2, tested.get(key2_1));
		Assert.assertEquals(value2, tested.get(key2_2));
		Assert.assertEquals(value3, tested.get(key3_1));
		Assert.assertEquals(value3, tested.get(key3_2));

		tested.flush();
		Assert.assertNull(tested.get(key));
		Assert.assertNull(tested.get(key2_1));
		Assert.assertNull(tested.get(key2_2));
		Assert.assertNull(tested.get(key3_1));
		Assert.assertNull(tested.get(key3_2));

	}
}
