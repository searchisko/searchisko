/*
 * JBoss, Home of Professional Open Source
 * Copyright 2013 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 */
package org.searchisko.persistence.service;

import java.util.List;
import org.searchisko.persistence.jpa.model.Tag;


/**
 * Interface for service used to persistently store "Custom tag"s.
 *
 * @author Jiri Mauritz (jirmauritz at gmail dot com)
 */
public interface CustomTagPersistenceService {

	/**
	 * Get all custom tags for given content.
	 *
	 * @param contentId identifier of the content
	 * @return list of tags
	 */
	List<Tag> getTagsByContent(String... contentId);

	/**
	 * Get tags for all contents. It can be used for tagcloud and others.
	 *
	 * @return list of tags
	 */
	List<Tag> getAllTags();

	/**
	 * Creates tag for the content given in the tag object parameter.
	 *
	 * @param tag representation of the tag
	 * @return false, if tag alrady exists for given content
	 *
	 */
	boolean createTag(Tag tag);

	/**
	 * Deletes tag for the content given in the tag object parameter.
	 *
	 * @param contentId identifier of the content
	 * @param tagLabel string representation of tag
	 *
	 */
	void deleteTag(String contentId, String tagLabel);

	/**
	 * Deletes all tags for given content.
	 *
	 * @param contentId to delete tags for
	 */
	void deleteTagsForContent(String... contentId);

	/**
	 * Changes ownership of all tags that belong to contributorFrom to contributorTo.
	 *
	 * @param contributorFrom id of contributor
	 * @param contributorTo  id of contributor
	 */
	void changeOwnershipOfTags(String contributorFrom, String contributorTo);

}
