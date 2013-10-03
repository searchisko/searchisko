/*
 * JBoss, Home of Professional Open Source
 * Copyright 2012 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 */
package org.searchisko.api.model;

import java.io.IOException;
import java.util.Properties;

import javax.annotation.PostConstruct;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Named;

import org.searchisko.api.util.SearchUtils;

/**
 * Configuration for statistics client
 *
 * @author Libor Krzyzanek
 * @author Vlastimil Elias (velias at redhat dot com)
 *
 */
@Named
@ApplicationScoped
@Singleton
@Startup
public class StatsConfiguration {

	protected boolean enabled;

	protected boolean useSearchCluster;

	/**
	 * Default constructor.
	 */
	public StatsConfiguration() {

	}

	/**
	 * Constructor.
	 *
	 * @param enabled to set
	 */
	public StatsConfiguration(boolean enabled) {
		super();
		this.enabled = enabled;
	}

	/**
	 * Constructor.
	 *
	 * @param enabled to set
	 */
	public StatsConfiguration(boolean enabled, boolean useSearchCluster) {
		super();
		this.enabled = enabled;
		this.useSearchCluster = useSearchCluster;
	}

	public boolean enabled() {
		return this.enabled;
	}

	public boolean isUseSearchCluster() {
		return useSearchCluster;
	}

	@PostConstruct
	public void init() throws IOException {
		Properties prop = SearchUtils.loadProperties("/stats_clinet_configuration.properties");
		enabled = Boolean.parseBoolean(prop.getProperty("stats.enabled", "true"));
		useSearchCluster = Boolean.parseBoolean(prop.getProperty("stats.useSearchCluster", "true"));
	}

}
