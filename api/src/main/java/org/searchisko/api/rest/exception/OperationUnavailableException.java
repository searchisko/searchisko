/*
 * JBoss, Home of Professional Open Source
 * Copyright 2014 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 */
package org.searchisko.api.rest.exception;

/**
 * Exception used when operation is currently not available.
 * 
 * @author Vlastimil Elias (velias at redhat dot com)
 */
public class OperationUnavailableException extends RuntimeException {

	/**
	 * Contructor.
	 * 
	 * @param message
	 */
	public OperationUnavailableException(String message) {
		super(message);
	}

}
