/*
 * JBoss, Home of Professional Open Source
 * Copyright 2013 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 */
package org.jboss.dcp.api.reindexer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.ejb.Singleton;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.inject.Named;

import org.jboss.dcp.api.DcpContentObjectFields;
import org.jboss.dcp.api.service.ProviderService;
import org.jboss.dcp.api.service.SearchClientService;
import org.jboss.dcp.api.tasker.Task;
import org.jboss.dcp.api.tasker.TaskConfigurationException;
import org.jboss.dcp.api.tasker.TaskFactory;
import org.jboss.dcp.api.tasker.UnsupportedTaskException;
import org.jboss.dcp.persistence.service.ContentPersistenceService;

/**
 * {@link TaskFactory} for DCP tasks. It's CDI singleton bean because needs to inject some other DCP components to pass
 * them into tasks. <br>
 * TODO TASKS types and configuration documentation in REST API doc
 * 
 * @author Vlastimil Elias (velias at redhat dot com)
 */
@Named
@ApplicationScoped
@Singleton
public class DcpTaskFactory implements TaskFactory {

	public static final String CFG_DCP_CONTENT_TYPE = "dcp_content_type";
	public static final String CFG_PROJECT_CODE = "project_code";
	public static final String CFG_CONTRIBUTOR_CODE = "contributor_code";

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
		case RENORMALIZE_BY_CONTENT_TYPE:
			return createRenormalizeByContentTypeTask(taskConfig);
		case RENORMALIZE_BY_PROJECT_CODE:
			return createRenormalizeByProjectCodeTask(taskConfig);
		case RENORMALIZE_BY_CONTRIBUTOR_CODE:
			return createRenormalizeByContributorCodeTask(taskConfig);
		}
		throw new UnsupportedTaskException(taskType);
	}

	private Task createRenormalizeByContentTypeTask(Map<String, Object> taskConfig) throws TaskConfigurationException {
		String dcpContentType = getMandatoryConfigString(taskConfig, CFG_DCP_CONTENT_TYPE);
		Map<String, Object> typeDef = providerService.findContentType(dcpContentType);
		if (typeDef == null) {
			throw new TaskConfigurationException("Content type '" + dcpContentType + "' doesn't exists.");
		}
		return new RenormalizeByContentTypeTask(providerService, searchClientService, dcpContentType);
	}

	private Task createRenormalizeByProjectCodeTask(Map<String, Object> taskConfig) throws TaskConfigurationException {
		return new RenormalizeByEsValueTask(providerService, searchClientService, DcpContentObjectFields.DCP_PROJECT,
				getMandatoryConfigStringArray(taskConfig, CFG_PROJECT_CODE));
	}

	private Task createRenormalizeByContributorCodeTask(Map<String, Object> taskConfig) throws TaskConfigurationException {
		return new RenormalizeByEsValueTask(providerService, searchClientService, DcpContentObjectFields.DCP_CONTRIBUTORS,
				getMandatoryConfigStringArray(taskConfig, CFG_CONTRIBUTOR_CODE));
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

		if (val == null || val.toString().trim().isEmpty())
			throw new TaskConfigurationException(propertyName + " configuration property must be defined");

		return val.toString().trim();
	}

	private String[] getMandatoryConfigStringArray(Map<String, Object> taskConfig, String propertyName)
			throws TaskConfigurationException {
		if (taskConfig == null)
			throw new TaskConfigurationException(propertyName + " configuration property must be defined");

		Object val = taskConfig.get(propertyName);

		if (val == null)
			throw new TaskConfigurationException(propertyName + " configuration property must be defined");

		Set<String> ret = new LinkedHashSet<String>();
		if (val instanceof Collection) {
			for (Object o : ((Collection<?>) val)) {
				if (o != null) {
					addToHash(ret, o.toString());
				}
			}
		} else if (val instanceof String[]) {
			for (String o : (String[]) val) {
				if (o != null) {
					addToHash(ret, o);
				}
			}
		} else {
			addToHash(ret, val.toString());
		}

		if (ret.isEmpty())
			throw new TaskConfigurationException(propertyName + " configuration property must be defined");

		return ret.toArray(new String[ret.size()]);
	}

	private void addToHash(Set<String> ret, String string) {
		if (string != null) {
			string = string.trim();
			if (!string.isEmpty()) {
				ret.add(string);
			}
		}
	}

	private String getConfigString(Map<String, Object> taskConfig, String propertyName) throws TaskConfigurationException {
		if (taskConfig == null)
			throw new TaskConfigurationException(propertyName + " configuration property must be defined");

		Object val = taskConfig.get(propertyName);

		if (val == null || val.toString().trim().isEmpty())
			return null;
		return val.toString().trim();
	}
}
