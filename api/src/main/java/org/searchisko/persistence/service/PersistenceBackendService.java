/*
 * JBoss, Home of Professional Open Source
 * Copyright 2012 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 */
package org.searchisko.persistence.service;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import javax.ejb.Lock;
import javax.ejb.LockType;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;
import javax.inject.Named;
import javax.persistence.EntityManager;

import org.searchisko.api.service.AppConfigurationService;
import org.searchisko.api.service.ProviderService;
import org.searchisko.persistence.jpa.model.*;

/**
 * Service for persistence backend. It produces {@link EntityService}s for distinct types of entities Searchisko needs
 * to store.<br/>
 * Persistence storage is implemented via JPA now, see {@link JpaEntityService}.
 * 
 * @author Libor Krzyzanek
 * @see JpaEntityService
 * 
 */
@Named
@Singleton
@Lock(LockType.READ)
@ApplicationScoped
@Startup
public class PersistenceBackendService {

	@Inject
	protected AppConfigurationService appConfigurationService;

	@Inject
	protected Logger log;

	@Inject
	protected EntityManager em;

	@Produces
	@Named("providerServiceBackend")
	@ApplicationScoped
	public EntityService produceProviderService() {
		JpaEntityService<Provider> serv = new JpaEntityService<>(em, new ProviderConverter(), Provider.class);

		if (appConfigurationService.getAppConfiguration().isProviderCreateInitData()) {
			final String initialProviderName = "jbossorg";
			Map<String, Object> initialProvider = serv.get(initialProviderName);
			if (initialProvider == null) {
				log.info("Provider entity doesn't exists. Creating initial entity for first authentication.");
				Map<String, Object> jbossorgEntity = new HashMap<>();
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
	@ApplicationScoped
	public EntityService produceProjectService() {
		return new JpaEntityService<>(em, new ProjectConverter(), Project.class);
	}

	@Produces
	@Named("contributorServiceBackend")
	@ApplicationScoped
	public EntityService produceContributorService() {
		return new JpaEntityService<>(em, new ContributorConverter(), Contributor.class);
	}

	@Produces
	@Named("configServiceBackend")
	@ApplicationScoped
	public EntityService produceConfigService() {
		return new JpaEntityService<>(em, new ConfigConverter(), Config.class);
	}

	@Produces
	@Named("queryServiceBackend")
	@ApplicationScoped
	public EntityService produceQueryService() {
		return new JpaEntityService<>(em, new QueryConverter(), Query.class);
	}

}
