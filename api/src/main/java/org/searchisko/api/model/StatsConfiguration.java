/*
 * JBoss, Home of Professional Open Source
 * Copyright 2012 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 */
package org.searchisko.api.model;

import javax.annotation.PostConstruct;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Named;
import java.io.IOException;
import java.util.Properties;

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

	public static final String FILE = "/stats_client_configuration.properties";

	protected boolean enabled;

	protected boolean useSearchCluster;

	protected boolean async;

	protected Properties settingsProps = null;

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

	public boolean isAsync() {
		return async;
	}

	public void setAsync(boolean async) {
		this.async = async;
	}

	public Properties getSettingsProps() {
		return settingsProps;
	}

	@PostConstruct
	public void init() throws IOException {
		settingsProps = SearchUtils.loadProperties(FILE);
		enabled = Boolean.parseBoolean(settingsProps.getProperty("stats.enabled", "true"));
		useSearchCluster = Boolean.parseBoolean(settingsProps.getProperty("stats.useSearchCluster", "true"));
		async = Boolean.parseBoolean(settingsProps.getProperty("stats.async", "true"));
	}

}
