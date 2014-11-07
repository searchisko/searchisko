/*
 * JBoss, Home of Professional Open Source
 * Copyright 2014 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 */
package org.searchisko.api.rest.exception;

import org.jboss.elasticsearch.tools.content.InvalidDataException;

/**
 * Application Exception used to wrap {@link InvalidDataException} not to rollback transaction in EJB.
 * 
 * @author Vlastimil Elias (velias at redhat dot com)
 */
public class PreprocessorInvalidDataException extends Exception {

	public PreprocessorInvalidDataException(InvalidDataException e) {
		super(e.getMessage(), e);
	}

}
