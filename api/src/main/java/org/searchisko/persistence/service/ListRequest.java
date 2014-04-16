/*
 * JBoss, Home of Professional Open Source
 * Copyright 2012 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 */
package org.searchisko.persistence.service;

import java.util.List;
import java.util.Map;

/**
 * Interface of classes used by persistence services for efficient iterations over large sets of data.
 * 
 * @author Vlastimil Elias (velias at redhat dot com)
 * 
 * @see EntityService#listRequestInit()
 * @see EntityService#listRequestNext(ListRequest)
 * @see ContentPersistenceService#listRequestInit(String)
 * @see ContentPersistenceService#listRequestNext(ListRequest)
 */
public interface ListRequest {

	/**
	 * Return true if request has some content to process, false otherwise.
	 * 
	 * @return true if there is some content to process. False if there is not any other content so next call to
	 *         <code>listRequestNext(ListRequest)</code> has no meaning.
	 */
	public boolean hasContent();

	/**
	 * Get content to be processed in this iteration.
	 * 
	 * @return content to process.
	 */
	public List<ContentTuple<String, Map<String, Object>>> content();

}