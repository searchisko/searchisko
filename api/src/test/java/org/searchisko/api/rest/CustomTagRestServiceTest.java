/*
 * JBoss, Home of Professional Open Source
 * Copyright 2015 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 */
package org.searchisko.api.rest;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import javax.ws.rs.core.Response.Status;

import org.elasticsearch.action.get.GetResponse;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.searchisko.api.rest.exception.RequiredFieldException;
import org.searchisko.api.security.Role;
import org.searchisko.api.service.AuthenticationUtilService;
import org.searchisko.api.service.CustomTagService;
import org.searchisko.api.service.ProviderService;
import org.searchisko.api.service.ProviderServiceTest;
import org.searchisko.api.service.SearchClientService;
import org.searchisko.api.service.SearchIndexMissingException;
import org.searchisko.api.testtools.TestUtils;
import org.searchisko.persistence.jpa.model.Tag;
import org.searchisko.persistence.service.CustomTagPersistenceService;

/**
 * Unit test for {@link CustomTagRestService}
 * 
 * @author Jiri Mauritz (jirmauritz at gmail dot com)
 */
public class CustomTagRestServiceTest {

	private static final String MOCK_TYPE_NAME = "mytype";
	private static final String MOCK_INDEX_NAME = "myindex";
	private static final String MOCK_CONTENT_ID_1 = "jb-45";
	private static final String MOCK_CONTENT_ID_2 = "jb-425";
	private static final String MOCK_CONTENT_TYPE = "jb";
	private static final String MOCK_CONTRIB_ID = "test <test@test.org>";

	@Test(expected = RequiredFieldException.class)
	public void getTagsByContent_invalidParam_1() {
		CustomTagRestService tested = getTested();
		tested.getTagsByContent(null);
	}

	@Test(expected = RequiredFieldException.class)
	public void getTagsByContent_invalidParam_2() {
		CustomTagRestService tested = getTested();
		tested.getTagsByContent(" ");
	}

	@Test
	public void getTagsByContent() {
		CustomTagRestService tested = getTested();
		prepareContentId(tested);

		// case - no tag in DB, null returned from service
		{
			Mockito.when(tested.customTagPersistenceService.getTagsByContent(Mockito.eq(MOCK_CONTENT_ID_1))).thenReturn(null);
			TestUtils.assertResponseStatus(tested.getTagsByContent(MOCK_CONTENT_ID_1), Status.NOT_FOUND);
			Mockito.verify(tested.customTagPersistenceService).getTagsByContent(Mockito.eq(MOCK_CONTENT_ID_1));
		}

		// case - no tag in DB, empty list returned from service
		{
			Mockito.reset(tested.customTagPersistenceService);
			Mockito.when(tested.customTagPersistenceService.getTagsByContent(Mockito.eq(MOCK_CONTENT_ID_1))).thenReturn(
					new ArrayList<Tag>());
			TestUtils.assertResponseStatus(tested.getTagsByContent(MOCK_CONTENT_ID_1), Status.NOT_FOUND);
			Mockito.verify(tested.customTagPersistenceService).getTagsByContent(Mockito.eq(MOCK_CONTENT_ID_1));
		}

		// case - tag in DB so returned
		{
			List<Tag> tags = new ArrayList<Tag>();
			tags.add(new Tag(MOCK_CONTENT_ID_1, MOCK_CONTRIB_ID, "label"));
			Mockito.reset(tested.customTagPersistenceService);
			Mockito.when(tested.customTagPersistenceService.getTagsByContent(Mockito.eq(MOCK_CONTENT_ID_1))).thenReturn(tags);
			@SuppressWarnings("unchecked")
			Map<String, Object> aret = (Map<String, Object>) tested.getTagsByContent(MOCK_CONTENT_ID_1);
			List<String> testingList = new ArrayList<>();
			testingList.add("label");
			assertCustomTagJSON(aret, testingList);
		}
	}

	protected void assertCustomTagJSON(Map<String, Object> customTagJsonMap, List<String> labels) {
		Assert.assertNotNull(customTagJsonMap);
		Assert.assertEquals(1, customTagJsonMap.size());
		Assert.assertEquals(labels, customTagJsonMap.get(CustomTagRestService.DATA_FIELD_TAGGING));
	}

	@Test(expected = RequiredFieldException.class)
	public void getTagsByContentType_invalidParam_1() {
		CustomTagRestService tested = getTested();
		tested.getTagsByContentType(null);
	}

	@Test(expected = RequiredFieldException.class)
	public void getTagsByContentType_invalidParam_2() {
		CustomTagRestService tested = getTested();
		tested.getTagsByContentType(" ");
	}

	@SuppressWarnings("unchecked")
	@Test
	public void getTagsByContentType() {
		CustomTagRestService tested = getTested();
		Map<String, Object> typeDef = mockTypeDef();
		Mockito.when(tested.providerService.findContentType(MOCK_CONTENT_TYPE)).thenReturn(
				ProviderServiceTest.createProviderContentTypeInfo(typeDef));

		// case - no tags in DB, null returned from service
		{
			Mockito.when(tested.customTagPersistenceService.getTagsByContentType(Mockito.anyString())).thenReturn(null);
			TestUtils.assertResponseStatus(tested.getTagsByContentType(MOCK_CONTENT_TYPE), Status.NOT_FOUND);
			Mockito.verify(tested.customTagPersistenceService).getTagsByContentType(MOCK_CONTENT_TYPE);
		}

		// case - no customTag in DB, empty list returned from service
		{
			Mockito.reset(tested.customTagPersistenceService);
			Mockito.when(tested.customTagPersistenceService.getTagsByContentType(Mockito.anyString())).thenReturn(
					new ArrayList<Tag>());
			TestUtils.assertResponseStatus(tested.getTagsByContentType(MOCK_CONTENT_TYPE), Status.NOT_FOUND);
			Mockito.verify(tested.customTagPersistenceService).getTagsByContentType(MOCK_CONTENT_TYPE);
		}

		// case - customTag in DB so returned
		{
			List<Tag> tags = new ArrayList<Tag>();
			tags.add(new Tag(MOCK_CONTENT_ID_1, MOCK_CONTRIB_ID, "label1"));
			tags.add(new Tag(MOCK_CONTENT_ID_2, MOCK_CONTRIB_ID, "label2"));
			Mockito.reset(tested.customTagPersistenceService);
			Mockito.when(tested.customTagPersistenceService.getTagsByContentType(MOCK_CONTENT_TYPE)).thenReturn(tags);
			Map<String, Object> aret = (Map<String, Object>) tested.getTagsByContentType(MOCK_CONTENT_TYPE);
			Mockito.verify(tested.customTagPersistenceService).getTagsByContentType(MOCK_CONTENT_TYPE);
			List<String> testingList = new ArrayList<>();
			testingList.add("label1");
			testingList.add("label2");
			assertCustomTagJSON(aret, testingList);
		}
	}

	@Test(expected = RequiredFieldException.class)
	public void postTag_invalidParam_contentSysId_1() {
		CustomTagRestService tested = getTested();
		Map<String, Object> content = new HashMap<>();
		content.put(CustomTagRestService.DATA_FIELD_TAGGING, "label");
		tested.postTag(null, content);
	}

	@Test(expected = RequiredFieldException.class)
	public void postTag_invalidParam_contentSysId_2() {
		CustomTagRestService tested = getTested();
		Map<String, Object> content = new HashMap<>();
		content.put(CustomTagRestService.DATA_FIELD_TAGGING, "label");
		tested.postTag(" ", content);
	}

	@Test
	public void postTag_invalidParam_contentSysId_3() {
		CustomTagRestService tested = getTested();
		Map<String, Object> content = new HashMap<>();
		content.put(CustomTagRestService.DATA_FIELD_TAGGING, "label");

		// invalid format of contentSysId checked over providerService
		Mockito.when(tested.providerService.parseTypeNameFromSysId("aa")).thenThrow(new IllegalArgumentException());
		TestUtils.assertResponseStatus(tested.postTag("aa", content), Status.BAD_REQUEST);
	}

	@Test(expected = RequiredFieldException.class)
	public void postTag_invalidParam_customTagValue_1() {
		CustomTagRestService tested = getTested();
		tested.postTag(MOCK_CONTENT_ID_1, null);
	}

	@Test(expected = RequiredFieldException.class)
	public void postTag_invalidParam_customTagValue_2() {
		CustomTagRestService tested = getTested();
		prepareContentId(tested);
		Map<String, Object> content = new HashMap<>();
		tested.postTag(MOCK_CONTENT_ID_1, content);
	}

	@Test
	public void postTag_invalidParam_customTagValue_3() {
		CustomTagRestService tested = getTested();
		prepareContentId(tested);
		Map<String, Object> content = new HashMap<>();
		content.put(CustomTagRestService.DATA_FIELD_TAGGING, new Object());
		TestUtils.assertResponseStatus(tested.postTag(MOCK_CONTENT_ID_1, content), Status.BAD_REQUEST);

		content.put(CustomTagRestService.DATA_FIELD_TAGGING, new Integer(6));
		TestUtils.assertResponseStatus(tested.postTag(MOCK_CONTENT_ID_1, content), Status.BAD_REQUEST);
	}

	private static final String MOCK_PROVIDER_NAME = "jboss";

	@Test
	public void postTag() throws SearchIndexMissingException {
		// case - unknown type
		{
			CustomTagRestService tested = getTested();
			Mockito.when(tested.providerService.parseTypeNameFromSysId(MOCK_CONTENT_ID_1)).thenReturn(MOCK_PROVIDER_NAME);
			Mockito.when(tested.providerService.findContentType(MOCK_PROVIDER_NAME)).thenReturn(null);

			Map<String, Object> content = new HashMap<>();
			content.put(CustomTagRestService.DATA_FIELD_TAGGING, "label");
			TestUtils.assertResponseStatus(tested.postTag(MOCK_CONTENT_ID_1, content), Status.NOT_FOUND);

			Mockito.verify(tested.providerService).findContentType(MOCK_PROVIDER_NAME);
		}

		// case - non existing document
		{
			CustomTagRestService tested = getTested();

			Mockito.when(tested.providerService.parseTypeNameFromSysId(MOCK_CONTENT_ID_1)).thenReturn(MOCK_PROVIDER_NAME);

			Map<String, Object> typeDef = mockTypeDef();
			Mockito.when(tested.providerService.findContentType(MOCK_PROVIDER_NAME)).thenReturn(
					ProviderServiceTest.createProviderContentTypeInfo(typeDef));

			GetResponse grMock = Mockito.mock(GetResponse.class);
			Mockito.when(grMock.isExists()).thenReturn(false);
			Mockito.when(tested.searchClientService.performGet(MOCK_INDEX_NAME, MOCK_TYPE_NAME, MOCK_CONTENT_ID_1))
					.thenReturn(grMock);

			Map<String, Object> content = new HashMap<>();
			content.put(CustomTagRestService.DATA_FIELD_TAGGING, "label");
			TestUtils.assertResponseStatus(tested.postTag(MOCK_CONTENT_ID_1, content), Status.NOT_FOUND);

			Mockito.verify(tested.searchClientService).performGet(MOCK_INDEX_NAME, MOCK_TYPE_NAME, MOCK_CONTENT_ID_1);
		}

		// case - customTag OK with document upgrade
		{
			CustomTagRestService tested = getTested();

			Mockito.when(tested.providerService.parseTypeNameFromSysId(MOCK_CONTENT_ID_1)).thenReturn(MOCK_PROVIDER_NAME);
			Mockito.when(tested.customTagPersistenceService.createTag(Mockito.any(Tag.class))).thenReturn(false);

			Map<String, Object> typeDef = mockTypeDef();
			Mockito.when(tested.providerService.findContentType(MOCK_PROVIDER_NAME)).thenReturn(
					ProviderServiceTest.createProviderContentTypeInfo(typeDef));

			GetResponse grMock = Mockito.mock(GetResponse.class);
			Mockito.when(grMock.isExists()).thenReturn(true);
			Mockito.when(tested.searchClientService.performGet(MOCK_INDEX_NAME, MOCK_TYPE_NAME, MOCK_CONTENT_ID_1))
					.thenReturn(grMock);

			Map<String, Object> requestContent = new HashMap<>();
			requestContent.put(CustomTagRestService.DATA_FIELD_TAGGING, "label");
			TestUtils.assertResponseStatus(tested.postTag(MOCK_CONTENT_ID_1, requestContent), Status.OK);

			// verify service call
			Mockito.verify(tested.searchClientService).performGet(MOCK_INDEX_NAME, MOCK_TYPE_NAME, MOCK_CONTENT_ID_1);

			// verify jpa call
			ArgumentCaptor<Tag> argument = ArgumentCaptor.forClass(Tag.class);
			Mockito.verify(tested.customTagPersistenceService).createTag((Tag) argument.capture());
			Assert.assertEquals("label", ((Tag) argument.getValue()).getTagLabel());
		}
	}

	@Test(expected = RequiredFieldException.class)
	public void deleteTag_invalidParam_contentSysId_1() {
		CustomTagRestService tested = getTested();
		Map<String, Object> content = new HashMap<>();
		content.put(CustomTagRestService.DATA_FIELD_TAGGING, "label");
		tested.deleteTag(null, content);
	}

	@Test(expected = RequiredFieldException.class)
	public void deleteTag_invalidParam_contentSysId_2() {
		CustomTagRestService tested = getTested();
		Map<String, Object> content = new HashMap<>();
		content.put(CustomTagRestService.DATA_FIELD_TAGGING, "label");
		tested.deleteTag(" ", content);
	}

	@Test
	public void deleteTag_invalidParam_contentSysId_3() {
		CustomTagRestService tested = getTested();
		Map<String, Object> content = new HashMap<>();
		content.put(CustomTagRestService.DATA_FIELD_TAGGING, "label");

		// invalid format of contentSysId checked over providerService
		Mockito.when(tested.providerService.parseTypeNameFromSysId("aa")).thenThrow(new IllegalArgumentException());
		TestUtils.assertResponseStatus(tested.deleteTag("aa", content), Status.BAD_REQUEST);
	}

	@Test(expected = RequiredFieldException.class)
	public void deleteTag_invalidParam_customTagValue_1() {
		CustomTagRestService tested = getTested();
		tested.deleteTag(MOCK_CONTENT_ID_1, null);
	}

	@Test(expected = RequiredFieldException.class)
	public void deleteTag_invalidParam_customTagValue_2() {
		CustomTagRestService tested = getTested();
		prepareContentId(tested);
		Map<String, Object> content = new HashMap<>();
		tested.deleteTag(MOCK_CONTENT_ID_1, content);
	}

	@Test
	public void deleteTag_invalidParam_customTagValue_3() {
		CustomTagRestService tested = getTested();
		prepareContentId(tested);
		Map<String, Object> content = new HashMap<>();
		content.put(CustomTagRestService.DATA_FIELD_TAGGING, new Object());
		TestUtils.assertResponseStatus(tested.deleteTag(MOCK_CONTENT_ID_1, content), Status.BAD_REQUEST);

		content.put(CustomTagRestService.DATA_FIELD_TAGGING, new Integer(6));
		TestUtils.assertResponseStatus(tested.deleteTag(MOCK_CONTENT_ID_1, content), Status.BAD_REQUEST);
	}

	@Test
	public void deleteTag() throws SearchIndexMissingException {
		// case - unknown type
		{
			CustomTagRestService tested = getTested();
			Mockito.when(tested.providerService.parseTypeNameFromSysId(MOCK_CONTENT_ID_1)).thenReturn(MOCK_PROVIDER_NAME);
			Mockito.when(tested.providerService.findContentType(MOCK_PROVIDER_NAME)).thenReturn(null);

			Map<String, Object> content = new HashMap<>();
			content.put(CustomTagRestService.DATA_FIELD_TAGGING, "label");
			TestUtils.assertResponseStatus(tested.deleteTag(MOCK_CONTENT_ID_1, content), Status.NOT_FOUND);

			Mockito.verify(tested.providerService).findContentType(MOCK_PROVIDER_NAME);
		}

		// case - non existing document
		{
			CustomTagRestService tested = getTested();

			Mockito.when(tested.providerService.parseTypeNameFromSysId(MOCK_CONTENT_ID_1)).thenReturn(MOCK_PROVIDER_NAME);

			Map<String, Object> typeDef = mockTypeDef();
			Mockito.when(tested.providerService.findContentType(MOCK_PROVIDER_NAME)).thenReturn(
					ProviderServiceTest.createProviderContentTypeInfo(typeDef));

			GetResponse grMock = Mockito.mock(GetResponse.class);
			Mockito.when(grMock.isExists()).thenReturn(false);
			Mockito.when(tested.searchClientService.performGet(MOCK_INDEX_NAME, MOCK_TYPE_NAME, MOCK_CONTENT_ID_1))
					.thenReturn(grMock);

			Map<String, Object> content = new HashMap<>();
			content.put(CustomTagRestService.DATA_FIELD_TAGGING, "label");
			TestUtils.assertResponseStatus(tested.deleteTag(MOCK_CONTENT_ID_1, content), Status.NOT_FOUND);

			Mockito.verify(tested.searchClientService).performGet(MOCK_INDEX_NAME, MOCK_TYPE_NAME, MOCK_CONTENT_ID_1);
		}

		// case - customTag OK with document upgrade
		{
			CustomTagRestService tested = getTested();

			Mockito.when(tested.providerService.parseTypeNameFromSysId(MOCK_CONTENT_ID_1)).thenReturn(MOCK_PROVIDER_NAME);
			Mockito.when(tested.customTagPersistenceService.createTag(Mockito.any(Tag.class))).thenReturn(true);

			Map<String, Object> typeDef = mockTypeDef();
			Mockito.when(tested.providerService.findContentType(MOCK_PROVIDER_NAME)).thenReturn(
					ProviderServiceTest.createProviderContentTypeInfo(typeDef));

			GetResponse grMock = Mockito.mock(GetResponse.class);
			Mockito.when(grMock.isExists()).thenReturn(true);
			Mockito.when(tested.searchClientService.performGet(MOCK_INDEX_NAME, MOCK_TYPE_NAME, MOCK_CONTENT_ID_1))
					.thenReturn(grMock);

			Map<String, Object> requestContent = new HashMap<>();
			requestContent.put(CustomTagRestService.DATA_FIELD_TAGGING, "label");
			TestUtils.assertResponseStatus(tested.deleteTag(MOCK_CONTENT_ID_1, requestContent), Status.OK);

			// verify service call
			Mockito.verify(tested.searchClientService).performGet(MOCK_INDEX_NAME, MOCK_TYPE_NAME, MOCK_CONTENT_ID_1);

			// verify jpa call
			Mockito.verify(tested.customTagPersistenceService).deleteTag(MOCK_CONTENT_ID_1, "label");
		}
	}

	@Test(expected = RequiredFieldException.class)
	public void deleteTagsForContent_invalidParam_contentSysId_1() {
		CustomTagRestService tested = getTested();
		tested.deleteTagsForContent(null);
	}

	@Test(expected = RequiredFieldException.class)
	public void deleteTagsForContent_invalidParam_contentSysId_2() {
		CustomTagRestService tested = getTested();
		tested.deleteTagsForContent(" ");
	}

	@Test
	public void deleteTagsForContent_invalidParam_contentSysId_3() {
		CustomTagRestService tested = getTested();

		// invalid format of contentSysId checked over providerService
		Mockito.when(tested.providerService.parseTypeNameFromSysId("aa")).thenThrow(new IllegalArgumentException());
		TestUtils.assertResponseStatus(tested.deleteTagsForContent("aa"), Status.BAD_REQUEST);
	}

	@Test
	public void deleteTagsForContent() throws SearchIndexMissingException {
		// case - unknown type
		{
			CustomTagRestService tested = getTested();
			Mockito.when(tested.providerService.parseTypeNameFromSysId(MOCK_CONTENT_ID_1)).thenReturn(MOCK_PROVIDER_NAME);
			Mockito.when(tested.providerService.findContentType(MOCK_PROVIDER_NAME)).thenReturn(null);

			TestUtils.assertResponseStatus(tested.deleteTagsForContent(MOCK_CONTENT_ID_1), Status.NOT_FOUND);

			Mockito.verify(tested.providerService).findContentType(MOCK_PROVIDER_NAME);
		}

		// case - non existing document
		{
			CustomTagRestService tested = getTested();

			Mockito.when(tested.providerService.parseTypeNameFromSysId(MOCK_CONTENT_ID_1)).thenReturn(MOCK_PROVIDER_NAME);

			Map<String, Object> typeDef = mockTypeDef();
			Mockito.when(tested.providerService.findContentType(MOCK_PROVIDER_NAME)).thenReturn(
					ProviderServiceTest.createProviderContentTypeInfo(typeDef));

			GetResponse grMock = Mockito.mock(GetResponse.class);
			Mockito.when(grMock.isExists()).thenReturn(false);
			Mockito.when(tested.searchClientService.performGet(MOCK_INDEX_NAME, MOCK_TYPE_NAME, MOCK_CONTENT_ID_1))
					.thenReturn(grMock);

			TestUtils.assertResponseStatus(tested.deleteTagsForContent(MOCK_CONTENT_ID_1), Status.NOT_FOUND);

			Mockito.verify(tested.searchClientService).performGet(MOCK_INDEX_NAME, MOCK_TYPE_NAME, MOCK_CONTENT_ID_1);
		}

		// case - customTag OK with document upgrade
		{
			CustomTagRestService tested = getTested();

			Mockito.when(tested.providerService.parseTypeNameFromSysId(MOCK_CONTENT_ID_1)).thenReturn(MOCK_PROVIDER_NAME);

			Map<String, Object> typeDef = mockTypeDef();
			Mockito.when(tested.providerService.findContentType(MOCK_PROVIDER_NAME)).thenReturn(
					ProviderServiceTest.createProviderContentTypeInfo(typeDef));

			GetResponse grMock = Mockito.mock(GetResponse.class);
			Mockito.when(grMock.isExists()).thenReturn(true);
			Mockito.when(tested.searchClientService.performGet(MOCK_INDEX_NAME, MOCK_TYPE_NAME, MOCK_CONTENT_ID_1))
					.thenReturn(grMock);

			TestUtils.assertResponseStatus(tested.deleteTagsForContent(MOCK_CONTENT_ID_1), Status.OK);

			// verify service call
			Mockito.verify(tested.searchClientService).performGet(MOCK_INDEX_NAME, MOCK_TYPE_NAME, MOCK_CONTENT_ID_1);

			// verify jpa call
			Mockito.verify(tested.customTagPersistenceService).deleteTagsForContent(MOCK_CONTENT_ID_1);
		}
	}

	protected Map<String, Object> mockTypeDef() {
		Map<String, Object> typeDef = new HashMap<String, Object>();
		Map<String, Object> indexElement = new HashMap<String, Object>();
		typeDef.put(ProviderService.INDEX, indexElement);
		indexElement.put(ProviderService.NAME, MOCK_INDEX_NAME);
		indexElement.put(ProviderService.TYPE, MOCK_TYPE_NAME);
		return typeDef;
	}

	protected CustomTagRestService getTested() {
		CustomTagRestService tested = new CustomTagRestService();
		tested.log = Logger.getLogger("testlogger");
		tested.customTagPersistenceService = Mockito.mock(CustomTagPersistenceService.class);
		tested.customTagService = Mockito.mock(CustomTagService.class);
		tested.providerService = Mockito.mock(ProviderService.class);
		tested.searchClientService = Mockito.mock(SearchClientService.class);
		tested.authenticationUtilService = Mockito.mock(AuthenticationUtilService.class);
		Mockito.when(tested.authenticationUtilService.getAuthenticatedContributor(Mockito.anyBoolean())).thenReturn(
				MOCK_CONTRIB_ID);
		// Pretend authenticated
		Mockito.when(
				tested.authenticationUtilService.isUserInAnyOfRoles(Mockito.eq(true), Mockito.eq(Role.TAGS_MANAGER),
						Mockito.anyString())).thenReturn(true);
		Mockito.when(tested.authenticationUtilService.isUserInAnyOfRoles(true, Role.TAGS_MANAGER)).thenReturn(true);

		return tested;
	}

	private void prepareContentId(CustomTagRestService tested) {

		Mockito.when(tested.providerService.parseTypeNameFromSysId(MOCK_CONTENT_ID_1)).thenReturn(MOCK_PROVIDER_NAME);

		Map<String, Object> typeDef = mockTypeDef();
		Mockito.when(tested.providerService.findContentType(MOCK_PROVIDER_NAME)).thenReturn(
				ProviderServiceTest.createProviderContentTypeInfo(typeDef));
	}

}
