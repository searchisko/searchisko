/*
 * JBoss, Home of Professional Open Source
 * Copyright 2013 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 */
package org.searchisko.api.tasker;

/**
 * Task execution context. Passed into {@link Task} instance over
 * {@link Task#setExecutionContext(String, TaskExecutionContext)} before task is started.
 *
 * @author Vlastimil Elias (velias at redhat dot com)
 */
public interface TaskExecutionContext {

	void changeTaskStatus(String id, TaskStatus status, String message);

	void writeTaskLog(String id, String message);

}
