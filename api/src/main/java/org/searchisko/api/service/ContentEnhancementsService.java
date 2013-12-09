/*
 * JBoss, Home of Professional Open Source
 * Copyright 2013 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 */
package org.searchisko.api.service;

import java.util.Map;

import javax.ejb.LocalBean;
import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.inject.Named;

import org.searchisko.api.ContentObjectFields;
import org.searchisko.api.rest.RatingRestService;
import org.searchisko.persistence.service.RatingPersistenceService;
import org.searchisko.persistence.service.RatingPersistenceService.RatingStats;

/**
 * Service implementing business logic regarding distinct content enhancement features, like content rating, external
 * tags etc.
 * 
 * @author Vlastimil Elias (velias at redhat dot com)
 */
@Named
@Stateless
@LocalBean
public class ContentEnhancementsService {

	@Inject
	protected RatingPersistenceService ratingPersistenceService;

	/**
	 * Add content rating fields into content data.
	 * 
	 * @param content to add rating fields into
	 * @param sysContentId id of content to handle rating for
	 * 
	 * @see RatingPersistenceService
	 * @see RatingRestService
	 * @see ContentObjectFields#SYS_RATING_AVG
	 * @see ContentObjectFields#SYS_RATING_NUM
	 */
	public void handleContentRatingFields(Map<String, Object> content, String sysContentId) {
		RatingStats rs = ratingPersistenceService.countRatingStats(sysContentId);
		if (rs != null) {
			content.put(ContentObjectFields.SYS_RATING_AVG, rs.getAverage());
			content.put(ContentObjectFields.SYS_RATING_NUM, rs.getNumber());
		} else {
			content.remove(ContentObjectFields.SYS_RATING_AVG);
			content.remove(ContentObjectFields.SYS_RATING_NUM);
		}
	}

	/**
	 * TODO EXTERNAL_TAGS - add external tags for this document into sys_tags field.
	 * 
	 * @param content to add external tags into
	 * @param sysContentId id of content to handle external tags for
	 */
	public void handleExternalTags(Map<String, Object> content, String sysContentId) {
	}

}
