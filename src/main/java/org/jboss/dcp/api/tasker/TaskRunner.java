/*
 * JBoss, Home of Professional Open Source
 * Copyright 2013 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 */
package org.jboss.dcp.api.tasker;

import java.util.logging.Logger;

/**
 * Component used to run tasks inside {@link TaskManager}.
 * 
 * @author Vlastimil Elias (velias at redhat dot com)
 */
public class TaskRunner extends Thread {

	private static final Logger log = Logger.getLogger(TaskRunner.class.getName());

	protected String nodeId;
	protected TaskFactory taskFactory;
	protected TaskPersister taskPersister;

	public TaskRunner(String nodeId, TaskFactory taskFactory, TaskPersister taskPersister) {
		super();
		this.nodeId = nodeId;
		this.taskFactory = taskFactory;
		this.taskPersister = taskPersister;
		setDaemon(true);
		setName("TaskRunner thread");
	}

	@Override
	public void run() {
		log.info("Started tasks execution for cluster node " + nodeId);
		try {
			while (!isInterrupted()) {
				// TODO TASKS task execution code
			}
			if (isInterrupted()) {
				// TODO stop all tasks and mark them as FAILOVER if not finished yet
			}
		} finally {
			log.info("Stopped tasks execution for cluster node " + nodeId);
		}
	}

	private class TaskExecutionContenxtImpl implements TaskExecutionContext {

		@Override
		public void changeTaskStatus(String id, TaskStatus status, String message) {
			taskPersister.changeTaskStatus(id, status, message);
		}

		@Override
		public void writeTaskLog(String id, String message) {
			taskPersister.writeTaskLog(id, message);
		}
	}

}
