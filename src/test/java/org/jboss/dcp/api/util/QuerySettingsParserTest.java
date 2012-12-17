/*
 * JBoss, Home of Professional Open Source
 * Copyright 2012 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 */
package org.jboss.dcp.api.util;

import junit.framework.Assert;

import org.jboss.dcp.api.model.QuerySettings;
import org.jboss.dcp.api.util.QuerySettingsParser.MonthIntervalNames;
import org.jboss.dcp.api.util.QuerySettingsParser.PastIntervalNames;
import org.junit.Test;

/**
 * Unit test for {@link QuerySettingsParser}
 * 
 * @author Vlastimil Elias (velias at redhat dot com)
 */
public class QuerySettingsParserTest {

	@Test
	public void value_MonthIntervalNames() {
		Assert.assertEquals(3, MonthIntervalNames.values().length);
		Assert.assertEquals("day", MonthIntervalNames.DAY.toString());
		Assert.assertEquals("1w", MonthIntervalNames.WEEK.toString());
		Assert.assertEquals("month", MonthIntervalNames.MONTH.toString());
	}

	@Test
	public void value_PastIntervalNames() {
		Assert.assertEquals(4, PastIntervalNames.values().length);
		Assert.assertEquals("week", PastIntervalNames.WEEK.toString());
		Assert.assertEquals("month", PastIntervalNames.MONTH.toString());
		Assert.assertEquals("quarter", PastIntervalNames.QUARTER.toString());
		Assert.assertEquals("year", PastIntervalNames.YEAR.toString());
	}

	@Test
	public void normalizeQueryString() {
		Assert.assertNull(QuerySettingsParser.normalizeQueryString(null));
		Assert.assertNull(QuerySettingsParser.normalizeQueryString(""));
		Assert.assertNull(QuerySettingsParser.normalizeQueryString("    "));
		Assert.assertEquals("trim test", QuerySettingsParser.normalizeQueryString("  trim test  "));

		// case - wildchar normalization
		Assert.assertEquals("trim* test *", QuerySettingsParser.normalizeQueryString("  trim** test ** "));
		Assert.assertEquals("trim? *test ?", QuerySettingsParser.normalizeQueryString("  trim??? **test ?? "));

		Assert.assertEquals("? * ? * * *", QuerySettingsParser.normalizeQueryString("??? ** ?? ?* ** *? "));
	}

	@Test
	public void sanityQuery() {
		try {
			QuerySettingsParser.sanityQuery(null);
			Assert.fail("IllegalArgumentException expected");
		} catch (IllegalArgumentException e) {
			// OK
		}

		QuerySettings settings = new QuerySettings();

		// case - query is null
		settings.setQuery(null);
		QuerySettingsParser.sanityQuery(settings);
		Assert.assertEquals("match_all:{}", settings.getQuery());

		// case - query is empty
		settings.setQuery("");
		QuerySettingsParser.sanityQuery(settings);
		Assert.assertEquals("", settings.getQuery());
		settings.setQuery("   ");
		QuerySettingsParser.sanityQuery(settings);
		Assert.assertEquals("", settings.getQuery());

		// case - trimming
		settings.setQuery(" test query  ");
		QuerySettingsParser.sanityQuery(settings);
		Assert.assertEquals("test query", settings.getQuery());

		// case - wildchar normalization
		settings.setQuery(" test ** ?? * query *? ?* ** ?  ");
		QuerySettingsParser.sanityQuery(settings);
		Assert.assertEquals("test * ? * query * * * ?", settings.getQuery());
	}

}
