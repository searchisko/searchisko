/*
 * JBoss, Home of Professional Open Source
 * Copyright 2012 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 */
package org.jboss.dcp.api.model;

import java.io.IOException;
import java.util.Properties;

import javax.annotation.PostConstruct;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Named;

import org.jboss.dcp.api.util.SearchUtils;

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

	private boolean enabled;

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

	public boolean enabled() {
		return this.enabled;
	}

	@PostConstruct
	public void init() throws IOException {
		Properties prop = SearchUtils.loadProperties("/stats_clinet_configuration.properties");
		enabled = Boolean.parseBoolean(prop.getProperty("stats.enabled", "true"));
	}

}
