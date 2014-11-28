/*
 * JBoss, Home of Professional Open Source
 * Copyright 2013 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 */
package org.searchisko.api.rest;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.core.UriInfo;

import org.elasticsearch.action.get.GetResponse;
import org.jboss.resteasy.specimpl.MultivaluedMapImpl;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;
import org.searchisko.api.ContentObjectFields;
import org.searchisko.api.rest.exception.NotAuthorizedException;
import org.searchisko.api.rest.exception.RequiredFieldException;
import org.searchisko.api.service.AuthenticationUtilService;
import org.searchisko.api.service.ProviderService;
import org.searchisko.api.service.ProviderServiceTest;
import org.searchisko.api.service.SearchClientService;
import org.searchisko.api.service.SearchIndexMissingException;
import org.searchisko.api.testtools.TestUtils;
import org.searchisko.persistence.jpa.model.Rating;
import org.searchisko.persistence.service.RatingPersistenceService;
import org.searchisko.persistence.service.RatingPersistenceService.RatingStats;

/**
 * Unit test for {@link RatingRestService}
 * 
 * @author Vlastimil Elias (velias at redhat dot com)
 */
public class RatingRestServiceTest {

	/**
	 * 
	 */
	private static final String MOCK_ROLE = "role1";
	private static final String MOCK_TYPE_NAME = "mytype";
	private static final String MOCK_INDEX_NAME = "myindex";
	private static final String MOCK_CONTENT_ID_1 = "jb-45";
	private static final String MOCK_CONTENT_ID_2 = "jb-425";
	private static final String MOCK_CONTENT_ID_3 = "jb-456";
	private static final String MOCK_CONTRIB_ID = "test <test@test.org>";

	@Test(expected = RequiredFieldException.class)
	public void getRating_invalidParam_1() {
		RatingRestService tested = getTested();
		tested.getRating(null);
	}

	@Test(expected = RequiredFieldException.class)
	public void getRating_invalidParam_2() {
		RatingRestService tested = getTested();
		tested.getRating(" ");
	}

	@Test
	public void getRating() {
		RatingRestService tested = getTested();

		// case - no rating in DB, null returned from service
		{
			Mockito.when(
					tested.ratingPersistenceService.getRatings(Mockito.eq(MOCK_CONTRIB_ID), Mockito.eq(MOCK_CONTENT_ID_1)))
					.thenReturn(null);
			TestUtils.assertResponseStatus(tested.getRating(MOCK_CONTENT_ID_1), Status.NOT_FOUND);
			Mockito.verify(tested.ratingPersistenceService).getRatings(Mockito.eq(MOCK_CONTRIB_ID),
					Mockito.eq(MOCK_CONTENT_ID_1));
		}

		// case - no rating in DB, empty list returned from service
		{
			Mockito.reset(tested.ratingPersistenceService);
			Mockito.when(
					tested.ratingPersistenceService.getRatings(Mockito.eq(MOCK_CONTRIB_ID), Mockito.eq(MOCK_CONTENT_ID_1)))
					.thenReturn(new ArrayList<Rating>());
			TestUtils.assertResponseStatus(tested.getRating(MOCK_CONTENT_ID_1), Status.NOT_FOUND);
			Mockito.verify(tested.ratingPersistenceService).getRatings(Mockito.eq(MOCK_CONTRIB_ID),
					Mockito.eq(MOCK_CONTENT_ID_1));
		}

		// case - rating in DB so returned
		{
			List<Rating> ret = new ArrayList<Rating>();
			ret.add(new Rating(MOCK_CONTENT_ID_1, MOCK_CONTRIB_ID, 3, null));
			Mockito.reset(tested.ratingPersistenceService);
			Mockito.when(
					tested.ratingPersistenceService.getRatings(Mockito.eq(MOCK_CONTRIB_ID), Mockito.eq(MOCK_CONTENT_ID_1)))
					.thenReturn(ret);
			@SuppressWarnings("unchecked")
			Map<String, Object> aret = (Map<String, Object>) tested.getRating(MOCK_CONTENT_ID_1);
			assertRatingJSON(aret, 3);
		}
	}

	protected void assertRatingJSON(Map<String, Object> ratingJsonMap, int ratingValue) {
		Assert.assertNotNull(ratingJsonMap);
		Assert.assertEquals(1, ratingJsonMap.size());
		Assert.assertEquals(ratingValue, ratingJsonMap.get(RatingRestService.DATA_FIELD_RATING));
	}

	@SuppressWarnings("unchecked")
	@Test
	public void getRatings() {
		RatingRestService tested = getTested();

		// case - no input params provided
		{
			Map<String, Object> aret = tested.getRatings(null);
			Assert.assertNotNull(aret);
			Assert.assertEquals(0, aret.size());
			Mockito.verifyZeroInteractions(tested.ratingPersistenceService);
		}

		// case - no rating in DB, null returned from service
		{
			Mockito.when(
					tested.ratingPersistenceService.getRatings(Mockito.eq(MOCK_CONTRIB_ID), Mockito.eq(MOCK_CONTENT_ID_1)))
					.thenReturn(null);
			UriInfo uriInfoMock = Mockito.mock(UriInfo.class);
			MultivaluedMap<String, String> qp = new MultivaluedMapImpl<String, String>();
			qp.add(RatingRestService.QUERY_PARAM_ID, MOCK_CONTENT_ID_1);
			Mockito.when(uriInfoMock.getQueryParameters()).thenReturn(qp);
			Map<String, Object> aret = tested.getRatings(uriInfoMock);
			Assert.assertNotNull(aret);
			Assert.assertEquals(0, aret.size());
			Mockito.verify(tested.ratingPersistenceService).getRatings(Mockito.eq(MOCK_CONTRIB_ID),
					Mockito.eq(MOCK_CONTENT_ID_1));
		}

		// case - no rating in DB, empty list returned from service
		{
			Mockito.reset(tested.ratingPersistenceService);
			Mockito.when(
					tested.ratingPersistenceService.getRatings(Mockito.eq(MOCK_CONTRIB_ID), Mockito.eq(MOCK_CONTENT_ID_1),
							Mockito.eq(MOCK_CONTENT_ID_2))).thenReturn(new ArrayList<Rating>());
			UriInfo uriInfoMock = Mockito.mock(UriInfo.class);
			MultivaluedMap<String, String> qp = new MultivaluedMapImpl<String, String>();
			qp.add(RatingRestService.QUERY_PARAM_ID, MOCK_CONTENT_ID_1);
			qp.add(RatingRestService.QUERY_PARAM_ID, MOCK_CONTENT_ID_2);
			Mockito.when(uriInfoMock.getQueryParameters()).thenReturn(qp);
			Map<String, Object> aret = tested.getRatings(uriInfoMock);
			Assert.assertNotNull(aret);
			Assert.assertEquals(0, aret.size());
			Mockito.verify(tested.ratingPersistenceService).getRatings(Mockito.eq(MOCK_CONTRIB_ID),
					Mockito.eq(MOCK_CONTENT_ID_1), Mockito.eq(MOCK_CONTENT_ID_2));
		}

		// case - rating in DB so returned
		{
			List<Rating> ret = new ArrayList<Rating>();
			ret.add(new Rating(MOCK_CONTENT_ID_1, MOCK_CONTRIB_ID, 3, null));
			ret.add(new Rating(MOCK_CONTENT_ID_2, MOCK_CONTRIB_ID, 5, null));
			Mockito.reset(tested.ratingPersistenceService);
			Mockito.when(
					tested.ratingPersistenceService.getRatings(Mockito.eq(MOCK_CONTRIB_ID), Mockito.eq(MOCK_CONTENT_ID_1),
							Mockito.eq(MOCK_CONTENT_ID_2), Mockito.eq(MOCK_CONTENT_ID_3))).thenReturn(ret);
			UriInfo uriInfoMock = Mockito.mock(UriInfo.class);
			MultivaluedMap<String, String> qp = new MultivaluedMapImpl<String, String>();
			qp.add(RatingRestService.QUERY_PARAM_ID, MOCK_CONTENT_ID_1);
			qp.add(RatingRestService.QUERY_PARAM_ID, MOCK_CONTENT_ID_2);
			qp.add(RatingRestService.QUERY_PARAM_ID, MOCK_CONTENT_ID_3);
			Mockito.when(uriInfoMock.getQueryParameters()).thenReturn(qp);
			Map<String, Object> aret = tested.getRatings(uriInfoMock);
			Mockito.verify(tested.ratingPersistenceService).getRatings(Mockito.eq(MOCK_CONTRIB_ID),
					Mockito.eq(MOCK_CONTENT_ID_1), Mockito.eq(MOCK_CONTENT_ID_2), Mockito.eq(MOCK_CONTENT_ID_3));
			Assert.assertNotNull(aret);
			Assert.assertEquals(2, aret.size());
			assertRatingJSON((Map<String, Object>) aret.get(MOCK_CONTENT_ID_1), 3);
			assertRatingJSON((Map<String, Object>) aret.get(MOCK_CONTENT_ID_2), 5);
		}
	}

	@Test(expected = RequiredFieldException.class)
	public void postRating_invalidParam_contentSysId_1() {
		RatingRestService tested = getTested();
		Map<String, Object> content = new HashMap<>();
		content.put(RatingRestService.DATA_FIELD_RATING, "1");
		tested.postRating(null, content);
	}

	@Test(expected = RequiredFieldException.class)
	public void postRating_invalidParam_contentSysId_2() {
		RatingRestService tested = getTested();
		Map<String, Object> content = new HashMap<>();
		content.put(RatingRestService.DATA_FIELD_RATING, "1");
		tested.postRating(" ", content);
	}

	@Test
	public void postRating_invalidParam_contentSysId_3() {
		RatingRestService tested = getTested();
		Map<String, Object> content = new HashMap<>();
		content.put(RatingRestService.DATA_FIELD_RATING, "1");

		// invalid format of contentSysId checked over providerService
		Mockito.when(tested.providerService.parseTypeNameFromSysId("aa")).thenThrow(new IllegalArgumentException());
		TestUtils.assertResponseStatus(tested.postRating("aa", content), Status.BAD_REQUEST);
	}

	@Test(expected = RequiredFieldException.class)
	public void postRating_invalidParam_ratingValue_1() {
		RatingRestService tested = getTested();
		tested.postRating(MOCK_CONTENT_ID_1, null);
	}

	@Test(expected = RequiredFieldException.class)
	public void postRating_invalidParam_ratingValue_2() {
		RatingRestService tested = getTested();
		Map<String, Object> content = new HashMap<>();
		tested.postRating(MOCK_CONTENT_ID_1, content);
	}

	@Test
	public void postRating_invalidParam_ratingValue_3() {
		RatingRestService tested = getTested();
		Map<String, Object> content = new HashMap<>();
		content.put(RatingRestService.DATA_FIELD_RATING, "aa");
		TestUtils.assertResponseStatus(tested.postRating(MOCK_CONTENT_ID_1, content), Status.BAD_REQUEST);

		content.put(RatingRestService.DATA_FIELD_RATING, new Object());
		TestUtils.assertResponseStatus(tested.postRating(MOCK_CONTENT_ID_1, content), Status.BAD_REQUEST);

		content.put(RatingRestService.DATA_FIELD_RATING, "0");
		TestUtils.assertResponseStatus(tested.postRating(MOCK_CONTENT_ID_1, content), Status.BAD_REQUEST);

		content.put(RatingRestService.DATA_FIELD_RATING, new Integer(6));
		TestUtils.assertResponseStatus(tested.postRating(MOCK_CONTENT_ID_1, content), Status.BAD_REQUEST);
	}

	private static final String MOCK_PROVIDER_NAME = "jboss";

	@Test
	public void postRating() throws SearchIndexMissingException {
		// case - unknown type
		{
			RatingRestService tested = getTested();
			Mockito.when(tested.providerService.parseTypeNameFromSysId(MOCK_CONTENT_ID_1)).thenReturn(MOCK_PROVIDER_NAME);
			Mockito.when(tested.providerService.findContentType(MOCK_PROVIDER_NAME)).thenReturn(null);

			Map<String, Object> content = new HashMap<>();
			content.put(RatingRestService.DATA_FIELD_RATING, "1");
			TestUtils.assertResponseStatus(tested.postRating(MOCK_CONTENT_ID_1, content), Status.NOT_FOUND);

			Mockito.verify(tested.providerService).findContentType(MOCK_PROVIDER_NAME);

			Mockito.verify(tested.authenticationUtilService).getAuthenticatedContributor(true);
			Mockito.verifyNoMoreInteractions(tested.authenticationUtilService);
		}

		// case - non existing document
		{
			RatingRestService tested = getTested();

			Mockito.when(tested.providerService.parseTypeNameFromSysId(MOCK_CONTENT_ID_1)).thenReturn(MOCK_PROVIDER_NAME);

			Map<String, Object> typeDef = mockTypeDef();
			Mockito.when(tested.providerService.findContentType(MOCK_PROVIDER_NAME)).thenReturn(
					ProviderServiceTest.createProviderContentTypeInfo(typeDef));

			GetResponse grMock = Mockito.mock(GetResponse.class);
			Mockito.when(grMock.isExists()).thenReturn(false);
			Mockito.when(tested.searchClientService.performGet(MOCK_INDEX_NAME, MOCK_TYPE_NAME, MOCK_CONTENT_ID_1))
					.thenReturn(grMock);

			Map<String, Object> content = new HashMap<>();
			content.put(RatingRestService.DATA_FIELD_RATING, "1");
			TestUtils.assertResponseStatus(tested.postRating(MOCK_CONTENT_ID_1, content), Status.NOT_FOUND);

			Mockito.verify(tested.searchClientService).performGet(MOCK_INDEX_NAME, MOCK_TYPE_NAME, MOCK_CONTENT_ID_1);

			Mockito.verify(tested.authenticationUtilService).getAuthenticatedContributor(true);
			Mockito.verifyNoMoreInteractions(tested.authenticationUtilService);
		}

		// case - rating OK with document upgrade
		{
			RatingRestService tested = getTested();

			Mockito.when(tested.providerService.parseTypeNameFromSysId(MOCK_CONTENT_ID_1)).thenReturn(MOCK_PROVIDER_NAME);

			Map<String, Object> typeDef = mockTypeDef();
			Mockito.when(tested.providerService.findContentType(MOCK_PROVIDER_NAME)).thenReturn(
					ProviderServiceTest.createProviderContentTypeInfo(typeDef));

			GetResponse grMock = Mockito.mock(GetResponse.class);
			Mockito.when(grMock.isExists()).thenReturn(true);
			Map<String, Object> indexDocumentContent = new HashMap<>();
			Mockito.when(grMock.getSource()).thenReturn(indexDocumentContent);
			Mockito.when(tested.searchClientService.performGet(MOCK_INDEX_NAME, MOCK_TYPE_NAME, MOCK_CONTENT_ID_1))
					.thenReturn(grMock);

			Mockito.when(tested.ratingPersistenceService.countRatingStats(MOCK_CONTENT_ID_1)).thenReturn(
					new RatingStats(MOCK_CONTENT_ID_1, 3, 20));

			Map<String, Object> requestContent = new HashMap<>();
			requestContent.put(RatingRestService.DATA_FIELD_RATING, "1");
			@SuppressWarnings("unchecked")
			Map<String, Object> responseContent = (Map<String, Object>) tested.postRating(MOCK_CONTENT_ID_1, requestContent);

			// assert response content
			Assert.assertEquals(3, responseContent.size());
			Assert.assertEquals(MOCK_CONTENT_ID_1, responseContent.get(ContentObjectFields.SYS_ID));
			Assert.assertEquals(new Double(3), responseContent.get(ContentObjectFields.SYS_RATING_AVG));
			Assert.assertEquals(new Long(20), responseContent.get(ContentObjectFields.SYS_RATING_NUM));

			// verify service calls
			Mockito.verify(tested.searchClientService).performGet(MOCK_INDEX_NAME, MOCK_TYPE_NAME, MOCK_CONTENT_ID_1);
			Mockito.verify(tested.ratingPersistenceService).rate(MOCK_CONTRIB_ID, MOCK_CONTENT_ID_1, 1);
			Mockito.verify(tested.searchClientService).performPutAsync(MOCK_INDEX_NAME, MOCK_TYPE_NAME, MOCK_CONTENT_ID_1,
					indexDocumentContent);

			// assert rating stats added to the search index
			Assert.assertEquals(new Double(3), indexDocumentContent.get(ContentObjectFields.SYS_RATING_AVG));
			Assert.assertEquals(new Long(20), indexDocumentContent.get(ContentObjectFields.SYS_RATING_NUM));

			Mockito.verify(tested.authenticationUtilService).getAuthenticatedContributor(true);
			Mockito.verifyNoMoreInteractions(tested.authenticationUtilService);
		}
	}

	@Test
	public void postRating_contentsecurity_typeLevel() throws SearchIndexMissingException {

		// case - #191 - rating OK with user in role
		{
			RatingRestService tested = getTested();

			Mockito.when(tested.providerService.parseTypeNameFromSysId(MOCK_CONTENT_ID_1)).thenReturn(MOCK_PROVIDER_NAME);

			Map<String, Object> typeDef = mockTypeDef();
			typeDef.put(ProviderService.SYS_VISIBLE_FOR_ROLES, MOCK_ROLE);

			Mockito.when(tested.providerService.findContentType(MOCK_PROVIDER_NAME)).thenReturn(
					ProviderServiceTest.createProviderContentTypeInfo(typeDef));

			List<String> expectedRoles = new ArrayList<>();
			expectedRoles.add(MOCK_ROLE);
			Mockito.when(tested.authenticationUtilService.isUserInAnyOfRoles(true, expectedRoles)).thenReturn(true);

			GetResponse grMock = Mockito.mock(GetResponse.class);
			Mockito.when(grMock.isExists()).thenReturn(true);
			Map<String, Object> indexDocumentContent = new HashMap<>();
			Mockito.when(grMock.getSource()).thenReturn(indexDocumentContent);
			Mockito.when(tested.searchClientService.performGet(MOCK_INDEX_NAME, MOCK_TYPE_NAME, MOCK_CONTENT_ID_1))
					.thenReturn(grMock);

			Mockito.when(tested.ratingPersistenceService.countRatingStats(MOCK_CONTENT_ID_1)).thenReturn(
					new RatingStats(MOCK_CONTENT_ID_1, 3, 20));

			Map<String, Object> requestContent = new HashMap<>();
			requestContent.put(RatingRestService.DATA_FIELD_RATING, "1");

			tested.postRating(MOCK_CONTENT_ID_1, requestContent);

			// verify service calls
			Mockito.verify(tested.searchClientService).performGet(MOCK_INDEX_NAME, MOCK_TYPE_NAME, MOCK_CONTENT_ID_1);
			Mockito.verify(tested.ratingPersistenceService).rate(MOCK_CONTRIB_ID, MOCK_CONTENT_ID_1, 1);
			Mockito.verify(tested.searchClientService).performPutAsync(MOCK_INDEX_NAME, MOCK_TYPE_NAME, MOCK_CONTENT_ID_1,
					indexDocumentContent);

			Mockito.verify(tested.authenticationUtilService).getAuthenticatedContributor(true);
			Mockito.verify(tested.authenticationUtilService).isUserInAnyOfRoles(true, expectedRoles);
			Mockito.verifyNoMoreInteractions(tested.authenticationUtilService);
		}

		// case - #191 - rating not performed with user not in role
		{
			RatingRestService tested = getTested();

			Mockito.when(tested.providerService.parseTypeNameFromSysId(MOCK_CONTENT_ID_1)).thenReturn(MOCK_PROVIDER_NAME);

			Map<String, Object> typeDef = mockTypeDef();
			typeDef.put(ProviderService.SYS_VISIBLE_FOR_ROLES, MOCK_ROLE);

			Mockito.when(tested.providerService.findContentType(MOCK_PROVIDER_NAME)).thenReturn(
					ProviderServiceTest.createProviderContentTypeInfo(typeDef));

			List<String> expectedRoles = new ArrayList<>();
			expectedRoles.add(MOCK_ROLE);
			Mockito.when(tested.authenticationUtilService.isUserInAnyOfRoles(true, expectedRoles)).thenReturn(false);

			GetResponse grMock = Mockito.mock(GetResponse.class);
			Mockito.when(grMock.isExists()).thenReturn(true);
			Map<String, Object> indexDocumentContent = new HashMap<>();
			Mockito.when(grMock.getSource()).thenReturn(indexDocumentContent);
			Mockito.when(tested.searchClientService.performGet(MOCK_INDEX_NAME, MOCK_TYPE_NAME, MOCK_CONTENT_ID_1))
					.thenReturn(grMock);

			Mockito.when(tested.ratingPersistenceService.countRatingStats(MOCK_CONTENT_ID_1)).thenReturn(
					new RatingStats(MOCK_CONTENT_ID_1, 3, 20));

			Map<String, Object> requestContent = new HashMap<>();
			requestContent.put(RatingRestService.DATA_FIELD_RATING, "1");

			try {
				tested.postRating(MOCK_CONTENT_ID_1, requestContent);
				Assert.fail("NotAuthorizedException expected");
			} catch (NotAuthorizedException e) {

				Mockito.verify(tested.authenticationUtilService).getAuthenticatedContributor(true);
				Mockito.verify(tested.authenticationUtilService).isUserInAnyOfRoles(true, expectedRoles);
				Mockito.verifyNoMoreInteractions(tested.authenticationUtilService, tested.searchClientService);
			}
		}
	}

	@Test
	public void postRating_contentsecurity_documentLevel() throws SearchIndexMissingException {

		// case - #191 - rating OK with user in role
		{
			RatingRestService tested = getTested();

			Mockito.when(tested.providerService.parseTypeNameFromSysId(MOCK_CONTENT_ID_1)).thenReturn(MOCK_PROVIDER_NAME);

			Map<String, Object> typeDef = mockTypeDef();
			Mockito.when(tested.providerService.findContentType(MOCK_PROVIDER_NAME)).thenReturn(
					ProviderServiceTest.createProviderContentTypeInfo(typeDef));

			List<String> expectedRoles = new ArrayList<>();
			expectedRoles.add(MOCK_ROLE);
			Mockito.when(tested.authenticationUtilService.isUserInAnyOfRoles(true, expectedRoles)).thenReturn(true);

			GetResponse grMock = Mockito.mock(GetResponse.class);
			Mockito.when(grMock.isExists()).thenReturn(true);
			Map<String, Object> indexDocumentContent = new HashMap<>();
			indexDocumentContent.put(ContentObjectFields.SYS_VISIBLE_FOR_ROLES, MOCK_ROLE);
			Mockito.when(grMock.getSource()).thenReturn(indexDocumentContent);
			Mockito.when(tested.searchClientService.performGet(MOCK_INDEX_NAME, MOCK_TYPE_NAME, MOCK_CONTENT_ID_1))
					.thenReturn(grMock);

			Mockito.when(tested.ratingPersistenceService.countRatingStats(MOCK_CONTENT_ID_1)).thenReturn(
					new RatingStats(MOCK_CONTENT_ID_1, 3, 20));

			Map<String, Object> requestContent = new HashMap<>();
			requestContent.put(RatingRestService.DATA_FIELD_RATING, "1");

			tested.postRating(MOCK_CONTENT_ID_1, requestContent);

			// verify service calls
			Mockito.verify(tested.searchClientService).performGet(MOCK_INDEX_NAME, MOCK_TYPE_NAME, MOCK_CONTENT_ID_1);
			Mockito.verify(tested.ratingPersistenceService).rate(MOCK_CONTRIB_ID, MOCK_CONTENT_ID_1, 1);
			Mockito.verify(tested.searchClientService).performPutAsync(MOCK_INDEX_NAME, MOCK_TYPE_NAME, MOCK_CONTENT_ID_1,
					indexDocumentContent);

			Mockito.verify(tested.authenticationUtilService).getAuthenticatedContributor(true);
			Mockito.verify(tested.authenticationUtilService).isUserInAnyOfRoles(true, expectedRoles);
			Mockito.verifyNoMoreInteractions(tested.authenticationUtilService);
		}

		// case - #191 - rating not performed with user not in role
		{
			RatingRestService tested = getTested();

			Mockito.when(tested.providerService.parseTypeNameFromSysId(MOCK_CONTENT_ID_1)).thenReturn(MOCK_PROVIDER_NAME);

			Map<String, Object> typeDef = mockTypeDef();

			Mockito.when(tested.providerService.findContentType(MOCK_PROVIDER_NAME)).thenReturn(
					ProviderServiceTest.createProviderContentTypeInfo(typeDef));

			List<String> expectedRoles = new ArrayList<>();
			expectedRoles.add(MOCK_ROLE);
			Mockito.when(tested.authenticationUtilService.isUserInAnyOfRoles(true, expectedRoles)).thenReturn(false);

			GetResponse grMock = Mockito.mock(GetResponse.class);
			Mockito.when(grMock.isExists()).thenReturn(true);
			Map<String, Object> indexDocumentContent = new HashMap<>();
			indexDocumentContent.put(ContentObjectFields.SYS_VISIBLE_FOR_ROLES, MOCK_ROLE);
			Mockito.when(grMock.getSource()).thenReturn(indexDocumentContent);
			Mockito.when(tested.searchClientService.performGet(MOCK_INDEX_NAME, MOCK_TYPE_NAME, MOCK_CONTENT_ID_1))
					.thenReturn(grMock);

			Mockito.when(tested.ratingPersistenceService.countRatingStats(MOCK_CONTENT_ID_1)).thenReturn(
					new RatingStats(MOCK_CONTENT_ID_1, 3, 20));

			Map<String, Object> requestContent = new HashMap<>();
			requestContent.put(RatingRestService.DATA_FIELD_RATING, "1");

			try {
				tested.postRating(MOCK_CONTENT_ID_1, requestContent);
				Assert.fail("NotAuthorizedException expected");
			} catch (NotAuthorizedException e) {

				Mockito.verify(tested.searchClientService).performGet(MOCK_INDEX_NAME, MOCK_TYPE_NAME, MOCK_CONTENT_ID_1);

				Mockito.verify(tested.authenticationUtilService).getAuthenticatedContributor(true);
				Mockito.verify(tested.authenticationUtilService).isUserInAnyOfRoles(true, expectedRoles);
				Mockito.verifyNoMoreInteractions(tested.authenticationUtilService, tested.searchClientService);
			}
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

	protected RatingRestService getTested() {
		RatingRestService tested = new RatingRestService();
		tested.log = Logger.getLogger("testlogger");
		tested.ratingPersistenceService = Mockito.mock(RatingPersistenceService.class);
		tested.providerService = Mockito.mock(ProviderService.class);
		tested.searchClientService = Mockito.mock(SearchClientService.class);
		tested.authenticationUtilService = Mockito.mock(AuthenticationUtilService.class);
		Mockito.when(tested.authenticationUtilService.getAuthenticatedContributor(Mockito.anyBoolean())).thenReturn(
				MOCK_CONTRIB_ID);
		// Pretend authenticated
		tested.securityContext = Mockito.mock(SecurityContext.class);
		Mockito.when(tested.securityContext.isUserInRole(Mockito.anyString())).thenReturn(true);

		return tested;
	}

}
