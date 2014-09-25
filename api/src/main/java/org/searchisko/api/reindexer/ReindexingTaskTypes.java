/*
 * JBoss, Home of Professional Open Source
 * Copyright 2013 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 */
package org.searchisko.api.reindexer;

import org.searchisko.api.tasker.UnsupportedTaskException;

/**
 * Reindexing task types supported in Searchisko. See {@link ReindexingTaskFactory}.
 * 
 * @author Vlastimil Elias (velias at redhat dot com)
 */
public enum ReindexingTaskTypes {
	REINDEX_FROM_PERSISTENCE, RENORMALIZE_BY_CONTENT_TYPE, RENORMALIZE_BY_PROJECT_CODE, RENORMALIZE_BY_CONTRIBUTOR_CODE, RENORMALIZE_BY_CONTRIBUTOR_LOOKUP_ID, RENORMALIZE_BY_PROJECT_LOOKUP_ID, UPDATE_CONTRIBUTOR_PROFILE, SYNC_CONTRIBUTORS_AND_PROFILES, REINDEX_CONTRIBUTOR, REINDEX_PROJECT;

	/**
	 * @return task type identifier for this type
	 * @see #getInstance(String)
	 */
	public String getTaskType() {
		return name().toLowerCase();
	}

	/**
	 * Get instance for given task type identifier.
	 * 
	 * @param taskType to get instance for
	 * @return
	 * @throws UnsupportedTaskException if requested task type is not in enum.
	 */
	public static ReindexingTaskTypes getInstance(String taskType) throws UnsupportedTaskException {
		for (ReindexingTaskTypes t : ReindexingTaskTypes.values()) {
			if (t.getTaskType().equals(taskType))
				return t;
		}
		throw new UnsupportedTaskException(taskType);
	}

}