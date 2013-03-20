/*
 * JBoss, Home of Professional Open Source
 * Copyright 2013 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 */
package org.jboss.dcp.api.tasker;

import java.util.logging.Logger;

/**
 * Abstract base class for task implementation. Main task work is done inside {@link #performTask()} method.
 * 
 * @author Vlastimil Elias (velias at redhat dot com)
 */
public abstract class Task implements Runnable {

	private String id;

	private Logger log = null;

	private TaskExecutionContext context;

	private boolean canceled;

	public Task() {
		log = Logger.getLogger(getClass().getName());
	}

	public void setExecutionContext(String taskId, TaskExecutionContext context) {
		this.id = taskId;
		this.context = context;
	}

	/**
	 * Implement task here.
	 * 
	 * @throws RuntimeException runtime exception is treated as system error, so task is marked as
	 *           {@link TaskStatus#FAILOVER} and run again later
	 * @throws Exception checked permission is treated as permanent error, so task is marked as
	 *           {@link TaskStatus#FINISHED_ERROR}
	 */
	protected abstract void performTask() throws Exception;

	@Override
	public void run() {
		log.fine("Starting task " + id);
		try {
			performTask();
			writeStatus(TaskStatus.FINISHED_OK, null);
		} catch (RuntimeException e) {
			writeStatus(TaskStatus.FAILOVER, e.getMessage());
		} catch (Exception e) {
			writeStatus(TaskStatus.FINISHED_ERROR, e.getMessage());
		} finally {
			log.fine("Finished task " + id);
		}
	}

	private void writeStatus(TaskStatus status, String message) {
		context.changeTaskStatus(id, status, message);
	}

	/**
	 * Write new row into task log.
	 * 
	 * @param message
	 */
	protected void writeTaskLog(String message) {
		context.writeTaskLog(id, message);
	}

	public boolean isCanceled() {
		return canceled;
	}

	public void setCanceled(boolean canceled) {
		this.canceled = canceled;
	}

}
