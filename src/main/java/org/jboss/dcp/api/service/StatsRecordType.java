/*
 * JBoss, Home of Professional Open Source
 * Copyright 2012 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 */
package org.jboss.dcp.api.service;

/**
 * Types of statistics records written over {@link StatsClientService}.
 * 
 * @author Vlastimil Elias (velias at redhat dot com)
 */
public enum StatsRecordType {

	SEARCH, DOCUMENT_DETAIL, DOCUMENT_REFS_COUNT, DOCUMENT_REFS_SEARCH, DOCUMENT_SUBJECT_COUNT, DOCUMENT_SUBJECT_SEARCH, SUBJECT_SUGGESTIONS, OPENSEARCH_SUGGESTIONS;

}
