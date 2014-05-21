/*
 * JBoss, Home of Professional Open Source
 * Copyright 2013 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 */
package org.searchisko.api.tasker;

import java.io.InterruptedIOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.elasticsearch.indices.IndexMissingException;

/**
 * Abstract base class for task implementation. Main task work is done inside {@link #performTask()} method.
 * 
 * @author Vlastimil Elias (velias at redhat dot com)
 */
public abstract class Task extends Thread {

	protected String taskId;

	private Logger log = null;

	protected TaskExecutionContext context;

	private transient boolean canceled;

	public Task() {
		log = Logger.getLogger(getClass().getName());
	}

	/**
	 * Called from {@link TaskManager} before task execution is started.
	 * 
	 * @param taskId id of task
	 * @param context callback
	 */
	public void setExecutionContext(String taskId, TaskExecutionContext context) {
		this.taskId = taskId;
		this.context = context;
		setName("Task thread for task.id=" + taskId);
		setDaemon(false);
	}

	/**
	 * Implement your long running task here. Do not forget to check {@link #isCanceledOrInterrupted()} and return from
	 * method immediately.
	 * 
	 * @throws RuntimeException runtime exception is treated as system error, so task is marked as
	 *           {@link TaskStatus#FAILOVER} and run again later
	 * @throws Exception checked permission is treated as permanent error, so task is marked as
	 *           {@link TaskStatus#FINISHED_ERROR}
	 */
	protected abstract void performTask() throws Exception;

	@Override
	public void run() {
		log.fine("Starting task " + taskId);
		try {
			performTask();
			if (isInterrupted()) {
				writeStatus(TaskStatus.FAILOVER, "Task execution was interrupted");
			} else if (canceled) {
				writeStatus(TaskStatus.CANCELED, null);
			} else {
				writeStatus(TaskStatus.FINISHED_OK, null);
			}
		} catch (InterruptedException | InterruptedIOException e) {
			writeStatus(TaskStatus.FAILOVER, "Task execution was interrupted");
		} catch (IndexMissingException e) {
			writeStatus(TaskStatus.FINISHED_ERROR, "ERROR: Task finished due missing search index: " + e.getMessage());
		} catch (RuntimeException e) {
			String msg = "Task execution interrupted due " + e.getClass().getName() + ": " + e.getMessage();
			if (e instanceof NullPointerException) {
				msg = "Task execution interrupted due NullPointerException, see log file for stacktrace";
				log.log(Level.SEVERE, "Task finished due NullPointerException", e);
			}
			writeStatus(TaskStatus.FAILOVER, "ERROR: " + msg);
		} catch (Exception e) {
			writeStatus(TaskStatus.FINISHED_ERROR, "ERROR: Task finished due exception: " + e.getMessage());
		} finally {
			log.fine("Finished task " + taskId);
		}
	}

	private void writeStatus(TaskStatus status, String message) {
		context.changeTaskStatus(taskId, status, message);
	}

	/**
	 * Write new row into task log.
	 * 
	 * @param message
	 */
	protected void writeTaskLog(String message) {
		context.writeTaskLog(taskId, message);
	}

	/**
	 * Check if task is cancelled or interrupted. Use this check in {@link #performTask()} implementation to finish long
	 * running task if this method returns true!
	 * 
	 * @return true if task interruption/cancel is requested.
	 */
	public boolean isCanceledOrInterrupted() {
		return canceled || isInterrupted();
	}

	/**
	 * Used from {@link TaskManager} to request cancellation for this task. You have to use
	 * {@link #isCanceledOrInterrupted()} in your {@link #performTask()} implementation to allow correct task
	 * cancellation!
	 * 
	 * @param canceled
	 */
	public void setCanceled(boolean canceled) {
		this.canceled = canceled;
	}

}
