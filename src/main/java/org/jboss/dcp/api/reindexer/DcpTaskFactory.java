/*
 * JBoss, Home of Professional Open Source
 * Copyright 2013 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 */
package org.jboss.dcp.api.reindexer;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.ejb.Singleton;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.inject.Named;

import org.jboss.dcp.api.service.ProviderService;
import org.jboss.dcp.api.service.SearchClientService;
import org.jboss.dcp.api.tasker.Task;
import org.jboss.dcp.api.tasker.TaskConfigurationException;
import org.jboss.dcp.api.tasker.TaskFactory;
import org.jboss.dcp.api.tasker.UnsupportedTaskException;
import org.jboss.dcp.persistence.service.ContentPersistenceService;

/**
 * {@link TaskFactory} for DCP tasks. It's CDI singleton bean because needs to inject some other DCP components to pass
 * them into tasks.
 * 
 * @author Vlastimil Elias (velias at redhat dot com)
 */
@Named
@ApplicationScoped
@Singleton
public class DcpTaskFactory implements TaskFactory {

	public static final String CFG_DCP_CONTENT_TYPE = "dcp_content_type";

	@Inject
	protected ContentPersistenceService contentPersistenceService;

	@Inject
	protected ProviderService providerService;

	@Inject
	protected SearchClientService searchClientService;

	@Override
	public List<String> listSupportedTaskTypes() {
		List<String> ret = new ArrayList<String>();
		for (DcpTaskTypes t : DcpTaskTypes.values()) {
			ret.add(t.getTaskType());
		}
		return ret;
	}

	@Override
	public Task createTask(String taskType, Map<String, Object> taskConfig) throws UnsupportedTaskException,
			TaskConfigurationException {
		switch (DcpTaskTypes.getInstance(taskType)) {
		case REINDEX_FROM_PERSISTENCE:
			return createReindexFromPersistenceTask(taskConfig);

		}
		throw new UnsupportedTaskException(taskType);
	}

	private Task createReindexFromPersistenceTask(Map<String, Object> taskConfig) throws TaskConfigurationException {
		String dcpContentType = getMandatoryConfigString(taskConfig, CFG_DCP_CONTENT_TYPE);
		Map<String, Object> typeDef = providerService.findContentType(dcpContentType);
		if (typeDef == null) {
			throw new TaskConfigurationException("Content type '" + dcpContentType + "' doesn't exists.");
		}
		if (!ProviderService.extractPersist(typeDef)) {
			throw new TaskConfigurationException("Content type '" + dcpContentType + "' is not persisted.");
		}
		return new ReindexFromPersistenceTask(contentPersistenceService, providerService, searchClientService,
				dcpContentType);
	}

	private String getMandatoryConfigString(Map<String, Object> taskConfig, String propertyName)
			throws TaskConfigurationException {
		if (taskConfig == null)
			throw new TaskConfigurationException(propertyName + " configuration property must be defined");

		Object val = taskConfig.get(propertyName);

		if (val == null || val.toString().trim().length() == 0)
			throw new TaskConfigurationException(propertyName + " configuration property must be defined");

		return val.toString().trim();
	}
}
