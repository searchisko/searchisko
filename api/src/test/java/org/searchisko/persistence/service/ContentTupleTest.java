/*
 * JBoss, Home of Professional Open Source
 * Copyright 2014 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 */
package org.searchisko.persistence.service;

import org.junit.Assert;
import org.junit.Test;
import org.searchisko.persistence.service.ContentTuple;

/**
 * Unit test for {@link ContentTuple}.
 * 
 * @author Vlastimil Elias (velias at redhat dot com)
 */
public class ContentTupleTest {

	@Test(expected = IllegalArgumentException.class)
	public void constructor_idMandatory() {
		new ContentTuple<>(null, "content");
	}

	@Test
	public void getters() {

		ContentTuple<String, ?> tested = new ContentTuple<>("a", null);
		Assert.assertEquals("a", tested.getId());
		Assert.assertEquals(null, tested.getContent());

		ContentTuple<String, ?> tested2 = new ContentTuple<>("b", "content");
		Assert.assertEquals("b", tested2.getId());
		Assert.assertEquals("content", tested2.getContent());
	}

	@Test
	public void equals_hashCode() {

		ContentTuple<String, ?> tested = new ContentTuple<>("a", null);
		ContentTuple<String, ?> tested2 = new ContentTuple<>("a", "content");
		ContentTuple<String, ?> tested3 = new ContentTuple<>("b", "content");

		Assert.assertTrue(tested.equals(tested));
		Assert.assertTrue(tested.equals(tested2));
		Assert.assertTrue(tested2.equals(tested));
		Assert.assertEquals(tested2.hashCode(), tested.hashCode());

		Assert.assertFalse(tested.equals(tested3));
		Assert.assertFalse(tested3.equals(tested));

		Assert.assertFalse(tested.equals(null));
		Assert.assertFalse(tested.equals("a"));
	}

}
