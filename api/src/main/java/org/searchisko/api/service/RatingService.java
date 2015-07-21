/*
 * JBoss, Home of Professional Open Source
 * Copyright 2014 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 */
package org.searchisko.api.service;

import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ejb.Lock;
import javax.ejb.LockType;
import javax.ejb.Singleton;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;
import javax.inject.Named;

import org.searchisko.api.ContentObjectFields;
import org.searchisko.api.events.ContentBeforeIndexedEvent;
import org.searchisko.api.events.ContentDeletedEvent;
import org.searchisko.api.events.ContributorCodeChangedEvent;
import org.searchisko.api.events.ContributorDeletedEvent;
import org.searchisko.api.events.ContributorMergedEvent;
import org.searchisko.persistence.service.RatingPersistenceService;
import org.searchisko.persistence.service.RatingPersistenceService.RatingStats;

/**
 * Business logic for Content Ratings. It can provide some more complicated operations at top of
 * {@link RatingPersistenceService} but you can use {@link RatingPersistenceService} directly when appropriate.
 * 
 * @author Vlastimil Elias (velias at redhat dot com)
 */
@Named
@ApplicationScoped
@Singleton
@Lock(LockType.READ)
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
		if (event != null && event.getContributorCode() != null) {
			ratingPersistenceService.deleteRatingsForContributor(event.getContributorCode());
		} else {
			log.warning("Invalid event " + event);
		}
	}

	/**
	 * CDI Event handler for {@link ContentDeletedEvent} used to remove ratings when content is deleted.
	 * 
	 * @param event to process
	 */
	public void contentDeletedEventHandler(@Observes ContentDeletedEvent event) {
		log.log(Level.FINE, "contentDeletedEventHandler called for event {0}", event);
		if (event != null && event.getContentId() != null) {
			ratingPersistenceService.deleteRatingsForContent(event.getContentId());
		} else {
			log.warning("Invalid event " + event);
		}
	}

	/**
	 * CDI event handler for {@link ContentBeforeIndexedEvent}. Used to add content rating fields into content data before
	 * indexed.
	 */
	public void handleContentRatingFields(@Observes ContentBeforeIndexedEvent event) {
		log.log(Level.FINE, "handleContentRatingFields called for event {0}", event);
		if (event == null || event.getContentId() == null || event.getContentData() == null) {
			log.warning("Invalid event " + event);
			return;
		}
		RatingStats rs = ratingPersistenceService.countRatingStats(event.getContentId());
		Map<String, Object> content = event.getContentData();
		if (rs != null) {
			content.put(ContentObjectFields.SYS_RATING_AVG, rs.getAverage());
			content.put(ContentObjectFields.SYS_RATING_NUM, rs.getNumber());
		} else {
			content.remove(ContentObjectFields.SYS_RATING_AVG);
			content.remove(ContentObjectFields.SYS_RATING_NUM);
		}
	}

	/**
	 * CDI event handler for {@link ContributorMergedEvent} used to merge ratings from both Contibutors to final one.
	 * 
	 * @param event
	 */
	public void contributorMergedEventHandler(@Observes ContributorMergedEvent event) {
		if (event != null && event.getContributorCodeFrom() != null && event.getContributorCodeTo() != null) {
			ratingPersistenceService
					.mergeRatingsForContributors(event.getContributorCodeFrom(), event.getContributorCodeTo());
		} else {
			log.warning("Invalid event " + event);
		}
	}

	/**
	 * CDI event handler for {@link ContributorCodeChangedEvent} used to change code in ratings.
	 * 
	 * @param event
	 */
	public void contributorCodeChangedEventHandler(@Observes ContributorCodeChangedEvent event) {
		if (event != null && event.getContributorCodeFrom() != null && event.getContributorCodeTo() != null) {
			ratingPersistenceService
					.mergeRatingsForContributors(event.getContributorCodeFrom(), event.getContributorCodeTo());
		} else {
			log.warning("Invalid event " + event);
		}
	}

}
