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
import javax.enterprise.inject.Produces;
import javax.inject.Inject;
import javax.inject.Named;

import org.searchisko.api.model.AppConfiguration;
import org.searchisko.api.model.AppConfiguration.ClientType;

/**
 * Application configuration service
 * 
 * @author Libor Krzyzanek
 * @author Vlastimil Elias (velias at redhat dot com)
 */
@Named
@ApplicationScoped
@Singleton
@Startup
public class AppConfigurationService {

	@Inject
	protected Logger log;

	protected AppConfiguration appConfiguration;

	@Produces
	public AppConfiguration getAppConfiguration() {
		return appConfiguration;
	}

	@PostConstruct
	public void loadConfig() throws IOException {
		loadConfig("/app.properties");
	}

	protected void loadConfig(String filename) throws IOException {
		Properties prop = new Properties();
		InputStream inStream = AppConfigurationService.class.getResourceAsStream(filename);
		if (inStream == null) {
			log.log(Level.WARNING, "Cannot load " + filename + " because not found");
			throw new IOException("Cannot load " + filename + " because not found");
		}
		try {
			prop.load(inStream);
		} catch (IOException e) {
			log.log(Level.WARNING, "Cannot load " + filename, e);
			throw e;
		} finally {
			inStream.close();
		}

		inStream = AppConfigurationService.class
				.getResourceAsStream(filename.replace(".properties", "-overlay.properties"));
		if (inStream != null) {
			try {
				prop.load(inStream);
			} catch (IOException e) {
				log.log(Level.WARNING, "Cannot load app.properties", e);
			} finally {
				inStream.close();
			}
		}
		appConfiguration = new AppConfiguration(prop.getProperty("es.client.embedded.data.path"));

		String clientType = prop.getProperty("es.client.type", "transport");
		if ("embedded".equals(clientType)) {
			appConfiguration.setClientType(ClientType.EMBEDDED);
		} else {
			appConfiguration.setClientType(ClientType.TRANSPORT);
		}

		appConfiguration.setProviderCreateInitData(Boolean.parseBoolean(prop
				.getProperty("provider.createInitData", "false")));
		appConfiguration.setContributorProfileUpdateThreshold(Integer.parseInt(prop.getProperty(
				"contributorprofile.updatethreshold", "10")));

		AppConfiguration.ContributorProfileProviderConfig cppc = new AppConfiguration.ContributorProfileProviderConfig(
				prop.getProperty("contributorprofile.provider.urlbase"),
				prop.getProperty("contributorprofile.provider.username"),
				prop.getProperty("contributorprofile.provider.password"));
		appConfiguration.setContributorProfileProviderConfig(cppc);

		log.log(Level.INFO, "App Configuration: {0}", appConfiguration);
	}
}
