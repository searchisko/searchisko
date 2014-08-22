/*
 * JBoss, Home of Professional Open Source
 * Copyright 2013 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 */
package org.searchisko.api.reindexer;

import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.index.query.TermsFilterBuilder;
import org.searchisko.api.service.ProviderService;
import org.searchisko.api.service.SearchClientService;

/**
 * Task used to update document in ES search indices by selecting them over values in defined field. Used for example
 * when project name is changed (so we reindex over project code in sys_project field), or contributor configuration is
 * split (so we reindex over sys_contributors value).
 * <p>
 * All documents for given value in given field are loaded from all ES indices (only indices where Searchisko content is
 * stored), all preprocessors are applied to content, and then it is stored back to the ES index.
 *
 * @author Vlastimil Elias (velias at redhat dot com)
 */
public class RenormalizeByEsValueTask extends RenormalizeTaskBase {

	protected String esField;
	protected String[] esValues;

	/**
	 * @param providerService
	 * @param searchClientService
	 * @param esField field in ElasticSearch indices to search over
	 * @param esValues values for given esField to reindex documents for
	 */
	public RenormalizeByEsValueTask(ProviderService providerService, SearchClientService searchClientService,
			String esField, String[] esValues) {
		super(providerService, searchClientService);
		this.esField = esField;
		this.esValues = esValues;
	}

	/**
	 * Constructor for unit tests.
	 */
	protected RenormalizeByEsValueTask() {
	}

	@Override
	protected void addFilters(SearchRequestBuilder srb) {
		srb.setPostFilter(new TermsFilterBuilder(esField, esValues));
	}

}
