/*
 * JBoss, Home of Professional Open Source
 * Copyright 2014 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 */
package org.searchisko.api.service;

import java.util.logging.Logger;

import org.junit.Test;
import org.mockito.Mockito;
import org.searchisko.api.events.ContributorDeletedEvent;
import org.searchisko.persistence.service.RatingPersistenceService;

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
		Mockito.verifyZeroInteractions(tested.ratingPersistenceService);

		Mockito.reset(tested.ratingPersistenceService);
		tested.contributorDeletedEventHandler(new ContributorDeletedEvent("id", null));
		Mockito.verifyZeroInteractions(tested.ratingPersistenceService);

		Mockito.reset(tested.ratingPersistenceService);
		tested.contributorDeletedEventHandler(new ContributorDeletedEvent("id", "code"));
		Mockito.verify(tested.ratingPersistenceService).deleteRatingsForContributor("code");

	}

	private RatingService getTested() {
		RatingService ret = new RatingService();
		ret.ratingPersistenceService = Mockito.mock(RatingPersistenceService.class);
		ret.log = Logger.getLogger("testlogger");
		return ret;
	}

}
