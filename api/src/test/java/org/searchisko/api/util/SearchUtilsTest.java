/*
 * JBoss, Home of Professional Open Source
 * Copyright 2013 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 */
package org.searchisko.api.util;

import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.map.JsonMappingException;
import org.elasticsearch.common.joda.time.LocalDateTime;
import org.junit.Assert;
import org.junit.Test;
import org.searchisko.api.testtools.TestUtils;

import java.io.IOException;
import java.text.ParseException;
import java.util.*;

/**
 * Unit test for {@link SearchUtils}.
 * 
 * @author Vlastimil Elias (velias at redhat dot com)
 */
public class SearchUtilsTest {

	@Test
	public void dateFromISOString() throws ParseException {
		Assert.assertNull(SearchUtils.dateFromISOString(null, false));
		Assert.assertNull(SearchUtils.dateFromISOString(null, true));

		try {
			Assert.assertNull(SearchUtils.dateFromISOString("", false));
			Assert.fail("IllegalArgumentException expected");
		} catch (IllegalArgumentException e) {
			// OK
		}
		Assert.assertNull(SearchUtils.dateFromISOString("", true));

		try {
			Assert.assertNull(SearchUtils.dateFromISOString("badvalue", false));
			Assert.fail("IllegalArgumentException expected");
		} catch (IllegalArgumentException e) {
			// OK
		}
		// case - silent mode test
		Assert.assertNull(SearchUtils.dateFromISOString("badvalue", true));

		Assert.assertEquals(1361386810123l, SearchUtils.dateFromISOString("2013-02-20T20:00:10.123+0100", false).getTime());
		Assert.assertEquals(1361386810123l, SearchUtils.dateFromISOString("2013-02-20T20:00:10.123+01", false).getTime());
		Assert
				.assertEquals(1361386810123l, SearchUtils.dateFromISOString("2013-02-20T20:00:10.123+01:00", false).getTime());

		Assert.assertEquals(1361390410123l, SearchUtils.dateFromISOString("2013-02-20T20:00:10.123Z", false).getTime());

	}

	@Test
	public void isDateBefore() {
		Assert.assertTrue(SearchUtils.isDateAfter(new Date(), 1));
		Assert.assertFalse(SearchUtils.isDateAfter(SearchUtils.dateFromISOString("2013-02-20T20:00:10.123+0100", false), 10));
		Assert.assertTrue(SearchUtils.isDateAfter(SearchUtils.dateFromISOString("2100-02-20T20:00:10.123+0100", false), 10));
		LocalDateTime now = new LocalDateTime();
		Assert.assertTrue(SearchUtils.isDateAfter(now.minusMinutes(9).toDate(), 10));

	}

	@Test
	public void trimToNull() {
		Assert.assertNull(SearchUtils.trimToNull(null));
		Assert.assertNull(SearchUtils.trimToNull(""));
		Assert.assertNull(SearchUtils.trimToNull(" "));
		Assert.assertNull(SearchUtils.trimToNull("     \t "));

		Assert.assertEquals("a", SearchUtils.trimToNull("a"));
		Assert.assertEquals("a", SearchUtils.trimToNull("a "));
		Assert.assertEquals("a", SearchUtils.trimToNull(" a"));
		Assert.assertEquals("abcd aaa", SearchUtils.trimToNull("   abcd aaa \t   "));
	}

	@Test
	public void isBlank() {
		Assert.assertTrue(SearchUtils.isBlank(null));
		Assert.assertTrue(SearchUtils.isBlank(""));
		Assert.assertTrue(SearchUtils.isBlank(" "));
		Assert.assertTrue(SearchUtils.isBlank("  "));
		Assert.assertTrue(SearchUtils.isBlank("   \t"));

		Assert.assertFalse(SearchUtils.isBlank("a"));
		Assert.assertFalse(SearchUtils.isBlank("Z"));
		Assert.assertFalse(SearchUtils.isBlank("1"));
		Assert.assertFalse(SearchUtils.isBlank("-"));
	}

	@Test
	public void convertJsonMapToString() throws IOException {

		{
			Assert.assertEquals("", SearchUtils.convertJsonMapToString(null));
		}

		Map<String, Object> data = new HashMap<String, Object>();
		{
			TestUtils.assertJsonContent("{}", SearchUtils.convertJsonMapToString(data));
		}

		// case - conversion of distinct basic data types
		{
			Map<String, Object> sysTitleConfig = new HashMap<String, Object>();
			sysTitleConfig.put("fragment_size", "-1");
			sysTitleConfig.put("number_of_fragments", "0");
			sysTitleConfig.put("fragment_offset", 0);
			sysTitleConfig.put("fragment_offset_long", new Long(25l));
			sysTitleConfig.put("boolean", true);
			sysTitleConfig.put("boolean2", false);
			sysTitleConfig.put("date", new Date(65132498465l));
			data.put("sys_title", sysTitleConfig);
			TestUtils.assertJsonContent("{\"sys_title\":{" + "\"fragment_size\":\"-1\"," + "\"number_of_fragments\":\"0\","
					+ "\"fragment_offset\":0," + "\"fragment_offset_long\":25," + "\"boolean\":true," + "\"boolean2\":false,"
					+ "\"date\":\"1972-01-24T20:21:38.465+0000\"" + "}}", SearchUtils.convertJsonMapToString(data));
		}

		// case - conversion of List data type
		Map<String, Object> map = new HashMap<String, Object>();
		List<String> l = new ArrayList<String>();
		l.add("aaa");
		l.add("bbb");
		map.put("l", l);
		TestUtils.assertJsonContent("{\"l\":[\"aaa\",\"bbb\"]}", SearchUtils.convertJsonMapToString(map));
	}

	@SuppressWarnings("unchecked")
	@Test
	public void convertToJsonMap() throws JsonParseException, JsonMappingException, IOException {
		Assert.assertNull(SearchUtils.convertToJsonMap(null));
		try {
			SearchUtils.convertToJsonMap("");
			Assert.fail("IOException expected");
		} catch (IOException e) {
			// OK
		}
		try {
			SearchUtils.convertToJsonMap("sdfqwrewr");
			Assert.fail("JsonParseException expected");
		} catch (JsonParseException e) {
			// OK
		}
		try {
			SearchUtils.convertToJsonMap("{aers");
			Assert.fail("JsonParseException expected");
		} catch (JsonParseException e) {
			// OK
		}

		try {
			SearchUtils.convertToJsonMap("{ \"dsdsd\": true,}");
			Assert.fail("JsonParseException expected");
		} catch (JsonParseException e) {
			// OK
		}

		// case - basic data types
		{
			Map<String, Object> m = SearchUtils
					.convertToJsonMap("{ \"test\": true, \"int\":10, \"str\":\"string\", \"datestring\":\"1972-01-24T20:21:38.465+0000\"}");
			Assert.assertEquals(4, m.size());
			Assert.assertEquals(true, m.get("test"));
			Assert.assertEquals(10, m.get("int"));
			Assert.assertEquals("string", m.get("str"));
			Assert.assertEquals("1972-01-24T20:21:38.465+0000", m.get("datestring"));
		}

		// case - array JSON type to List java type
		{
			Map<String, Object> m = SearchUtils
					.convertToJsonMap("{ \"test\": [true, 10, \"string\", \"1972-01-24T20:21:38.465+0000\"]}");
			Assert.assertEquals(1, m.size());
			List<Object> l = (List<Object>) m.get("test");
			Assert.assertEquals(true, l.get(0));
			Assert.assertEquals(10, l.get(1));
			Assert.assertEquals("string", l.get(2));
			Assert.assertEquals("1972-01-24T20:21:38.465+0000", l.get(3));
		}
	}

	private static final String KEY = "key";

	@Test
	public void getIntegerFromJsonMap() {
		Assert.assertNull(SearchUtils.getIntegerFromJsonMap(null, KEY));
		Map<String, Object> m = new HashMap<>();
		Assert.assertNull(SearchUtils.getIntegerFromJsonMap(m, KEY));

		m.put(KEY, new Integer(10));
		Assert.assertEquals(new Integer(10), SearchUtils.getIntegerFromJsonMap(m, KEY));
		m.put(KEY, new Long(12));
		Assert.assertEquals(new Integer(12), SearchUtils.getIntegerFromJsonMap(m, KEY));
		m.put(KEY, new Float(15));
		Assert.assertEquals(new Integer(15), SearchUtils.getIntegerFromJsonMap(m, KEY));
		m.put(KEY, "20");
		Assert.assertEquals(new Integer(20), SearchUtils.getIntegerFromJsonMap(m, KEY));
		m.put(KEY, "-20");
		Assert.assertEquals(new Integer(-20), SearchUtils.getIntegerFromJsonMap(m, KEY));

		m.put(KEY, "a");
		try {
			SearchUtils.getIntegerFromJsonMap(m, KEY);
			Assert.fail("NumberFormatException must be thrown");
		} catch (NumberFormatException e) {
			// OK
		}

		m.put(KEY, new Object());
		try {
			SearchUtils.getIntegerFromJsonMap(m, KEY);
			Assert.fail("NumberFormatException must be thrown");
		} catch (NumberFormatException e) {
			// OK
		}
	}

	@SuppressWarnings("rawtypes")
	@Test
	public void mergeJsonMaps_basic() {

		// case - call is null safe and do not modify nothing in this case
		{
			Map<String, Object> source = new HashMap<>();
			source.put("ks", "vs");
			Map<String, Object> target = new HashMap<>();
			target.put("kt", "vt");
			SearchUtils.mergeJsonMaps(source, null);
			SearchUtils.mergeJsonMaps(null, target);
			Assert.assertEquals(1, source.size());
			Assert.assertEquals(1, target.size());
		}

		// case - simple value, same key in source and target so list is created for different values but not for same
		{
			Map<String, Object> source = new HashMap<>();
			Map<String, Object> target = new HashMap<>();
			// create List for these two values for same key
			source.put("key1", "v1");
			target.put("key1", "v2");
			// common merge of distinct keys
			source.put("key2", "v22");
			target.put("key3", "v33");
			source.put("key4", "v44");
			target.put("key5", "v55");
			// do not create List for these two values for same key as they are same
			source.put("key6", "v1");
			target.put("key6", "v1");
			SearchUtils.mergeJsonMaps(source, target);
			Assert.assertEquals(6, target.size());
			Assert.assertTrue(target.get("key1") instanceof List);
			List l = (List) target.get("key1");
			Assert.assertTrue(l.contains("v1"));
			Assert.assertTrue(l.contains("v2"));
			Assert.assertEquals("v22", target.get("key2"));
			Assert.assertEquals("v33", target.get("key3"));
			Assert.assertEquals("v44", target.get("key4"));
			Assert.assertEquals("v55", target.get("key5"));
			Assert.assertEquals("v1", target.get("key6"));
		}
	}

	@SuppressWarnings("rawtypes")
	@Test
	public void mergeJsonMaps_Lists() {

		// case - simple value in source, List in target - must be merged
		{
			Map<String, Object> source = new HashMap<>();
			Map<String, Object> target = new HashMap<>();
			source.put("key1", "v1");
			target.put("key1", TestUtils.createListOfStrings("v2", "v3"));
			SearchUtils.mergeJsonMaps(source, target);
			Assert.assertTrue(target.get("key1") instanceof List);
			List tl = (List) target.get("key1");
			Assert.assertEquals(3, tl.size());
			Assert.assertTrue(tl.contains("v1"));
			Assert.assertTrue(tl.contains("v2"));
			Assert.assertTrue(tl.contains("v3"));
		}

		// case - simple value in source duplicate to value in List in target - must be merged without duplication
		{
			Map<String, Object> source = new HashMap<>();
			Map<String, Object> target = new HashMap<>();
			source.put("key1", "v1");
			target.put("key1", TestUtils.createListOfStrings("v1", "v3"));
			SearchUtils.mergeJsonMaps(source, target);
			Assert.assertTrue(target.get("key1") instanceof List);
			List tl = (List) target.get("key1");
			Assert.assertEquals(2, tl.size());
			Assert.assertTrue(tl.contains("v1"));
			Assert.assertTrue(tl.contains("v3"));
		}

		// case - map in source, List in target - must be merged
		{
			Map<String, Object> source = new HashMap<>();
			Map<String, Object> target = new HashMap<>();
			Map sourceMap = new HashMap<>();
			source.put("key1", sourceMap);
			target.put("key1", TestUtils.createListOfStrings("v1", "v3"));
			SearchUtils.mergeJsonMaps(source, target);
			Assert.assertTrue(target.get("key1") instanceof List);
			List tl = (List) target.get("key1");
			Assert.assertEquals(3, tl.size());
			Assert.assertTrue(tl.contains("v1"));
			Assert.assertTrue(tl.contains("v3"));
			Assert.assertTrue(tl.contains(sourceMap));
		}

		// case - List in source (with simple values and Map) with duplicates to values in List in target - must be merged
		// without duplication.
		{
			Map<String, Object> source = new HashMap<>();
			Map<String, Object> target = new HashMap<>();
			List<Object> sourceList = new ArrayList<>();
			sourceList.add("v1");
			sourceList.add("v2");
			Map mapValue = new HashMap<>();
			sourceList.add(mapValue);
			source.put("key1", sourceList);
			target.put("key1", TestUtils.createListOfStrings("v1", "v3"));
			SearchUtils.mergeJsonMaps(source, target);
			Assert.assertTrue(target.get("key1") instanceof List);
			List tl = (List) target.get("key1");
			Assert.assertEquals(4, tl.size());
			Assert.assertTrue(tl.contains("v1"));
			Assert.assertTrue(tl.contains("v2"));
			Assert.assertTrue(tl.contains("v3"));
			Assert.assertTrue(tl.contains(mapValue));
		}
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Test
	public void mergeJsonMaps_Maps() {

		// case - simple value in source, Map in target - source is ignored
		{
			Map<String, Object> source = new HashMap<>();
			Map<String, Object> target = new HashMap<>();
			source.put("key1", "v1");
			Map targetMap = new HashMap<>();
			targetMap.put("tk1", "tv1");
			target.put("key1", targetMap);
			SearchUtils.mergeJsonMaps(source, target);
			Assert.assertTrue(target.get("key1") instanceof Map);
			Map tl = (Map) target.get("key1");
			Assert.assertEquals(1, tl.size());
			Assert.assertEquals("tv1", tl.get("tk1"));
		}

		// case - List value in source, Map in target - source is ignored
		{
			Map<String, Object> source = new HashMap<>();
			Map<String, Object> target = new HashMap<>();
			source.put("key1", TestUtils.createListOfStrings("v1"));
			Map targetMap = new HashMap<>();
			targetMap.put("tk1", "tv1");
			target.put("key1", targetMap);
			SearchUtils.mergeJsonMaps(source, target);
			Assert.assertTrue(target.get("key1") instanceof Map);
			Map tl = (Map) target.get("key1");
			Assert.assertEquals(1, tl.size());
			Assert.assertEquals("tv1", tl.get("tk1"));
		}

		// case - Map in source and target - must be merged
		{
			Map<String, Object> source = new HashMap<>();
			Map sourceMap = new HashMap<>();
			sourceMap.put("sk1", "sv1");
			sourceMap.put("k1", "v1");
			source.put("key1", sourceMap);
			Map<String, Object> target = new HashMap<>();
			Map targetMap = new HashMap<>();
			targetMap.put("tk1", "tv1");
			targetMap.put("k1", "v2");
			target.put("key1", targetMap);
			SearchUtils.mergeJsonMaps(source, target);
			Assert.assertTrue(target.get("key1") instanceof Map);
			Map tl = (Map) target.get("key1");
			Assert.assertEquals(3, tl.size());
			Assert.assertEquals("tv1", tl.get("tk1"));
			Assert.assertEquals("sv1", tl.get("sk1"));
			Assert.assertTrue(tl.get("k1") instanceof List);
			Assert.assertEquals(2, ((List) tl.get("k1")).size());
		}
	}
}
