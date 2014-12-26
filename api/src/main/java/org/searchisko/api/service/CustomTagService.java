/*
 * JBoss, Home of Professional Open Source
 * Copyright 2014 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 */
package org.searchisko.api.service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ejb.Singleton;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;
import javax.inject.Named;
import org.searchisko.api.ContentObjectFields;

import org.searchisko.api.events.ContentDeletedEvent;
import org.searchisko.api.events.ContributorMergedEvent;
import org.searchisko.persistence.jpa.model.Tag;
import org.searchisko.persistence.service.CustomTagPersistenceService;

/**
 * Business logic for Custom Tags. It can provide some more complicated operations at top of
 * {@link CustomTagPersistenceService} but you can use {@link CustomTagPersistenceService} directly when appropriate.
 *
 * @author Jiri Mauritz (jirmauritz at gmail dot com)
 */
@Named
@ApplicationScoped
@Singleton
public class CustomTagService {

	@Inject
	protected Logger log;

	@Inject
	protected CustomTagPersistenceService customTagPersistenceService;

	/**
	 * CDI Event handler for {@link ContentDeletedEvent} used to remove ratings when content is deleted.
	 *
	 * @param event to process
	 */
	public void contentDeletedEventHandler(@Observes ContentDeletedEvent event) {
		log.log(Level.FINE, "contentDeletedEventHandler called for event {0}", event);
		if (event != null && event.getContentId() != null) {
			customTagPersistenceService.deleteTagsForContent(event.getContentId());
		} else {
			log.warning("Invalid event " + event);
		}
	}

	/**
	 * CDI event handler for {@link ContributorMergedEvent} used to merge ratings from both Contibutors to final one.
	 *
	 * @param event
	 */
	public void contributorMergedEventHandler(@Observes ContributorMergedEvent event) {
		if (event != null && event.getContributorCodeFrom() != null && event.getContributorCodeTo() != null) {
			customTagPersistenceService.changeOwnershipOfTags(event.getContributorCodeFrom(), event.getContributorCodeTo());
		} else {
			log.warning("Invalid event " + event);
		}
	}

	public void updateSysTagsField(Map<String,Object> source) {
		List<String> tags = (List<String>) source.get(ContentObjectFields.TAGS);
		List<Tag> customTags = customTagPersistenceService.getTagsByContent((String) source.get(ContentObjectFields.SYS_ID));
		Set<String> sysTags = new HashSet<>();

		sysTags.addAll(tags);
		for (Tag tag : customTags) {
			sysTags.add(tag.getTagLabel());
		}

		source.put(ContentObjectFields.SYS_TAGS, sysTags);
	}
}
