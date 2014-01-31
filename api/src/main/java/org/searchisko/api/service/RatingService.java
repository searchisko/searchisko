/*
 * JBoss, Home of Professional Open Source
 * Copyright 2014 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 */
package org.searchisko.api.service;

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ejb.Singleton;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;
import javax.inject.Named;

import org.searchisko.api.events.ContentDeletedEvent;
import org.searchisko.api.events.ContributorDeletedEvent;
import org.searchisko.persistence.service.RatingPersistenceService;

/**
 * Business logic for Content Ratings. It can provide some more complicated operations at top of
 * {@link RatingPersistenceService} but you can use {@link RatingPersistenceService} directly when appropriate.
 * 
 * @author Vlastimil Elias (velias at redhat dot com)
 */
@Named
@ApplicationScoped
@Singleton
public class RatingService {

	@Inject
	protected Logger log;

	@Inject
	protected RatingPersistenceService ratingPersistenceService;

	/**
	 * CDI Event handler for {@link ContributorDeletedEvent} used to remove ratings when contributor is deleted.
	 * 
	 * @param event to process
	 */
	public void contributorDeletedEventHandler(@Observes ContributorDeletedEvent event) {
		log.log(Level.FINE, "contributorDeletedEventHandler called for event {0}", event);
		if (event != null && event.getContributorCode() != null)
			ratingPersistenceService.deleteRatingsForContributor(event.getContributorCode());
	}

	/**
	 * CDI Event handler for {@link ContentDeletedEvent} used to remove ratings when content is deleted.
	 * 
	 * @param event to process
	 */
	public void contentDeletedEventHandler(@Observes ContentDeletedEvent event) {
		log.log(Level.FINE, "contentDeletedEventHandler called for event {0}", event);
		if (event != null && event.getContentId() != null)
			ratingPersistenceService.deleteRatingsForContent(event.getContentId());
	}

}
