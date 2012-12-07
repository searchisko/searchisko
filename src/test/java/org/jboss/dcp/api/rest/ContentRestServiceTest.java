/*
 * JBoss, Home of Professional Open Source
 * Copyright 2012 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 */
package org.jboss.dcp.api.rest;

import java.io.IOException;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;

import org.elasticsearch.common.joda.time.format.ISODateTimeFormat;
import org.elasticsearch.common.settings.SettingsException;
import org.jboss.dcp.api.service.ProviderService;
import org.jboss.dcp.api.testtools.ESRealClientTestBase;
import org.jboss.dcp.api.testtools.TestUtils;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

/**
 * Unit test for {@link ContentRestService}
 * 
 * @author Vlastimil Elias (velias at redhat dot com)
 */
public class ContentRestServiceTest extends ESRealClientTestBase {

	private static final String INDEX_TYPE = "index_type";
	private static final String INDEX_NAME = "index_name";
	private static final List<Map<String, Object>> PREPROCESSORS = new ArrayList<Map<String, Object>>();

	@SuppressWarnings("unchecked")
	@Test
	public void pushContent() throws Exception {

		ContentRestService tested = getTested(false);

		Map<String, Object> content = new HashMap<String, Object>();
		content.put("test", "test");
		// case - invalid input parameters
		TestUtils.assertResponseStatus(tested.pushContent(null, "1", content), Response.Status.BAD_REQUEST);
		TestUtils.assertResponseStatus(tested.pushContent("", "1", content), Response.Status.BAD_REQUEST);
		TestUtils.assertResponseStatus(tested.pushContent("known", null, content), Response.Status.BAD_REQUEST);
		TestUtils.assertResponseStatus(tested.pushContent("known", "", content), Response.Status.BAD_REQUEST);
		TestUtils.assertResponseStatus(tested.pushContent("known", "1", null), Response.Status.BAD_REQUEST);
		content.clear();
		TestUtils.assertResponseStatus(tested.pushContent("known", "1", content), Response.Status.BAD_REQUEST);

		// case - type is unknown
		content.put("test", "test");
		TestUtils.assertResponseStatus(tested.pushContent("unknown", "1", content), Response.Status.BAD_REQUEST);

		// case - type configuration is invalid - do not contains index name and/or index type
		TestUtils.assertResponseStatus(tested.pushContent("invalid", "1", content), Response.Status.INTERNAL_SERVER_ERROR);

		// case - type configuration is invalid - do not contains dcp_type definition
		TestUtils
				.assertResponseStatus(tested.pushContent("invalid_2", "1", content), Response.Status.INTERNAL_SERVER_ERROR);

		try {
			tested = getTested(true);

			// case - insert when index is not found
			{
				indexDelete(INDEX_NAME);
				content.clear();
				content.put("test", "testvalue");
				Date d = new Date();
				content.put(ContentRestService.DCP_UPDATED, d);
				Response r = TestUtils.assertResponseStatus(tested.pushContent("known", "1", content), Response.Status.OK);
				Assert.assertEquals("insert", ((Map<String, String>) r.getEntity()).get("status"));
				Mockito.verify(tested.providerService).runPreprocessors(PREPROCESSORS, content);
				indexFlush(INDEX_NAME);
				Map<String, Object> doc = indexGetDocument(INDEX_NAME, INDEX_TYPE, "known-1");
				Assert.assertNotNull(doc);
				Assert.assertEquals("testvalue", doc.get("test"));
				Assert.assertEquals("jbossorg", doc.get(ContentRestService.DCP_CONTENT_PROVIDER));
				Assert.assertEquals("1", doc.get(ContentRestService.DCP_CONTENT_ID));
				Assert.assertEquals("known", doc.get(ContentRestService.DCP_CONTENT_TYPE));
				Assert.assertEquals("my_dcp_type", doc.get(ContentRestService.DCP_TYPE));
				Assert.assertEquals("known-1", doc.get(ContentRestService.DCP_ID));
				Assert
						.assertEquals(d,
								ISODateTimeFormat.dateTimeParser().parseDateTime((String) doc.get(ContentRestService.DCP_UPDATED))
										.toDate());
				Assert.assertEquals(null, doc.get(ContentRestService.DCP_TAGS));
			}

			// case - insert when index is found, fill dcp_updated if not provided in content, process tags provided in
			// content
			{
				Mockito.reset(tested.providerService);
				setupProviderServiceMock(tested);
				content.put("test2", "testvalue2");
				content.remove(ContentRestService.DCP_UPDATED);
				String[] tags = new String[] { "tag_value" };
				content.put("tags", tags);
				Response r = TestUtils.assertResponseStatus(tested.pushContent("known", "2", content), Response.Status.OK);
				Assert.assertEquals("insert", ((Map<String, String>) r.getEntity()).get("status"));
				Mockito.verify(tested.providerService).runPreprocessors(PREPROCESSORS, content);
				indexFlush(INDEX_NAME);
				Map<String, Object> doc = indexGetDocument(INDEX_NAME, INDEX_TYPE, "known-2");
				Assert.assertNotNull(doc);
				Assert.assertEquals("testvalue", doc.get("test"));
				Assert.assertEquals("testvalue2", doc.get("test2"));
				Assert.assertEquals("jbossorg", doc.get(ContentRestService.DCP_CONTENT_PROVIDER));
				Assert.assertEquals("2", doc.get(ContentRestService.DCP_CONTENT_ID));
				Assert.assertEquals("known", doc.get(ContentRestService.DCP_CONTENT_TYPE));
				Assert.assertEquals("my_dcp_type", doc.get(ContentRestService.DCP_TYPE));
				Assert.assertEquals("known-2", doc.get(ContentRestService.DCP_ID));
				Assert.assertNotNull(doc.get(ContentRestService.DCP_UPDATED));
				Assert.assertEquals("tag_value", ((List<String>) doc.get(ContentRestService.DCP_TAGS)).get(0));
			}

			// case - rewrite document in index
			{
				Mockito.reset(tested.providerService);
				setupProviderServiceMock(tested);
				content.clear();
				content.put("test3", "testvalue3");
				Date d = new Date();
				content.put(ContentRestService.DCP_UPDATED, d);
				Response r = TestUtils.assertResponseStatus(tested.pushContent("known", "1", content), Response.Status.OK);
				Assert.assertEquals("update", ((Map<String, String>) r.getEntity()).get("status"));
				Mockito.verify(tested.providerService).runPreprocessors(PREPROCESSORS, content);
				indexFlush(INDEX_NAME);
				Map<String, Object> doc = indexGetDocument(INDEX_NAME, INDEX_TYPE, "known-1");
				Assert.assertNotNull(doc);
				Assert.assertEquals(null, doc.get("test"));
				Assert.assertEquals("testvalue3", doc.get("test3"));
				Assert.assertEquals("jbossorg", doc.get(ContentRestService.DCP_CONTENT_PROVIDER));
				Assert.assertEquals("1", doc.get(ContentRestService.DCP_CONTENT_ID));
				Assert.assertEquals("known", doc.get(ContentRestService.DCP_CONTENT_TYPE));
				Assert.assertEquals("my_dcp_type", doc.get(ContentRestService.DCP_TYPE));
				Assert.assertEquals("known-1", doc.get(ContentRestService.DCP_ID));
				Assert
						.assertEquals(d,
								ISODateTimeFormat.dateTimeParser().parseDateTime((String) doc.get(ContentRestService.DCP_UPDATED))
										.toDate());
				Assert.assertEquals(null, doc.get(ContentRestService.DCP_TAGS));
			}
		} finally {
			indexDelete(INDEX_NAME);
			finalizeESClientForUnitTest();
		}
	}

	@Test
	public void deleteContent() throws Exception {
		ContentRestService tested = getTested(false);

		// case - invalid input parameters
		TestUtils.assertResponseStatus(tested.deleteContent(null, "1", null), Response.Status.BAD_REQUEST);
		TestUtils.assertResponseStatus(tested.deleteContent("", "1", null), Response.Status.BAD_REQUEST);
		TestUtils.assertResponseStatus(tested.deleteContent("known", null, null), Response.Status.BAD_REQUEST);
		TestUtils.assertResponseStatus(tested.deleteContent("known", "", null), Response.Status.BAD_REQUEST);

		// case - type is unknown
		TestUtils.assertResponseStatus(tested.deleteContent("unknown", "1", null), Response.Status.BAD_REQUEST);

		// case - type configuration is invalid - do not contains index name and/or index type
		TestUtils.assertResponseStatus(tested.deleteContent("invalid", "1", null), Response.Status.INTERNAL_SERVER_ERROR);
		try {
			tested = getTested(true);

			// case - delete when index is not found
			{
				indexDelete(INDEX_NAME);
				TestUtils.assertResponseStatus(tested.deleteContent("known", "1", null), Response.Status.NOT_FOUND);
			}

			// case - delete when document doesn't exist in index
			{
				indexDelete(INDEX_NAME);
				indexCreate(INDEX_NAME);
				Thread.sleep(100);
				indexInsertDocument(INDEX_NAME, INDEX_TYPE, "known-2", "{\"test2\":\"test2\"}");
				indexFlush(INDEX_NAME);
				TestUtils.assertResponseStatus(tested.deleteContent("known", "1", null), Response.Status.NOT_FOUND);
				Assert.assertNotNull(indexGetDocument(INDEX_NAME, INDEX_TYPE, "known-2"));
			}

			// case - delete when document exist in index
			{
				indexInsertDocument(INDEX_NAME, INDEX_TYPE, "known-1", "{\"test1\":\"test1\"}");
				indexFlush(INDEX_NAME);
				TestUtils.assertResponseStatus(tested.deleteContent("known", "1", null), Response.Status.OK);
				Assert.assertNull(indexGetDocument(INDEX_NAME, INDEX_TYPE, "known-1"));
				Assert.assertNotNull(indexGetDocument(INDEX_NAME, INDEX_TYPE, "known-2"));
				// another subsequent deletes
				TestUtils.assertResponseStatus(tested.deleteContent("known", "1", null), Response.Status.NOT_FOUND);
				TestUtils.assertResponseStatus(tested.deleteContent("known", "2", null), Response.Status.OK);
				Assert.assertNull(indexGetDocument(INDEX_NAME, INDEX_TYPE, "known-1"));
				Assert.assertNull(indexGetDocument(INDEX_NAME, INDEX_TYPE, "known-2"));

				// test ignore_missing parameter
				TestUtils.assertResponseStatus(tested.deleteContent("known", "1", null), Response.Status.NOT_FOUND);
				TestUtils.assertResponseStatus(tested.deleteContent("known", "1", "false"), Response.Status.NOT_FOUND);
				TestUtils.assertResponseStatus(tested.deleteContent("known", "1", "true"), Response.Status.OK);
			}

		} finally {
			indexDelete(INDEX_NAME);
			finalizeESClientForUnitTest();
		}
	}

	@Test
	public void getAllContent() throws IOException, InterruptedException {

		ContentRestService tested = getTested(false);

		// case - invalid input parameters
		TestUtils.assertResponseStatus(tested.getAllContent(null, null, null, null), Response.Status.BAD_REQUEST);
		TestUtils.assertResponseStatus(tested.getAllContent("", null, null, null), Response.Status.BAD_REQUEST);

		// case - type is unknown
		TestUtils.assertResponseStatus(tested.getAllContent("unknown", null, null, null), Response.Status.BAD_REQUEST);

		// case - type configuration is invalid (do not contains index name and type)
		TestUtils.assertResponseStatus(tested.getAllContent("invalid", null, null, null),
				Response.Status.INTERNAL_SERVER_ERROR);

		try {
			tested = getTested(true);

			// case - type is valid, but nothing is found because index is missing
			indexDelete(INDEX_NAME);
			TestUtils.assertResponseStatus(tested.getAllContent("known", null, null, null), Response.Status.NOT_FOUND);

			// case - nothing found because index is empty
			indexCreate(INDEX_NAME);
			Thread.sleep(100);
			TestUtils.assetStreamingOutputContent("{\"total\":0,\"hits\":[]}",
					tested.getAllContent("known", null, null, null));

			// case - something found, no from and size param used
			indexInsertDocument(INDEX_NAME, INDEX_TYPE, "known-1", "{\"name\":\"test1\"}");
			indexInsertDocument(INDEX_NAME, INDEX_TYPE, "known-2", "{\"name\":\"test2\"}");
			indexInsertDocument(INDEX_NAME, INDEX_TYPE, "known-3", "{\"name\":\"test3\"}");
			indexInsertDocument(INDEX_NAME, INDEX_TYPE, "known-4", "{\"name\":\"test4\"}");
			indexFlush(INDEX_NAME);
			TestUtils
					.assetStreamingOutputContent(
							"{\"total\":4,\"hits\":[{\"id\":\"known-1\",\"data\":{\"name\":\"test1\"}},{\"id\":\"known-2\",\"data\":{\"name\":\"test2\"}},{\"id\":\"known-3\",\"data\":{\"name\":\"test3\"}},{\"id\":\"known-4\",\"data\":{\"name\":\"test4\"}}]}",
							tested.getAllContent("known", null, null, null));

			// case - something found, from and size param used
			TestUtils
					.assetStreamingOutputContent(
							"{\"total\":4,\"hits\":[{\"id\":\"known-2\",\"data\":{\"name\":\"test2\"}},{\"id\":\"known-3\",\"data\":{\"name\":\"test3\"}}]}",
							tested.getAllContent("known", 1, 2, null));

			// case - sort param used
			indexInsertDocument(INDEX_NAME, INDEX_TYPE, "known-5", "{\"name\":\"test5\", \"dcp_updated\" : 1}");
			indexFlush(INDEX_NAME);
			// on ASC our record with id 5 is last, so we set from=4
			TestUtils.assetStreamingOutputContent(
					"{\"total\":5,\"hits\":[{\"id\":\"known-5\",\"data\":{\"name\":\"test5\",\"dcp_updated\":1}}]}",
					tested.getAllContent("known", 4, 1, "asc"));
			// on DESC our record with id 5 is first, so we set from=0
			TestUtils.assetStreamingOutputContent(
					"{\"total\":5,\"hits\":[{\"id\":\"known-5\",\"data\":{\"name\":\"test5\",\"dcp_updated\":1}}]}",
					tested.getAllContent("known", 0, 1, "DESC"));

		} finally {
			indexDelete(INDEX_NAME);
			finalizeESClientForUnitTest();
		}
	}

	@SuppressWarnings("unchecked")
	@Test
	public void getContent() {
		ContentRestService tested = getTested(false);

		// case - invalid input parameters
		TestUtils.assertResponseStatus(tested.getContent(null, "1"), Response.Status.BAD_REQUEST);
		TestUtils.assertResponseStatus(tested.getContent("", "1"), Response.Status.BAD_REQUEST);
		TestUtils.assertResponseStatus(tested.getContent("testtype", null), Response.Status.BAD_REQUEST);
		TestUtils.assertResponseStatus(tested.getContent("testtype", ""), Response.Status.BAD_REQUEST);

		// case - type is unknown
		TestUtils.assertResponseStatus(tested.getContent("unknown", "12"), Response.Status.BAD_REQUEST);

		// case - type configuration is invalid (do not contains index name and type)
		TestUtils.assertResponseStatus(tested.getContent("invalid", "12"), Response.Status.INTERNAL_SERVER_ERROR);

		try {
			tested = getTested(true);

			// case - type is valid, but nothing is found for passed id because index is missing
			indexDelete(INDEX_NAME);
			TestUtils.assertResponseStatus(tested.getContent("known", "12"), Response.Status.NOT_FOUND);

			// case - index is present bud document is not found
			indexCreate(INDEX_NAME);
			indexInsertDocument(INDEX_NAME, INDEX_TYPE, "known-1", "{\"name\":\"test\"}");
			TestUtils.assertResponseStatus(tested.getContent("known", "2"), Response.Status.NOT_FOUND);

			// case - document found
			Map<String, Object> ret = (Map<String, Object>) tested.getContent("known", "1");
			Assert.assertEquals("test", ret.get("name"));

		} finally {
			indexDelete(INDEX_NAME);
			finalizeESClientForUnitTest();
		}
	}

	@Test
	public void checkSearchIndexSettings() {

		try {
			ContentRestService.checkSearchIndexSettings("type", null, "doctype");
			Assert.fail("SettingsException expected");
		} catch (SettingsException e) {
			// OK
		}
		try {
			ContentRestService.checkSearchIndexSettings("type", "  ", "doctype");
			Assert.fail("SettingsException expected");
		} catch (SettingsException e) {
			// OK
		}
		try {
			ContentRestService.checkSearchIndexSettings("type", "index", null);
			Assert.fail("SettingsException expected");
		} catch (SettingsException e) {
			// OK
		}
		try {
			ContentRestService.checkSearchIndexSettings("type", "index", "   ");
			Assert.fail("SettingsException expected");
		} catch (SettingsException e) {
			// OK
		}
		ContentRestService.checkSearchIndexSettings("type", "index", "doctype");
	}

	/**
	 * Prepare tested instance with injected mocks.
	 * 
	 * @param initEsClient - searchClientService is initialized from
	 *          {@link ESRealClientTestBase#prepareSearchClientServiceMock()} if this is true, so do not forget to clean
	 *          up client in finally!
	 * @return instance for test
	 */
	protected ContentRestService getTested(boolean initEsClient) {
		ContentRestService tested = new ContentRestService();
		if (initEsClient)
			tested.searchClientService = prepareSearchClientServiceMock();

		tested.providerService = Mockito.mock(ProviderService.class);
		setupProviderServiceMock(tested);
		tested.log = Logger.getLogger("testlogger");
		tested.securityContext = Mockito.mock(SecurityContext.class);
		Mockito.when(tested.securityContext.getUserPrincipal()).thenReturn(new Principal() {

			@Override
			public String getName() {
				return "jbossorg";
			}
		});
		return tested;
	}

	/**
	 * @param tested
	 */
	protected void setupProviderServiceMock(ContentRestService tested) {
		Mockito.when(tested.providerService.findContentType("invalid")).thenReturn(new HashMap<String, Object>());
		Mockito.when(tested.providerService.findContentType("unknown")).thenReturn(null);
		Mockito.when(tested.providerService.generateDcpId(Mockito.anyString(), Mockito.anyString())).thenCallRealMethod();

		Map<String, Object> typeDefKnown = new HashMap<String, Object>();
		Map<String, Object> typeDefKnownIndex = new HashMap<String, Object>();
		typeDefKnown.put(ProviderService.INDEX, typeDefKnownIndex);
		typeDefKnownIndex.put("name", INDEX_NAME);
		typeDefKnownIndex.put("type", INDEX_TYPE);
		typeDefKnown.put("input_preprocessors", PREPROCESSORS);
		typeDefKnown.put(ProviderService.DCP_TYPE, "my_dcp_type");
		Mockito.when(tested.providerService.findContentType("known")).thenReturn(typeDefKnown);

		Map<String, Object> providerDef = new HashMap<String, Object>();
		Mockito.when(tested.providerService.findProvider("jbossorg")).thenReturn(providerDef);
		Map<String, Object> typesDef = new HashMap<String, Object>();
		providerDef.put(ProviderService.TYPE, typesDef);
		typesDef.put("invalid", new HashMap<String, Object>());
		Map<String, Object> typeDefInvalid2 = new HashMap<String, Object>();
		typeDefInvalid2.put(ProviderService.INDEX, typeDefKnownIndex);
		typesDef.put("invalid_2", typeDefInvalid2);
		typesDef.put("known", typeDefKnown);
	}
}
