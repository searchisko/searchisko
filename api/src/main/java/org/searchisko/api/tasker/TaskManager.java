/*
 * JBoss, Home of Professional Open Source
 * Copyright 2013 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 */
package org.searchisko.api.tasker;

import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Task manager used to run long running tasks in distributed cluster - loadbalancing and failover supported.
 *
 * @author Vlastimil Elias (velias at redhat dot com)
 */
public class TaskManager {

	private static final Logger log = Logger.getLogger(TaskManager.class.getName());

	/**
	 * Identifier of cluster node this manager runs on.
	 */
	protected String nodeId;

	protected TaskFactory taskFactory;
	protected TaskPersister taskPersister;
	protected TaskRunner taskRunner;

	/**
	 * Create task manager.
	 *
	 * @param nodeId - identifier of cluster node this manager runs on.
	 * @param taskFactory to be used
	 * @param taskPersister to be used
	 */
	public TaskManager(String nodeId, TaskFactory taskFactory, TaskPersister taskPersister) {
		super();
		this.nodeId = nodeId;
		this.taskFactory = taskFactory;
		this.taskPersister = taskPersister;
	}

	/**
	 * Start tasks execution in this manager.
	 */
	public synchronized void startTasksExecution() {
		if (taskRunner != null) {
			log.fine("Tasks execution started already.");
			return;
		}
		log.info("Starting tasks execution for cluster node " + nodeId + ". Task types supported: "
				+ listSupportedTaskTypes());
		taskRunner = new TaskRunner(nodeId, taskFactory, taskPersister);
		taskRunner.start();
	}

	/**
	 * Stop tasks execution in this manager.
	 */
	public synchronized void stopTasksExecution() {
		log.info("Stopping tasks execution for cluster node " + nodeId);
		if (taskRunner != null) {
			taskRunner.interrupt();
			taskRunner = null;
		}
	}

	/**
	 * Get list of task types supported by this manager.
	 *
	 * @return list of task types supported by this manager.
	 */
	public List<String> listSupportedTaskTypes() {
		return taskFactory.listSupportedTaskTypes();
	}

	/**
	 * Create task to be performed.
	 *
	 * @param taskType type of task to perform.
	 * @param taskConfig configuration for task. Depends on taskType
	 * @return identifier of task for subsequent calls on task lifecycle methods
	 * @throws UnsupportedTaskException
	 * @throws TaskConfigurationException
	 */
	public String createTask(String taskType, Map<String, Object> taskConfig) throws UnsupportedTaskException,
			TaskConfigurationException {

		// validation if task may be performed
		taskFactory.createTask(taskType, taskConfig);

		String id = taskPersister.createTask(taskType, taskConfig);

		taskRunner.notifyNewTaskAvailableForRun();

		return id;
	}

	/**
	 * Get info about task.
	 *
	 * @param id identifier of task to get status info for
	 * @return info about task. null if task doesn't exists
	 */
	public TaskStatusInfo getTaskStatusInfo(String id) {
		return taskPersister.getTaskStatusInfo(id);
	}

	/**
	 * Cancel task execution.
	 *
	 * @param id identifier of task to cancel
	 * @return true if task is canceled, false if not (because doesn't exist or is finished already)
	 */
	public boolean cancelTask(String id) {
		if (id == null)
			return false;
		boolean ret = taskPersister.markTaskToBeCancelled(id);
		if (ret && taskRunner != null) {
			taskRunner.cancelTask(id);
		}
		return ret;
	}

	/**
	 * List info about tasks exiting in this manager. Returned list is ordered from most recent tasks to older always.
	 *
	 * @param taskTypeFilter optional. If set then tasks of given type returned only.
	 * @param taskStatusFilter optional. If defined then tasks with defined statuses are returned only.
	 * @param from pager support - index of first task returned. 0 is first task.
	 * @param size pager support - maximal number of records returned.
	 * @return list of tasks matching filters.
	 */
	public List<TaskStatusInfo> listTasks(String taskTypeFilter, List<TaskStatus> taskStatusFilter, int from, int size) {
		return taskPersister.listTasks(taskTypeFilter, taskStatusFilter, from, size);
	}

	public String getNodeId() {
		return nodeId;
	}

	public TaskFactory getTaskFactory() {
		return taskFactory;
	}

	public TaskPersister getTaskPersister() {
		return taskPersister;
	}

	public TaskRunner getTaskRunner() {
		return taskRunner;
	}

}
