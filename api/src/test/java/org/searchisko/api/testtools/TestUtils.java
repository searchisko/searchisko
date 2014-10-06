/*
 * JBoss, Home of Professional Open Source
 * Copyright 2012 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 */
package org.searchisko.api.testtools;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;
import javax.ws.rs.core.UriInfo;

import org.apache.commons.io.IOUtils;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.DeserializationConfig;
import org.codehaus.jackson.map.ObjectMapper;
import org.elasticsearch.common.settings.SettingsException;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.elasticsearch.common.xcontent.XContentParser;
import org.elasticsearch.common.xcontent.XContentType;
import org.jboss.resteasy.specimpl.MultivaluedMapImpl;
import org.json.JSONException;
import org.junit.Assert;
import org.mockito.Mockito;
import org.searchisko.api.util.SearchUtils;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;

/**
 * Helper methods for Unit tests.
 * 
 * @author Vlastimil Elias (velias at redhat dot com)
 */
public abstract class TestUtils {

	/**
	 * Assert date is current timestamp.
	 * 
	 * @param actualDate to assert
	 * @see #assertCurrentDate(long)
	 */
	public static void assertCurrentDate(Date actualDate) {
		Assert.assertNotNull(actualDate);
		assertCurrentDate(actualDate.getTime());

	}

	/**
	 * Assert current timestamp. There is 1000ms tolerance for future because assertion may run with some delay against
	 * current date creation to asserted variable.
	 * 
	 * @param actualDate to assert in millis
	 */
	public static void assertCurrentDate(long actualDate) {
		long l = System.currentTimeMillis() - actualDate;
		Assert.assertTrue(l >= 0 && l <= 1000);
	}

	/**
	 * Assert list contains expected values.
	 * 
	 * @param expectedValuesCommaSeparated comma separated list of expected values
	 * @param actualValue actual list with string values
	 */
	public static void assertEqualsListValue(String expectedValuesCommaSeparated, List<String> actualValue) {
		Assert.assertArrayEquals(expectedValuesCommaSeparated != null ? expectedValuesCommaSeparated.split(",") : null,
				actualValue != null ? actualValue.toArray(new String[] {}) : null);
	}

	/**
	 * Assert passed in object is REST {@link Response} and has given status.
	 * 
	 * @param actual object to check
	 * @param expectedStatus to check
	 * @return actual object casted to {@link Response} so other assertions may be performed on it.
	 */
	public static Response assertResponseStatus(Object actual, Response.Status expectedStatus) {
		return assertResponseStatus(actual, expectedStatus, null);
	}

	/**
	 * Assert passed in object is REST {@link Response} and has given status and string as Entity object.
	 * 
	 * @param actual object to check
	 * @param expectedStatus to check
	 * @param expectedContent of response
	 * @return actual object casted to {@link Response} so other assertions may be performed on it.
	 */
	public static Response assertResponseStatus(Object actual, Response.Status expectedStatus, String expectedContent) {
		if (actual == null) {
			Assert.fail("Result must be response Response object, but is null");
		}
		if (!(actual instanceof Response))
			Assert.fail("Result must be response Response object, but is " + actual.getClass().getName());
		Response r = (Response) actual;
		Assert.assertEquals(expectedStatus.getStatusCode(), r.getStatus());
		if (expectedContent != null)
			Assert.assertEquals(expectedContent, r.getEntity());
		return r;
	}

	/**
	 * Assert JSON equals to written by the StreamingOutput.
	 * 
	 * @param expected value
	 * @param actual value. Has to be instance of StreamingOutput
	 * @throws IOException
	 * @throws JSONException
	 */
	public static void assetJsonStreamingOutputContent(String expected, Object actual) throws IOException, JSONException {
		if (!(actual instanceof StreamingOutput)) {
			Assert.fail("Result must be StreamingOutput but is " + actual);
		}
		ByteArrayOutputStream output = new ByteArrayOutputStream();
		((StreamingOutput) actual).write(output);
		JSONAssert.assertEquals(expected, output.toString(), JSONCompareMode.NON_EXTENSIBLE);
	}

	/**
	 * Assert string value equals one written by the StreamingOutput.
	 * 
	 * @param expectedPattern regexp value used to match
	 * @param actual value
	 * @throws IOException
	 */
	public static void assetStreamingOutputContentRegexp(String expectedPattern, Object actual) throws IOException {
		if (!(actual instanceof StreamingOutput)) {
			Assert.fail("Result must be StreamingOutput but is " + actual);
		}
		ByteArrayOutputStream output = new ByteArrayOutputStream();
		((StreamingOutput) actual).write(output);
		if (!output.toString().matches(expectedPattern)) {
			Assert.fail("Expected regexp pattern '" + expectedPattern + "' do not match with content: " + output.toString());
		}

	}

	/**
	 * Assert passed string is same as content of given file loaded from classpath.
	 * 
	 * @param expectedFilePath path to file inside classpath
	 * @param actual content to assert
	 * @throws IOException
	 */
	public static void assertStringFromClasspathFile(String expectedFilePath, String actual) throws IOException {
		Assert.assertEquals(readStringFromClasspathFile(expectedFilePath), actual);
	}

	/**
	 * Assert passed in JSON string is same as JSON content of given file loaded from classpath. JSONs are compared in
	 * NON_EXTENSIBLE way, this means array items do not have to be in the same order but additional fields
	 * (extensibility) is considered a fail.
	 * 
	 * @param expectedJsonFilePath path to JSON file inside classpath
	 * @param actualJsonString JSON content to assert for equality
	 * @throws IOException
	 */
	public static void assertJsonContentFromClasspathFile(String expectedJsonFilePath, String actualJsonString)
			throws IOException, JSONException {
		JSONAssert.assertEquals(readStringFromClasspathFile(expectedJsonFilePath), actualJsonString,
				JSONCompareMode.NON_EXTENSIBLE);
	}

	/**
	 * Assert passed in JSON string is same as JSON content of given file loaded from classpath.
	 * 
	 * @param expectedJsonString expected JSON content to assert for equality
	 * @param actualJsonString JSON content to assert for equality
	 * @throws IOException
	 */
	public static void assertJsonContent(String expectedJsonString, String actualJsonString) throws IOException {
		JsonNode actualRootNode = getMapper().readValue(actualJsonString, JsonNode.class);
		JsonNode expectedRootNode = getMapper().readValue(expectedJsonString, JsonNode.class);
		Assert.assertEquals(expectedRootNode, actualRootNode);
	}

	/**
	 * Assert passed in JSON string is same as JSON content of given file loaded from classpath.
	 * 
	 * @param expectedJsonString expected JSON content to assert for equality
	 * @param actualJsonMap JSON content to assert for equality
	 * @throws IOException
	 */
	public static void assertJsonContent(String expectedJsonString, Map<String, Object> actualJsonMap) throws IOException {
		JsonNode actualRootNode = getMapper().readValue(SearchUtils.convertJsonMapToString(actualJsonMap), JsonNode.class);
		JsonNode expectedRootNode = getMapper().readValue(expectedJsonString, JsonNode.class);
		Assert.assertEquals(expectedRootNode, actualRootNode);
	}

	/**
	 * Assert passed in JSON content is same as JSON content of given file loaded from classpath.
	 * 
	 * @param expectedJsonMap expected JSON content to assert for equality
	 * @param actualJsonMap JSON content to assert for equality
	 * @throws IOException
	 */
	public static void assertJsonContent(Map<String, Object> expectedJsonMap, Map<String, Object> actualJsonMap)
			throws IOException {
		JsonNode actualRootNode = getMapper().readValue(SearchUtils.convertJsonMapToString(actualJsonMap), JsonNode.class);
		JsonNode expectedRootNode = getMapper().readValue(SearchUtils.convertJsonMapToString(expectedJsonMap),
				JsonNode.class);
		Assert.assertEquals(expectedRootNode, actualRootNode);
	}

	private static ObjectMapper getMapper() {
		ObjectMapper mapper = new ObjectMapper();
		mapper.configure(DeserializationConfig.Feature.FAIL_ON_UNKNOWN_PROPERTIES, false);
		return mapper;
	}

	/**
	 * Read file from classpath into String. UTF-8 encoding expected.
	 * 
	 * @param filePath in classpath to read data from.
	 * @return file content.
	 * @throws IOException
	 */
	public static String readStringFromClasspathFile(String filePath) throws IOException {
		StringWriter stringWriter = new StringWriter();
		IOUtils.copy(TestUtils.class.getResourceAsStream(filePath), stringWriter, "UTF-8");
		return stringWriter.toString();
	}

	/**
	 * Read JSON file from classpath into Map of Map structure.
	 * 
	 * @param filePath path inside classpath pointing to JSON file to read
	 * @return parsed JSON file
	 * @throws SettingsException
	 */
	public static Map<String, Object> loadJSONFromClasspathFile(String filePath) {
		XContentParser parser = null;
		try {
			parser = XContentFactory.xContent(XContentType.JSON).createParser(TestUtils.class.getResourceAsStream(filePath));
			return parser.mapOrderedAndClose();
		} catch (IOException e) {
			throw new RuntimeException(e.getMessage(), e);
		} finally {
			if (parser != null)
				parser.close();
		}
	}

	public static UriInfo prepareUriInfiWithParams(String... params) {
		UriInfo uriInfoMock = Mockito.mock(UriInfo.class);
		MultivaluedMap<String, String> qp = new MultivaluedMapImpl<String, String>();
		for (int i = 0; i < params.length; i = i + 2) {
			qp.add(params[i], params[i + 1]);
		}
		Mockito.when(uriInfoMock.getQueryParameters()).thenReturn(qp);
		return uriInfoMock;
	}

	public static ArrayList<String> createListOfStrings(String... strings) {
		ArrayList<String> ret = new ArrayList<>();
		if (strings != null) {
			for (String s : strings) {
				ret.add(s);
			}
		}
		return ret;
	}

	public static Set<String> createSetOfStrings(String... strings) {
		Set<String> ret = new LinkedHashSet<>();
		if (strings != null) {
			for (String s : strings) {
				ret.add(s);
			}
		}
		return ret;
	}

}
