/*
 * JBoss, Home of Professional Open Source
 * Copyright 2014 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 */
package org.searchisko.api.indexer;

import javax.ejb.ObjectNotFoundException;

/**
 * Interface of class representing handler of indexer of given type.
 * 
 * @author Vlastimil Elias (velias at redhat dot com)
 */
public interface IndexerHandler {

	/**
	 * Force reindex for indexer of given name.
	 * 
	 * @param indexerName to force reindex for
	 * @throws ObjectNotFoundException if indexer of given name doesn't exist.
	 */
	public void forceReindex(String indexerName) throws ObjectNotFoundException;

	/**
	 * Get status information for indexer of given name
	 * 
	 * @param indexerName to get status for
	 * @return status information (must be convertable to JSON by JAX-RS)
	 * @throws ObjectNotFoundException if indexer of given name doesn't exist.
	 */
	public Object getStatus(String indexerName) throws ObjectNotFoundException;

	/**
	 * Stop indexer of given name. You have to use {@link #restart(String)} call to start it again.
	 * 
	 * @param indexerName to stop
	 * @throws ObjectNotFoundException if indexer of given name doesn't exist.
	 * @see #restart(String)
	 */
	public void stop(String indexerName) throws ObjectNotFoundException;

	/**
	 * Restart indexer of given name. Restart means it is stopped (if not stopped before), configuration is reloaded, and
	 * then it is started again.
	 * 
	 * @param indexerName to restart
	 * @throws ObjectNotFoundException if indexer of given name doesn't exist.
	 * @see #stop(String)
	 */
	public void restart(String indexerName) throws ObjectNotFoundException;

}
