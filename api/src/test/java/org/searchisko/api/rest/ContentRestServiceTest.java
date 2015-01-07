/*
 * JBoss, Home of Professional Open Source
 * Copyright 2012 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 */
package org.searchisko.api.rest;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import javax.enterprise.event.Event;
import javax.ws.rs.core.Response;

import org.elasticsearch.common.settings.SettingsException;
import org.hamcrest.CustomMatcher;
import org.json.JSONException;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;
import org.searchisko.api.ContentObjectFields;
import org.searchisko.api.events.ContentBeforeIndexedEvent;
import org.searchisko.api.events.ContentDeletedEvent;
import org.searchisko.api.events.ContentStoredEvent;
import org.searchisko.api.rest.exception.BadFieldException;
import org.searchisko.api.rest.exception.NotAuthenticatedException;
import org.searchisko.api.rest.exception.NotAuthorizedException;
import org.searchisko.api.rest.exception.OperationUnavailableException;
import org.searchisko.api.rest.exception.RequiredFieldException;
import org.searchisko.api.security.AuthenticatedUserType;
import org.searchisko.api.service.AuthenticationUtilService;
import org.searchisko.api.service.ContentManipulationLockService;
import org.searchisko.api.service.ProviderService;
import org.searchisko.api.service.ProviderService.ProviderContentTypeInfo;
import org.searchisko.api.service.ProviderServiceTest;
import org.searchisko.api.testtools.ESRealClientTestBase;
import org.searchisko.api.testtools.TestUtils;
import org.searchisko.persistence.service.ContentPersistenceService;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import static org.searchisko.api.testtools.TestUtils.assertResponseStatus;
import static org.searchisko.api.testtools.TestUtils.assetJsonStreamingOutputContent;

/**
 * Unit test for {@link ContentRestService}
 * 
 * @author Vlastimil Elias (velias at redhat dot com)
 */
public class ContentRestServiceTest extends ESRealClientTestBase {

	private static final String TYPE_INVALID = "invalid";
	private static final String TYPE_PERSIST = "persist";
	private static final String TYPE_INVALID_CONTENT_TYPE = "invalid-content-type";
	private static final String TYPE_UNKNOWN = "unknown";
	private static final String TYPE_KNOWN = "known";
	private static final String INDEX_TYPE = "index_type";
	private static final String INDEX_NAME = "index_name";
	private static final List<Map<String, Object>> PREPROCESSORS = new ArrayList<Map<String, Object>>();

	@SuppressWarnings("unchecked")
	@Test
	public void processFieldSysVisibleForRoles() {
		ContentRestService tested = getTested(false);

		Map<String, Object> content = new HashMap<>();

		// case - no given field present at all
		tested.processFieldSysVisibleForRoles(content);
		Assert.assertEquals(0, content.size());

		// case - empty list is removed
		{
			content.put(ContentObjectFields.SYS_VISIBLE_FOR_ROLES, new ArrayList<>());
			tested.processFieldSysVisibleForRoles(content);
			Assert.assertEquals(0, content.size());

			ArrayList<String> l = new ArrayList<>();
			l.add("");
			content.put(ContentObjectFields.SYS_VISIBLE_FOR_ROLES, l);
			tested.processFieldSysVisibleForRoles(content);
			Assert.assertEquals(0, content.size());
		}

		// case - list is preserved, empty strings removed, strings trimmed
		{
			List<String> l = new ArrayList<>();
			l.add("role1");
			l.add("");
			l.add("role2 ");
			content.put(ContentObjectFields.SYS_VISIBLE_FOR_ROLES, l);
			tested.processFieldSysVisibleForRoles(content);
			Assert.assertEquals(1, content.size());
			List<String> ret = (List<String>) content.get(ContentObjectFields.SYS_VISIBLE_FOR_ROLES);
			Assert.assertEquals(2, ret.size());
			Assert.assertTrue(ret.contains("role1"));
			Assert.assertTrue(ret.contains("role2"));
		}

		// case - one string is converted to list
		content.put(ContentObjectFields.SYS_VISIBLE_FOR_ROLES, "role1");
		tested.processFieldSysVisibleForRoles(content);
		Assert.assertEquals(1, content.size());
		List<String> ret = (List<String>) content.get(ContentObjectFields.SYS_VISIBLE_FOR_ROLES);
		Assert.assertEquals(1, ret.size());
		Assert.assertTrue(ret.contains("role1"));

		// case - bad type in data throws exception
		try {
			content.put(ContentObjectFields.SYS_VISIBLE_FOR_ROLES, new Integer(1));
			tested.processFieldSysVisibleForRoles(content);
			Assert.fail("BadFieldException expected");
		} catch (BadFieldException e) {
			// OK
		}
		try {
			content.put(ContentObjectFields.SYS_VISIBLE_FOR_ROLES, new HashMap<>());
			tested.processFieldSysVisibleForRoles(content);
			Assert.fail("BadFieldException expected");
		} catch (BadFieldException e) {
			// OK
		}

	}

	@Test
	public void getTypeInfoWithManagePermissionCheck() {
		ContentRestService tested = getTested(false);

		// case - field param required validation
		try {
			tested.getTypeInfoWithManagePermissionCheck(null);
			Assert.fail("RequiredFieldException expected");
		} catch (RequiredFieldException e) {
			// OK
		}
		try {
			tested.getTypeInfoWithManagePermissionCheck("");
			Assert.fail("RequiredFieldException expected");
		} catch (RequiredFieldException e) {
			// OK
		}
		try {
			tested.getTypeInfoWithManagePermissionCheck("   ");
			Assert.fail("RequiredFieldException expected");
		} catch (RequiredFieldException e) {
			// OK
		}

		// case - unknown type
		try {
			tested.getTypeInfoWithManagePermissionCheck(TYPE_UNKNOWN);
			Assert.fail("BadFieldException expected");
		} catch (BadFieldException e) {
			// OK
		}

		// known type, provider has permission
		{
			ProviderContentTypeInfo ret = tested.getTypeInfoWithManagePermissionCheck(TYPE_KNOWN);
			Assert.assertNotNull(ret);
			Assert.assertEquals(TYPE_KNOWN, ret.getTypeName());
			Assert.assertEquals(ProviderServiceTest.TEST_PROVIDER_NAME, ret.getProviderName());
			verify(tested.authenticationUtilService)
					.checkProviderManagementPermission(ProviderServiceTest.TEST_PROVIDER_NAME);
		}

		// known type, provider has no permission
		try {
			Mockito.doThrow(new NotAuthorizedException("no perm")).when(tested.authenticationUtilService)
					.checkProviderManagementPermission(ProviderServiceTest.TEST_PROVIDER_NAME);
			tested.getTypeInfoWithManagePermissionCheck(TYPE_KNOWN);
			Assert.fail("NotAuthorizedException expected");
		} catch (NotAuthorizedException e) {
			// OK
		}

		// known type, provider not authenticated
		try {
			Mockito.doThrow(new NotAuthenticatedException(AuthenticatedUserType.PROVIDER))
					.when(tested.authenticationUtilService)
					.checkProviderManagementPermission(ProviderServiceTest.TEST_PROVIDER_NAME);
			tested.getTypeInfoWithManagePermissionCheck(TYPE_KNOWN);
			Assert.fail("NotAuthenticatedException expected");
		} catch (NotAuthenticatedException e) {
			// OK
		}

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
		getTested(false).pushContent(TYPE_KNOWN, null, content);
	}

	@Test(expected = RequiredFieldException.class)
	public void pushContent_invalidParams_4() throws Exception {
		Map<String, Object> content = new HashMap<String, Object>();
		content.put("test", "test");
		getTested(false).pushContent(TYPE_KNOWN, "", content);
	}

	@Test(expected = BadFieldException.class)
	public void pushContent_invalidParams_id_1() throws Exception {
		Map<String, Object> content = new HashMap<String, Object>();
		content.put("test", "test");
		getTested(false).pushContent("_id", "1", content);
	}

	@Test(expected = BadFieldException.class)
	public void pushContent_invalidParams_id_2() throws Exception {
		Map<String, Object> content = new HashMap<String, Object>();
		content.put("test", "test");
		getTested(false).pushContent("id,withcomma", "1", content);
	}

	@Test(expected = BadFieldException.class)
	public void pushContent_invalidParams_id_3() throws Exception {
		Map<String, Object> content = new HashMap<String, Object>();
		content.put("test", "test");
		getTested(false).pushContent("id*with*star", "1", content);
	}

	@Test(expected = BadFieldException.class)
	public void pushContent_invalidParams_MissingContent1() throws Exception {
		getTested(false).pushContent(TYPE_KNOWN, "1", null);
	}

	@Test(expected = BadFieldException.class)
	public void pushContent_invalidParams_MissingContent2() throws Exception {
		Map<String, Object> content = new HashMap<String, Object>();
		getTested(false).pushContent(TYPE_KNOWN, "1", content);
	}

	@Test(expected = BadFieldException.class)
	public void pushContent_invalidParams_UnknownType() throws Exception {
		// case - type is unknown
		Map<String, Object> content = new HashMap<String, Object>();
		content.put("test", "test");
		getTested(false).pushContent(TYPE_UNKNOWN, "1", content);
	}

	@Test(expected = Exception.class)
	public void pushContent_invalidParams_TypeInvalid() throws Exception {
		// case - type configuration is invalid - do not contains index name and/or index type
		Map<String, Object> content = new HashMap<String, Object>();
		content.put("test", "test");
		getTested(false).pushContent(TYPE_INVALID, "1", content);
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
		getTested(false).pushContent(TYPE_INVALID_CONTENT_TYPE, "1", content);
	}

	@Test(expected = NotAuthorizedException.class)
	public void pushContent_noPermission() throws Exception {
		ContentRestService tested = getTested(false);
		Map<String, Object> content = new HashMap<String, Object>();
		content.put("test", "testvalue");
		Mockito.doThrow(new NotAuthorizedException("no perm")).when(tested.authenticationUtilService)
				.checkProviderManagementPermission(ProviderServiceTest.TEST_PROVIDER_NAME);
		tested.pushContent(TYPE_KNOWN, "1", content);
	}

	@Test(expected = OperationUnavailableException.class)
	public void pushContent_apiLockedDown() throws Exception {
		ContentRestService tested = getTested(false);
		Mockito.when(tested.contentManipulationLockService.isLockedForProvider(Mockito.anyString())).thenReturn(true);
		Map<String, Object> content = new HashMap<String, Object>();
		content.put("test", "testvalue");
		tested.pushContent(TYPE_KNOWN, "1", content);
	}

	@SuppressWarnings("unchecked")
	@Test
	public void pushContent_noPersistence() throws Exception {
		try {
			ContentRestService tested = getTested(true);
			Map<String, Object> content = new HashMap<String, Object>();

			// case - insert when index is not found
			String sys_content_type = TYPE_KNOWN;
			{
				reset(tested.contentPersistenceService, tested.eventContentStored);
				indexDelete(INDEX_NAME);
				content.clear();
				content.put("test", "testvalue");
				content.put(ContentObjectFields.SYS_VISIBLE_FOR_ROLES, "role1");
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
				assertEquals(TestUtils.createListOfStrings("role1"), doc.get(ContentObjectFields.SYS_VISIBLE_FOR_ROLES));
				verify(tested.eventContentStored).fire(prepareContentStoredEventMatcher(sysId));
				verifyNoMoreInteractions(tested.contentPersistenceService);
			}

			// case - insert when index is found, fill sys_updated if not provided in content, process tags provided in
			// content, fill sys_content_content-type because sys_content is present
			{
				reset(tested.providerService, tested.contentPersistenceService, tested.eventContentStored,
						tested.eventBeforeIndexed);
				setupProviderServiceMock(tested.providerService);
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
				setupProviderServiceMock(tested.providerService);
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
			String sys_content_type = TYPE_PERSIST;

			{
				Mockito.reset(tested.contentPersistenceService, tested.eventContentStored, tested.eventBeforeIndexed);
				indexDelete(INDEX_NAME);
				content.clear();
				content.put("test", "testvalue");
				content.put(ContentObjectFields.SYS_VISIBLE_FOR_ROLES, new ArrayList<String>());
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
				assertFalse(doc.containsKey(ContentObjectFields.SYS_VISIBLE_FOR_ROLES));
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
				setupProviderServiceMock(tested.providerService);
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
				setupProviderServiceMock(tested.providerService);
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

	@Test(expected = NotAuthorizedException.class)
	public void pushContentBulk_noPermission() throws Exception {
		ContentRestService tested = getTested(false);
		Map<String, Object> content = new HashMap<String, Object>();
		Map<String, Object> contentItem = new HashMap<String, Object>();
		contentItem.put("title", "aaa");
		content.put("1", contentItem);
		Mockito.doThrow(new NotAuthorizedException("no perm")).when(tested.authenticationUtilService)
				.checkProviderManagementPermission(ProviderServiceTest.TEST_PROVIDER_NAME);
		tested.pushContentBulk(TYPE_KNOWN, content);
	}

	@Test(expected = OperationUnavailableException.class)
	public void pushContentBulk_apiLockedDown() throws Exception {
		ContentRestService tested = getTested(false);
		Mockito.when(tested.contentManipulationLockService.isLockedForProvider(Mockito.anyString())).thenReturn(true);
		Map<String, Object> content = new HashMap<String, Object>();
		Map<String, Object> contentItem = new HashMap<String, Object>();
		contentItem.put("title", "aaa");
		content.put("1", contentItem);
		tested.pushContentBulk(TYPE_KNOWN, content);
	}

	@SuppressWarnings("unchecked")
	@Test
	public void pushContentBulk_noPersistence() throws Exception {
		try {
			ContentRestService tested = getTested(true);

			String sys_content_type = TYPE_KNOWN;

			// case - insert/update, fill sys_updated if not provided in content, process tags provided in
			// content, fill sys_content_content-type because sys_content is present
			{
				String sysId_1 = tested.providerService.generateSysId(sys_content_type, "1");
				String sysId_2 = tested.providerService.generateSysId(sys_content_type, "2");

				indexInsertDocument(INDEX_NAME, INDEX_TYPE, sysId_1, "{\"test\":\"old\"}");
				indexFlushAndRefresh(INDEX_NAME);

				reset(tested.providerService, tested.contentPersistenceService, tested.eventContentStored,
						tested.eventBeforeIndexed);
				setupProviderServiceMock(tested.providerService);
				Map<String, Object> contentStructure = new LinkedHashMap<>();

				// empty content means error
				contentStructure.put("empty_content", new HashMap<>());

				Map<String, Object> content_1 = new HashMap<>();
				contentStructure.put("1", content_1);
				content_1.put("test", "testvalue");
				content_1.put(ContentObjectFields.SYS_CONTENT, "sys content");
				content_1.remove(ContentObjectFields.SYS_UPDATED);
				String[] tags = new String[] { "tag_value" };
				content_1.put("tags", tags);

				Map<String, Object> content_2 = new HashMap<>();
				contentStructure.put("2", content_2);
				content_2.put("test2", "testvalue2");
				content_2.put(ContentObjectFields.SYS_CONTENT, "sys content");
				content_2.remove(ContentObjectFields.SYS_UPDATED);
				String[] tags2 = new String[] { "tag_value" };
				content_2.put("tags", tags2);

				// validation of id format
				contentStructure.put("_bad_id_format", content_2);
				contentStructure.put("bad_id,format", content_2);
				contentStructure.put("bad_id*format", content_2);

				Map<String, Map<String, Object>> ret = (Map<String, Map<String, Object>>) tested.pushContentBulk(
						sys_content_type, contentStructure);
				Assert.assertEquals(6, ret.size());
				assertBulkPushRetItem(ret.get("empty_content"), "error",
						"fieldName=content, description=Some content for pushing must be defined");
				assertBulkPushRetItem(ret.get("_bad_id_format"), "error",
						"fieldName=contentId, description=contentId can't start with underscore or contain comma, star");
				assertBulkPushRetItem(ret.get("bad_id,format"), "error",
						"fieldName=contentId, description=contentId can't start with underscore or contain comma, star");
				assertBulkPushRetItem(ret.get("bad_id*format"), "error",
						"fieldName=contentId, description=contentId can't start with underscore or contain comma, star");

				assertBulkPushRetItem(ret.get("1"), "update", "Content updated successfully.");
				assertBulkPushRetItem(ret.get("2"), "insert", "Content inserted successfully.");

				verify(tested.eventBeforeIndexed).fire(prepareContentBeforeIndexedEventMatcher(sysId_1, content_1));
				verify(tested.eventBeforeIndexed).fire(prepareContentBeforeIndexedEventMatcher(sysId_2, content_2));
				verify(tested.providerService).runPreprocessors(sys_content_type, PREPROCESSORS, content_1);
				verify(tested.providerService).runPreprocessors(sys_content_type, PREPROCESSORS, content_2);
				verify(tested.eventContentStored).fire(prepareContentStoredEventMatcher(sysId_1));
				verify(tested.eventContentStored).fire(prepareContentStoredEventMatcher(sysId_2));
				verifyNoMoreInteractions(tested.contentPersistenceService, tested.eventBeforeIndexed,
						tested.eventContentDeleted, tested.eventContentStored);

				indexFlushAndRefresh(INDEX_NAME);

				// assert documents in index
				{
					Map<String, Object> doc = indexGetDocument(INDEX_NAME, INDEX_TYPE, sysId_2);
					assertNotNull(doc);
					assertEquals("testvalue2", doc.get("test2"));
					assertEquals("jbossorg", doc.get(ContentObjectFields.SYS_CONTENT_PROVIDER));
					assertEquals("2", doc.get(ContentObjectFields.SYS_CONTENT_ID));
					assertEquals(sys_content_type, doc.get(ContentObjectFields.SYS_CONTENT_TYPE));
					assertEquals("my_sys_type", doc.get(ContentObjectFields.SYS_TYPE));
					assertEquals("text/plain", doc.get(ContentObjectFields.SYS_CONTENT_CONTENT_TYPE));
					assertEquals(sysId_2, doc.get(ContentObjectFields.SYS_ID));
					assertNotNull(doc.get(ContentObjectFields.SYS_UPDATED));
					assertEquals("tag_value", ((List<String>) doc.get(ContentObjectFields.SYS_TAGS)).get(0));
				}
				{
					Map<String, Object> doc = indexGetDocument(INDEX_NAME, INDEX_TYPE, sysId_1);
					assertNotNull(doc);
					assertEquals("testvalue", doc.get("test"));
					assertEquals("jbossorg", doc.get(ContentObjectFields.SYS_CONTENT_PROVIDER));
				}
			}

		} finally {
			indexDelete(INDEX_NAME);
			finalizeESClientForUnitTest();
		}
	}

	@SuppressWarnings("unchecked")
	@Test
	public void pushContentBulk_persistence() throws Exception {
		try {
			ContentRestService tested = getTested(true);

			String sys_content_type = TYPE_PERSIST;

			// case - insert/update, fill sys_updated if not provided in content, process tags provided in
			// content, fill sys_content_content-type because sys_content is present
			{
				String sysId_1 = tested.providerService.generateSysId(sys_content_type, "1");
				String sysId_2 = tested.providerService.generateSysId(sys_content_type, "2");

				indexInsertDocument(INDEX_NAME, INDEX_TYPE, sysId_1, "{\"test\":\"old\"}");
				indexFlushAndRefresh(INDEX_NAME);

				reset(tested.providerService, tested.contentPersistenceService, tested.eventContentStored,
						tested.eventBeforeIndexed);
				setupProviderServiceMock(tested.providerService);
				Map<String, Object> contentStructure = new LinkedHashMap<>();

				// empty content means error
				contentStructure.put("empty_content", new HashMap<>());

				Map<String, Object> content_1 = new HashMap<>();
				contentStructure.put("1", content_1);
				content_1.put("test", "testvalue");
				content_1.put(ContentObjectFields.SYS_CONTENT, "sys content");
				content_1.remove(ContentObjectFields.SYS_UPDATED);
				String[] tags = new String[] { "tag_value" };
				content_1.put("tags", tags);

				Map<String, Object> content_2 = new HashMap<>();
				contentStructure.put("2", content_2);
				content_2.put("test2", "testvalue2");
				content_2.put(ContentObjectFields.SYS_CONTENT, "sys content");
				content_2.remove(ContentObjectFields.SYS_UPDATED);
				String[] tags2 = new String[] { "tag_value" };
				content_2.put("tags", tags2);

				// validation of id format
				contentStructure.put("_bad_id_format", content_2);
				contentStructure.put("bad_id,format", content_2);
				contentStructure.put("bad_id*format", content_2);

				Map<String, Map<String, Object>> ret = (Map<String, Map<String, Object>>) tested.pushContentBulk(
						sys_content_type, contentStructure);
				Assert.assertEquals(6, ret.size());
				assertBulkPushRetItem(ret.get("empty_content"), "error",
						"fieldName=content, description=Some content for pushing must be defined");
				assertBulkPushRetItem(ret.get("_bad_id_format"), "error",
						"fieldName=contentId, description=contentId can't start with underscore or contain comma, star");
				assertBulkPushRetItem(ret.get("bad_id,format"), "error",
						"fieldName=contentId, description=contentId can't start with underscore or contain comma, star");
				assertBulkPushRetItem(ret.get("bad_id*format"), "error",
						"fieldName=contentId, description=contentId can't start with underscore or contain comma, star");

				assertBulkPushRetItem(ret.get("1"), "update", "Content updated successfully.");
				assertBulkPushRetItem(ret.get("2"), "insert", "Content inserted successfully.");

				verify(tested.eventBeforeIndexed).fire(prepareContentBeforeIndexedEventMatcher(sysId_1, content_1));
				verify(tested.eventBeforeIndexed).fire(prepareContentBeforeIndexedEventMatcher(sysId_2, content_2));
				verify(tested.providerService).runPreprocessors(sys_content_type, null, content_1);
				verify(tested.providerService).runPreprocessors(sys_content_type, null, content_2);
				verify(tested.contentPersistenceService).store(sysId_1, sys_content_type, content_1);
				verify(tested.contentPersistenceService).store(sysId_2, sys_content_type, content_2);
				verify(tested.eventContentStored).fire(prepareContentStoredEventMatcher(sysId_1));
				verify(tested.eventContentStored).fire(prepareContentStoredEventMatcher(sysId_2));
				verifyNoMoreInteractions(tested.contentPersistenceService, tested.eventBeforeIndexed,
						tested.eventContentDeleted, tested.eventContentStored);

				indexFlushAndRefresh(INDEX_NAME);

				// assert documents in index
				{
					Map<String, Object> doc = indexGetDocument(INDEX_NAME, INDEX_TYPE, sysId_2);
					assertNotNull(doc);
					assertEquals("testvalue2", doc.get("test2"));
					assertEquals("jbossorg", doc.get(ContentObjectFields.SYS_CONTENT_PROVIDER));
					assertEquals("2", doc.get(ContentObjectFields.SYS_CONTENT_ID));
					assertEquals(sys_content_type, doc.get(ContentObjectFields.SYS_CONTENT_TYPE));
					assertEquals("my_sys_type", doc.get(ContentObjectFields.SYS_TYPE));
					assertEquals("text/plain", doc.get(ContentObjectFields.SYS_CONTENT_CONTENT_TYPE));
					assertEquals(sysId_2, doc.get(ContentObjectFields.SYS_ID));
					assertNotNull(doc.get(ContentObjectFields.SYS_UPDATED));
					assertEquals("tag_value", ((List<String>) doc.get(ContentObjectFields.SYS_TAGS)).get(0));
				}
				{
					Map<String, Object> doc = indexGetDocument(INDEX_NAME, INDEX_TYPE, sysId_1);
					assertNotNull(doc);
					assertEquals("testvalue", doc.get("test"));
					assertEquals("jbossorg", doc.get(ContentObjectFields.SYS_CONTENT_PROVIDER));
				}
			}

		} finally {
			indexDelete(INDEX_NAME);
			finalizeESClientForUnitTest();
		}
	}

	private void assertBulkPushRetItem(Map<String, Object> map, String expectedStatus, String expectedMessage) {
		Assert.assertNotNull(map);
		Assert.assertEquals(expectedStatus, map.get(ContentRestService.RETFIELD_STATUS));
		Assert.assertEquals(expectedMessage, map.get(ContentRestService.RETFIELD_MESSAGE));
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
		getTested(false).deleteContent(TYPE_KNOWN, null, null);
	}

	@Test(expected = RequiredFieldException.class)
	public void deleteContent_ParamValidation_4() throws Exception {
		getTested(false).deleteContent(TYPE_KNOWN, "", null);
	}

	@Test(expected = BadFieldException.class)
	public void deleteContent_ParamValidation_UnknownType() throws Exception {
		// case - type is unknown
		getTested(false).deleteContent(TYPE_UNKNOWN, "1", null);
	}

	@Test(expected = Exception.class)
	public void deleteContent_ParamValidation_TypeInvalid() throws Exception {
		// case - type configuration is invalid - do not contains index name and/or index type
		getTested(false).deleteContent(TYPE_INVALID, "1", null);
	}

	@Test(expected = NotAuthorizedException.class)
	public void deleteContent_noPermission() throws Exception {
		ContentRestService tested = getTested(false);
		Map<String, Object> content = new HashMap<String, Object>();
		content.put("test", "testvalue");
		Mockito.doThrow(new NotAuthorizedException("no perm")).when(tested.authenticationUtilService)
				.checkProviderManagementPermission(ProviderServiceTest.TEST_PROVIDER_NAME);
		tested.deleteContent(TYPE_KNOWN, "1", null);
	}

	@Test(expected = OperationUnavailableException.class)
	public void deleteContent_apiLockedDown() throws Exception {
		ContentRestService tested = getTested(false);
		Map<String, Object> content = new HashMap<String, Object>();
		content.put("test", "testvalue");
		Mockito.when(tested.contentManipulationLockService.isLockedForProvider(Mockito.anyString())).thenReturn(true);
		tested.deleteContent(TYPE_KNOWN, "1", null);
	}

	@Test
	public void deleteContent() throws Exception {

		try {
			ContentRestService tested = getTested(true);

			// case - delete when index is not found
			{
				reset(tested.contentPersistenceService, tested.eventContentDeleted);
				indexDelete(INDEX_NAME);
				assertResponseStatus(tested.deleteContent(TYPE_KNOWN, "1", null), Response.Status.NOT_FOUND);
				assertResponseStatus(tested.deleteContent(TYPE_PERSIST, "1", null), Response.Status.NOT_FOUND);
				verify(tested.contentPersistenceService).delete(tested.providerService.generateSysId(TYPE_PERSIST, "1"),
						TYPE_PERSIST);
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
				assertResponseStatus(tested.deleteContent(TYPE_KNOWN, "1", null), Response.Status.NOT_FOUND);
				assertResponseStatus(tested.deleteContent(TYPE_PERSIST, "1", null), Response.Status.NOT_FOUND);
				assertNotNull(indexGetDocument(INDEX_NAME, INDEX_TYPE, "known-2"));
				verify(tested.contentPersistenceService).delete(tested.providerService.generateSysId(TYPE_PERSIST, "1"),
						TYPE_PERSIST);
				verifyZeroInteractions(tested.eventContentDeleted);
				verifyNoMoreInteractions(tested.contentPersistenceService);
			}

			// case - delete when document exist in index
			{
				reset(tested.contentPersistenceService, tested.eventContentDeleted);
				indexInsertDocument(INDEX_NAME, INDEX_TYPE, "known-1", "{\"test1\":\"test1\"}");
				indexInsertDocument(INDEX_NAME, INDEX_TYPE, "persist-1", "{\"test1\":\"testper1\"}");
				indexFlushAndRefresh(INDEX_NAME);
				assertResponseStatus(tested.deleteContent(TYPE_KNOWN, "1", null), Response.Status.OK);
				verify(tested.eventContentDeleted).fire(prepareContentDeletedEventMatcher("known-1"));
				assertNull(indexGetDocument(INDEX_NAME, INDEX_TYPE, "known-1"));
				assertResponseStatus(tested.deleteContent(TYPE_PERSIST, "1", null), Response.Status.OK);
				assertNull(indexGetDocument(INDEX_NAME, INDEX_TYPE, "persist-1"));
				assertNotNull(indexGetDocument(INDEX_NAME, INDEX_TYPE, "known-2"));
				verify(tested.eventContentDeleted).fire(prepareContentDeletedEventMatcher("persist-1"));
				// another subsequent deletes
				assertResponseStatus(tested.deleteContent(TYPE_KNOWN, "1", null), Response.Status.NOT_FOUND);
				assertResponseStatus(tested.deleteContent(TYPE_PERSIST, "1", null), Response.Status.NOT_FOUND);
				assertResponseStatus(tested.deleteContent(TYPE_KNOWN, "2", null), Response.Status.OK);
				verify(tested.eventContentDeleted).fire(prepareContentDeletedEventMatcher("known-2"));
				assertNull(indexGetDocument(INDEX_NAME, INDEX_TYPE, "known-1"));
				assertNull(indexGetDocument(INDEX_NAME, INDEX_TYPE, "known-2"));

				// test ignore_missing parameter
				assertResponseStatus(tested.deleteContent(TYPE_KNOWN, "1", null), Response.Status.NOT_FOUND);
				assertResponseStatus(tested.deleteContent(TYPE_KNOWN, "1", "false"), Response.Status.NOT_FOUND);
				assertResponseStatus(tested.deleteContent(TYPE_KNOWN, "1", "true"), Response.Status.OK);

				verify(tested.contentPersistenceService, Mockito.times(2)).delete(
						tested.providerService.generateSysId(TYPE_PERSIST, "1"), TYPE_PERSIST);

				verifyNoMoreInteractions(tested.eventContentDeleted);
				verifyNoMoreInteractions(tested.contentPersistenceService);

			}

		} finally {
			indexDelete(INDEX_NAME);
			finalizeESClientForUnitTest();
		}
	}

	@Test(expected = RequiredFieldException.class)
	public void deleteContentBulk_ParamValidation_1() throws Exception {
		Map<String, Object> content = new HashMap<String, Object>();
		content.put("id", new ArrayList<String>());
		getTested(false).deleteContentBulk(null, content);
	}

	@Test(expected = RequiredFieldException.class)
	public void deleteContentBulk_ParamValidation_2() throws Exception {
		Map<String, Object> content = new HashMap<String, Object>();
		content.put("id", new ArrayList<String>());
		getTested(false).deleteContentBulk("", content);
	}

	@Test
	public void deleteContentBulk_invalidRequestContent() throws Exception {
		TestUtils.assertResponseStatus(getTested(false).deleteContentBulk(TYPE_KNOWN, null), Response.Status.BAD_REQUEST);
		Map<String, Object> content = new HashMap<String, Object>();
		TestUtils
				.assertResponseStatus(getTested(false).deleteContentBulk(TYPE_KNOWN, content), Response.Status.BAD_REQUEST);
		content.put("id", new Object());
		TestUtils
				.assertResponseStatus(getTested(false).deleteContentBulk(TYPE_KNOWN, content), Response.Status.BAD_REQUEST);
	}

	@Test(expected = NotAuthorizedException.class)
	public void deleteContentBulk_noPermission() throws Exception {
		ContentRestService tested = getTested(false);
		Map<String, Object> content = new HashMap<String, Object>();
		content.put("id", new ArrayList<String>());
		Mockito.doThrow(new NotAuthorizedException("no perm")).when(tested.authenticationUtilService)
				.checkProviderManagementPermission(ProviderServiceTest.TEST_PROVIDER_NAME);
		tested.deleteContentBulk(TYPE_KNOWN, content);
	}

	@Test(expected = OperationUnavailableException.class)
	public void deleteContentBulk_apiLockedDown() throws Exception {
		ContentRestService tested = getTested(false);
		Mockito.when(tested.contentManipulationLockService.isLockedForProvider(Mockito.anyString())).thenReturn(true);
		Map<String, Object> content = new HashMap<String, Object>();
		content.put("id", new ArrayList<String>());
		tested.deleteContentBulk(TYPE_KNOWN, content);
	}

	@SuppressWarnings("unchecked")
	@Test
	public void deleteContentBulk() {
		try {
			ContentRestService tested = getTested(true);

			Map<String, Object> content = new HashMap<String, Object>();
			List<String> ids = new ArrayList<String>();
			content.put("id", ids);

			// case - delete when index is not found
			{
				ids.add("1");
				ids.add("2");
				reset(tested.contentPersistenceService, tested.eventContentDeleted);
				indexDelete(INDEX_NAME);

				// without persistence
				Map<String, String> ret = (Map<String, String>) tested.deleteContentBulk(TYPE_KNOWN, content);
				Assert.assertEquals(2, ret.size());
				Assert.assertEquals("not_found", ret.get("1"));
				Assert.assertEquals("not_found", ret.get("2"));

				// persistently stored content
				ret = (Map<String, String>) tested.deleteContentBulk(TYPE_PERSIST, content);
				Assert.assertEquals(2, ret.size());
				Assert.assertEquals("not_found", ret.get("1"));
				Assert.assertEquals("not_found", ret.get("2"));

				verify(tested.contentPersistenceService).delete(tested.providerService.generateSysId(TYPE_PERSIST, "1"),
						TYPE_PERSIST);
				verify(tested.contentPersistenceService).delete(tested.providerService.generateSysId(TYPE_PERSIST, "2"),
						TYPE_PERSIST);
				verifyZeroInteractions(tested.eventContentDeleted);
				verifyNoMoreInteractions(tested.contentPersistenceService);
			}

			// case - delete when no any of documents exist in index
			{
				reset(tested.contentPersistenceService, tested.eventContentDeleted);
				indexDelete(INDEX_NAME);
				indexCreate(INDEX_NAME);
				indexInsertDocument(INDEX_NAME, INDEX_TYPE, "known-3", "{\"test3\":\"test3\"}");
				indexFlushAndRefresh(INDEX_NAME);

				// without persistence
				Map<String, String> ret = (Map<String, String>) tested.deleteContentBulk(TYPE_KNOWN, content);
				Assert.assertEquals(2, ret.size());
				Assert.assertEquals("not_found", ret.get("1"));
				Assert.assertEquals("not_found", ret.get("2"));
				assertNotNull(indexGetDocument(INDEX_NAME, INDEX_TYPE, "known-3"));

				// persistently stored content
				ret = (Map<String, String>) tested.deleteContentBulk(TYPE_PERSIST, content);
				Assert.assertEquals(2, ret.size());
				Assert.assertEquals("not_found", ret.get("1"));
				Assert.assertEquals("not_found", ret.get("2"));

				verify(tested.contentPersistenceService).delete(tested.providerService.generateSysId(TYPE_PERSIST, "1"),
						TYPE_PERSIST);
				verify(tested.contentPersistenceService).delete(tested.providerService.generateSysId(TYPE_PERSIST, "2"),
						TYPE_PERSIST);
				verifyZeroInteractions(tested.eventContentDeleted);
				verifyNoMoreInteractions(tested.contentPersistenceService);
			}

			// case - delete when part of documents exist in index
			{
				ids.add("10");
				reset(tested.contentPersistenceService, tested.eventContentDeleted);
				indexInsertDocument(INDEX_NAME, INDEX_TYPE, "known-1", "{\"test1\":\"test1\"}");
				indexInsertDocument(INDEX_NAME, INDEX_TYPE, "persist-1", "{\"test1\":\"testper1\"}");
				indexInsertDocument(INDEX_NAME, INDEX_TYPE, "known-2", "{\"test1\":\"test1\"}");
				indexInsertDocument(INDEX_NAME, INDEX_TYPE, "persist-2", "{\"test1\":\"testper1\"}");
				indexInsertDocument(INDEX_NAME, INDEX_TYPE, "known-3", "{\"test1\":\"test1\"}");
				indexInsertDocument(INDEX_NAME, INDEX_TYPE, "persist-3", "{\"test1\":\"testper1\"}");
				indexInsertDocument(INDEX_NAME, INDEX_TYPE, "known-4", "{\"test1\":\"test1\"}");
				indexInsertDocument(INDEX_NAME, INDEX_TYPE, "persist-4", "{\"test1\":\"testper1\"}");
				indexFlushAndRefresh(INDEX_NAME);

				// without persistence
				Map<String, String> ret = (Map<String, String>) tested.deleteContentBulk(TYPE_KNOWN, content);
				Assert.assertEquals(3, ret.size());
				Assert.assertEquals("ok", ret.get("1"));
				Assert.assertEquals("ok", ret.get("2"));
				Assert.assertEquals("not_found", ret.get("10"));
				verify(tested.eventContentDeleted).fire(prepareContentDeletedEventMatcher("known-1"));
				verify(tested.eventContentDeleted).fire(prepareContentDeletedEventMatcher("known-2"));
				assertNull(indexGetDocument(INDEX_NAME, INDEX_TYPE, "known-1"));
				assertNull(indexGetDocument(INDEX_NAME, INDEX_TYPE, "known-2"));
				assertNotNull(indexGetDocument(INDEX_NAME, INDEX_TYPE, "known-3"));
				assertNotNull(indexGetDocument(INDEX_NAME, INDEX_TYPE, "known-4"));
				assertNotNull(indexGetDocument(INDEX_NAME, INDEX_TYPE, "persist-1"));
				assertNotNull(indexGetDocument(INDEX_NAME, INDEX_TYPE, "persist-2"));
				assertNotNull(indexGetDocument(INDEX_NAME, INDEX_TYPE, "persist-3"));
				assertNotNull(indexGetDocument(INDEX_NAME, INDEX_TYPE, "persist-4"));

				// persistently stored content
				ret = (Map<String, String>) tested.deleteContentBulk(TYPE_PERSIST, content);
				Assert.assertEquals(3, ret.size());
				Assert.assertEquals("ok", ret.get("1"));
				Assert.assertEquals("ok", ret.get("2"));
				Assert.assertEquals("not_found", ret.get("10"));
				assertNull(indexGetDocument(INDEX_NAME, INDEX_TYPE, "persist-1"));
				assertNull(indexGetDocument(INDEX_NAME, INDEX_TYPE, "persist-2"));
				assertNotNull(indexGetDocument(INDEX_NAME, INDEX_TYPE, "persist-3"));
				assertNotNull(indexGetDocument(INDEX_NAME, INDEX_TYPE, "persist-4"));
				assertNotNull(indexGetDocument(INDEX_NAME, INDEX_TYPE, "known-3"));
				assertNotNull(indexGetDocument(INDEX_NAME, INDEX_TYPE, "known-4"));
				verify(tested.contentPersistenceService).delete(tested.providerService.generateSysId(TYPE_PERSIST, "1"),
						TYPE_PERSIST);
				verify(tested.contentPersistenceService).delete(tested.providerService.generateSysId(TYPE_PERSIST, "2"),
						TYPE_PERSIST);
				verify(tested.contentPersistenceService).delete(tested.providerService.generateSysId(TYPE_PERSIST, "10"),
						TYPE_PERSIST);
				verify(tested.eventContentDeleted).fire(prepareContentDeletedEventMatcher("persist-1"));
				verify(tested.eventContentDeleted).fire(prepareContentDeletedEventMatcher("persist-2"));
				// event is not fired for 10 as it is not deleted actually because doesn't exist
				verifyNoMoreInteractions(tested.eventContentDeleted);
				verifyNoMoreInteractions(tested.contentPersistenceService);

			}

		} finally {
			indexDelete(INDEX_NAME);
			finalizeESClientForUnitTest();
		}

	}

	@Test(expected = RequiredFieldException.class)
	public void getAllContent_ParamValidation_1() throws IOException, InterruptedException {
		getTested(false).getAllContent(null, null, null, null);
	}

	@Test(expected = RequiredFieldException.class)
	public void getAllContent_ParamValidation_2() throws IOException, InterruptedException {
		getTested(false).getAllContent("", null, null, null);
	}

	@Test(expected = BadFieldException.class)
	public void getAllContent_ParamValidation_UnknownType() throws IOException, InterruptedException {
		// case - type is unknown
		getTested(false).getAllContent(TYPE_UNKNOWN, null, null, null);
	}

	@Test(expected = SettingsException.class)
	public void getAllContent_ParamValidation_TypeConfigInvalid() throws IOException, InterruptedException {

		// case - type configuration is invalid (do not contains index name and type)
		getTested(false).getAllContent(TYPE_INVALID, null, null, null);
	}

	@Test
	public void getAllContent() throws IOException, InterruptedException, JSONException {

		try {
			ContentRestService tested = getTested(true);

			// case - type is valid, but nothing is found because index is missing
			indexDelete(INDEX_NAME);
			assertResponseStatus(tested.getAllContent(TYPE_KNOWN, null, null, null), Response.Status.NOT_FOUND);

			// case - nothing found because index is empty
			indexCreate(INDEX_NAME);
			assetJsonStreamingOutputContent("{\"total\":0,\"hits\":[]}", tested.getAllContent(TYPE_KNOWN, null, null, null));

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
			assetJsonStreamingOutputContent("{\"total\":4,\"hits\":["
					+ "{\"id\":\"1\",\"data\":{\"sys_updated\":0,\"sys_content_id\":\"1\",\"name\":\"test1\"}},"
					+ "{\"id\":\"2\",\"data\":{\"sys_updated\":1,\"sys_content_id\":\"2\",\"name\":\"test2\"}},"
					+ "{\"id\":\"3\",\"data\":{\"sys_updated\":2,\"sys_content_id\":\"3\",\"name\":\"test3\"}},"
					+ "{\"id\":\"4\",\"data\":{\"sys_updated\":3,\"sys_content_id\":\"4\",\"name\":\"test4\"}}" + "]}",
					tested.getAllContent(TYPE_KNOWN, null, null, null));

			// case - something found, from and size param used
			assetJsonStreamingOutputContent("{\"total\":4,\"hits\":["
					+ "{\"id\":\"2\",\"data\":{\"sys_updated\":1,\"sys_content_id\":\"2\",\"name\":\"test2\"}},"
					+ "{\"id\":\"3\",\"data\":{\"sys_updated\":2,\"sys_content_id\":\"3\",\"name\":\"test3\"}}" + "]}",
					tested.getAllContent(TYPE_KNOWN, 1, 2, null));

			// case - sort param used
			indexInsertDocument(INDEX_NAME, INDEX_TYPE, "known-5",
					"{\"name\":\"test5\", \"sys_updated\" : 4,\"sys_content_id\":\"5\"}");
			indexFlushAndRefresh(INDEX_NAME);
			// on ASC our record with id 5 is last, so we set from=4
			assetJsonStreamingOutputContent(
					"{\"total\":5,\"hits\":[{\"id\":\"5\",\"data\":{\"sys_updated\":4,\"sys_content_id\":\"5\",\"name\":\"test5\"}}]}",
					tested.getAllContent(TYPE_KNOWN, 4, 1, "asc"));
			// on DESC our record with id 5 is first, so we set from=0
			assetJsonStreamingOutputContent(
					"{\"total\":5,\"hits\":[{\"id\":\"5\",\"data\":{\"sys_updated\":4,\"sys_content_id\":\"5\",\"name\":\"test5\"}}]}",
					tested.getAllContent(TYPE_KNOWN, 0, 1, "DESC"));

		} finally {
			indexDelete(INDEX_NAME);
			finalizeESClientForUnitTest();
		}
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
		getTested(true).getContent(TYPE_UNKNOWN, "12");

	}

	@Test(expected = Exception.class)
	public void getContent_ParamVvalidation_TypeConfigInvalid() {

		// case - type configuration is invalid (do not contains index name and type)
		getTested(true).getContent(TYPE_INVALID, "12");
	}

	@Test
	public void getContent() {
		try {
			ContentRestService tested = getTested(true);

			// case - type is valid, but nothing is found for passed id because index is missing
			indexDelete(INDEX_NAME);
			assertResponseStatus(tested.getContent(TYPE_KNOWN, "12"), Response.Status.NOT_FOUND);

			// case - index is present bud document is not found
			indexCreate(INDEX_NAME);
			indexInsertDocument(INDEX_NAME, INDEX_TYPE, "known-1", "{\"name\":\"test\"}");
			assertResponseStatus(tested.getContent(TYPE_KNOWN, "2"), Response.Status.NOT_FOUND);

			// case - document found
			@SuppressWarnings("unchecked")
			Map<String, Object> ret = (Map<String, Object>) tested.getContent(TYPE_KNOWN, "1");
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
	 *          {@link ESRealClientTestBase#prepareSearchClientServiceMock(String clusterName)} if this is true, so do not forget to clean
	 *          up client in finally!
	 * @return instance for test
	 */
	@SuppressWarnings("unchecked")
	protected ContentRestService getTested(boolean initEsClient) {
		ContentRestService tested = new ContentRestService();
		if (initEsClient)
			tested.searchClientService = prepareSearchClientServiceMock("ContentRestServiceTest");

		tested.providerService = mock(ProviderService.class);
		setupProviderServiceMock(tested.providerService);

		tested.contentPersistenceService = mock(ContentPersistenceService.class);
		tested.log = Logger.getLogger("testlogger");
		tested.authenticationUtilService = mock(AuthenticationUtilService.class);

		tested.contentManipulationLockService = Mockito.mock(ContentManipulationLockService.class);

		tested.eventContentDeleted = mock(Event.class);
		tested.eventContentStored = mock(Event.class);
		tested.eventBeforeIndexed = mock(Event.class);

		when(tested.authenticationUtilService.getAuthenticatedProvider())
				.thenReturn(ProviderServiceTest.TEST_PROVIDER_NAME);

		return tested;
	}

	private void setupProviderServiceMock(ProviderService providerServiceMock) {
		Mockito.when(providerServiceMock.findContentType(TYPE_INVALID)).thenReturn(
				ProviderServiceTest.createProviderContentTypeInfo(new HashMap<String, Object>()));
		Mockito.when(providerServiceMock.findContentType(TYPE_UNKNOWN)).thenReturn(null);
		Mockito.when(providerServiceMock.generateSysId(Mockito.anyString(), Mockito.anyString())).thenCallRealMethod();

		Map<String, Object> typeDefKnown = new HashMap<String, Object>();
		Map<String, Object> typeDefKnownIndex = new HashMap<String, Object>();
		typeDefKnown.put(ProviderService.INDEX, typeDefKnownIndex);
		typeDefKnownIndex.put("name", INDEX_NAME);
		typeDefKnownIndex.put("type", INDEX_TYPE);
		typeDefKnown.put("input_preprocessors", PREPROCESSORS);
		typeDefKnown.put(ProviderService.SYS_TYPE, "my_sys_type");
		typeDefKnown.put(ProviderService.SYS_CONTENT_CONTENT_TYPE, "text/plain");
		Mockito.when(providerServiceMock.findContentType(TYPE_KNOWN)).thenReturn(
				ProviderServiceTest.createProviderContentTypeInfo(typeDefKnown, TYPE_KNOWN));

		Map<String, Object> typeDefPersist = new HashMap<String, Object>();
		Map<String, Object> typeDefPersistIndex = new HashMap<String, Object>();
		typeDefPersist.put(ProviderService.INDEX, typeDefPersistIndex);
		typeDefPersistIndex.put("name", INDEX_NAME);
		typeDefPersistIndex.put("type", INDEX_TYPE);
		typeDefPersist.put(ProviderService.SYS_TYPE, "my_sys_type");
		typeDefPersist.put(ProviderService.PERSIST, "true");
		typeDefPersist.put(ProviderService.SYS_CONTENT_CONTENT_TYPE, "text/plain");
		when(providerServiceMock.findContentType(TYPE_PERSIST)).thenReturn(
				ProviderServiceTest.createProviderContentTypeInfo(typeDefPersist, TYPE_PERSIST));

		Map<String, Object> typeDefSysContent = new HashMap<String, Object>();
		Map<String, Object> typeDefSysContentIndex = new HashMap<String, Object>();
		typeDefSysContent.put(ProviderService.INDEX, typeDefPersistIndex);
		typeDefSysContentIndex.put("name", INDEX_NAME);
		typeDefSysContentIndex.put("type", INDEX_TYPE);
		typeDefSysContent.put(ProviderService.SYS_TYPE, "my_sys_type");
		when(providerServiceMock.findContentType(TYPE_INVALID_CONTENT_TYPE)).thenReturn(
				ProviderServiceTest.createProviderContentTypeInfo(typeDefSysContent, TYPE_INVALID_CONTENT_TYPE));

		Map<String, Object> providerDef = new HashMap<String, Object>();
		providerDef.put(ProviderService.NAME, ProviderServiceTest.TEST_PROVIDER_NAME);
		when(providerServiceMock.findProvider(ProviderServiceTest.TEST_PROVIDER_NAME)).thenReturn(providerDef);
		Map<String, Object> typesDef = new HashMap<String, Object>();
		providerDef.put(ProviderService.TYPE, typesDef);
		typesDef.put(TYPE_INVALID, new HashMap<String, Object>());
		Map<String, Object> typeDefInvalid2 = new HashMap<String, Object>();
		typeDefInvalid2.put(ProviderService.INDEX, typeDefKnownIndex);
		typesDef.put("invalid_2", typeDefInvalid2);
		typesDef.put(TYPE_KNOWN, typeDefKnown);
		typesDef.put(TYPE_PERSIST, typeDefPersist);
		typesDef.put(TYPE_INVALID_CONTENT_TYPE, typeDefSysContent);
	}
}
