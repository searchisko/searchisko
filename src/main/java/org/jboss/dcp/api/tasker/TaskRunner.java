/*
 * JBoss, Home of Professional Open Source
 * Copyright 2013 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 */
package org.jboss.dcp.api.tasker;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
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

	protected long runnerThreadWaits = 2000;

	protected int maxRunningTasks = 2;

	protected Map<String, Task> runningTasks = new HashMap<String, Task>();

	protected TaskExecutionContext taskExecutionContextInstance = new TaskExecutionContextImpl();

	public TaskRunner(String nodeId, TaskFactory taskFactory, TaskPersister taskPersister) {
		super();
		this.nodeId = nodeId;
		this.taskFactory = taskFactory;
		this.taskPersister = taskPersister;
		setDaemon(false);
		setName("TaskRunner thread");
	}

	protected TaskRunner() {

	}

	@Override
	public void run() {
		log.info("Started tasks execution for cluster node " + nodeId);
		try {
			while (!isInterrupted()) {
				try {
					handleCancelRequests();
					// TODO TASKS sometimes check if there is some failed task in persister and mark it as FAILOVER to be run
					// again
					removeFinished();
					startTasks();
				} catch (Exception e) {
					log.fine(e.getMessage());
				}
				synchronized (this) {
					wait(runnerThreadWaits);
				}
			}
		} catch (InterruptedException e) {
			// nothing to do, just finish
		} finally {
			interruptRunningTasks();
			log.info("Stopped tasks execution for cluster node " + nodeId);
		}
	}

	protected void startTasks() {
		try {
			while (runningTasks.size() < maxRunningTasks) {
				TaskStatusInfo tsi = taskPersister.getTaskToRun(nodeId);
				if (tsi == null)
					return;
				Task t = taskFactory.createTask(tsi.taskType, tsi.taskConfig);
				t.setExecutionContext(tsi.id, taskExecutionContextInstance);
				runningTasks.put(tsi.id, t);
				t.start();
			}
		} catch (Exception e) {
			log.fine(e.getMessage());
		}
	}

	protected void removeFinished() {
		synchronized (runningTasks) {
			Set<String> rem = new HashSet<String>();
			for (String taskId : runningTasks.keySet()) {
				if (!runningTasks.get(taskId).isAlive()) {
					rem.add(taskId);
				}
			}
			if (!rem.isEmpty()) {
				for (String t : rem)
					runningTasks.remove(t);
			}
		}
	}

	protected void handleCancelRequests() {
		try {
			for (String taskId : runningTasks.keySet()) {
				Task t = runningTasks.get(taskId);
				if (t.isAlive() && !t.isCanceledOrInterrupted()) {
					TaskStatusInfo tsi = taskPersister.getTaskStatusInfo(taskId);
					if (tsi != null && tsi.cancelRequsted) {
						t.setCanceled(true);
						t.interrupt();
					}
				}
			}
		} catch (Exception e) {
			log.fine(e.getMessage());
		}
	}

	protected void interruptRunningTasks() {
		boolean interruptedSomething = false;
		synchronized (runningTasks) {
			for (Task task : runningTasks.values()) {
				if (task.isAlive()) {
					task.interrupt();
					interruptedSomething = true;
				}
			}
		}
		if (interruptedSomething) {
			log.info("Giving some time for executed tasks to be finished");
			try {
				Thread.sleep(2000);
			} catch (InterruptedException e) {
				// OK
			}
		}
	}

	/**
	 * Cancel task execution if it runs in this runner.
	 * 
	 * @param taskId to cancel
	 */
	public void cancelTask(String taskId) {
		synchronized (runningTasks) {
			Task t = runningTasks.get(taskId);
			if (t != null && t.isAlive()) {
				t.setCanceled(true);
				t.interrupt();
			}
		}
	}

	/**
	 * Notify this runner new task is available, so it can start it immediately if there is some room for it.
	 */
	public void notifyNewTaskAvailableForRun() {
		if (runningTasks.size() < maxRunningTasks)
			this.notifyAll();
	}

	protected class TaskExecutionContextImpl implements TaskExecutionContext {

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
