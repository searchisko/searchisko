/*
 * JBoss, Home of Professional Open Source
 * Copyright 2012 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 */
package org.searchisko.api.service;

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.ejb.Lock;
import javax.ejb.LockType;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.inject.Named;

import org.elasticsearch.common.Strings;
import org.searchisko.api.tasker.TaskFactory;
import org.searchisko.api.tasker.TaskManager;
import org.searchisko.api.tasker.TaskPersister;

/**
 * Service used to run long running tasks.
 *
 * @author Vlastimil Elias (velias at redhat dot com)
 */
@Named
@ApplicationScoped
@Singleton
@Lock(LockType.READ)
@Startup
public class TaskService {

	@Inject
	protected Logger log;

	@Inject
	protected TaskPersister taskPersister;

	@Inject
	protected TaskFactory taskFactory;

	protected TaskManager taskManager;

	@PostConstruct
	public synchronized void init() {
		if (taskManager == null) {
			log.log(Level.INFO, "Starting TaskManager");
			String nodeId = System.getProperty("jboss.node.name");
			if (nodeId == null || nodeId.trim().isEmpty())
				nodeId = Strings.randomBase64UUID();
			taskManager = new TaskManager(nodeId, taskFactory, taskPersister);
			taskManager.startTasksExecution();
		} else {
			log.log(Level.INFO, "TaskManager is started already");
		}
	}

	@PreDestroy
	public synchronized void destroy() {
		if (taskManager != null) {
			taskManager.stopTasksExecution();
			taskManager = null;
		}
	}

	/**
	 * @return task manager to be used in REST API service.
	 */
	public TaskManager getTaskManager() {
		return taskManager;
	}

}
