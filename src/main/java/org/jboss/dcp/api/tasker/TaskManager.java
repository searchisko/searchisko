/*
 * JBoss, Home of Professional Open Source
 * Copyright 2013 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 */
package org.jboss.dcp.api.tasker;

import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Task manager used to run longrunning tasks in distributed cluster - loadbalancing and failover supported.
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

	/**
	 * Create task manager.
	 * 
	 * @param nodeId - identifier of cluster node this manager runs on.
	 * @param taskFactory to be used
	 * @param taskPersister to be used
	 */
	private TaskManager(String nodeId, TaskFactory taskFactory, TaskPersister taskPersister) {
		super();
		this.nodeId = nodeId;
		this.taskFactory = taskFactory;
		this.taskPersister = taskPersister;
	}

	/**
	 * Start tasks execution in this manager.
	 */
	public void startTasksExecution() {
		log.info("Starting TaskManager for cluster node " + nodeId + ". Task types supported: " + listSupportedTaskTypes());
		// TODO TASKS start execution
	}

	/**
	 * Stop tasks execution in this manager.
	 */
	public void stopTasksExecution() {
		// TODO TASKS stop execution
		log.info("Stopped TaskManager for cluster node " + nodeId);
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

		// TODO TASKS persist task
		String id = null;

		// TODO TASKS notify tasks runtime there is new task to perform

		return id;
	}

	/**
	 * Get info about task.
	 * 
	 * @param id identifier of task to get status info for
	 */
	public TaskStatusInfo getTaskStatus(String id) {
		// TODO TASKS implement get status
		return null;
	}

	/**
	 * Cancel task execution.
	 * 
	 * @param id identifier of task to cancel
	 * @return true if task is canceled, false if not (because doesn't exist or is finished already)
	 */
	public boolean cancelTask(String id) {
		// TODO TASKS implement get status
		return false;
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
		// TODO TASKS implement list tasks
		return null;
	}

}
