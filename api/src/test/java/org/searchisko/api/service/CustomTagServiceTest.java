/*
 * JBoss, Home of Professional Open Source
 * Copyright 2014 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 */
package org.searchisko.api.service;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import org.junit.Assert;

import org.junit.Test;
import org.mockito.Mockito;
import org.searchisko.api.events.ContentDeletedEvent;
import org.searchisko.api.events.ContributorMergedEvent;
import org.searchisko.persistence.service.CustomTagPersistenceService;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import org.searchisko.api.ContentObjectFields;
import org.searchisko.api.events.ContentBeforeIndexedEvent;
import org.searchisko.api.events.ContributorCodeChangedEvent;
import org.searchisko.persistence.jpa.model.Tag;

/**
 * Unit test for {@link CustomTagService}.
 *
 * @author Jiri Mauritz (jirmauritz at gmail dot com)
 */
public class CustomTagServiceTest {

	@Test
	public void contentDeletedEventHandler() {
		CustomTagService tested = getTested();

		// case - invalid event
		tested.contentDeletedEventHandler(null);
		verifyZeroInteractions(tested.customTagPersistenceService);

		reset(tested.customTagPersistenceService);
		tested.contentDeletedEventHandler(new ContentDeletedEvent(null));
		verifyZeroInteractions(tested.customTagPersistenceService);

		// case - valid event
		reset(tested.customTagPersistenceService);
		tested.contentDeletedEventHandler(new ContentDeletedEvent("id"));
		verify(tested.customTagPersistenceService).deleteTagsForContent("id");

	}

	@Test
	public void contributorMergedEventHandler() {
		CustomTagService tested = getTested();

		// case - invalid event
		tested.contributorMergedEventHandler(null);
		verifyZeroInteractions(tested.customTagPersistenceService);

		reset(tested.customTagPersistenceService);
		tested.contributorMergedEventHandler(new ContributorMergedEvent(null, null));
		verifyZeroInteractions(tested.customTagPersistenceService);

		reset(tested.customTagPersistenceService);
		tested.contributorMergedEventHandler(new ContributorMergedEvent("idFrom", null));
		verifyZeroInteractions(tested.customTagPersistenceService);

		reset(tested.customTagPersistenceService);
		tested.contributorMergedEventHandler(new ContributorMergedEvent(null, "idTo"));
		verifyZeroInteractions(tested.customTagPersistenceService);

		// case - valid event
		reset(tested.customTagPersistenceService);
		tested.contributorMergedEventHandler(new ContributorMergedEvent("idFrom", "idTo"));
		verify(tested.customTagPersistenceService).changeOwnershipOfTags("idFrom", "idTo");
	}

	@Test
	public void contributorCodeChangedEventHandler() {
		CustomTagService tested = getTested();

		// case - invalid event
		tested.contributorCodeChangedEventHandler(null);
		verifyZeroInteractions(tested.customTagPersistenceService);

		reset(tested.customTagPersistenceService);
		tested.contributorCodeChangedEventHandler(new ContributorCodeChangedEvent(null, null));
		verifyZeroInteractions(tested.customTagPersistenceService);

		reset(tested.customTagPersistenceService);
		tested.contributorCodeChangedEventHandler(new ContributorCodeChangedEvent("idFrom", null));
		verifyZeroInteractions(tested.customTagPersistenceService);

		reset(tested.customTagPersistenceService);
		tested.contributorCodeChangedEventHandler(new ContributorCodeChangedEvent(null, "idTo"));
		verifyZeroInteractions(tested.customTagPersistenceService);

		// case - valid event
		reset(tested.customTagPersistenceService);
		tested.contributorCodeChangedEventHandler(new ContributorCodeChangedEvent("idFrom", "idTo"));
		verify(tested.customTagPersistenceService).changeOwnershipOfTags("idFrom", "idTo");
	}

	@Test
	public void contentBeforeIndexedHandler() {
		CustomTagService tested = getTested();

		// case - invalid event
		tested.contentBeforeIndexedHandler(null);
		verifyZeroInteractions(tested.customTagPersistenceService);

		reset(tested.customTagPersistenceService);
		tested.contentBeforeIndexedHandler(new ContentBeforeIndexedEvent(null, null));
		verifyZeroInteractions(tested.customTagPersistenceService);

		reset(tested.customTagPersistenceService);
		tested.contentBeforeIndexedHandler(new ContentBeforeIndexedEvent("id", null));
		verifyZeroInteractions(tested.customTagPersistenceService);

		reset(tested.customTagPersistenceService);
		tested.contentBeforeIndexedHandler(new ContentBeforeIndexedEvent(null, new HashMap<String, Object>()));
		verifyZeroInteractions(tested.customTagPersistenceService);

		// test of updateSysTagsField method

		// case - copying of provider tag
		{
			reset(tested.customTagPersistenceService);
			Map<String, Object> content = new HashMap<String, Object>();
			List<String> listOfLabels = new ArrayList<String>();
			listOfLabels.add("label1");
			listOfLabels.add("label2");
			content.put(ContentObjectFields.TAGS, listOfLabels);
			Mockito.when(tested.customTagPersistenceService.getTagsByContent("id")).thenReturn(new ArrayList<Tag>());
			tested.contentBeforeIndexedHandler(new ContentBeforeIndexedEvent("id", content));
			Assert.assertEquals(listOfLabels, content.get(ContentObjectFields.SYS_TAGS));
		}

		// case - copying of custom tag
		{
			reset(tested.customTagPersistenceService);
			Map<String, Object> content = new HashMap<String, Object>();
			content.put(ContentObjectFields.TAGS, null);
			content.put(ContentObjectFields.SYS_ID, "contentId");
			List<Tag> listOfTags = new ArrayList<Tag>();
			listOfTags.add(new Tag("contentId", "contributorId", "label"));
			Mockito.when(tested.customTagPersistenceService.getTagsByContent("contentId")).thenReturn(listOfTags);
			tested.contentBeforeIndexedHandler(new ContentBeforeIndexedEvent("contentId", content));
			List<String> listOfLabel = new ArrayList<String>();
			listOfLabel.add("label");
			Assert.assertEquals(listOfLabel, content.get(ContentObjectFields.SYS_TAGS));
		}

		//TODO merge obou skupin i s osetrenim duplicit

	}

	private CustomTagService getTested() {
		CustomTagService ret = new CustomTagService();
		ret.customTagPersistenceService = mock(CustomTagPersistenceService.class);
		ret.log = Logger.getLogger("testlogger");
		return ret;
	}

}
