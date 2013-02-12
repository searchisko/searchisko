/*
 * JBoss, Home of Professional Open Source
 * Copyright 2013 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 */
package org.jboss.dcp.api.util;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.map.JsonMappingException;
import org.jboss.dcp.api.testtools.TestUtils;
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

	@Test
	public void convertJsonMapToString() throws IOException {

		{
			Assert.assertNull(SearchUtils.convertJsonMapToString(null));
		}

		Map<String, Object> data = new HashMap<String, Object>();
		{
			TestUtils.assertJsonContent("{}", SearchUtils.convertJsonMapToString(data));
		}

		// case - conversion of distinct basic data types
		{
			Map<String, Object> dcpTitleConfig = new HashMap<String, Object>();
			dcpTitleConfig.put("fragment_size", "-1");
			dcpTitleConfig.put("number_of_fragments", "0");
			dcpTitleConfig.put("fragment_offset", 0);
			dcpTitleConfig.put("fragment_offset_long", new Long(25l));
			dcpTitleConfig.put("boolean", true);
			dcpTitleConfig.put("boolean2", false);
			dcpTitleConfig.put("date", new Date(65132498465l));
			data.put("dcp_title", dcpTitleConfig);
			TestUtils.assertJsonContent("{\"dcp_title\":{" + "\"fragment_size\":\"-1\"," + "\"number_of_fragments\":\"0\","
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
}
