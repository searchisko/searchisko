/*
 * JBoss, Home of Professional Open Source
 * Copyright 2012 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 */
package org.searchisko.api.service;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.PostConstruct;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.inject.Named;

/**
 * Service used to provide system info.
 *
 * @author Vlastimil Elias (velias at redhat dot com)
 */
@Named
@ApplicationScoped
@Singleton
@Startup
public class SystemInfoService {

	@Inject
	protected Logger log;

	Properties prop = new Properties();

	@PostConstruct
	public void init() throws IOException {
		InputStream inStream = AppConfigurationService.class.getResourceAsStream("/systeminfo.properties");
		if (inStream == null) {
			log.log(Level.WARNING, "Cannot load systeminfo.properties because not found");
			throw new IOException("Cannot load systeminfo.properties because not found");
		}
		try {
			prop.load(inStream);
		} catch (IOException e) {
			log.log(Level.WARNING, "Cannot load systeminfo.properties", e);
			throw e;
		} finally {
			inStream.close();
		}

		log.log(Level.INFO, "Starting Searchisko version {0} built at {1}", new Object[] { getVersion(), getBuildTimestamp() });
	}

	public String getVersion() {
		return prop.getProperty("version");
	}

	public String getBuildTimestamp() {
		return prop.getProperty("build-timestamp");
	}
}
