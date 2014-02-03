/*
 * JBoss, Home of Professional Open Source
 * Copyright 2014 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 */
package org.searchisko.api.service;

import javax.ejb.ApplicationException;

import org.elasticsearch.indices.IndexMissingException;

/**
 * Exception used to propagate {@link IndexMissingException} from {@link SearchClientService} CDI bean to caller without
 * transaction rollback.
 * 
 * @author Vlastimil Elias (velias at redhat dot com)
 */
@ApplicationException(rollback = false)
public class SearchIndexMissingException extends Exception {

	public SearchIndexMissingException(IndexMissingException cause) {
		super(cause);
	}

}
