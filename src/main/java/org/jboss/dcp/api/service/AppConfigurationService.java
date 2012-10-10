/*
 * JBoss, Home of Professional Open Source
 * Copyright 2012 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 */
package org.jboss.dcp.api.service;

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

import org.jboss.dcp.api.model.AppConfiguration;
import org.jboss.dcp.api.model.AppConfiguration.ClientType;

/**
 * Application configuration service
 * 
 * @author Libor Krzyzanek
 */
@Named
@ApplicationScoped
@Singleton
@Startup
public class AppConfigurationService {

	@Inject
	private Logger log;

	private AppConfiguration appConfiguration;

	public AppConfiguration getAppConfiguration() {
		return appConfiguration;
	}

	@PostConstruct
	public void loadConfig() throws IOException {
		Properties prop = new Properties();
		InputStream inStream = AppConfigurationService.class.getResourceAsStream("/app.properties");
		try {
			prop.load(inStream);
			inStream.close();
		} catch (IOException e) {
			log.log(Level.INFO, "Cannot load app.properties", e);
			throw e;
		}

		appConfiguration = new AppConfiguration();

		String clientType = prop.getProperty("es.client.type", "transport");
		if ("embedded".equals(clientType)) {
			appConfiguration.setClientType(ClientType.EMBEDDED);
		} else {
			appConfiguration.setClientType(ClientType.TRANSPORT);
		}

		log.info("Data path: " + prop.getProperty("es.client.embedded.data.path"));

		appConfiguration.setAppDataPath(prop.getProperty("es.client.embedded.data.path"));
		appConfiguration.setProviderCreateInitData(Boolean.parseBoolean(prop.getProperty("provider.createInitData",
				"false")));

	}
}
