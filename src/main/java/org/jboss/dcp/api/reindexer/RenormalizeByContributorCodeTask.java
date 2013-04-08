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
 * Task used to update document in ES search indices by contributor code stored in dcp_contributors field. Used for
 * example when contributor configuration is splitted.
 * <p>
 * All documents for given contributor code are loaded from all ES indices with dcp content, all preprocessors are
 * applied to content, and then it is stored back to the ES index.
 * 
 * @author Vlastimil Elias (velias at redhat dot com)
 */
public class RenormalizeByContributorCodeTask extends RenormalizeTaskBase {

	protected String[] contributorCodes;

	/**
	 * @param providerService
	 * @param searchClientService
	 * @param contributorCodes codes of contributor to reindex documents for.
	 */
	public RenormalizeByContributorCodeTask(ProviderService providerService, SearchClientService searchClientService,
			String[] contributorCodes) {
		super(providerService, searchClientService);
		this.contributorCodes = contributorCodes;
	}

	/**
	 * Constructor for unit tests.
	 */
	protected RenormalizeByContributorCodeTask() {
	}

	@Override
	protected void addFilters(SearchRequestBuilder srb) {
		srb.setFilter(new TermsFilterBuilder(DcpContentObjectFields.DCP_CONTRIBUTORS, contributorCodes));
	}

}
