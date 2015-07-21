/*
 * JBoss, Home of Professional Open Source
 * Copyright 2012 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 */
package org.searchisko.api.model;

import java.io.IOException;
import java.util.Properties;

import javax.annotation.PostConstruct;
import javax.ejb.Lock;
import javax.ejb.LockType;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Named;

import org.searchisko.api.util.SearchUtils;

/**
 * Configuration object for individual servlet search timeouts.
 *
 * @author Lukas Vlcek
 * @author Libor Krzyzanek
 */
@Named
@ApplicationScoped
@Singleton
@Startup
@Lock(LockType.READ)
public class TimeoutConfiguration {

	private int stats;

	private int search;

	private int ping;

	private int documentDetail;

	private int documentReferencesCount;

	private int documentReferencesSearch;

	private int subjectPatternCount;

	private int subjectPatternSearch;

	@PostConstruct
	public void init() throws IOException {
		Properties prop = SearchUtils.loadProperties("/search_timeouts.properties");

		stats = Integer.parseInt(prop.getProperty("stats", "2"));
		search = Integer.parseInt(prop.getProperty("search", "10"));
		ping = Integer.parseInt(prop.getProperty("ping", "10"));
		documentDetail = Integer.parseInt(prop.getProperty("documentDetail", "10"));
		documentReferencesCount = Integer.parseInt(prop.getProperty("documentReferencesCount", "10"));
		documentReferencesSearch = Integer.parseInt(prop.getProperty("documentReferencesSearch", "10"));
		subjectPatternCount = Integer.parseInt(prop.getProperty("subjectPatternCount", "10"));
		subjectPatternSearch = Integer.parseInt(prop.getProperty("subjectPatternSearch", "10"));
	}

	public int stats() {
		return stats;
	}

	public int search() {
		return search;
	}

	public int ping() {
		return ping;
	}

	public int documentDetail() {
		return documentDetail;
	}

	public int documentReferencesCount() {
		return documentReferencesCount;
	}

	public int documentReferencesSearch() {
		return documentReferencesSearch;
	}

	public int subjectPatternCount() {
		return subjectPatternCount;
	}

	public int subjectPatternSearch() {
		return subjectPatternSearch;
	}
}
