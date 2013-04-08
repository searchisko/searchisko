/*
 * JBoss, Home of Professional Open Source
 * Copyright 2013 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 */
package org.jboss.dcp.api.reindexer;

import org.jboss.dcp.api.tasker.UnsupportedTaskException;

/**
 * Task types supported in DCP. See {@link DcpTaskFactory}.
 * 
 * @author Vlastimil Elias (velias at redhat dot com)
 */
public enum DcpTaskTypes {
	REINDEX_FROM_PERSISTENCE, RENORMALIZE_BY_CONTENT_TYPE, RENORMALIZE_BY_PROJECT_CODE, RENORMALIZE_BY_CONTRIBUTOR_CODE;

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
	public static DcpTaskTypes getInstance(String taskType) throws UnsupportedTaskException {
		for (DcpTaskTypes t : DcpTaskTypes.values()) {
			if (t.getTaskType().equals(taskType))
				return t;
		}
		throw new UnsupportedTaskException(taskType);
	}

}