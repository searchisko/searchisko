/*
 * JBoss, Home of Professional Open Source
 * Copyright 2014 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 */
package org.searchisko.api.service;

import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

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
 * Business logic for Custom Tags. It can provide some more complicated operations at top of
 * {@link CustomTagPersistenceService} but you can use {@link CustomTagPersistenceService} directly when appropriate.
 *
 * @author Jiri Mauritz (jirmauritz at gmail dot com)
 */
@Named
@ApplicationScoped
@Singleton
public class CustomTagsService {

	@Inject
	protected Logger log;

	@Inject
	protected RatingPersistenceService ratingPersistenceService;

	

}
