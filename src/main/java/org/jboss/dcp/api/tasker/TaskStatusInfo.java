/*
 * JBoss, Home of Professional Open Source
 * Copyright 2013 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 */
package org.jboss.dcp.api.tasker;

import java.util.Date;
import java.util.Map;

/**
 * Information about task existing in {@link TaskManager} and about status of execution.
 * 
 * @author Vlastimil Elias (velias at redhat dot com)
 */
public class TaskStatusInfo {

	String id;

	String taskType;

	Map<String, Object> taskConfig;

	Date taskCreatedAt;

	Date lastRunStartedAt;

	/**
	 * Counter how much times this task was started.
	 */
	int runCount;

	Date lastRunFinishedAt;

	/**
	 * Identifier of cluster node where task runs or ran last time.
	 */
	String executionNodeId;

	TaskStatus taskStatus;

	String processingLog;

	boolean cancelRequsted;

}
