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
 * Configuration for statistics client
 *
 * @author Libor Krzyzanek
 * @author Vlastimil Elias (velias at redhat dot com)
 */
@Named
@ApplicationScoped
@Singleton
@Startup
@Lock(LockType.READ)
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
		this.async = true;
	}

	/**
	 * Constructor.
	 *
	 * @param enabled          to set
	 * @param useSearchCluster to set
	 * @param async            to set
	 */
	public StatsConfiguration(boolean enabled, boolean useSearchCluster, boolean async) {
		super();
		this.enabled = enabled;
		this.useSearchCluster = useSearchCluster;
		this.async = async;
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
