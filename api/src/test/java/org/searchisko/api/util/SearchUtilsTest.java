/*
 * JBoss, Home of Professional Open Source
 * Copyright 2013 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 */
package org.searchisko.api.util;

import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.map.JsonMappingException;
import org.junit.Assert;
import org.junit.Test;
import org.searchisko.api.testtools.TestUtils;

/**
 * Unit test for {@link SearchUtils}
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
	public void extractContributorName() {
		Assert.assertNull(SearchUtils.extractContributorName(null));
		Assert.assertNull(SearchUtils.extractContributorName(""));

		// no email present
		Assert.assertEquals("John Doe", SearchUtils.extractContributorName("John Doe"));
		Assert.assertEquals("John Doe", SearchUtils.extractContributorName(" John Doe "));
		Assert.assertEquals("John > Doe", SearchUtils.extractContributorName("John > Doe"));
		Assert.assertEquals("John < Doe", SearchUtils.extractContributorName("John < Doe"));
		Assert.assertEquals("John Doe <", SearchUtils.extractContributorName("John Doe <"));
		Assert.assertEquals("John Doe >", SearchUtils.extractContributorName("John Doe >"));
		Assert.assertEquals("John >< Doe", SearchUtils.extractContributorName("John >< Doe"));

		// remove email
		Assert.assertEquals("John Doe", SearchUtils.extractContributorName("John Doe<john@doe.org>"));
		Assert.assertEquals("John Doe", SearchUtils.extractContributorName("John Doe <john@doe.org>"));
		Assert.assertEquals("John > Doe", SearchUtils.extractContributorName("John > Doe <john@doe.org>"));
		Assert.assertEquals("John Doe", SearchUtils.extractContributorName("John Doe<john@doe.org> "));
		Assert.assertEquals("John Doe", SearchUtils.extractContributorName("John Doe <john@doe.org> "));
		Assert.assertEquals("John Doe", SearchUtils.extractContributorName("John Doe <> "));
		Assert.assertEquals("John Doe", SearchUtils.extractContributorName("John Doe<> "));
		Assert.assertNull(SearchUtils.extractContributorName("<john@doe.org>"));
		Assert.assertNull(SearchUtils.extractContributorName(" <john@doe.org>"));
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
}
