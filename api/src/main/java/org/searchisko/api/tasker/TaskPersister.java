/*
 * JBoss, Home of Professional Open Source
 * Copyright 2013 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 */
package org.searchisko.api.tasker;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Interface for task persister component. It is used inside {@link TaskManager} to persist tasks information.
 *
 * @author Vlastimil Elias (velias at redhat dot com)
 */
public interface TaskPersister {

	/**
	 * Create task to be performed.
	 *
	 * @param taskType type of task to perform.
	 * @param taskConfig configuration for task. Depends on taskType
	 * @return identifier of task for subsequent calls on task lifecycle methods
	 */
	public String createTask(String taskType, Map<String, Object> taskConfig);

	/**
	 * List info about tasks exiting in this manager. Returned list is ordered from most recent tasks to older always.
	 *
	 * @param taskTypeFilter optional. If set then tasks of given type returned only.
	 * @param taskStatusFilter optional. If defined then tasks with defined statuses are returned only.
	 * @param from pager support - index of first task returned. 0 is first task.
	 * @param size pager support - maximal number of records returned.
	 * @return list of tasks matching filters.
	 */
	public List<TaskStatusInfo> listTasks(String taskTypeFilter, List<TaskStatus> taskStatusFilter, int from, int size);

	/**
	 * Get info about task.
	 *
	 * @param id identifier of task to get status info for
	 * @return info about task. null if task doesn't exists
	 */
	public TaskStatusInfo getTaskStatusInfo(String id);

	/**
	 * Mark tasks with cancel request.
	 *
	 * @param id identifier of task to cancel
	 * @return true if task is canceled, false if not (because doesn't exist or is finished already)
	 */
	public boolean markTaskToBeCancelled(String id);

	/**
	 * Change task status. This method MUST NOT allow bad task status transitions! Just must ignore them with false
	 * returned.
	 *
	 * @param id of task
	 * @param taskStatus to be set
	 * @param message optional message to be written into task log
	 * @return true if status was changed
	 */
	public boolean changeTaskStatus(String id, TaskStatus taskStatus, String message);

	/**
	 * Write new row into task log.
	 *
	 * @param message to be written
	 */
	public void writeTaskLog(String id, String message);

	/**
	 * Get task to be stared. Persister must switch status of task to {@link TaskStatus#RUNNING} before return it.
	 * Persister must handle cluster concurrency also to prevent task starting on more nodes.
	 *
	 * @param nodeId to start task on (current cluster node)
	 * @return task to be started or null if no any available
	 */
	public TaskStatusInfo getTaskToRun(String nodeId);

	/**
	 * Method periodically called by task runner to perform node heartbeat operations.
	 *
	 * @param nodeId of current cluster node
	 * @param runningTasksId identifiers of tasks currently running on this cluster node
	 * @param failoverTimeout failover timeout in milliseconds. Must be accurate to this method call period (two or three
	 *          times higher!).
	 */
	public void heartbeat(String nodeId, Set<String> runningTasksId, long failoverTimeout);

}
