/*
 * JBoss, Home of Professional Open Source
 * Copyright 2012 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 */
package org.jboss.dcp.persistence.service;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Logger;

import javax.annotation.PostConstruct;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;
import javax.inject.Named;
import javax.persistence.EntityManager;

import org.jboss.dcp.api.service.AppConfigurationService;
import org.jboss.dcp.api.service.ElasticsearchClientService;
import org.jboss.dcp.api.service.ProviderService;
import org.jboss.dcp.api.util.SearchUtils;
import org.jboss.dcp.persistence.jpa.model.Config;
import org.jboss.dcp.persistence.jpa.model.ConfigConverter;
import org.jboss.dcp.persistence.jpa.model.Contributor;
import org.jboss.dcp.persistence.jpa.model.ContributorConverter;
import org.jboss.dcp.persistence.jpa.model.Project;
import org.jboss.dcp.persistence.jpa.model.ProjectConverter;
import org.jboss.dcp.persistence.jpa.model.Provider;
import org.jboss.dcp.persistence.jpa.model.ProviderConverter;

/**
 * Service for persistence backend.<br/>
 * Now persistence storage is implemented as embedded elasticsearch node
 * 
 * @author Libor Krzyzanek
 * 
 */
@Named
@Singleton
@ApplicationScoped
@Startup
public class PersistanceBackendService extends ElasticsearchClientService {

	@Inject
	protected AppConfigurationService appConfigurationService;

	@Inject
	protected Logger log;

	@Inject
	protected EntityManager em;

	// Everything is stored in one index
	protected static final String INDEX_NAME = "data";
	protected static final String INDEX_TYPE_PROVIDER = "provider";

	@PostConstruct
	public void init() throws Exception {
		Properties settings = SearchUtils.loadProperties("/persistance_settings.properties");

		node = createEmbeddedNode("persistance", settings);
		client = node.client();
	}

	@Produces
	@Named("providerServiceBackend")
	public EntityService produceProviderService() {
		JpaEntityService<Provider> serv = new JpaEntityService<Provider>(em, new ProviderConverter(), Provider.class);

		if (appConfigurationService.getAppConfiguration().isProviderCreateInitData()) {
			final String initialProviderName = "jbossorg";
			Map<String, Object> initialProvider = serv.get(initialProviderName);
			if (initialProvider == null) {
				log.info("Provider entity doesn't exists. Creating initial entity for first authentication.");
				Map<String, Object> jbossorgEntity = new HashMap<String, Object>();
				jbossorgEntity.put(ProviderService.NAME, initialProviderName);
				// initial password: jbossorgjbossorg
				jbossorgEntity.put(ProviderService.PASSWORD_HASH, "47dc8a4d65fe0cd5b1236b7e8612634e604a0c2f");
				jbossorgEntity.put(ProviderService.SUPER_PROVIDER, true);

				serv.create(initialProviderName, jbossorgEntity);
			}
		}

		return serv;
	}

	@Produces
	@Named("projectServiceBackend")
	public EntityService produceProjectService() {
		return new JpaEntityService<Project>(em, new ProjectConverter(), Project.class);
	}

	@Produces
	@Named("contributorServiceBackend")
	public EntityService produceContributorService() {
		// return new ElasticsearchEntityService(client, INDEX_NAME, "contributor", false);
		return new JpaEntityService<Contributor>(em, new ContributorConverter(), Contributor.class);

	}

	@Produces
	@Named("configServiceBackend")
	public EntityService produceConfigService() {
		// return new ElasticsearchEntityService(client, INDEX_NAME, "config", false);
		return new JpaEntityService<Config>(em, new ConfigConverter(), Config.class);

	}

}
