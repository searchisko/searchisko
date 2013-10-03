/*
 * JBoss, Home of Professional Open Source
 * Copyright 2013 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 */
package org.searchisko.api.tasker;

import java.util.List;
import java.util.Map;

/**
 * Interface for factory used to produce task instances. Used from {@link TaskManager}.
 *
 * @author Vlastimil Elias (velias at redhat dot com)
 */
public interface TaskFactory {

	/**
	 * Create task instance.
	 *
	 * @param taskType type of task to create - see {@link #listSupportedTaskTypes()}
	 * @param taskConfig configuration for task in Map of Maps structure.
	 * @return task instance
	 * @throws UnsupportedTaskException if task of given type is not supported by this factory
	 * @throws TaskConfigurationException if configuration is invalid for given task type
	 */
	public Task createTask(String taskType, Map<String, Object> taskConfig) throws UnsupportedTaskException,
			TaskConfigurationException;

	/**
	 * Get list of task types supported by this factory.
	 *
	 * @return list of task types supported by this factory.
	 */
	public List<String> listSupportedTaskTypes();

}
