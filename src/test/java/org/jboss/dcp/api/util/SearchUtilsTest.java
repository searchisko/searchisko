/*
 * JBoss, Home of Professional Open Source
 * Copyright 2013 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 */
package org.jboss.dcp.api.util;

import org.junit.Assert;
import org.junit.Test;

/**
 * Unit test for {@link SearchUtils}
 * 
 * @author Vlastimil Elias (velias at redhat dot com)
 */
public class SearchUtilsTest {

	@Test
	public void trimmToNull() {
		Assert.assertNull(SearchUtils.trimmToNull(null));
		Assert.assertNull(SearchUtils.trimmToNull(""));
		Assert.assertNull(SearchUtils.trimmToNull(" "));
		Assert.assertNull(SearchUtils.trimmToNull("     \t "));

		Assert.assertEquals("a", SearchUtils.trimmToNull("a"));
		Assert.assertEquals("a", SearchUtils.trimmToNull("a "));
		Assert.assertEquals("a", SearchUtils.trimmToNull(" a"));
		Assert.assertEquals("abcd aaa", SearchUtils.trimmToNull("   abcd aaa \t   "));
	}

}
