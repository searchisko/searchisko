/*
 * JBoss, Home of Professional Open Source
 * Copyright 2014 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 */
package org.searchisko.api.service;

import java.util.logging.Logger;

import org.junit.Test;
import org.searchisko.api.events.ContentDeletedEvent;
import org.searchisko.api.events.ContributorDeletedEvent;
import org.searchisko.persistence.service.RatingPersistenceService;

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

		tested.contributorDeletedEventHandler(null);
		verifyZeroInteractions(tested.ratingPersistenceService);

		reset(tested.ratingPersistenceService);
		tested.contributorDeletedEventHandler(new ContributorDeletedEvent("id", null));
		verifyZeroInteractions(tested.ratingPersistenceService);

		reset(tested.ratingPersistenceService);
		tested.contributorDeletedEventHandler(new ContributorDeletedEvent("id", "code"));
		verify(tested.ratingPersistenceService).deleteRatingsForContributor("code");

	}

	@Test
	public void contentDeletedEventHandler() {
		RatingService tested = getTested();

		tested.contentDeletedEventHandler(null);
		verifyZeroInteractions(tested.ratingPersistenceService);

		reset(tested.ratingPersistenceService);
		tested.contentDeletedEventHandler(new ContentDeletedEvent(null));
		verifyZeroInteractions(tested.ratingPersistenceService);

		reset(tested.ratingPersistenceService);
		tested.contentDeletedEventHandler(new ContentDeletedEvent("id"));
		verify(tested.ratingPersistenceService).deleteRatingsForContent("id");

	}

	private RatingService getTested() {
		RatingService ret = new RatingService();
		ret.ratingPersistenceService = mock(RatingPersistenceService.class);
		ret.log = Logger.getLogger("testlogger");
		return ret;
	}

}
