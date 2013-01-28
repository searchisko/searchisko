/*
 * JBoss, Home of Professional Open Source
 * Copyright 2013 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 */
package org.jboss.dcp.api.model;

import org.junit.Assert;
import org.junit.Test;

/**
 * Unit test for {@link FacetValue}
 * 
 * @author Vlastimil Elias (velias at redhat dot com)
 */
public class FacetValueTest {

	@Test
	public void values() {
		Assert.assertEquals(5, FacetValue.values().length);
		Assert.assertEquals("activity_dates_histogram", FacetValue.ACTIVITY_DATES_HISTOGRAM.toString());
		Assert.assertEquals("per_dcp_type_counts", FacetValue.PER_DCP_TYPE_COUNTS.toString());
		Assert.assertEquals("per_project_counts", FacetValue.PER_PROJECT_COUNTS.toString());
		Assert.assertEquals("tag_cloud", FacetValue.TAG_CLOUD.toString());
		Assert.assertEquals("top_contributors", FacetValue.TOP_CONTRIBUTORS.toString());
	}

	@Test
	public void parseRequestParameterValue() {
		Assert.assertNull(FacetValue.parseRequestParameterValue(null));
		Assert.assertNull(FacetValue.parseRequestParameterValue(" "));
		Assert.assertNull(FacetValue.parseRequestParameterValue(" \t\n"));
		for (FacetValue n : FacetValue.values()) {
			Assert.assertEquals(n, FacetValue.parseRequestParameterValue(n.toString()));
		}
		try {
			FacetValue.parseRequestParameterValue("unknown");
			Assert.fail("IllegalArgumentException expected");
		} catch (IllegalArgumentException e) {
			// OK
		}
	}

}
