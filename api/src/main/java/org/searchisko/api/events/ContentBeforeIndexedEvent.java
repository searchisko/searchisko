/*
 * JBoss, Home of Professional Open Source
 * Copyright 2014 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 */
package org.searchisko.api.events;

import java.util.Map;

import org.searchisko.api.ContentObjectFields;

/**
 * CDI Event emitted just before some Content is stored into search index. So it is possible to add some data into
 * content by Searchisko extensions. It is emitted when content is pushed over REST API or reindexed from persistent
 * store.
 * 
 * @author Vlastimil Elias (velias at redhat dot com)
 */
public class ContentBeforeIndexedEvent {

	private String contentId;

	private Map<String, Object> contentData;

	/**
	 * Create event.
	 * 
	 * @param contentId Searchisko wide unique identifier of content - from field {@link ContentObjectFields#SYS_ID}
	 * @param contentData content data object
	 */
	public ContentBeforeIndexedEvent(String contentId, Map<String, Object> contentData) {
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
		return "ContentBeforeIndexedEvent [contentId=" + contentId + ", contentData=" + contentData + "]";
	}

}
