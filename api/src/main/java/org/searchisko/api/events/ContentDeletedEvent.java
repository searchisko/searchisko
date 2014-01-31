/*
 * JBoss, Home of Professional Open Source
 * Copyright 2014 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 */
package org.searchisko.api.events;

import org.searchisko.api.ContentObjectFields;

/**
 * CDI Event emitted when some Content is deleted from Searchisko.
 * 
 * @author Vlastimil Elias (velias at redhat dot com)
 */
public class ContentDeletedEvent {

	private String contentId;

	/**
	 * Create event.
	 * 
	 * @param contentId Searchisko wide unique identifier of deleted content - from field
	 *          {@link ContentObjectFields#SYS_ID}
	 * @param contentData content data object
	 */
	public ContentDeletedEvent(String contentId) {
		super();
		this.contentId = contentId;
	}

	public String getContentId() {
		return contentId;
	}

	@Override
	public String toString() {
		return "ContentDeletedEvent [contentId=" + contentId + "]";
	}

}
