/*
 * JBoss, Home of Professional Open Source
 * Copyright 2013 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 */
package org.searchisko.api.model;

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
		Assert.assertEquals(8, PastIntervalValue.values().length);
		Assert.assertEquals("day", PastIntervalValue.DAY.toString());
		Assert.assertEquals("week", PastIntervalValue.WEEK.toString());
		Assert.assertEquals("month", PastIntervalValue.MONTH.toString());
		Assert.assertEquals("quarter", PastIntervalValue.QUARTER.toString());
		Assert.assertEquals("year", PastIntervalValue.YEAR.toString());
		Assert.assertEquals("null", PastIntervalValue.NULL.toString());
		Assert.assertEquals("undefined", PastIntervalValue.UNDEFINED.toString());
	}

	@Test
	public void parseRequestParameterValue() {
		Assert.assertEquals(PastIntervalValue.NULL,PastIntervalValue.parseRequestParameterValue(null));
		Assert.assertEquals(PastIntervalValue.NULL,PastIntervalValue.parseRequestParameterValue(" "));
		Assert.assertEquals(PastIntervalValue.NULL,PastIntervalValue.parseRequestParameterValue(" \t\n"));
		for (PastIntervalValue n : PastIntervalValue.values()) {
			Assert.assertEquals(n, PastIntervalValue.parseRequestParameterValue(n.toString()));
		}
		Assert.assertEquals(PastIntervalValue.UNDEFINED,PastIntervalValue.parseRequestParameterValue("unknown"));
	}

	@Test
	public void getGteValue() {
		long now = System.currentTimeMillis();
		long v = PastIntervalValue.DAY.getGteValue(now);
		long day = 1000L * 60L * 60L * 24L;
		Assert.assertTrue((now - day) == v);
		Assert.assertTrue((now - day - 1) < v);
	}

	@Test
	public void getLteValue() {
		long now = System.currentTimeMillis();
		long v = PastIntervalValue.DAY.getLteValue(now);
		long day = 1000L * 60L * 60L * 24L;
		Assert.assertTrue((now - day) == v);
		Assert.assertTrue((now - day - 1) < v);
	}

}
