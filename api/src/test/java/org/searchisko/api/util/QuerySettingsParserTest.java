/*
 * JBoss, Home of Professional Open Source
 * Copyright 2012 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 */
package org.searchisko.api.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Set;

import javax.ws.rs.core.MultivaluedMap;

import org.searchisko.api.model.PastIntervalValue;
import org.searchisko.api.model.QuerySettings;
import org.searchisko.api.model.SortByValue;
import org.searchisko.api.service.SearchService;
import org.searchisko.api.testtools.TestUtils;
import org.jboss.resteasy.specimpl.MultivaluedMapImpl;
import org.junit.Assert;
import org.junit.Test;

/**
 * Unit test for {@link QuerySettingsParser}
 *
 * @author Vlastimil Elias (velias at redhat dot com)
 */
public class QuerySettingsParserTest {

	final static private String CONTENT_TYPE_KEY = "type";
	final static private String SYS_TYPES_KEY = "sys_type";
	final static private String SYS_CONTENT_PROVIDER = "content_provider";
	final static private String PROJECTS_KEY = "project";
	final static private String TAGS_KEY = "tag";
	final static private String CONTRIBUTORS_KEY = "contributor";
	final static private String ACTIVITY_DATE_INTERVAL_KEY = "actvity_date_interval";
	final static private String ACTIVITY_DATE_FROM_KEY = "actvity_date_from";
	final static private String ACTIVITY_DATE_TO_KEY = "actvity_date_to";

	@Test
	public void parseUriParams_common() {

		QuerySettingsParser tested = getTested();

		// null params
		{
			QuerySettings ret = tested.parseUriParams(null);
			Assert.assertNotNull(ret);
			assertQuerySettingsEmpty(ret);
		}

		// empty params
		MultivaluedMap<String, String> params = new MultivaluedMapImpl<String, String>();
		{
			QuerySettings ret = tested.parseUriParams(params);
			Assert.assertNotNull(ret);
			assertQuerySettingsEmpty(ret);
		}

		// all params used
		{
			params.add(CONTENT_TYPE_KEY, "mytype ");
			params.add(SYS_TYPES_KEY, "mySysType ");
			params.add(SYS_TYPES_KEY, " mySysType2");
			params.add(SYS_TYPES_KEY, " ");
			params.add(SYS_CONTENT_PROVIDER, "myprovider ");
			params.add(QuerySettings.QUERY_KEY, "query ** query2");
			params.add(QuerySettings.QUERY_HIGHLIGHT_KEY, "true ");
			params.add(QuerySettings.SORT_BY_KEY, "new");
			params.add(QuerySettings.FIELDS_KEY, "rf1");
			params.add(QuerySettings.FIELDS_KEY, "_rf2 ");
			params.add(PROJECTS_KEY, "proj1 ");
			params.add(PROJECTS_KEY, "proj2");
			params.add(PROJECTS_KEY, " ");
			params.add(QuerySettings.FROM_KEY, "10");
			params.add(QuerySettings.SIZE_KEY, "20");
			params.add(TAGS_KEY, "tg1 ");
			params.add(TAGS_KEY, "tg2");
			params.add(TAGS_KEY, "  ");
			params.add(CONTRIBUTORS_KEY, "John Doe <john@doe.com> ");
			params.add(CONTRIBUTORS_KEY, " Dan Boo <boo@boo.net>");
			params.add(CONTRIBUTORS_KEY, "  ");
			params.add(ACTIVITY_DATE_INTERVAL_KEY, "week");
			params.add(ACTIVITY_DATE_FROM_KEY, "2013-01-26T20:32:36.456Z");
			params.add(ACTIVITY_DATE_TO_KEY, "2013-01-26T20:32:46.456+0100");
			params.add(QuerySettings.AGGREGATION_KEY, "per_project_counts");
			params.add(QuerySettings.AGGREGATION_KEY, "activity_dates_histogram");
			QuerySettings ret = tested.parseUriParams(params);
			Assert.assertNotNull(ret);
			// note - we test here query is sanitized in settings!
			assertQuerySettings(ret, "mytype", "mySysType,mySysType2", "query * query2", SortByValue.NEW, "proj1,proj2", 10,
					20, "tg1,tg2", "myprovider", "John Doe <john@doe.com>,Dan Boo <boo@boo.net>",
					PastIntervalValue.WEEK, "2013-01-26T20:32:36.456Z", "2013-01-26T20:32:46.456+0100",
					"rf1,_rf2", true, "per_project_counts,activity_dates_histogram");
		}
	}

	private QuerySettingsParser getTested() {
		return new QuerySettingsParser();
	}

	private void assertQuerySettingsEmpty(QuerySettings qs) {
		assertQuerySettings(qs, null, null, null, null, null, null, null, null, null, null, null, null, null, null, false,
				null);
	}

	private void assertQuerySettings(QuerySettings qs, String expectedContentType, String expectedSysTypes,
			String expectedQuery, SortByValue expectedSortBy, String expectedFilterProjects, Integer expectedFilterStart,
			Integer expectedFilterCount, String expectedFilterTags, String expectedSysProvider, String expectedContributors,
			PastIntervalValue expectedADInterval, String expectedADFrom, String expectedADTo, String expectedFields,
			boolean expectedQueryHighlight, String expectedAggregations) {

		Assert.assertEquals(expectedQuery, qs.getQuery());
		Assert.assertEquals(expectedQueryHighlight, qs.isQueryHighlight());
		Assert.assertEquals(expectedSortBy, qs.getSortBy());
		Assert.assertEquals(expectedFilterStart, qs.getFrom());
		Assert.assertEquals(expectedFilterCount, qs.getSize());
		TestUtils.assertEqualsListValue(expectedFields, qs.getFields());
		assertEqualsAggregationValueList(expectedAggregations, qs.getAggregations());

		QuerySettings.Filters filters = qs.getFilters();
		Assert.assertNotNull("Filters instance expected not null", filters);
		Assert.assertEquals(expectedContentType, filters.getFirstValueForFilterCandidate(CONTENT_TYPE_KEY));
		TestUtils.assertEqualsListValue(expectedSysTypes, filters.getFilterCandidateValues(SYS_TYPES_KEY));
		TestUtils.assertEqualsListValue(expectedSysProvider, filters.getFilterCandidateValues(SYS_CONTENT_PROVIDER));
		TestUtils.assertEqualsListValue(expectedFilterProjects, filters.getFilterCandidateValues(PROJECTS_KEY));
		TestUtils.assertEqualsListValue(expectedFilterTags, filters.getFilterCandidateValues(TAGS_KEY));
		TestUtils.assertEqualsListValue(expectedContributors, filters.getFilterCandidateValues(CONTRIBUTORS_KEY));
		TestUtils.assertEqualsListValue(expectedADInterval != null ? expectedADInterval.toString() : null, filters.getFilterCandidateValues(ACTIVITY_DATE_INTERVAL_KEY));
		TestUtils.assertEqualsListValue(expectedADFrom, filters.getFilterCandidateValues(ACTIVITY_DATE_FROM_KEY));
		TestUtils.assertEqualsListValue(expectedADTo, filters.getFilterCandidateValues(ACTIVITY_DATE_TO_KEY));
	}

	private void assertEqualsAggregationValueList(String expectedValuesCommaSeparated, Set<String> actualValue) {

		if (expectedValuesCommaSeparated == null) {
			Assert.assertNull(actualValue);
			return;
		}

		String[] expected = expectedValuesCommaSeparated.split(",");

		Assert.assertArrayEquals(expected, actualValue != null ? actualValue.toArray(new String[] {}) : null);
	}

	@Test
	public void parseUriParams_sysType() {
		QuerySettingsParser tested = getTested();
		// case - no param in request
		{
			MultivaluedMap<String, String> params = new MultivaluedMapImpl<String, String>();
			QuerySettings ret = tested.parseUriParams(params);
			Assert.assertNotNull(ret);
			Assert.assertNull(ret.getFilters().getFilterCandidateValues(SYS_TYPES_KEY));
		}
		// case - one empty param in request leads to null
		{
			MultivaluedMap<String, String> params = new MultivaluedMapImpl<String, String>();
			params.add(SYS_TYPES_KEY, " ");
			QuerySettings ret = tested.parseUriParams(params);
			Assert.assertNotNull(ret);
			Assert.assertNull(ret.getFilters().getFilterCandidateValues(SYS_TYPES_KEY));
		}
		// case - more empty params in request lead to null
		{
			MultivaluedMap<String, String> params = new MultivaluedMapImpl<String, String>();
			params.add(SYS_TYPES_KEY, " ");
			params.add(SYS_TYPES_KEY, "");
			QuerySettings ret = tested.parseUriParams(params);
			Assert.assertNotNull(ret);
			Assert.assertNull(ret.getFilters().getFilterCandidateValues(SYS_TYPES_KEY));
		}
	}

	@Test
	public void parseUriParams_projects() {
		QuerySettingsParser tested = getTested();
		// case - no param in request
		{
			MultivaluedMap<String, String> params = new MultivaluedMapImpl<String, String>();
			QuerySettings ret = tested.parseUriParams(params);
			Assert.assertNotNull(ret);
			Assert.assertNull(ret.getFilters().getFilterCandidateValues(PROJECTS_KEY));
		}
		// case - one empty param in request leads to null
		{
			MultivaluedMap<String, String> params = new MultivaluedMapImpl<String, String>();
			params.add(PROJECTS_KEY, " ");
			QuerySettings ret = tested.parseUriParams(params);
			Assert.assertNotNull(ret);
			Assert.assertNull(ret.getFilters().getFilterCandidateValues(PROJECTS_KEY));
		}
		// case - more empty params in request lead to null
		{
			MultivaluedMap<String, String> params = new MultivaluedMapImpl<String, String>();
			params.add(PROJECTS_KEY, " ");
			params.add(PROJECTS_KEY, "");
			QuerySettings ret = tested.parseUriParams(params);
			Assert.assertNotNull(ret);
			Assert.assertNull(ret.getFilters().getFilterCandidateValues(PROJECTS_KEY));
		}
	}

	@Test
	public void parseUriParams_tags() {
		QuerySettingsParser tested = getTested();
		// case - no param in request
		{
			MultivaluedMap<String, String> params = new MultivaluedMapImpl<String, String>();
			QuerySettings ret = tested.parseUriParams(params);
			Assert.assertNotNull(ret);
			Assert.assertNull(ret.getFilters().getFilterCandidateValues(TAGS_KEY));
		}
		// case - one empty param in request leads to null
		{
			MultivaluedMap<String, String> params = new MultivaluedMapImpl<String, String>();
			params.add(TAGS_KEY, " ");
			QuerySettings ret = tested.parseUriParams(params);
			Assert.assertNotNull(ret);
			Assert.assertNull(ret.getFilters().getFilterCandidateValues(TAGS_KEY));
		}
		// case - more empty params in request lead to null
		{
			MultivaluedMap<String, String> params = new MultivaluedMapImpl<String, String>();
			params.add(TAGS_KEY, " ");
			params.add(TAGS_KEY, "");
			QuerySettings ret = tested.parseUriParams(params);
			Assert.assertNotNull(ret);
			Assert.assertNull(ret.getFilters().getFilterCandidateValues(TAGS_KEY));
		}
	}

	@Test
	public void parseUriParams_contributors() {
		QuerySettingsParser tested = getTested();
		// case - no param in request
		{
			MultivaluedMap<String, String> params = new MultivaluedMapImpl<String, String>();
			QuerySettings ret = tested.parseUriParams(params);
			Assert.assertNotNull(ret);
			Assert.assertNull(ret.getFilters().getFilterCandidateValues(CONTRIBUTORS_KEY));
		}
		// case - one empty param in request leads to null
		{
			MultivaluedMap<String, String> params = new MultivaluedMapImpl<String, String>();
			params.add(CONTRIBUTORS_KEY, " ");
			QuerySettings ret = tested.parseUriParams(params);
			Assert.assertNotNull(ret);
			Assert.assertNull(ret.getFilters().getFilterCandidateValues(CONTRIBUTORS_KEY));
		}
		// case - more empty params in request lead to null
		{
			MultivaluedMap<String, String> params = new MultivaluedMapImpl<String, String>();
			params.add(CONTRIBUTORS_KEY, " ");
			params.add(CONTRIBUTORS_KEY, "");
			QuerySettings ret = tested.parseUriParams(params);
			Assert.assertNotNull(ret);
			Assert.assertNull(ret.getFilters().getFilterCandidateValues(CONTRIBUTORS_KEY));
		}
	}

	@Test
	public void parseUriParams_activityDateInterval() {
		QuerySettingsParser tested = getTested();
		// case - no param in request
		{
			MultivaluedMap<String, String> params = new MultivaluedMapImpl<String, String>();
			QuerySettings ret = tested.parseUriParams(params);
			Assert.assertNotNull(ret);
			Assert.assertNull(ret.getFilters().getFilterCandidateValues(ACTIVITY_DATE_INTERVAL_KEY));
		}
		// case - empty param in request leads to null
		{
			MultivaluedMap<String, String> params = new MultivaluedMapImpl<String, String>();
			params.add(ACTIVITY_DATE_INTERVAL_KEY, " ");
			QuerySettings ret = tested.parseUriParams(params);
			Assert.assertNotNull(ret);
			Assert.assertNull(ret.getFilters().getFilterCandidateValues(ACTIVITY_DATE_INTERVAL_KEY));
		}
	}

	@Test
	public void parseUriParams_activityDateFrom() {
		QuerySettingsParser tested = getTested();
		// case - no param in request
		{
			MultivaluedMap<String, String> params = new MultivaluedMapImpl<String, String>();
			QuerySettings ret = tested.parseUriParams(params);
			Assert.assertNotNull(ret);
			Assert.assertNull(ret.getFilters().getFilterCandidateValues(ACTIVITY_DATE_FROM_KEY));
		}
		// case - empty param in request leads to null
		{
			MultivaluedMap<String, String> params = new MultivaluedMapImpl<String, String>();
			params.add(ACTIVITY_DATE_FROM_KEY, " ");
			QuerySettings ret = tested.parseUriParams(params);
			Assert.assertNotNull(ret);
			Assert.assertNull(ret.getFilters().getFilterCandidateValues(ACTIVITY_DATE_FROM_KEY));
		}
	}

	@Test
	public void parseUriParams_activityDateTo() {
		QuerySettingsParser tested = getTested();
		// case - no param in request
		{
			MultivaluedMap<String, String> params = new MultivaluedMapImpl<String, String>();
			QuerySettings ret = tested.parseUriParams(params);
			Assert.assertNotNull(ret);
			Assert.assertNull(ret.getFilters().getFilterCandidateValues(ACTIVITY_DATE_TO_KEY));
		}
		// case - empty param in request leads to null
		{
			MultivaluedMap<String, String> params = new MultivaluedMapImpl<String, String>();
			params.add(ACTIVITY_DATE_TO_KEY, " ");
			QuerySettings ret = tested.parseUriParams(params);
			Assert.assertNotNull(ret);
			Assert.assertNull(ret.getFilters().getFilterCandidateValues(ACTIVITY_DATE_TO_KEY));
		}
	}

	@Test
	public void parseUriParams_sortBy() {
		QuerySettingsParser tested = getTested();
		{
			MultivaluedMap<String, String> params = new MultivaluedMapImpl<String, String>();
			params.add(QuerySettings.SORT_BY_KEY, "new");
			QuerySettings ret = tested.parseUriParams(params);
			Assert.assertNotNull(ret);
			Assert.assertEquals(SortByValue.NEW, ret.getSortBy());
		}
		{
			MultivaluedMap<String, String> params = new MultivaluedMapImpl<String, String>();
			params.add(QuerySettings.SORT_BY_KEY, "old");
			QuerySettings ret = tested.parseUriParams(params);
			Assert.assertNotNull(ret);
			Assert.assertEquals(SortByValue.OLD, ret.getSortBy());
		}
		try {
			MultivaluedMap<String, String> params = new MultivaluedMapImpl<String, String>();
			params.add(QuerySettings.SORT_BY_KEY, "bad");
			tested.parseUriParams(params);
			Assert.fail("IllegalArgumentException expected");
		} catch (IllegalArgumentException e) {
			// OK
		}
	}

	@Test
	public void parseUriParams_aggregation() {
		QuerySettingsParser tested = getTested();
		{
			MultivaluedMap<String, String> params = new MultivaluedMapImpl<>();
			params.add(QuerySettings.AGGREGATION_KEY, "activity_dates_histogram");
			QuerySettings ret = tested.parseUriParams(params);
			Assert.assertNotNull(ret);
			Assert.assertTrue(ret.getAggregations().contains("activity_dates_histogram"));
		}
		{
			MultivaluedMap<String, String> params = new MultivaluedMapImpl<>();
			params.add(QuerySettings.AGGREGATION_KEY, " ");
			QuerySettings ret = tested.parseUriParams(params);
			Assert.assertNotNull(ret);
			Assert.assertNull(ret.getAggregations());
		}
	}

	@Test
	public void parseUriParams_filter_from() {
		QuerySettingsParser tested = getTested();
		{
			MultivaluedMap<String, String> params = new MultivaluedMapImpl<String, String>();
			params.add(QuerySettings.FROM_KEY, "10");
			QuerySettings ret = tested.parseUriParams(params);
			Assert.assertNotNull(ret);
			Assert.assertEquals(new Integer(10), ret.getFrom());
		}
		{
			MultivaluedMap<String, String> params = new MultivaluedMapImpl<String, String>();
			params.add(QuerySettings.FROM_KEY, "0");
			QuerySettings ret = tested.parseUriParams(params);
			Assert.assertNotNull(ret);
			Assert.assertEquals(new Integer(0), ret.getFrom());
		}
		{
			MultivaluedMap<String, String> params = new MultivaluedMapImpl<String, String>();
			params.add(QuerySettings.FROM_KEY, "");
			QuerySettings ret = tested.parseUriParams(params);
			Assert.assertNotNull(ret);
			Assert.assertNull(ret.getFrom());
		}
		// bad format of value
		try {
			MultivaluedMap<String, String> params = new MultivaluedMapImpl<String, String>();
			params.add(QuerySettings.FROM_KEY, "10bad");
			tested.parseUriParams(params);
			Assert.fail("IllegalArgumentException expected");
		} catch (IllegalArgumentException e) {
			// OK
		}
		// value too low
		try {
			MultivaluedMap<String, String> params = new MultivaluedMapImpl<String, String>();
			params.add(QuerySettings.FROM_KEY, "-1");
			tested.parseUriParams(params);
			Assert.fail("IllegalArgumentException expected");
		} catch (IllegalArgumentException e) {
			// OK
		}
	}

	@Test
	public void parseUriParams_filter_size() {
		QuerySettingsParser tested = getTested();
		{
			MultivaluedMap<String, String> params = new MultivaluedMapImpl<String, String>();
			params.add(QuerySettings.SIZE_KEY, "10");
			QuerySettings ret = tested.parseUriParams(params);
			Assert.assertNotNull(ret);
			Assert.assertEquals(new Integer(10), ret.getSize());
		}
		{
			MultivaluedMap<String, String> params = new MultivaluedMapImpl<String, String>();
			params.add(QuerySettings.SIZE_KEY, "0");
			QuerySettings ret = tested.parseUriParams(params);
			Assert.assertNotNull(ret);
			Assert.assertEquals(new Integer(0), ret.getSize());
		}
		{
			MultivaluedMap<String, String> params = new MultivaluedMapImpl<String, String>();
			params.add(QuerySettings.SIZE_KEY, "" + SearchService.RESPONSE_MAX_SIZE);
			QuerySettings ret = tested.parseUriParams(params);
			Assert.assertNotNull(ret);
			Assert.assertEquals(new Integer(SearchService.RESPONSE_MAX_SIZE), ret.getSize());
		}
		{
			MultivaluedMap<String, String> params = new MultivaluedMapImpl<String, String>();
			params.add(QuerySettings.SIZE_KEY, "");
			QuerySettings ret = tested.parseUriParams(params);
			Assert.assertNotNull(ret);
			Assert.assertNull(ret.getSize());
		}
		// bad format of value
		try {
			MultivaluedMap<String, String> params = new MultivaluedMapImpl<String, String>();
			params.add(QuerySettings.SIZE_KEY, "10bad");
			tested.parseUriParams(params);
			Assert.fail("IllegalArgumentException expected");
		} catch (IllegalArgumentException e) {
			// OK
		}
		// too low value
		try {
			MultivaluedMap<String, String> params = new MultivaluedMapImpl<String, String>();
			params.add(QuerySettings.SIZE_KEY, "-1");
			tested.parseUriParams(params);
			Assert.fail("IllegalArgumentException expected");
		} catch (IllegalArgumentException e) {
			// OK
		}
		// too high value
		try {
			MultivaluedMap<String, String> params = new MultivaluedMapImpl<String, String>();
			params.add(QuerySettings.SIZE_KEY, (SearchService.RESPONSE_MAX_SIZE + 1) + "");
			tested.parseUriParams(params);
			Assert.fail("IllegalArgumentException expected");
		} catch (IllegalArgumentException e) {
			// OK
		}
	}

	@Test
	public void parseUriParams_field() {
		QuerySettingsParser tested = getTested();
		// case - no param in request
		{
			MultivaluedMap<String, String> params = new MultivaluedMapImpl<String, String>();
			QuerySettings ret = tested.parseUriParams(params);
			Assert.assertNotNull(ret);
			Assert.assertNull(ret.getFields());
		}
		// case - one empty param in request leads to null
		{
			MultivaluedMap<String, String> params = new MultivaluedMapImpl<String, String>();
			params.add(QuerySettings.FIELDS_KEY, " ");
			QuerySettings ret = tested.parseUriParams(params);
			Assert.assertNotNull(ret);
			Assert.assertNull(ret.getFields());
		}
		// case - more empty params in request lead to null
		{
			MultivaluedMap<String, String> params = new MultivaluedMapImpl<String, String>();
			params.add(QuerySettings.FIELDS_KEY, " ");
			params.add(QuerySettings.FIELDS_KEY, "");
			QuerySettings ret = tested.parseUriParams(params);
			Assert.assertNotNull(ret);
			Assert.assertNull(ret.getFields());
		}
	}

	@Test
	public void normalizeQueryString() {
		QuerySettingsParser tested = getTested();
		Assert.assertNull(tested.normalizeQueryString(null));
		Assert.assertNull(tested.normalizeQueryString(""));
		Assert.assertNull(tested.normalizeQueryString("    "));
		Assert.assertEquals("trim test", tested.normalizeQueryString("  trim test  "));

		// case - wildchar normalization
		Assert.assertEquals("trim* test *", tested.normalizeQueryString("  trim** test ** "));
		Assert.assertEquals("trim? *test ?", tested.normalizeQueryString("  trim??? **test ?? "));

		Assert.assertEquals("? * ? * * *", tested.normalizeQueryString("??? ** ?? ?* ** *? "));
	}

	@Test
	public void sanityQuery() {
		QuerySettingsParser tested = getTested();
		try {
			tested.sanityQuery(null);
			Assert.fail("IllegalArgumentException expected");
		} catch (IllegalArgumentException e) {
			// OK
		}

		QuerySettings settings = new QuerySettings();

		// case - query is null
		settings.setQuery(null);
		tested.sanityQuery(settings);
		Assert.assertEquals("match_all:{}", settings.getQuery());

		// case - query is empty
		settings.setQuery("");
		tested.sanityQuery(settings);
		Assert.assertEquals("", settings.getQuery());
		settings.setQuery("   ");
		tested.sanityQuery(settings);
		Assert.assertEquals("", settings.getQuery());

		// case - trimming
		settings.setQuery(" test query  ");
		tested.sanityQuery(settings);
		Assert.assertEquals("test query", settings.getQuery());

		// case - wildchar normalization
		settings.setQuery(" test ** ?? * query *? ?* ** ?  ");
		tested.sanityQuery(settings);
		Assert.assertEquals("test * ? * query * * * ?", settings.getQuery());
	}

	@Test
	public void normalizeListParam() {
		QuerySettingsParser tested = getTested();
		Assert.assertNull(tested.normalizeListParam(null));
		Assert.assertNull(tested.normalizeListParam(new ArrayList<String>()));
		Assert.assertNull(tested.normalizeListParam(Arrays.asList("")));
		Assert.assertNull(tested.normalizeListParam(Arrays.asList("", " ", " \n  \t")));
		Assert.assertArrayEquals(new String[] { "ahoj" }, tested.normalizeListParam(Arrays.asList("", " ", "ahoj "))
				.toArray());
		Assert.assertArrayEquals(new String[] { "ahoj", "cao" },
				tested.normalizeListParam(Arrays.asList("", " ", "ahoj ", "\tcao ")).toArray());

	}

	@Test
	public void readIntegerParam() {
		QuerySettingsParser tested = getTested();
		Assert.assertNull(tested.readIntegerParam(null, "key"));

		MultivaluedMap<String, String> params = new MultivaluedMapImpl<String, String>();
		Assert.assertNull(tested.readIntegerParam(params, "key"));

		params.add("key", "");
		Assert.assertNull(tested.readIntegerParam(params, "key"));

		params.clear();
		params.add("key", "  \t");
		Assert.assertNull(tested.readIntegerParam(params, "key"));

		params.clear();
		params.add("key", "10");
		Assert.assertEquals(new Integer(10), tested.readIntegerParam(params, "key"));

		params.clear();
		params.add("key", "10err");
		try {
			tested.readIntegerParam(params, "key");
			Assert.fail("IllegalArgumentException expected");
		} catch (IllegalArgumentException e) {
			// OK
		}
	}

	@Test
	public void readBooleanParam() {
		QuerySettingsParser tested = getTested();
		Assert.assertFalse(tested.readBooleanParam(null, "key"));

		MultivaluedMap<String, String> params = new MultivaluedMapImpl<String, String>();
		Assert.assertFalse(tested.readBooleanParam(params, "key"));

		params.clear();
		params.add("key", "");
		Assert.assertFalse(tested.readBooleanParam(params, "key"));

		params.clear();
		params.add("key", "false");
		Assert.assertFalse(tested.readBooleanParam(params, "key"));

		params.clear();
		params.add("key", "False");
		Assert.assertFalse(tested.readBooleanParam(params, "key"));

		params.clear();
		params.add("key", "nonsense");
		Assert.assertFalse(tested.readBooleanParam(params, "key"));

		params.clear();
		params.add("key", "true");
		Assert.assertTrue(tested.readBooleanParam(params, "key"));

		params.clear();
		params.add("key", " True");
		Assert.assertTrue(tested.readBooleanParam(params, "key"));

		params.clear();
		params.add("key", "TRUE ");
		Assert.assertTrue(tested.readBooleanParam(params, "key"));
	}

	@Test
	public void readDateParam() {
		QuerySettingsParser tested = getTested();
		Assert.assertNull(tested.readDateParam(null, "key"));

		MultivaluedMap<String, String> params = new MultivaluedMapImpl<String, String>();
		Assert.assertNull(tested.readDateParam(params, "key"));

		params.add("key", "");
		Assert.assertNull(tested.readDateParam(params, "key"));

		params.clear();
		params.add("key", "  \t");
		Assert.assertNull(tested.readDateParam(params, "key"));

		params.clear();
		params.add("key", "2013-01-16T12:23:45.454Z");
		Assert.assertEquals(new Long(1358339025454L), tested.readDateParam(params, "key"));

		params.clear();
		params.add("key", "2013-01-16T12:23:45Z");
		Assert.assertEquals(new Long(1358339025000L), tested.readDateParam(params, "key"));

		params.clear();
		params.add("key", "2013-01-16T12:23:45.454+0100");
		Assert.assertEquals(new Long(1358335425454L), tested.readDateParam(params, "key"));

		params.clear();
		params.add("key", "2013-01-16T12:23:45-0500");
		Assert.assertEquals(new Long(1358357025000L), tested.readDateParam(params, "key"));

		params.clear();
		params.add("key", "2/2/2013");
		try {
			tested.readDateParam(params, "key");
			Assert.fail("IllegalArgumentException expected");
		} catch (IllegalArgumentException e) {
			// OK
		}

	}

}
