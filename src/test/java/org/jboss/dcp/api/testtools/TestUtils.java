/*
 * JBoss, Home of Professional Open Source
 * Copyright 2012 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 */
package org.jboss.dcp.api.testtools;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;

import org.apache.commons.io.IOUtils;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.DeserializationConfig;
import org.codehaus.jackson.map.ObjectMapper;
import org.elasticsearch.common.settings.SettingsException;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.elasticsearch.common.xcontent.XContentParser;
import org.elasticsearch.common.xcontent.XContentType;
import org.jboss.dcp.api.annotations.security.ProviderAllowed;
import org.jboss.dcp.api.rest.SecurityPreProcessInterceptor;
import org.junit.Assert;

/**
 * Helper methods for Unit tests.
 * 
 * @author Vlastimil Elias (velias at redhat dot com)
 */
public abstract class TestUtils {

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
	 * Assert REST API method is accessible by all users (even non authenticated).
	 * 
	 * @param testedClass class method is in
	 * @param methodName name of method
	 * @param methodParamTypes method parameter types
	 */
	public static void assertPermissionGuest(Class<?> testedClass, String methodName, Class<?>... methodParamTypes) {
		Method method;
		try {
			method = testedClass.getMethod(methodName, methodParamTypes);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		ProviderAllowed pa = SecurityPreProcessInterceptor.getProviderAlowedAnnotation(testedClass, method);
		if (pa != null && !SecurityPreProcessInterceptor.isGuestAllowed(method)) {
			Assert.fail("Method must be GuestAllowed too");
		}
	}

	/**
	 * Assert REST API method is accessible by authenticated Providers only.
	 * 
	 * @param testedClass class method is in
	 * @param methodName name of method
	 * @param methodParamTypes method parameter types
	 */
	public static void assertPermissionProvider(Class<?> testedClass, String methodName, Class<?>... methodParamTypes) {
		Method method;
		try {
			method = testedClass.getMethod(methodName, methodParamTypes);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		ProviderAllowed pa = SecurityPreProcessInterceptor.getProviderAlowedAnnotation(testedClass, method);
		if (pa == null || SecurityPreProcessInterceptor.isGuestAllowed(method)) {
			Assert.fail("Method must be ProviderAllowed only");
		}
	}

	/**
	 * Assert REST API method is accessible by authenticated Providers only.
	 * 
	 * @param testedClass class method is in
	 * @param methodName name of method
	 * @param methodParamTypes method parameter types
	 */
	public static void assertPermissionSuperProvider(Class<?> testedClass, String methodName,
			Class<?>... methodParamTypes) {
		Method method;
		try {
			method = testedClass.getMethod(methodName, methodParamTypes);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		ProviderAllowed pa = SecurityPreProcessInterceptor.getProviderAlowedAnnotation(testedClass, method);
		if (pa == null || !pa.superProviderOnly() || SecurityPreProcessInterceptor.isGuestAllowed(method)) {
			Assert.fail("Method must be ProviderAllowed.superProviderOnly");
		}
	}

	/**
	 * Assert passed in object is REST {@link Response} and has given status.
	 * 
	 * @param actual object to check
	 * @param status to check
	 * @return actual object casted to {@link Response} so other assertions may be performed on it.
	 */
	public static Response assertResponseStatus(Object actual, Response.Status status) {
		if (actual == null) {
			Assert.fail("Result must be response Response object, but is null");
		}
		if (!(actual instanceof Response))
			Assert.fail("Result must be response Response object, but is " + actual.getClass().getName());
		Response r = (Response) actual;
		Assert.assertEquals(status.getStatusCode(), r.getStatus());
		return r;
	}

	/**
	 * Assert string value equals one written by the StreamingOutput.
	 * 
	 * @param expected value
	 * @param actual value
	 * @throws IOException
	 */
	public static void assetStreamingOutputContent(String expected, Object actual) throws IOException {
		if (!(actual instanceof StreamingOutput)) {
			Assert.fail("Result must be StreamingOutput but is " + actual);
		}
		ByteArrayOutputStream output = new ByteArrayOutputStream();
		((StreamingOutput) actual).write(output);
		Assert.assertEquals(expected, output.toString());
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
	 * Assert passed in JSON string is same as JSON content of given file loaded from classpath.
	 * 
	 * @param expectedJsonFilePath path to JSON file inside classpath
	 * @param actualJsonString JSON content to assert for equality
	 * @throws IOException
	 */
	public static void assertJsonContentFromClasspathFile(String expectedJsonFilePath, String actualJsonString)
			throws IOException {
		JsonNode actualRootNode = getMapper().readValue(new ByteArrayInputStream(actualJsonString.getBytes()),
				JsonNode.class);
		JsonNode expectedRootNode = getMapper().readValue(TestUtils.class.getResourceAsStream(expectedJsonFilePath),
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
			return parser.mapAndClose();
		} catch (IOException e) {
			throw new RuntimeException(e.getMessage(), e);
		} finally {
			if (parser != null)
				parser.close();
		}
	}
}
