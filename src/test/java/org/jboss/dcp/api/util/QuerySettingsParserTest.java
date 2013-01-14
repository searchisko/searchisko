/*
 * JBoss, Home of Professional Open Source
 * Copyright 2012 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 */
package org.jboss.dcp.api.util;

import javax.ws.rs.core.MultivaluedMap;

import org.jboss.dcp.api.model.QuerySettings;
import org.jboss.dcp.api.model.QuerySettings.SortByValue;
import org.jboss.dcp.api.util.QuerySettingsParser.MonthIntervalNames;
import org.jboss.dcp.api.util.QuerySettingsParser.PastIntervalNames;
import org.jboss.resteasy.specimpl.MultivaluedMapImpl;
import org.junit.Assert;
import org.junit.Test;

/**
 * Unit test for {@link QuerySettingsParser}
 * 
 * @author Vlastimil Elias (velias at redhat dot com)
 */
public class QuerySettingsParserTest {

	@Test
	public void parseUriParams_common() {

		// null params
		{
			QuerySettings ret = QuerySettingsParser.parseUriParams(null);
			Assert.assertNotNull(ret);
			assertQuerySettingsEmpty(ret);
		}

		// empty params
		MultivaluedMap<String, String> params = new MultivaluedMapImpl<String, String>();
		{
			QuerySettings ret = QuerySettingsParser.parseUriParams(params);
			Assert.assertNotNull(ret);
			assertQuerySettingsEmpty(ret);
		}

		// all params used
		{
			params.add(QuerySettings.Filters.CONTENT_TYPE_KEY, "mytype ");
			params.add(QuerySettings.Filters.DCP_TYPE_KEY, "myDcpType ");
			params.add(QuerySettings.Filters.DCP_CONTENT_PROVIDER, "myprovider ");
			params.add(QuerySettings.QUERY_KEY, "query ** query2");
			params.add(QuerySettings.SORT_BY_KEY, "new");
			params.add(QuerySettings.Filters.PROJECTS_KEY, "proj1");
			params.add(QuerySettings.Filters.PROJECTS_KEY, "proj2");
			params.add(QuerySettings.Filters.START_KEY, "10");
			params.add(QuerySettings.Filters.COUNT_KEY, "20");
			params.add(QuerySettings.Filters.TAGS_KEY, "tg1");
			params.add(QuerySettings.Filters.TAGS_KEY, "tg2");
			QuerySettings ret = QuerySettingsParser.parseUriParams(params);
			Assert.assertNotNull(ret);
			// note query is sanitized in settings!
			assertQuerySettings(ret, "mytype", "myDcpType", "query * query2", SortByValue.NEW, "proj1,proj2", 10, 20,
					"tg1,tg2", "myprovider");
		}
	}

	private void assertQuerySettingsEmpty(QuerySettings qs) {
		assertQuerySettings(qs, null, null, null, null, null, null, null, null, null);
	}

	private void assertQuerySettings(QuerySettings qs, String expectedContentType, String expectedDcpType,
			String expectedQuery, SortByValue expectedSortBy, String expectedFilterProjects, Integer expectedFilterStart,
			Integer expectedFilterCount, String expectedFilterTags, String expectedDcpProvider) {

		Assert.assertEquals(expectedQuery, qs.getQuery());
		Assert.assertEquals(expectedSortBy, qs.getSortBy());
		QuerySettings.Filters filters = qs.getFilters();
		Assert.assertNotNull("Filters instance expected not null", filters);
		Assert.assertEquals(expectedContentType, filters.getContentType());
		Assert.assertEquals(expectedDcpType, filters.getDcpType());
		Assert.assertEquals(expectedDcpProvider, filters.getDcpContentProvider());
		Assert.assertArrayEquals(expectedFilterProjects != null ? expectedFilterProjects.split(",") : null,
				filters.getProjects() != null ? filters.getProjects().toArray(new String[] {}) : null);
		Assert.assertEquals(expectedFilterStart, filters.getStart());
		Assert.assertEquals(expectedFilterCount, filters.getCount());
		Assert.assertArrayEquals(expectedFilterTags != null ? expectedFilterTags.split(",") : null,
				filters.getTags() != null ? filters.getTags().toArray(new String[] {}) : null);

	}

	@Test
	public void parseUriParams_sortBy() {
		{
			MultivaluedMap<String, String> params = new MultivaluedMapImpl<String, String>();
			params.add(QuerySettings.SORT_BY_KEY, "new");
			QuerySettings ret = QuerySettingsParser.parseUriParams(params);
			Assert.assertNotNull(ret);
			Assert.assertEquals(SortByValue.NEW, ret.getSortBy());
		}
		{
			MultivaluedMap<String, String> params = new MultivaluedMapImpl<String, String>();
			params.add(QuerySettings.SORT_BY_KEY, "old");
			QuerySettings ret = QuerySettingsParser.parseUriParams(params);
			Assert.assertNotNull(ret);
			Assert.assertEquals(SortByValue.OLD, ret.getSortBy());
		}
		try {
			MultivaluedMap<String, String> params = new MultivaluedMapImpl<String, String>();
			params.add(QuerySettings.SORT_BY_KEY, "bad");
			QuerySettingsParser.parseUriParams(params);
			Assert.fail("IllegalArgumentException expected");
		} catch (IllegalArgumentException e) {
			// OK
		}
	}

	@Test
	public void parseUriParams_filter_start() {
		{
			MultivaluedMap<String, String> params = new MultivaluedMapImpl<String, String>();
			params.add(QuerySettings.Filters.START_KEY, "10");
			QuerySettings ret = QuerySettingsParser.parseUriParams(params);
			Assert.assertNotNull(ret);
			Assert.assertEquals(new Integer(10), ret.getFilters().getStart());
		}
		try {
			MultivaluedMap<String, String> params = new MultivaluedMapImpl<String, String>();
			params.add(QuerySettings.Filters.START_KEY, "10bad");
			QuerySettingsParser.parseUriParams(params);
			Assert.fail("IllegalArgumentException expected");
		} catch (IllegalArgumentException e) {
			// OK
		}
	}

	@Test
	public void parseUriParams_filter_count() {
		{
			MultivaluedMap<String, String> params = new MultivaluedMapImpl<String, String>();
			params.add(QuerySettings.Filters.COUNT_KEY, "10");
			QuerySettings ret = QuerySettingsParser.parseUriParams(params);
			Assert.assertNotNull(ret);
			Assert.assertEquals(new Integer(10), ret.getFilters().getCount());
		}
		try {
			MultivaluedMap<String, String> params = new MultivaluedMapImpl<String, String>();
			params.add(QuerySettings.Filters.COUNT_KEY, "10bad");
			QuerySettingsParser.parseUriParams(params);
			Assert.fail("IllegalArgumentException expected");
		} catch (IllegalArgumentException e) {
			// OK
		}
	}

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

	@Test
	public void trimmToNull() {
		Assert.assertNull(QuerySettingsParser.trimmToNull(null));
		Assert.assertNull(QuerySettingsParser.trimmToNull(""));
		Assert.assertNull(QuerySettingsParser.trimmToNull(" "));
		Assert.assertNull(QuerySettingsParser.trimmToNull("     \t "));

		Assert.assertEquals("a", QuerySettingsParser.trimmToNull("a"));
		Assert.assertEquals("a", QuerySettingsParser.trimmToNull("a "));
		Assert.assertEquals("a", QuerySettingsParser.trimmToNull(" a"));
		Assert.assertEquals("abcd aaa", QuerySettingsParser.trimmToNull("   abcd aaa \t   "));
	}

}
