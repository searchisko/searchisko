/*
 * JBoss, Home of Professional Open Source
 * Copyright 2012 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 */
package org.jboss.dcp.api.testtools;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.util.Map;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;

import org.apache.commons.io.IOUtils;
import org.elasticsearch.common.settings.SettingsException;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.elasticsearch.common.xcontent.XContentParser;
import org.elasticsearch.common.xcontent.XContentType;
import org.junit.Assert;

/**
 * Helper methods for Unit tests.
 * 
 * @author Vlastimil Elias (velias at redhat dot com)
 */
public abstract class TestUtils {

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
