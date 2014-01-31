/*
 * JBoss, Home of Professional Open Source
 * Copyright 2014 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 */
package org.searchisko.api.events;

import java.util.Map;

import org.searchisko.api.ContentObjectFields;

/**
 * CDI Event emitted when some Content is stored (it means created or updated) in Searchisko.
 * 
 * @author Vlastimil Elias (velias at redhat dot com)
 */
public class ContentStoredEvent {

	private String contentId;

	private Map<String, Object> contentData;

	/**
	 * Create event.
	 * 
	 * @param contentId Searchisko wide unique identifier of stored content - from field
	 *          {@link ContentObjectFields#SYS_ID}
	 * @param contentData content data object
	 */
	public ContentStoredEvent(String contentId, Map<String, Object> contentData) {
		super();
		this.contentId = contentId;
		this.contentData = contentData;
	}

	public String getContentId() {
		return contentId;
	}

	public Map<String, Object> getContentData() {
		return contentData;
	}

	@Override
	public String toString() {
		return "ContentStoredEvent [contentId=" + contentId + ", contentData=" + contentData + "]";
	}

}
