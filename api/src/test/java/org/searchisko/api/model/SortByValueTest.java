/*
 * JBoss, Home of Professional Open Source
 * Copyright 2013 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 */
package org.searchisko.api.model;

import org.junit.Assert;
import org.junit.Test;

/**
 * Unit test for {@link SortByValue}.
 *
 * @author Vlastimil Elias (velias at redhat dot com)
 */
public class SortByValueTest {

	@Test
	public void values() {
		Assert.assertEquals(4, SortByValue.values().length);
		Assert.assertEquals("new", SortByValue.NEW.toString());
		Assert.assertEquals("old", SortByValue.OLD.toString());
		Assert.assertEquals("new-create", SortByValue.NEW_CREATION.toString());
		Assert.assertEquals("score", SortByValue.SCORE.toString());
	}

	@Test
	public void parseRequestParameterValue() {
		Assert.assertNull(SortByValue.parseRequestParameterValue(null));
		Assert.assertNull(SortByValue.parseRequestParameterValue(" "));
		Assert.assertNull(SortByValue.parseRequestParameterValue(" \t\n"));
		for (SortByValue n : SortByValue.values()) {
			Assert.assertEquals(n, SortByValue.parseRequestParameterValue(n.toString()));
		}
		try {
			SortByValue.parseRequestParameterValue("unknown");
			Assert.fail("IllegalArgumentException expected");
		} catch (IllegalArgumentException e) {
			// OK
		}
	}

}
