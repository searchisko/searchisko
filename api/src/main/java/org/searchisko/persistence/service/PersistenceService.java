/*
 * JBoss, Home of Professional Open Source
 * Copyright 2012 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 */
package org.searchisko.persistence.service;

import javax.ejb.LocalBean;
import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.inject.Named;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Persistence service can provide basic statistics about persisted Entity objects.
 *
 * @author Lukas Vlcek
 */
@Named
@Stateless
@LocalBean
public class PersistenceService {

	@Inject
	protected JdbcContentPersistenceService jdbcContentPersistenceService;

	/**
	 * List all tables from persistence and number of records stored in them.
	 * <p/>
	 * Getting count from some tables may not be possible (or results into runtime errors).
	 * In such cases we should not get an exception but the count value should contain some
	 * error message.
	 *
	 * @return map containing table names as keys and number of records as values
	 */
	public Map<String, Object> getTableCounts() {
		Map<String, Object> ret = new LinkedHashMap<>();
		List<String> tableNames = jdbcContentPersistenceService.getAllTableNames();
		Collections.sort(tableNames);

		for (String tableName: tableNames) {
			try {
				ret.put(tableName, jdbcContentPersistenceService.rowCount(tableName));
			} catch (Throwable e) {
				ret.put(tableName, "ERROR: " + e.getMessage());
			}
		}

		return ret;
	}
}
