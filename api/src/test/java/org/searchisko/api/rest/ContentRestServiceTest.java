/*
 * JBoss, Home of Professional Open Source
 * Copyright 2012 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 */
package org.searchisko.api.rest;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import javax.enterprise.event.Event;
import javax.ws.rs.core.Response;

import org.hamcrest.CustomMatcher;
import org.junit.Test;
import org.mockito.Mockito;
import org.searchisko.api.ContentObjectFields;
import org.searchisko.api.events.ContentBeforeIndexedEvent;
import org.searchisko.api.events.ContentDeletedEvent;
import org.searchisko.api.events.ContentStoredEvent;
import org.searchisko.api.rest.exception.BadFieldException;
import org.searchisko.api.rest.exception.RequiredFieldException;
import org.searchisko.api.rest.security.AuthenticationUtilService;
import org.searchisko.api.service.ProviderService;
import org.searchisko.api.service.ProviderServiceTest;
import org.searchisko.api.testtools.ESRealClientTestBase;
import org.searchisko.api.testtools.TestUtils;
import org.searchisko.persistence.service.ContentPersistenceService;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import static org.searchisko.api.testtools.TestUtils.assertResponseStatus;
import static org.searchisko.api.testtools.TestUtils.assetStreamingOutputContent;

/**
 * Unit test for {@link ContentRestService}
 * 
 * @author Vlastimil Elias (velias at redhat dot com)
 */
public class ContentRestServiceTest extends ESRealClientTestBase {

	private static final String INDEX_TYPE = "index_type";
	private static final String INDEX_NAME = "index_name";
	private static final List<Map<String, Object>> PREPROCESSORS = new ArrayList<Map<String, Object>>();

	@Test
	public void pushContent_permissions() throws Exception {
		TestUtils.assertPermissionProvider(ContentRestService.class, "pushContent", String.class, String.class, Map.class);
	}

	@Test(expected = RequiredFieldException.class)
	public void pushContent_invalidParams_1() throws Exception {
		Map<String, Object> content = new HashMap<String, Object>();
		content.put("test", "test");
		getTested(false).pushContent(null, "1", content);
	}

	@Test(expected = RequiredFieldException.class)
	public void pushContent_invalidParams_2() throws Exception {
		Map<String, Object> content = new HashMap<String, Object>();
		content.put("test", "test");
		getTested(false).pushContent("", "1", content);
	}

	@Test(expected = RequiredFieldException.class)
	public void pushContent_invalidParams_3() throws Exception {
		Map<String, Object> content = new HashMap<String, Object>();
		content.put("test", "test");
		getTested(false).pushContent("known", null, content);
	}

	@Test(expected = RequiredFieldException.class)
	public void pushContent_invalidParams_4() throws Exception {
		Map<String, Object> content = new HashMap<String, Object>();
		content.put("test", "test");
		getTested(false).pushContent("known", "", content);
	}

	@Test
	public void pushContent_invalidParams_MissingContent1() throws Exception {
		TestUtils.assertResponseStatus(getTested(false).pushContent("known", "1", null), Response.Status.BAD_REQUEST);
	}

	@Test
	public void pushContent_invalidParams_MissingContent2() throws Exception {
		Map<String, Object> content = new HashMap<String, Object>();
		TestUtils.assertResponseStatus(getTested(false).pushContent("known", "1", content), Response.Status.BAD_REQUEST);
	}

	@Test(expected = BadFieldException.class)
	public void pushContent_invalidParams_UnknownType() throws Exception {
		// case - type is unknown
		Map<String, Object> content = new HashMap<String, Object>();
		content.put("test", "test");
		getTested(false).pushContent("unknown", "1", content);
	}

	@Test(expected = Exception.class)
	public void pushContent_invalidParams_TypeInvalid() throws Exception {
		// case - type configuration is invalid - do not contains index name and/or index type
		Map<String, Object> content = new HashMap<String, Object>();
		content.put("test", "test");
		getTested(false).pushContent("invalid", "1", content);
	}

	@Test(expected = Exception.class)
	public void pushContent_invalidParams_TypeInvalid2() throws Exception {
		// case - type configuration is invalid - do not contains sys_type definition
		Map<String, Object> content = new HashMap<String, Object>();
		content.put("test", "test");
		getTested(false).pushContent("invalid_2", "1", content);
	}

	@Test(expected = Exception.class)
	public void pushContent_invalidParams_1SCCTMandatory() throws Exception {
		// case - sys_content_content-type not defined if input value contains sys_content
		Map<String, Object> content = new HashMap<String, Object>();
		content.put("test", "test");
		content.put(ContentObjectFields.SYS_CONTENT, "some content");
		getTested(false).pushContent("invalid-content-type", "1", content);

	}

	@SuppressWarnings("unchecked")
	@Test
	public void pushContent_noPersistence() throws Exception {
		try {
			ContentRestService tested = getTested(true);
			Map<String, Object> content = new HashMap<String, Object>();

			// case - insert when index is not found
			String sys_content_type = "known";
			{
				reset(tested.contentPersistenceService, tested.eventContentStored);
				indexDelete(INDEX_NAME);
				content.clear();
				content.put("test", "testvalue");
				String sysId = tested.providerService.generateSysId(sys_content_type, "1");
				Response r = assertResponseStatus(tested.pushContent(sys_content_type, "1", content), Response.Status.OK);
				assertEquals("insert", ((Map<String, String>) r.getEntity()).get("status"));
				verify(tested.providerService).runPreprocessors(sys_content_type, PREPROCESSORS, content);
				// verify enhancements called
				verify(tested.eventBeforeIndexed).fire(prepareContentBeforeIndexedEventMatcher(sysId, content));
				indexFlushAndRefresh(INDEX_NAME);
				Map<String, Object> doc = indexGetDocument(INDEX_NAME, INDEX_TYPE,
						tested.providerService.generateSysId(sys_content_type, "1"));
				assertNotNull(doc);
				assertEquals("testvalue", doc.get("test"));
				assertEquals("jbossorg", doc.get(ContentObjectFields.SYS_CONTENT_PROVIDER));
				assertEquals("1", doc.get(ContentObjectFields.SYS_CONTENT_ID));
				assertEquals(sys_content_type, doc.get(ContentObjectFields.SYS_CONTENT_TYPE));
				assertEquals("my_sys_type", doc.get(ContentObjectFields.SYS_TYPE));
				assertEquals(null, doc.get(ContentObjectFields.SYS_CONTENT_CONTENT_TYPE));
				assertEquals(sysId, doc.get(ContentObjectFields.SYS_ID));
				assertNotNull(doc.get(ContentObjectFields.SYS_UPDATED));
				assertEquals(null, doc.get(ContentObjectFields.SYS_TAGS));
				verify(tested.eventContentStored).fire(prepareContentStoredEventMatcher(sysId));
				verifyNoMoreInteractions(tested.contentPersistenceService);
			}

			// case - insert when index is found, fill sys_updated if not provided in content, process tags provided in
			// content, fill sys_content_content-type because sys_content is present
			{
				reset(tested.providerService, tested.contentPersistenceService, tested.eventContentStored,
						tested.eventBeforeIndexed);
				setupProviderServiceMock(tested);
				content.put("test2", "testvalue2");
				content.put(ContentObjectFields.SYS_CONTENT, "sys content");
				content.remove(ContentObjectFields.SYS_UPDATED);
				String[] tags = new String[] { "tag_value" };
				content.put("tags", tags);
				Response r = assertResponseStatus(tested.pushContent(sys_content_type, "2", content), Response.Status.OK);
				String sysId = tested.providerService.generateSysId(sys_content_type, "2");
				verify(tested.eventBeforeIndexed).fire(prepareContentBeforeIndexedEventMatcher(sysId, content));
				assertEquals("insert", ((Map<String, String>) r.getEntity()).get("status"));
				verify(tested.providerService).runPreprocessors(sys_content_type, PREPROCESSORS, content);
				indexFlushAndRefresh(INDEX_NAME);
				Map<String, Object> doc = indexGetDocument(INDEX_NAME, INDEX_TYPE, sysId);
				assertNotNull(doc);
				assertEquals("testvalue", doc.get("test"));
				assertEquals("testvalue2", doc.get("test2"));
				assertEquals("jbossorg", doc.get(ContentObjectFields.SYS_CONTENT_PROVIDER));
				assertEquals("2", doc.get(ContentObjectFields.SYS_CONTENT_ID));
				assertEquals(sys_content_type, doc.get(ContentObjectFields.SYS_CONTENT_TYPE));
				assertEquals("my_sys_type", doc.get(ContentObjectFields.SYS_TYPE));
				assertEquals("text/plain", doc.get(ContentObjectFields.SYS_CONTENT_CONTENT_TYPE));
				String expectedContentId = tested.providerService.generateSysId(sys_content_type, "2");

				assertEquals(expectedContentId, doc.get(ContentObjectFields.SYS_ID));
				assertNotNull(doc.get(ContentObjectFields.SYS_UPDATED));
				assertEquals("tag_value", ((List<String>) doc.get(ContentObjectFields.SYS_TAGS)).get(0));
				verify(tested.eventContentStored).fire(prepareContentStoredEventMatcher(expectedContentId));
				verifyNoMoreInteractions(tested.contentPersistenceService);
			}

			// case - rewrite document in index
			{
				reset(tested.providerService, tested.contentPersistenceService, tested.eventContentStored,
						tested.eventBeforeIndexed);
				setupProviderServiceMock(tested);
				content.clear();
				content.put("test3", "testvalue3");
				Response r = assertResponseStatus(tested.pushContent(sys_content_type, "1", content), Response.Status.OK);
				String sysId = tested.providerService.generateSysId(sys_content_type, "1");
				verify(tested.eventBeforeIndexed).fire(prepareContentBeforeIndexedEventMatcher(sysId, content));
				assertEquals("update", ((Map<String, String>) r.getEntity()).get("status"));
				verify(tested.providerService).runPreprocessors(sys_content_type, PREPROCESSORS, content);
				indexFlushAndRefresh(INDEX_NAME);
				Map<String, Object> doc = indexGetDocument(INDEX_NAME, INDEX_TYPE, sysId);
				assertNotNull(doc);
				assertEquals(null, doc.get("test"));
				assertEquals("testvalue3", doc.get("test3"));
				assertEquals("jbossorg", doc.get(ContentObjectFields.SYS_CONTENT_PROVIDER));
				assertEquals("1", doc.get(ContentObjectFields.SYS_CONTENT_ID));
				assertEquals(sys_content_type, doc.get(ContentObjectFields.SYS_CONTENT_TYPE));
				assertEquals("my_sys_type", doc.get(ContentObjectFields.SYS_TYPE));
				assertEquals(null, doc.get(ContentObjectFields.SYS_CONTENT_CONTENT_TYPE));
				String expectedContentId = tested.providerService.generateSysId(sys_content_type, "1");
				assertEquals(expectedContentId, doc.get(ContentObjectFields.SYS_ID));
				assertNotNull(doc.get(ContentObjectFields.SYS_UPDATED));
				assertEquals(null, doc.get(ContentObjectFields.SYS_TAGS));

				verify(tested.eventContentStored).fire(prepareContentStoredEventMatcher(expectedContentId));
				verifyNoMoreInteractions(tested.contentPersistenceService);
			}
		} finally {
			indexDelete(INDEX_NAME);
			finalizeESClientForUnitTest();
		}
	}

	private ContentBeforeIndexedEvent prepareContentBeforeIndexedEventMatcher(final String expectedId,
			final Map<String, Object> expectedContentObject) {
		return Mockito.argThat(new CustomMatcher<ContentBeforeIndexedEvent>("ContentBeforeIndexedEvent [contentId="
				+ expectedId + " data=" + expectedContentObject + "]") {

			@Override
			public boolean matches(Object paramObject) {
				ContentBeforeIndexedEvent e = (ContentBeforeIndexedEvent) paramObject;
				return e.getContentId().equals(expectedId) && e.getContentData() == expectedContentObject;
			}

		});
	}

	@SuppressWarnings("unchecked")
	@Test
	public void pushContent_persistence() throws Exception {
		try {
			ContentRestService tested = getTested(true);
			Map<String, Object> content = new HashMap<String, Object>();

			// case - insert when index is not found, remove sys_content_content-type because sys_content not present
			String sys_content_type = "persist";

			{
				Mockito.reset(tested.contentPersistenceService, tested.eventContentStored, tested.eventBeforeIndexed);
				indexDelete(INDEX_NAME);
				content.clear();
				content.put("test", "testvalue");
				content.put(ContentObjectFields.SYS_CONTENT_CONTENT_TYPE, "text/html");
				Response r = assertResponseStatus(tested.pushContent(sys_content_type, "1", content), Response.Status.OK);
				// verify enhancements called
				String sysId = tested.providerService.generateSysId(sys_content_type, "1");
				verify(tested.eventBeforeIndexed).fire(prepareContentBeforeIndexedEventMatcher(sysId, content));
				assertEquals("insert", ((Map<String, String>) r.getEntity()).get("status"));
				indexFlushAndRefresh(INDEX_NAME);
				Map<String, Object> doc = indexGetDocument(INDEX_NAME, INDEX_TYPE,
						tested.providerService.generateSysId(sys_content_type, "1"));
				assertNotNull(doc);
				assertEquals("testvalue", doc.get("test"));
				assertEquals("jbossorg", doc.get(ContentObjectFields.SYS_CONTENT_PROVIDER));
				assertEquals("1", doc.get(ContentObjectFields.SYS_CONTENT_ID));
				assertEquals(sys_content_type, doc.get(ContentObjectFields.SYS_CONTENT_TYPE));
				assertEquals("my_sys_type", doc.get(ContentObjectFields.SYS_TYPE));
				assertEquals(null, doc.get(ContentObjectFields.SYS_CONTENT_CONTENT_TYPE));
				String expectedContentId = tested.providerService.generateSysId(sys_content_type, "1");
				assertEquals(expectedContentId, doc.get(ContentObjectFields.SYS_ID));
				assertNotNull(doc.get(ContentObjectFields.SYS_UPDATED));
				assertEquals(null, doc.get(ContentObjectFields.SYS_TAGS));
				verify(tested.contentPersistenceService).store(tested.providerService.generateSysId(sys_content_type, "1"),
						sys_content_type, content);
				verify(tested.eventContentStored).fire(prepareContentStoredEventMatcher(expectedContentId));
				verifyNoMoreInteractions(tested.contentPersistenceService);
			}

			// case - insert when index is found, fill sys_updated if not provided in content, process tags provided in
			// content, rewrite sys_content_content-type because sys_content is present
			{
				reset(tested.providerService, tested.contentPersistenceService, tested.eventContentStored,
						tested.eventBeforeIndexed);
				setupProviderServiceMock(tested);
				content.put("test2", "testvalue2");
				content.put(ContentObjectFields.SYS_CONTENT, "sys content");
				content.put(ContentObjectFields.SYS_CONTENT_CONTENT_TYPE, "text/html");
				content.remove(ContentObjectFields.SYS_UPDATED);
				String[] tags = new String[] { "tag_value" };
				content.put("tags", tags);
				Response r = assertResponseStatus(tested.pushContent(sys_content_type, "2", content), Response.Status.OK);
				// verify enhancements called
				String sysId = tested.providerService.generateSysId(sys_content_type, "2");
				verify(tested.eventBeforeIndexed).fire(prepareContentBeforeIndexedEventMatcher(sysId, content));
				assertEquals("insert", ((Map<String, String>) r.getEntity()).get("status"));
				indexFlushAndRefresh(INDEX_NAME);
				Map<String, Object> doc = indexGetDocument(INDEX_NAME, INDEX_TYPE, sysId);
				assertNotNull(doc);
				assertEquals("testvalue", doc.get("test"));
				assertEquals("testvalue2", doc.get("test2"));
				assertEquals("jbossorg", doc.get(ContentObjectFields.SYS_CONTENT_PROVIDER));
				assertEquals("2", doc.get(ContentObjectFields.SYS_CONTENT_ID));
				assertEquals(sys_content_type, doc.get(ContentObjectFields.SYS_CONTENT_TYPE));
				assertEquals("my_sys_type", doc.get(ContentObjectFields.SYS_TYPE));
				assertEquals("text/plain", doc.get(ContentObjectFields.SYS_CONTENT_CONTENT_TYPE));
				String expectedContentId = tested.providerService.generateSysId(sys_content_type, "2");
				assertEquals(expectedContentId, doc.get(ContentObjectFields.SYS_ID));
				assertNotNull(doc.get(ContentObjectFields.SYS_UPDATED));
				assertEquals("tag_value", ((List<String>) doc.get(ContentObjectFields.SYS_TAGS)).get(0));
				verify(tested.contentPersistenceService).store(tested.providerService.generateSysId(sys_content_type, "2"),
						sys_content_type, content);

				verify(tested.eventContentStored).fire(prepareContentStoredEventMatcher(expectedContentId));
				verifyNoMoreInteractions(tested.contentPersistenceService);
			}

			// case - rewrite document in index
			{
				reset(tested.providerService, tested.contentPersistenceService, tested.eventContentStored,
						tested.eventBeforeIndexed);
				setupProviderServiceMock(tested);
				content.clear();
				content.put("test3", "testvalue3");
				Response r = assertResponseStatus(tested.pushContent(sys_content_type, "1", content), Response.Status.OK);
				// verify enhancements called
				String sysId = tested.providerService.generateSysId(sys_content_type, "1");
				verify(tested.eventBeforeIndexed).fire(prepareContentBeforeIndexedEventMatcher(sysId, content));
				assertEquals("update", ((Map<String, String>) r.getEntity()).get("status"));
				indexFlushAndRefresh(INDEX_NAME);
				Map<String, Object> doc = indexGetDocument(INDEX_NAME, INDEX_TYPE, sysId);
				assertNotNull(doc);
				assertEquals(null, doc.get("test"));
				assertEquals("testvalue3", doc.get("test3"));
				assertEquals("jbossorg", doc.get(ContentObjectFields.SYS_CONTENT_PROVIDER));
				assertEquals("1", doc.get(ContentObjectFields.SYS_CONTENT_ID));
				assertEquals(sys_content_type, doc.get(ContentObjectFields.SYS_CONTENT_TYPE));
				assertEquals("my_sys_type", doc.get(ContentObjectFields.SYS_TYPE));
				assertEquals(null, doc.get(ContentObjectFields.SYS_CONTENT_CONTENT_TYPE));
				assertEquals(tested.providerService.generateSysId(sys_content_type, "1"), doc.get(ContentObjectFields.SYS_ID));
				assertNotNull(doc.get(ContentObjectFields.SYS_UPDATED));
				assertEquals(null, doc.get(ContentObjectFields.SYS_TAGS));
				String expectedContentId = tested.providerService.generateSysId(sys_content_type, "1");
				verify(tested.contentPersistenceService).store(expectedContentId, sys_content_type, content);
				verify(tested.eventContentStored).fire(prepareContentStoredEventMatcher(expectedContentId));
				verifyNoMoreInteractions(tested.contentPersistenceService);
			}
		} finally {
			indexDelete(INDEX_NAME);
			finalizeESClientForUnitTest();
		}
	}

	private ContentStoredEvent prepareContentStoredEventMatcher(final String expectedContentId) {
		return Mockito.argThat(new CustomMatcher<ContentStoredEvent>("ContentStoredEvent [contributorId="
				+ expectedContentId + "]") {

			@Override
			public boolean matches(Object paramObject) {
				ContentStoredEvent e = (ContentStoredEvent) paramObject;
				return e.getContentId().equals(expectedContentId) && e.getContentData() != null;
			}

		});
	}

	private ContentDeletedEvent prepareContentDeletedEventMatcher(final String expectedContentId) {
		return Mockito.argThat(new CustomMatcher<ContentDeletedEvent>("ContentDeletedEvent [contributorId="
				+ expectedContentId + "]") {

			@Override
			public boolean matches(Object paramObject) {
				ContentDeletedEvent e = (ContentDeletedEvent) paramObject;
				return e.getContentId().equals(expectedContentId);
			}

		});
	}

	@Test
	public void deleteContent_permissions() throws Exception {
		TestUtils.assertPermissionProvider(ContentRestService.class, "deleteContent", String.class, String.class,
				String.class);
	}

	@Test(expected = RequiredFieldException.class)
	public void deleteContent_ParamValidation_1() throws Exception {
		getTested(false).deleteContent(null, "1", null);
	}

	@Test(expected = RequiredFieldException.class)
	public void deleteContent_ParamValidation_2() throws Exception {
		getTested(false).deleteContent("", "1", null);
	}

	@Test(expected = RequiredFieldException.class)
	public void deleteContent_ParamValidation_3() throws Exception {
		getTested(false).deleteContent("known", null, null);
	}

	@Test(expected = RequiredFieldException.class)
	public void deleteContent_ParamValidation_4() throws Exception {
		getTested(false).deleteContent("known", "", null);
	}

	@Test(expected = BadFieldException.class)
	public void deleteContent_ParamValidation_UnknownType() throws Exception {
		// case - type is unknown
		getTested(false).deleteContent("unknown", "1", null);
	}

	@Test(expected = Exception.class)
	public void deleteContent_ParamValidation_TypeInvalid() throws Exception {
		// case - type configuration is invalid - do not contains index name and/or index type
		getTested(false).deleteContent("invalid", "1", null);
	}

	@Test
	public void deleteContent() throws Exception {

		try {
			ContentRestService tested = getTested(true);

			// case - delete when index is not found
			{
				reset(tested.contentPersistenceService, tested.eventContentDeleted);
				indexDelete(INDEX_NAME);
				assertResponseStatus(tested.deleteContent("known", "1", null), Response.Status.NOT_FOUND);
				assertResponseStatus(tested.deleteContent("persist", "1", null), Response.Status.NOT_FOUND);
				verify(tested.contentPersistenceService)
						.delete(tested.providerService.generateSysId("persist", "1"), "persist");
				verifyZeroInteractions(tested.eventContentDeleted);
				verifyNoMoreInteractions(tested.contentPersistenceService);
			}

			// case - delete when document doesn't exist in index
			{
				reset(tested.contentPersistenceService, tested.eventContentDeleted);
				indexDelete(INDEX_NAME);
				indexCreate(INDEX_NAME);
				indexInsertDocument(INDEX_NAME, INDEX_TYPE, "known-2", "{\"test2\":\"test2\"}");
				indexFlushAndRefresh(INDEX_NAME);
				assertResponseStatus(tested.deleteContent("known", "1", null), Response.Status.NOT_FOUND);
				assertResponseStatus(tested.deleteContent("persist", "1", null), Response.Status.NOT_FOUND);
				assertNotNull(indexGetDocument(INDEX_NAME, INDEX_TYPE, "known-2"));
				verify(tested.contentPersistenceService)
						.delete(tested.providerService.generateSysId("persist", "1"), "persist");
				verifyZeroInteractions(tested.eventContentDeleted);
				verifyNoMoreInteractions(tested.contentPersistenceService);
			}

			// case - delete when document exist in index
			{
				reset(tested.contentPersistenceService, tested.eventContentDeleted);
				indexInsertDocument(INDEX_NAME, INDEX_TYPE, "known-1", "{\"test1\":\"test1\"}");
				indexInsertDocument(INDEX_NAME, INDEX_TYPE, "persist-1", "{\"test1\":\"testper1\"}");
				indexFlushAndRefresh(INDEX_NAME);
				assertResponseStatus(tested.deleteContent("known", "1", null), Response.Status.OK);
				verify(tested.eventContentDeleted).fire(prepareContentDeletedEventMatcher("known-1"));
				assertNull(indexGetDocument(INDEX_NAME, INDEX_TYPE, "known-1"));
				assertResponseStatus(tested.deleteContent("persist", "1", null), Response.Status.OK);
				assertNull(indexGetDocument(INDEX_NAME, INDEX_TYPE, "persist-1"));
				assertNotNull(indexGetDocument(INDEX_NAME, INDEX_TYPE, "known-2"));
				verify(tested.eventContentDeleted).fire(prepareContentDeletedEventMatcher("persist-1"));
				// another subsequent deletes
				assertResponseStatus(tested.deleteContent("known", "1", null), Response.Status.NOT_FOUND);
				assertResponseStatus(tested.deleteContent("persist", "1", null), Response.Status.NOT_FOUND);
				assertResponseStatus(tested.deleteContent("known", "2", null), Response.Status.OK);
				verify(tested.eventContentDeleted).fire(prepareContentDeletedEventMatcher("known-2"));
				assertNull(indexGetDocument(INDEX_NAME, INDEX_TYPE, "known-1"));
				assertNull(indexGetDocument(INDEX_NAME, INDEX_TYPE, "known-2"));

				// test ignore_missing parameter
				assertResponseStatus(tested.deleteContent("known", "1", null), Response.Status.NOT_FOUND);
				assertResponseStatus(tested.deleteContent("known", "1", "false"), Response.Status.NOT_FOUND);
				assertResponseStatus(tested.deleteContent("known", "1", "true"), Response.Status.OK);

				verify(tested.contentPersistenceService, Mockito.times(2)).delete(
						tested.providerService.generateSysId("persist", "1"), "persist");

				verifyNoMoreInteractions(tested.eventContentDeleted);
				verifyNoMoreInteractions(tested.contentPersistenceService);

			}

		} finally {
			indexDelete(INDEX_NAME);
			finalizeESClientForUnitTest();
		}
	}

	@Test
	public void getAllContent_permissions() throws IOException, InterruptedException {
		TestUtils.assertPermissionGuest(ContentRestService.class, "getAllContent", String.class, Integer.class,
				Integer.class, String.class);
	}

	@Test(expected = RequiredFieldException.class)
	public void getAllContent_ParamValidation_1() throws IOException, InterruptedException {
		getTested(false).getAllContent(null, null, null, null);
	}

	public void getAllContent_ParamValidation_2() throws IOException, InterruptedException {
		getTested(false).getAllContent("", null, null, null);
	}

	public void getAllContent_ParamValidation_UnknownType() throws IOException, InterruptedException {
		// case - type is unknown
		getTested(false).getAllContent("unknown", null, null, null);
	}

	public void getAllContent_ParamValidation_TypeConfigInvalid() throws IOException, InterruptedException {

		// case - type configuration is invalid (do not contains index name and type)
		getTested(false).getAllContent("invalid", null, null, null);
	}

	@Test
	public void getAllContent() throws IOException, InterruptedException {

		try {
			ContentRestService tested = getTested(true);

			// case - type is valid, but nothing is found because index is missing
			indexDelete(INDEX_NAME);
			assertResponseStatus(tested.getAllContent("known", null, null, null), Response.Status.NOT_FOUND);

			// case - nothing found because index is empty
			indexCreate(INDEX_NAME);
			assetStreamingOutputContent("{\"total\":0,\"hits\":[]}", tested.getAllContent("known", null, null, null));

			// case - something found, no from and size param used
			indexInsertDocument(INDEX_NAME, INDEX_TYPE, "known-1",
					"{\"name\":\"test1\", \"sys_updated\" : 0,\"sys_content_id\":\"1\"}");
			indexInsertDocument(INDEX_NAME, INDEX_TYPE, "known-2",
					"{\"name\":\"test2\",\"sys_updated\":1,\"sys_content_id\":\"2\"}");
			indexInsertDocument(INDEX_NAME, INDEX_TYPE, "known-3",
					"{\"name\":\"test3\",\"sys_updated\":2,\"sys_content_id\":\"3\"}");
			indexInsertDocument(INDEX_NAME, INDEX_TYPE, "known-4",
					"{\"name\":\"test4\",\"sys_updated\":3,\"sys_content_id\":\"4\"}");
			indexFlushAndRefresh(INDEX_NAME);
			assetStreamingOutputContent("{\"total\":4,\"hits\":["
					+ "{\"id\":\"1\",\"data\":{\"sys_updated\":0,\"sys_content_id\":\"1\",\"name\":\"test1\"}},"
					+ "{\"id\":\"2\",\"data\":{\"sys_updated\":1,\"sys_content_id\":\"2\",\"name\":\"test2\"}},"
					+ "{\"id\":\"3\",\"data\":{\"sys_updated\":2,\"sys_content_id\":\"3\",\"name\":\"test3\"}},"
					+ "{\"id\":\"4\",\"data\":{\"sys_updated\":3,\"sys_content_id\":\"4\",\"name\":\"test4\"}}" + "]}",
					tested.getAllContent("known", null, null, null));

			// case - something found, from and size param used
			assetStreamingOutputContent("{\"total\":4,\"hits\":["
					+ "{\"id\":\"2\",\"data\":{\"sys_updated\":1,\"sys_content_id\":\"2\",\"name\":\"test2\"}},"
					+ "{\"id\":\"3\",\"data\":{\"sys_updated\":2,\"sys_content_id\":\"3\",\"name\":\"test3\"}}" + "]}",
					tested.getAllContent("known", 1, 2, null));

			// case - sort param used
			indexInsertDocument(INDEX_NAME, INDEX_TYPE, "known-5",
					"{\"name\":\"test5\", \"sys_updated\" : 4,\"sys_content_id\":\"5\"}");
			indexFlushAndRefresh(INDEX_NAME);
			// on ASC our record with id 5 is last, so we set from=4
			assetStreamingOutputContent(
					"{\"total\":5,\"hits\":[{\"id\":\"5\",\"data\":{\"sys_updated\":4,\"sys_content_id\":\"5\",\"name\":\"test5\"}}]}",
					tested.getAllContent("known", 4, 1, "asc"));
			// on DESC our record with id 5 is first, so we set from=0
			assetStreamingOutputContent(
					"{\"total\":5,\"hits\":[{\"id\":\"5\",\"data\":{\"sys_updated\":4,\"sys_content_id\":\"5\",\"name\":\"test5\"}}]}",
					tested.getAllContent("known", 0, 1, "DESC"));

		} finally {
			indexDelete(INDEX_NAME);
			finalizeESClientForUnitTest();
		}
	}

	@Test
	public void getContent_permissions() {
		TestUtils.assertPermissionGuest(ContentRestService.class, "getContent", String.class, String.class);
	}

	@Test(expected = RequiredFieldException.class)
	public void getContent_ParamValidation_1() {
		// case - invalid input parameters
		getTested(true).getContent(null, "1");

	}

	@Test(expected = RequiredFieldException.class)
	public void getContent_ParamValidation_2() {
		getTested(true).getContent("", "1");
	}

	@Test(expected = RequiredFieldException.class)
	public void getContent_ParamValidation_3() {
		getTested(true).getContent("testtype", null);

	}

	@Test(expected = RequiredFieldException.class)
	public void getContent_ParamVvalidation_4() {
		getTested(true).getContent("testtype", "");
	}

	@Test(expected = BadFieldException.class)
	public void getContent_ParamVvalidation_UnknownType() {

		// case - type is unknown
		getTested(true).getContent("unknown", "12");

	}

	@Test(expected = Exception.class)
	public void getContent_ParamVvalidation_TypeConfigInvalid() {

		// case - type configuration is invalid (do not contains index name and type)
		getTested(true).getContent("invalid", "12");
	}

	@Test
	public void getContent() {
		try {
			ContentRestService tested = getTested(true);

			// case - type is valid, but nothing is found for passed id because index is missing
			indexDelete(INDEX_NAME);
			assertResponseStatus(tested.getContent("known", "12"), Response.Status.NOT_FOUND);

			// case - index is present bud document is not found
			indexCreate(INDEX_NAME);
			indexInsertDocument(INDEX_NAME, INDEX_TYPE, "known-1", "{\"name\":\"test\"}");
			assertResponseStatus(tested.getContent("known", "2"), Response.Status.NOT_FOUND);

			// case - document found
			@SuppressWarnings("unchecked")
			Map<String, Object> ret = (Map<String, Object>) tested.getContent("known", "1");
			assertEquals("test", ret.get("name"));

		} finally {
			indexDelete(INDEX_NAME);
			finalizeESClientForUnitTest();
		}
	}

	/**
	 * Prepare tested instance with injected mocks.
	 * 
	 * @param initEsClient - searchClientService is initialized from
	 *          {@link ESRealClientTestBase#prepareSearchClientServiceMock()} if this is true, so do not forget to clean
	 *          up client in finally!
	 * @return instance for test
	 */
	@SuppressWarnings("unchecked")
	protected ContentRestService getTested(boolean initEsClient) {
		ContentRestService tested = new ContentRestService();
		if (initEsClient)
			tested.searchClientService = prepareSearchClientServiceMock();

		tested.providerService = mock(ProviderService.class);
		setupProviderServiceMock(tested);

		tested.contentPersistenceService = mock(ContentPersistenceService.class);
		tested.log = Logger.getLogger("testlogger");
		tested.authenticationUtilService = mock(AuthenticationUtilService.class);

		tested.eventContentDeleted = mock(Event.class);
		tested.eventContentStored = mock(Event.class);
		tested.eventBeforeIndexed = mock(Event.class);

		when(tested.authenticationUtilService.getAuthenticatedProvider(null)).thenReturn("jbossorg");

		return tested;
	}

	/**
	 * @param tested
	 */
	protected void setupProviderServiceMock(ContentRestService tested) {
		Mockito.when(tested.providerService.findContentType("invalid")).thenReturn(
				ProviderServiceTest.createProviderContentTypeInfo(new HashMap<String, Object>()));
		Mockito.when(tested.providerService.findContentType("unknown")).thenReturn(null);
		Mockito.when(tested.providerService.generateSysId(Mockito.anyString(), Mockito.anyString())).thenCallRealMethod();

		Map<String, Object> typeDefKnown = new HashMap<String, Object>();
		Map<String, Object> typeDefKnownIndex = new HashMap<String, Object>();
		typeDefKnown.put(ProviderService.INDEX, typeDefKnownIndex);
		typeDefKnownIndex.put("name", INDEX_NAME);
		typeDefKnownIndex.put("type", INDEX_TYPE);
		typeDefKnown.put("input_preprocessors", PREPROCESSORS);
		typeDefKnown.put(ProviderService.SYS_TYPE, "my_sys_type");
		typeDefKnown.put(ProviderService.SYS_CONTENT_CONTENT_TYPE, "text/plain");
		Mockito.when(tested.providerService.findContentType("known")).thenReturn(
				ProviderServiceTest.createProviderContentTypeInfo(typeDefKnown));

		Map<String, Object> typeDefPersist = new HashMap<String, Object>();
		Map<String, Object> typeDefPersistIndex = new HashMap<String, Object>();
		typeDefPersist.put(ProviderService.INDEX, typeDefPersistIndex);
		typeDefPersistIndex.put("name", INDEX_NAME);
		typeDefPersistIndex.put("type", INDEX_TYPE);
		typeDefPersist.put(ProviderService.SYS_TYPE, "my_sys_type");
		typeDefPersist.put(ProviderService.PERSIST, "true");
		typeDefPersist.put(ProviderService.SYS_CONTENT_CONTENT_TYPE, "text/plain");
		when(tested.providerService.findContentType("persist")).thenReturn(
				ProviderServiceTest.createProviderContentTypeInfo(typeDefPersist));

		Map<String, Object> typeDefSysContent = new HashMap<String, Object>();
		Map<String, Object> typeDefSysContentIndex = new HashMap<String, Object>();
		typeDefSysContent.put(ProviderService.INDEX, typeDefPersistIndex);
		typeDefSysContentIndex.put("name", INDEX_NAME);
		typeDefSysContentIndex.put("type", INDEX_TYPE);
		typeDefSysContent.put(ProviderService.SYS_TYPE, "my_sys_type");
		when(tested.providerService.findContentType("invalid-content-type")).thenReturn(
				ProviderServiceTest.createProviderContentTypeInfo(typeDefSysContent));

		Map<String, Object> providerDef = new HashMap<String, Object>();
		when(tested.providerService.findProvider("jbossorg")).thenReturn(providerDef);
		Map<String, Object> typesDef = new HashMap<String, Object>();
		providerDef.put(ProviderService.TYPE, typesDef);
		typesDef.put("invalid", new HashMap<String, Object>());
		Map<String, Object> typeDefInvalid2 = new HashMap<String, Object>();
		typeDefInvalid2.put(ProviderService.INDEX, typeDefKnownIndex);
		typesDef.put("invalid_2", typeDefInvalid2);
		typesDef.put("known", typeDefKnown);
		typesDef.put("persist", typeDefPersist);
		typesDef.put("invalid-content-type", typeDefSysContent);
	}
}
