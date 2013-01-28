/*
 * JBoss, Home of Professional Open Source
 * Copyright 2012 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 */
package org.jboss.dcp.api.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.ws.rs.core.MultivaluedMap;

import org.jboss.dcp.api.model.FacetValue;
import org.jboss.dcp.api.model.PastIntervalValue;
import org.jboss.dcp.api.model.QuerySettings;
import org.jboss.dcp.api.model.SortByValue;
import org.jboss.dcp.api.rest.SearchRestService;
import org.jboss.dcp.api.testtools.TestUtils;
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
			params.add(QuerySettings.Filters.DCP_TYPES_KEY, "myDcpType ");
			params.add(QuerySettings.Filters.DCP_TYPES_KEY, " myDcpType2");
			params.add(QuerySettings.Filters.DCP_TYPES_KEY, " ");
			params.add(QuerySettings.Filters.DCP_CONTENT_PROVIDER, "myprovider ");
			params.add(QuerySettings.QUERY_KEY, "query ** query2");
			params.add(QuerySettings.QUERY_HIGHLIGHT_KEY, "true ");
			params.add(QuerySettings.SORT_BY_KEY, "new");
			params.add(QuerySettings.FIELDS_KEY, "rf1");
			params.add(QuerySettings.FIELDS_KEY, "_rf2 ");
			params.add(QuerySettings.Filters.PROJECTS_KEY, "proj1 ");
			params.add(QuerySettings.Filters.PROJECTS_KEY, "proj2");
			params.add(QuerySettings.Filters.PROJECTS_KEY, " ");
			params.add(QuerySettings.Filters.FROM_KEY, "10");
			params.add(QuerySettings.Filters.SIZE_KEY, "20");
			params.add(QuerySettings.Filters.TAGS_KEY, "tg1 ");
			params.add(QuerySettings.Filters.TAGS_KEY, "tg2");
			params.add(QuerySettings.Filters.TAGS_KEY, "  ");
			params.add(QuerySettings.Filters.CONTRIBUTORS_KEY, "John Doe <john@doe.com> ");
			params.add(QuerySettings.Filters.CONTRIBUTORS_KEY, " Dan Boo <boo@boo.net>");
			params.add(QuerySettings.Filters.CONTRIBUTORS_KEY, "  ");
			params.add(QuerySettings.Filters.ACTIVITY_DATE_INTERVAL_KEY, "week");
			params.add(QuerySettings.Filters.ACTIVITY_DATE_FROM_KEY, "2013-01-26T20:32:36.456Z");
			params.add(QuerySettings.Filters.ACTIVITY_DATE_TO_KEY, "2013-01-26T20:32:46.456+0100");
			params.add(QuerySettings.FACETS_KEY, "per_project_counts");
			params.add(QuerySettings.FACETS_KEY, "activity_dates_histogram");
			QuerySettings ret = QuerySettingsParser.parseUriParams(params);
			Assert.assertNotNull(ret);
			// note - we test here query is sanitized in settings!
			assertQuerySettings(ret, "mytype", "myDcpType,myDcpType2", "query * query2", SortByValue.NEW, "proj1,proj2", 10,
					20, "tg1,tg2", "myprovider", "John Doe <john@doe.com>,Dan Boo <boo@boo.net>", PastIntervalValue.WEEK,
					1359232356456L, 1359228766456L, "rf1,_rf2", true, "per_project_counts,activity_dates_histogram");
		}
	}

	private void assertQuerySettingsEmpty(QuerySettings qs) {
		assertQuerySettings(qs, null, null, null, null, null, null, null, null, null, null, null, null, null, null, false,
				null);
	}

	private void assertQuerySettings(QuerySettings qs, String expectedContentType, String expectedDcpTypes,
			String expectedQuery, SortByValue expectedSortBy, String expectedFilterProjects, Integer expectedFilterStart,
			Integer expectedFilterCount, String expectedFilterTags, String expectedDcpProvider, String expectedContributors,
			PastIntervalValue expectedADInterval, Long expectedADFrom, Long expectedADTo, String expectedFields,
			boolean expectedQueryHighlight, String expectedFacets) {

		Assert.assertEquals(expectedQuery, qs.getQuery());
		Assert.assertEquals(expectedQueryHighlight, qs.isQueryHighlight());
		Assert.assertEquals(expectedSortBy, qs.getSortBy());
		QuerySettings.Filters filters = qs.getFilters();
		Assert.assertNotNull("Filters instance expected not null", filters);
		Assert.assertEquals(expectedContentType, filters.getContentType());
		TestUtils.assertEqualsListValue(expectedDcpTypes, filters.getDcpTypes());
		Assert.assertEquals(expectedDcpProvider, filters.getDcpContentProvider());
		TestUtils.assertEqualsListValue(expectedFilterProjects, filters.getProjects());
		Assert.assertEquals(expectedFilterStart, filters.getFrom());
		Assert.assertEquals(expectedFilterCount, filters.getSize());
		TestUtils.assertEqualsListValue(expectedFilterTags, filters.getTags());
		TestUtils.assertEqualsListValue(expectedContributors, filters.getContributors());
		Assert.assertEquals(expectedADInterval, filters.getActivityDateInterval());
		Assert.assertEquals(expectedADFrom, filters.getActivityDateFrom());
		Assert.assertEquals(expectedADTo, filters.getActivityDateTo());
		TestUtils.assertEqualsListValue(expectedFields, qs.getFields());
		assertEqualsFacetValueList(expectedFacets, qs.getFacets());
	}

	private void assertEqualsFacetValueList(String expectedValuesCommaSeparated, List<FacetValue> actualValue) {

		if (expectedValuesCommaSeparated == null) {
			Assert.assertNull(actualValue);
			return;
		}

		List<FacetValue> expected = new ArrayList<FacetValue>();
		for (String s : expectedValuesCommaSeparated.split(",")) {
			expected.add(FacetValue.parseRequestParameterValue(s));
		}

		Assert.assertArrayEquals(expected.toArray(new FacetValue[expected.size()]),
				actualValue != null ? actualValue.toArray(new FacetValue[] {}) : null);
	}

	@Test
	public void parseUriParams_dcpType() {
		// case - no param in request
		{
			MultivaluedMap<String, String> params = new MultivaluedMapImpl<String, String>();
			QuerySettings ret = QuerySettingsParser.parseUriParams(params);
			Assert.assertNotNull(ret);
			Assert.assertNull(ret.getFilters().getDcpTypes());
		}
		// case - one empty param in request leads to null
		{
			MultivaluedMap<String, String> params = new MultivaluedMapImpl<String, String>();
			params.add(QuerySettings.Filters.DCP_TYPES_KEY, " ");
			QuerySettings ret = QuerySettingsParser.parseUriParams(params);
			Assert.assertNotNull(ret);
			Assert.assertNull(ret.getFilters().getDcpTypes());
		}
		// case - more empty params in request lead to null
		{
			MultivaluedMap<String, String> params = new MultivaluedMapImpl<String, String>();
			params.add(QuerySettings.Filters.DCP_TYPES_KEY, " ");
			params.add(QuerySettings.Filters.DCP_TYPES_KEY, "");
			QuerySettings ret = QuerySettingsParser.parseUriParams(params);
			Assert.assertNotNull(ret);
			Assert.assertNull(ret.getFilters().getDcpTypes());
		}
	}

	@Test
	public void parseUriParams_projects() {
		// case - no param in request
		{
			MultivaluedMap<String, String> params = new MultivaluedMapImpl<String, String>();
			QuerySettings ret = QuerySettingsParser.parseUriParams(params);
			Assert.assertNotNull(ret);
			Assert.assertNull(ret.getFilters().getProjects());
		}
		// case - one empty param in request leads to null
		{
			MultivaluedMap<String, String> params = new MultivaluedMapImpl<String, String>();
			params.add(QuerySettings.Filters.PROJECTS_KEY, " ");
			QuerySettings ret = QuerySettingsParser.parseUriParams(params);
			Assert.assertNotNull(ret);
			Assert.assertNull(ret.getFilters().getProjects());
		}
		// case - more empty params in request lead to null
		{
			MultivaluedMap<String, String> params = new MultivaluedMapImpl<String, String>();
			params.add(QuerySettings.Filters.PROJECTS_KEY, " ");
			params.add(QuerySettings.Filters.PROJECTS_KEY, "");
			QuerySettings ret = QuerySettingsParser.parseUriParams(params);
			Assert.assertNotNull(ret);
			Assert.assertNull(ret.getFilters().getProjects());
		}
	}

	@Test
	public void parseUriParams_tags() {
		// case - no param in request
		{
			MultivaluedMap<String, String> params = new MultivaluedMapImpl<String, String>();
			QuerySettings ret = QuerySettingsParser.parseUriParams(params);
			Assert.assertNotNull(ret);
			Assert.assertNull(ret.getFilters().getTags());
		}
		// case - one empty param in request leads to null
		{
			MultivaluedMap<String, String> params = new MultivaluedMapImpl<String, String>();
			params.add(QuerySettings.Filters.TAGS_KEY, " ");
			QuerySettings ret = QuerySettingsParser.parseUriParams(params);
			Assert.assertNotNull(ret);
			Assert.assertNull(ret.getFilters().getTags());
		}
		// case - more empty params in request lead to null
		{
			MultivaluedMap<String, String> params = new MultivaluedMapImpl<String, String>();
			params.add(QuerySettings.Filters.TAGS_KEY, " ");
			params.add(QuerySettings.Filters.TAGS_KEY, "");
			QuerySettings ret = QuerySettingsParser.parseUriParams(params);
			Assert.assertNotNull(ret);
			Assert.assertNull(ret.getFilters().getTags());
		}
	}

	@Test
	public void parseUriParams_contributors() {
		// case - no param in request
		{
			MultivaluedMap<String, String> params = new MultivaluedMapImpl<String, String>();
			QuerySettings ret = QuerySettingsParser.parseUriParams(params);
			Assert.assertNotNull(ret);
			Assert.assertNull(ret.getFilters().getContributors());
		}
		// case - one empty param in request leads to null
		{
			MultivaluedMap<String, String> params = new MultivaluedMapImpl<String, String>();
			params.add(QuerySettings.Filters.CONTRIBUTORS_KEY, " ");
			QuerySettings ret = QuerySettingsParser.parseUriParams(params);
			Assert.assertNotNull(ret);
			Assert.assertNull(ret.getFilters().getContributors());
		}
		// case - more empty params in request lead to null
		{
			MultivaluedMap<String, String> params = new MultivaluedMapImpl<String, String>();
			params.add(QuerySettings.Filters.CONTRIBUTORS_KEY, " ");
			params.add(QuerySettings.Filters.CONTRIBUTORS_KEY, "");
			QuerySettings ret = QuerySettingsParser.parseUriParams(params);
			Assert.assertNotNull(ret);
			Assert.assertNull(ret.getFilters().getContributors());
		}
	}

	@Test
	public void parseUriParams_activityDateInterval() {
		// case - no param in request
		{
			MultivaluedMap<String, String> params = new MultivaluedMapImpl<String, String>();
			QuerySettings ret = QuerySettingsParser.parseUriParams(params);
			Assert.assertNotNull(ret);
			Assert.assertNull(ret.getFilters().getActivityDateInterval());
		}
		// case - empty param in request leads to null
		{
			MultivaluedMap<String, String> params = new MultivaluedMapImpl<String, String>();
			params.add(QuerySettings.Filters.ACTIVITY_DATE_INTERVAL_KEY, " ");
			QuerySettings ret = QuerySettingsParser.parseUriParams(params);
			Assert.assertNotNull(ret);
			Assert.assertNull(ret.getFilters().getActivityDateInterval());
		}
		// case - bad param in request leads to exception
		try {
			MultivaluedMap<String, String> params = new MultivaluedMapImpl<String, String>();
			params.add(QuerySettings.Filters.ACTIVITY_DATE_INTERVAL_KEY, "bad");
			QuerySettingsParser.parseUriParams(params);
			Assert.fail("IllegalArgumentException expected");
		} catch (IllegalArgumentException e) {
			// OK
		}
	}

	@Test
	public void parseUriParams_activityDateFrom() {
		// case - no param in request
		{
			MultivaluedMap<String, String> params = new MultivaluedMapImpl<String, String>();
			QuerySettings ret = QuerySettingsParser.parseUriParams(params);
			Assert.assertNotNull(ret);
			Assert.assertNull(ret.getFilters().getActivityDateFrom());
		}
		// case - empty param in request leads to null
		{
			MultivaluedMap<String, String> params = new MultivaluedMapImpl<String, String>();
			params.add(QuerySettings.Filters.ACTIVITY_DATE_FROM_KEY, " ");
			QuerySettings ret = QuerySettingsParser.parseUriParams(params);
			Assert.assertNotNull(ret);
			Assert.assertNull(ret.getFilters().getActivityDateFrom());
		}
		// case - bad param in request leads to exception
		try {
			MultivaluedMap<String, String> params = new MultivaluedMapImpl<String, String>();
			params.add(QuerySettings.Filters.ACTIVITY_DATE_FROM_KEY, "bad");
			QuerySettingsParser.parseUriParams(params);
			Assert.fail("IllegalArgumentException expected");
		} catch (IllegalArgumentException e) {
			// OK
		}
	}

	@Test
	public void parseUriParams_activityDateTo() {
		// case - no param in request
		{
			MultivaluedMap<String, String> params = new MultivaluedMapImpl<String, String>();
			QuerySettings ret = QuerySettingsParser.parseUriParams(params);
			Assert.assertNotNull(ret);
			Assert.assertNull(ret.getFilters().getActivityDateTo());
		}
		// case - empty param in request leads to null
		{
			MultivaluedMap<String, String> params = new MultivaluedMapImpl<String, String>();
			params.add(QuerySettings.Filters.ACTIVITY_DATE_TO_KEY, " ");
			QuerySettings ret = QuerySettingsParser.parseUriParams(params);
			Assert.assertNotNull(ret);
			Assert.assertNull(ret.getFilters().getActivityDateTo());
		}
		// case - bad param in request leads to exception
		try {
			MultivaluedMap<String, String> params = new MultivaluedMapImpl<String, String>();
			params.add(QuerySettings.Filters.ACTIVITY_DATE_TO_KEY, "bad");
			QuerySettingsParser.parseUriParams(params);
			Assert.fail("IllegalArgumentException expected");
		} catch (IllegalArgumentException e) {
			// OK
		}
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
	public void parseUriParams_facet() {
		{
			MultivaluedMap<String, String> params = new MultivaluedMapImpl<String, String>();
			params.add(QuerySettings.FACETS_KEY, FacetValue.ACTIVITY_DATES_HISTOGRAM.toString());
			QuerySettings ret = QuerySettingsParser.parseUriParams(params);
			Assert.assertNotNull(ret);
			Assert.assertEquals(FacetValue.ACTIVITY_DATES_HISTOGRAM, ret.getFacets().get(0));
		}
		{
			MultivaluedMap<String, String> params = new MultivaluedMapImpl<String, String>();
			params.add(QuerySettings.FACETS_KEY, " ");
			QuerySettings ret = QuerySettingsParser.parseUriParams(params);
			Assert.assertNotNull(ret);
			Assert.assertNull(ret.getFacets());
		}
		try {
			MultivaluedMap<String, String> params = new MultivaluedMapImpl<String, String>();
			params.add(QuerySettings.FACETS_KEY, "bad");
			QuerySettingsParser.parseUriParams(params);
			Assert.fail("IllegalArgumentException expected");
		} catch (IllegalArgumentException e) {
			// OK
		}
	}

	@Test
	public void parseUriParams_filter_from() {
		{
			MultivaluedMap<String, String> params = new MultivaluedMapImpl<String, String>();
			params.add(QuerySettings.Filters.FROM_KEY, "10");
			QuerySettings ret = QuerySettingsParser.parseUriParams(params);
			Assert.assertNotNull(ret);
			Assert.assertEquals(new Integer(10), ret.getFilters().getFrom());
		}
		{
			MultivaluedMap<String, String> params = new MultivaluedMapImpl<String, String>();
			params.add(QuerySettings.Filters.FROM_KEY, "0");
			QuerySettings ret = QuerySettingsParser.parseUriParams(params);
			Assert.assertNotNull(ret);
			Assert.assertEquals(new Integer(0), ret.getFilters().getFrom());
		}
		{
			MultivaluedMap<String, String> params = new MultivaluedMapImpl<String, String>();
			params.add(QuerySettings.Filters.FROM_KEY, "");
			QuerySettings ret = QuerySettingsParser.parseUriParams(params);
			Assert.assertNotNull(ret);
			Assert.assertNull(ret.getFilters().getFrom());
		}
		// bad format of value
		try {
			MultivaluedMap<String, String> params = new MultivaluedMapImpl<String, String>();
			params.add(QuerySettings.Filters.FROM_KEY, "10bad");
			QuerySettingsParser.parseUriParams(params);
			Assert.fail("IllegalArgumentException expected");
		} catch (IllegalArgumentException e) {
			// OK
		}
		// value too low
		try {
			MultivaluedMap<String, String> params = new MultivaluedMapImpl<String, String>();
			params.add(QuerySettings.Filters.FROM_KEY, "-1");
			QuerySettingsParser.parseUriParams(params);
			Assert.fail("IllegalArgumentException expected");
		} catch (IllegalArgumentException e) {
			// OK
		}
	}

	@Test
	public void parseUriParams_filter_size() {
		{
			MultivaluedMap<String, String> params = new MultivaluedMapImpl<String, String>();
			params.add(QuerySettings.Filters.SIZE_KEY, "10");
			QuerySettings ret = QuerySettingsParser.parseUriParams(params);
			Assert.assertNotNull(ret);
			Assert.assertEquals(new Integer(10), ret.getFilters().getSize());
		}
		{
			MultivaluedMap<String, String> params = new MultivaluedMapImpl<String, String>();
			params.add(QuerySettings.Filters.SIZE_KEY, "0");
			QuerySettings ret = QuerySettingsParser.parseUriParams(params);
			Assert.assertNotNull(ret);
			Assert.assertEquals(new Integer(0), ret.getFilters().getSize());
		}
		{
			MultivaluedMap<String, String> params = new MultivaluedMapImpl<String, String>();
			params.add(QuerySettings.Filters.SIZE_KEY, "" + SearchRestService.RESPONSE_MAX_SIZE);
			QuerySettings ret = QuerySettingsParser.parseUriParams(params);
			Assert.assertNotNull(ret);
			Assert.assertEquals(new Integer(SearchRestService.RESPONSE_MAX_SIZE), ret.getFilters().getSize());
		}
		{
			MultivaluedMap<String, String> params = new MultivaluedMapImpl<String, String>();
			params.add(QuerySettings.Filters.SIZE_KEY, "");
			QuerySettings ret = QuerySettingsParser.parseUriParams(params);
			Assert.assertNotNull(ret);
			Assert.assertNull(ret.getFilters().getSize());
		}
		// bad format of value
		try {
			MultivaluedMap<String, String> params = new MultivaluedMapImpl<String, String>();
			params.add(QuerySettings.Filters.SIZE_KEY, "10bad");
			QuerySettingsParser.parseUriParams(params);
			Assert.fail("IllegalArgumentException expected");
		} catch (IllegalArgumentException e) {
			// OK
		}
		// too low value
		try {
			MultivaluedMap<String, String> params = new MultivaluedMapImpl<String, String>();
			params.add(QuerySettings.Filters.SIZE_KEY, "-1");
			QuerySettingsParser.parseUriParams(params);
			Assert.fail("IllegalArgumentException expected");
		} catch (IllegalArgumentException e) {
			// OK
		}
		// too high value
		try {
			MultivaluedMap<String, String> params = new MultivaluedMapImpl<String, String>();
			params.add(QuerySettings.Filters.SIZE_KEY, (SearchRestService.RESPONSE_MAX_SIZE + 1) + "");
			QuerySettingsParser.parseUriParams(params);
			Assert.fail("IllegalArgumentException expected");
		} catch (IllegalArgumentException e) {
			// OK
		}
	}

	@Test
	public void parseUriParams_field() {
		// case - no param in request
		{
			MultivaluedMap<String, String> params = new MultivaluedMapImpl<String, String>();
			QuerySettings ret = QuerySettingsParser.parseUriParams(params);
			Assert.assertNotNull(ret);
			Assert.assertNull(ret.getFields());
		}
		// case - one empty param in request leads to null
		{
			MultivaluedMap<String, String> params = new MultivaluedMapImpl<String, String>();
			params.add(QuerySettings.FIELDS_KEY, " ");
			QuerySettings ret = QuerySettingsParser.parseUriParams(params);
			Assert.assertNotNull(ret);
			Assert.assertNull(ret.getFields());
		}
		// case - more empty params in request lead to null
		{
			MultivaluedMap<String, String> params = new MultivaluedMapImpl<String, String>();
			params.add(QuerySettings.FIELDS_KEY, " ");
			params.add(QuerySettings.FIELDS_KEY, "");
			QuerySettings ret = QuerySettingsParser.parseUriParams(params);
			Assert.assertNotNull(ret);
			Assert.assertNull(ret.getFields());
		}
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
	public void normalizeListParam() {
		Assert.assertNull(QuerySettingsParser.normalizeListParam(null));
		Assert.assertNull(QuerySettingsParser.normalizeListParam(new ArrayList<String>()));
		Assert.assertNull(QuerySettingsParser.normalizeListParam(Arrays.asList("")));
		Assert.assertNull(QuerySettingsParser.normalizeListParam(Arrays.asList("", " ", " \n  \t")));
		Assert.assertArrayEquals(new String[] { "ahoj" },
				QuerySettingsParser.normalizeListParam(Arrays.asList("", " ", "ahoj ")).toArray());
		Assert.assertArrayEquals(new String[] { "ahoj", "cao" },
				QuerySettingsParser.normalizeListParam(Arrays.asList("", " ", "ahoj ", "\tcao ")).toArray());

	}

	@Test
	public void readIntegerParam() {
		Assert.assertNull(QuerySettingsParser.readIntegerParam(null, "key"));

		MultivaluedMap<String, String> params = new MultivaluedMapImpl<String, String>();
		Assert.assertNull(QuerySettingsParser.readIntegerParam(params, "key"));

		params.add("key", "");
		Assert.assertNull(QuerySettingsParser.readIntegerParam(params, "key"));

		params.clear();
		params.add("key", "  \t");
		Assert.assertNull(QuerySettingsParser.readIntegerParam(params, "key"));

		params.clear();
		params.add("key", "10");
		Assert.assertEquals(new Integer(10), QuerySettingsParser.readIntegerParam(params, "key"));

		params.clear();
		params.add("key", "10err");
		try {
			QuerySettingsParser.readIntegerParam(params, "key");
			Assert.fail("IllegalArgumentException expected");
		} catch (IllegalArgumentException e) {
			// OK
		}
	}

	@Test
	public void readBooleanParam() {
		Assert.assertFalse(QuerySettingsParser.readBooleanParam(null, "key"));

		MultivaluedMap<String, String> params = new MultivaluedMapImpl<String, String>();
		Assert.assertFalse(QuerySettingsParser.readBooleanParam(params, "key"));

		params.clear();
		params.add("key", "");
		Assert.assertFalse(QuerySettingsParser.readBooleanParam(params, "key"));

		params.clear();
		params.add("key", "false");
		Assert.assertFalse(QuerySettingsParser.readBooleanParam(params, "key"));

		params.clear();
		params.add("key", "False");
		Assert.assertFalse(QuerySettingsParser.readBooleanParam(params, "key"));

		params.clear();
		params.add("key", "nonsense");
		Assert.assertFalse(QuerySettingsParser.readBooleanParam(params, "key"));

		params.clear();
		params.add("key", "true");
		Assert.assertTrue(QuerySettingsParser.readBooleanParam(params, "key"));

		params.clear();
		params.add("key", " True");
		Assert.assertTrue(QuerySettingsParser.readBooleanParam(params, "key"));

		params.clear();
		params.add("key", "TRUE ");
		Assert.assertTrue(QuerySettingsParser.readBooleanParam(params, "key"));
	}

	@Test
	public void readDateParam() {
		Assert.assertNull(QuerySettingsParser.readDateParam(null, "key"));

		MultivaluedMap<String, String> params = new MultivaluedMapImpl<String, String>();
		Assert.assertNull(QuerySettingsParser.readDateParam(params, "key"));

		params.add("key", "");
		Assert.assertNull(QuerySettingsParser.readDateParam(params, "key"));

		params.clear();
		params.add("key", "  \t");
		Assert.assertNull(QuerySettingsParser.readDateParam(params, "key"));

		params.clear();
		params.add("key", "2013-01-16T12:23:45.454Z");
		Assert.assertEquals(new Long(1358339025454L), QuerySettingsParser.readDateParam(params, "key"));

		params.clear();
		params.add("key", "2013-01-16T12:23:45Z");
		Assert.assertEquals(new Long(1358339025000L), QuerySettingsParser.readDateParam(params, "key"));

		params.clear();
		params.add("key", "2013-01-16T12:23:45.454+0100");
		Assert.assertEquals(new Long(1358335425454L), QuerySettingsParser.readDateParam(params, "key"));

		params.clear();
		params.add("key", "2013-01-16T12:23:45-0500");
		Assert.assertEquals(new Long(1358357025000L), QuerySettingsParser.readDateParam(params, "key"));

		params.clear();
		params.add("key", "2/2/2013");
		try {
			QuerySettingsParser.readDateParam(params, "key");
			Assert.fail("IllegalArgumentException expected");
		} catch (IllegalArgumentException e) {
			// OK
		}

	}

}
