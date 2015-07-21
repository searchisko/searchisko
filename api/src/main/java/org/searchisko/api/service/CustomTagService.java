/*
 * JBoss, Home of Professional Open Source
 * Copyright 2014 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 */
package org.searchisko.api.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
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
@Lock(LockType.READ)
public class CustomTagService {

	@Inject
	protected Logger log;

	@Inject
	protected CustomTagPersistenceService customTagPersistenceService;

	/**
	 * CDI Event handler for {@link ContentDeletedEvent} used to remove tags when content is deleted.
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
	 * CDI event handler for {@link ContentBeforeIndexedEvent}. Used to add content tags fields into content data before
	 * indexed.
	 */
	public void contentBeforeIndexedHandler(@Observes ContentBeforeIndexedEvent event) {
		log.log(Level.FINE, "contentBeforeIndexedHandler called for event {0}", event);
		if (event == null || event.getContentId() == null || event.getContentData() == null) {
			log.warning("Invalid event " + event);
			return;
		}
		Map<String, Object> content = event.getContentData();
		updateSysTagsField(content);
	}

	/**
	 * CDI event handler for {@link ContributorMergedEvent} used to merge tags from both Contibutors to final one.
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

	/**
	 * CDI event handler for {@link ContributorCodeChangedEvent} used to change code in tags.
	 * 
	 * @param event
	 */
	public void contributorCodeChangedEventHandler(@Observes ContributorCodeChangedEvent event) {
		if (event != null && event.getContributorCodeFrom() != null && event.getContributorCodeTo() != null) {
			customTagPersistenceService.changeOwnershipOfTags(event.getContributorCodeFrom(), event.getContributorCodeTo());
		} else {
			log.warning("Invalid event " + event);
		}
	}

	/**
	 * Method merge tags from TAGS field and custom tags from persistence layer and save them into SYS_TAGS field.
	 * 
	 * @param source
	 */
	@SuppressWarnings("unchecked")
	public void updateSysTagsField(Map<String, Object> source) {
		List<String> tags = null;
		try {
			tags = (List<String>) source.get(ContentObjectFields.TAGS);
		} catch (Exception e) {
			log.warning("Incorrect format (not an array) of 'tags' field in document: " + source);
		}

		List<Tag> customTags = customTagPersistenceService
				.getTagsByContent((String) source.get(ContentObjectFields.SYS_ID));
		Set<String> sysTags = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);

		if (customTags != null) {
			for (Tag tag : customTags) {
				sysTags.add(tag.getTagLabel());
			}
		}

		if (tags != null) {
			sysTags.addAll(tags);
		}

		source.put(ContentObjectFields.SYS_TAGS, new ArrayList<String>(sysTags));
	}
}
