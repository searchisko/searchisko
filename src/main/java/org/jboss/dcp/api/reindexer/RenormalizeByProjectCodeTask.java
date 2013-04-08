/*
 * JBoss, Home of Professional Open Source
 * Copyright 2013 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 */
package org.jboss.dcp.api.reindexer;

import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.index.query.TermsFilterBuilder;
import org.jboss.dcp.api.DcpContentObjectFields;
import org.jboss.dcp.api.service.ProviderService;
import org.jboss.dcp.api.service.SearchClientService;

/**
 * Task used to update document in ES search indices by project code stored in dcp_project field. Used for example when
 * project name is changed.
 * <p>
 * All documents for given project code are loaded from all ES indices with dcp content, all preprocessors are applied
 * to content, and then it is stored back to the ES index.
 * 
 * @author Vlastimil Elias (velias at redhat dot com)
 */
public class RenormalizeByProjectCodeTask extends RenormalizeTaskBase {

	protected String[] projectCodes;

	/**
	 * @param providerService
	 * @param searchClientService
	 * @param projectCodes code of project to reindex documents for.
	 */
	public RenormalizeByProjectCodeTask(ProviderService providerService, SearchClientService searchClientService,
			String[] projectCodes) {
		super(providerService, searchClientService);
		this.projectCodes = projectCodes;
	}

	/**
	 * Constructor for unit tests.
	 */
	protected RenormalizeByProjectCodeTask() {
	}

	@Override
	protected void addFilters(SearchRequestBuilder srb) {
		srb.setFilter(new TermsFilterBuilder(DcpContentObjectFields.DCP_PROJECT, projectCodes));
	}

}
