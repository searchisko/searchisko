/*
 * JBoss, Home of Professional Open Source
 * Copyright 2013 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 */
package org.searchisko.api.service;

import java.util.HashMap;
import java.util.Map;

import junit.framework.Assert;

import org.junit.Test;
import org.mockito.Mockito;
import org.searchisko.api.ContentObjectFields;
import org.searchisko.persistence.service.RatingPersistenceService;
import org.searchisko.persistence.service.RatingPersistenceService.RatingStats;

/**
 * Unit test for {@link ContentEnhancementsService}
 * 
 * @author Vlastimil Elias (velias at redhat dot com)
 */
public class ContentEnhancementsServiceTest {

	private static final String CID = "CID";

	@Test
	public void handleContentRatingFields() {
		RatingPersistenceService ratingPersistenceServiceMock = Mockito.mock(RatingPersistenceService.class);

		ContentEnhancementsService tested = new ContentEnhancementsService();
		tested.ratingPersistenceService = ratingPersistenceServiceMock;

		// case - rating not available yet and fields not present in content before call
		{
			Mockito.when(ratingPersistenceServiceMock.countRatingStats(CID)).thenReturn(null);
			Map<String, Object> content = new HashMap<>();
			tested.handleContentRatingFields(content, CID);
			Assert.assertFalse(content.containsKey(ContentObjectFields.SYS_RATING_AVG));
			Assert.assertFalse(content.containsKey(ContentObjectFields.SYS_RATING_NUM));
			Mockito.verify(ratingPersistenceServiceMock).countRatingStats(CID);
		}

		// case - rating not available yet but fields present in content before call
		{
			Mockito.reset(ratingPersistenceServiceMock);
			Mockito.when(ratingPersistenceServiceMock.countRatingStats(CID)).thenReturn(null);
			Map<String, Object> content = new HashMap<>();
			content.put(ContentObjectFields.SYS_RATING_AVG, "a");
			content.put(ContentObjectFields.SYS_RATING_NUM, "b");
			tested.handleContentRatingFields(content, CID);
			Assert.assertFalse(content.containsKey(ContentObjectFields.SYS_RATING_AVG));
			Assert.assertFalse(content.containsKey(ContentObjectFields.SYS_RATING_NUM));
			Mockito.verify(ratingPersistenceServiceMock).countRatingStats(CID);
		}

		// case - rating available and fields not present in content before call
		{
			Mockito.reset(ratingPersistenceServiceMock);
			Mockito.when(ratingPersistenceServiceMock.countRatingStats(CID)).thenReturn(new RatingStats(CID, 2.5, 25l));
			Map<String, Object> content = new HashMap<>();
			tested.handleContentRatingFields(content, CID);
			Assert.assertEquals(2.5, (Double) content.get(ContentObjectFields.SYS_RATING_AVG), 0.01);
			Assert.assertEquals(new Long(25), content.get(ContentObjectFields.SYS_RATING_NUM));
			Mockito.verify(ratingPersistenceServiceMock).countRatingStats(CID);
		}

		// case - rating available and fields present in content before call
		{
			Mockito.reset(ratingPersistenceServiceMock);
			Mockito.when(ratingPersistenceServiceMock.countRatingStats(CID)).thenReturn(new RatingStats(CID, 2.5, 25l));
			Map<String, Object> content = new HashMap<>();
			content.put(ContentObjectFields.SYS_RATING_AVG, "a");
			content.put(ContentObjectFields.SYS_RATING_NUM, "b");
			tested.handleContentRatingFields(content, CID);
			Assert.assertEquals(2.5, (Double) content.get(ContentObjectFields.SYS_RATING_AVG), 0.01);
			Assert.assertEquals(new Long(25), content.get(ContentObjectFields.SYS_RATING_NUM));
			Mockito.verify(ratingPersistenceServiceMock).countRatingStats(CID);
		}
	}

}
