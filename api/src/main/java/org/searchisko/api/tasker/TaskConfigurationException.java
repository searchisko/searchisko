/*
 * JBoss, Home of Professional Open Source
 * Copyright 2013 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 */
package org.searchisko.api.tasker;


/**
 * Exception thrown during {@link Task} instance initialization if configuration is invalid for given task type.
 *
 * @author Vlastimil Elias (velias at redhat dot com)
 */
public class TaskConfigurationException extends Exception {

	/**
	 * @param message
	 */
	public TaskConfigurationException(String message) {
		super(message);
	}

}
