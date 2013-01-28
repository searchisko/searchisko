/*
 * JBoss, Home of Professional Open Source
 * Copyright 2013 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 */
package org.jboss.dcp.api.model;

import org.junit.Assert;
import org.junit.Test;

/**
 * Unit test for {@link PastIntervalValue}
 * 
 * @author Vlastimil Elias (velias at redhat dot com)
 */
public class PastIntervalValueTest {

	@Test
	public void values() {
		Assert.assertEquals(6, PastIntervalValue.values().length);
		Assert.assertEquals("day", PastIntervalValue.DAY.toString());
		Assert.assertEquals("week", PastIntervalValue.WEEK.toString());
		Assert.assertEquals("month", PastIntervalValue.MONTH.toString());
		Assert.assertEquals("quarter", PastIntervalValue.QUARTER.toString());
		Assert.assertEquals("year", PastIntervalValue.YEAR.toString());
	}

	@Test
	public void parseRequestParameterValue() {
		Assert.assertNull(PastIntervalValue.parseRequestParameterValue(null));
		Assert.assertNull(PastIntervalValue.parseRequestParameterValue(" "));
		Assert.assertNull(PastIntervalValue.parseRequestParameterValue(" \t\n"));
		for (PastIntervalValue n : PastIntervalValue.values()) {
			Assert.assertEquals(n, PastIntervalValue.parseRequestParameterValue(n.toString()));
		}
		try {
			PastIntervalValue.parseRequestParameterValue("unknown");
			Assert.fail("IllegalArgumentException expected");
		} catch (IllegalArgumentException e) {
			// OK
		}
	}

	@Test
	public void getFromTimestamp() {
		long check = System.currentTimeMillis() - 1000L * 60L * 60L * 24L;
		long v = PastIntervalValue.DAY.getFromTimestamp();
		Assert.assertTrue((check - 500) < v);
		Assert.assertTrue(v < (check + 500));
	}

}
