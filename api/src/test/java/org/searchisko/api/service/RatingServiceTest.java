/*
 * JBoss, Home of Professional Open Source
 * Copyright 2014 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 */
package org.searchisko.api.service;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;
import org.searchisko.api.ContentObjectFields;
import org.searchisko.api.events.ContentBeforeIndexedEvent;
import org.searchisko.api.events.ContentDeletedEvent;
import org.searchisko.api.events.ContributorCodeChangedEvent;
import org.searchisko.api.events.ContributorDeletedEvent;
import org.searchisko.api.events.ContributorMergedEvent;
import org.searchisko.persistence.service.RatingPersistenceService;
import org.searchisko.persistence.service.RatingPersistenceService.RatingStats;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;

/**
 * Unit test for {@link RatingService}.
 * 
 * @author Vlastimil Elias (velias at redhat dot com)
 */
public class RatingServiceTest {

	@Test
	public void contributorDeletedEventHandler() {
		RatingService tested = getTested();

		// case - invalid event
		tested.contributorDeletedEventHandler(null);
		verifyZeroInteractions(tested.ratingPersistenceService);

		reset(tested.ratingPersistenceService);
		tested.contributorDeletedEventHandler(new ContributorDeletedEvent("id", null));
		verifyZeroInteractions(tested.ratingPersistenceService);

		// case - valid event
		reset(tested.ratingPersistenceService);
		tested.contributorDeletedEventHandler(new ContributorDeletedEvent("id", "code"));
		verify(tested.ratingPersistenceService).deleteRatingsForContributor("code");

	}

	@Test
	public void contentDeletedEventHandler() {
		RatingService tested = getTested();

		// case - invalid event
		tested.contentDeletedEventHandler(null);
		verifyZeroInteractions(tested.ratingPersistenceService);

		reset(tested.ratingPersistenceService);
		tested.contentDeletedEventHandler(new ContentDeletedEvent(null));
		verifyZeroInteractions(tested.ratingPersistenceService);

		// case - valid event
		reset(tested.ratingPersistenceService);
		tested.contentDeletedEventHandler(new ContentDeletedEvent("id"));
		verify(tested.ratingPersistenceService).deleteRatingsForContent("id");

	}

	@Test
	public void contributorMergedEventHandler() {
		RatingService tested = getTested();

		// case - invalid event
		tested.contributorMergedEventHandler(null);
		verifyZeroInteractions(tested.ratingPersistenceService);

		reset(tested.ratingPersistenceService);
		tested.contributorMergedEventHandler(new ContributorMergedEvent(null, null));
		verifyZeroInteractions(tested.ratingPersistenceService);

		reset(tested.ratingPersistenceService);
		tested.contributorMergedEventHandler(new ContributorMergedEvent("idFrom", null));
		verifyZeroInteractions(tested.ratingPersistenceService);

		reset(tested.ratingPersistenceService);
		tested.contributorMergedEventHandler(new ContributorMergedEvent(null, "idTo"));
		verifyZeroInteractions(tested.ratingPersistenceService);

		// case - valid event
		reset(tested.ratingPersistenceService);
		tested.contributorMergedEventHandler(new ContributorMergedEvent("idFrom", "idTo"));
		verify(tested.ratingPersistenceService).mergeRatingsForContributors("idFrom", "idTo");
	}

	@Test
	public void contributorCodeChangedEventHandler() {
		RatingService tested = getTested();

		// case - invalid event
		tested.contributorCodeChangedEventHandler(null);
		verifyZeroInteractions(tested.ratingPersistenceService);

		reset(tested.ratingPersistenceService);
		tested.contributorCodeChangedEventHandler(new ContributorCodeChangedEvent(null, null));
		verifyZeroInteractions(tested.ratingPersistenceService);

		reset(tested.ratingPersistenceService);
		tested.contributorCodeChangedEventHandler(new ContributorCodeChangedEvent("idFrom", null));
		verifyZeroInteractions(tested.ratingPersistenceService);

		reset(tested.ratingPersistenceService);
		tested.contributorCodeChangedEventHandler(new ContributorCodeChangedEvent(null, "idTo"));
		verifyZeroInteractions(tested.ratingPersistenceService);

		// case - valid event
		reset(tested.ratingPersistenceService);
		tested.contributorCodeChangedEventHandler(new ContributorCodeChangedEvent("idFrom", "idTo"));
		verify(tested.ratingPersistenceService).mergeRatingsForContributors("idFrom", "idTo");
	}

	@Test
	public void handleContentRatingFields() {
		RatingService tested = getTested();

		// case - invalid event
		tested.handleContentRatingFields(null);
		verifyZeroInteractions(tested.ratingPersistenceService);

		reset(tested.ratingPersistenceService);
		tested.handleContentRatingFields(new ContentBeforeIndexedEvent(null, null));
		verifyZeroInteractions(tested.ratingPersistenceService);

		reset(tested.ratingPersistenceService);
		tested.handleContentRatingFields(new ContentBeforeIndexedEvent("id", null));
		verifyZeroInteractions(tested.ratingPersistenceService);

		reset(tested.ratingPersistenceService);
		tested.handleContentRatingFields(new ContentBeforeIndexedEvent(null, new HashMap<String, Object>()));
		verifyZeroInteractions(tested.ratingPersistenceService);

		// case - rating found so we add/update fields in content
		{
			reset(tested.ratingPersistenceService);
			Map<String, Object> content = new HashMap<String, Object>();
			content.put(ContentObjectFields.SYS_RATING_NUM, "10");
			Mockito.when(tested.ratingPersistenceService.countRatingStats("id")).thenReturn(new RatingStats("id", 3, 5));
			tested.handleContentRatingFields(new ContentBeforeIndexedEvent("id", content));
			Assert.assertEquals(new Double(3), content.get(ContentObjectFields.SYS_RATING_AVG));
			Assert.assertEquals(new Long(5), content.get(ContentObjectFields.SYS_RATING_NUM));
		}

		// case - rating not found so we remove fields from content if present
		{
			reset(tested.ratingPersistenceService);
			Map<String, Object> content = new HashMap<String, Object>();
			content.put(ContentObjectFields.SYS_RATING_NUM, "10");
			Mockito.when(tested.ratingPersistenceService.countRatingStats("id")).thenReturn(null);
			tested.handleContentRatingFields(new ContentBeforeIndexedEvent("id", content));
			Assert.assertNull(content.get(ContentObjectFields.SYS_RATING_AVG));
			Assert.assertNull(content.get(ContentObjectFields.SYS_RATING_NUM));
		}

	}

	private RatingService getTested() {
		RatingService ret = new RatingService();
		ret.ratingPersistenceService = mock(RatingPersistenceService.class);
		ret.log = Logger.getLogger("testlogger");
		return ret;
	}

}
