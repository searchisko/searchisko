/*
 * JBoss, Home of Professional Open Source
 * Copyright 2013 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 */
package org.searchisko.api.tasker;

/**
 * Exception thrown from {@link TaskFactory} if requested task type is not supported by this factory.
 *
 * @author Vlastimil Elias (velias at redhat dot com)
 */
public class UnsupportedTaskException extends Exception {

	/**
	 * @param taskType type of task requested
	 */
	public UnsupportedTaskException(String taskType) {
		super("Task type " + taskType + " is not supported");
	}

}
