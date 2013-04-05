/*
 * JBoss, Home of Professional Open Source
 * Copyright 2013 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 */
package org.jboss.dcp.api.reindexer;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.elasticsearch.action.admin.indices.flush.FlushRequest;
import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.client.Client;
import org.elasticsearch.index.query.TermsFilterBuilder;
import org.elasticsearch.search.SearchHit;
import org.jboss.dcp.api.DcpContentObjectFields;
import org.jboss.dcp.api.service.ProviderService;
import org.jboss.dcp.api.service.SearchClientService;
import org.jboss.elasticsearch.tools.content.InvalidDataException;

/**
 * Task used to update document in ES search indices by project key stored in dcp_project field. Used for example when
 * project name is changed. All documents for given project key are loaded from all ES indices with dcp content, all
 * preprocessors are applied to content, and then it is stored back to the ES index.
 * 
 * @author Vlastimil Elias (velias at redhat dot com)
 */
public class RenormalizeByProjectCodeTask extends ReindexingTaskBase {

	protected String projectCode;

	private List<String> indexName = new ArrayList<String>();
	private List<String> indexType = new ArrayList<String>();

	/**
	 * @param providerService
	 * @param searchClientService
	 * @param projectCode - optional code of project to reindex only documents for project with this code. Can be used
	 *          mainly when project name is changed.
	 */
	public RenormalizeByProjectCodeTask(ProviderService providerService, SearchClientService searchClientService, String projectCode) {
		super(providerService, searchClientService);
		this.projectCode = projectCode;
	}

	/**
	 * Constructor for unit tests.
	 */
	protected RenormalizeByProjectCodeTask() {
	}

	@Override
	protected void validateTaskConfiguration() throws Exception {
		List<Map<String, Object>> providers = providerService.getAll();

		for (Map<String, Object> providerDef : providers) {
			Map<String, Map<String, Object>> allTypes = ProviderService.extractAllContentTypes(providerDef);
			if (allTypes != null) {
				for (String dcpContentType : allTypes.keySet()) {
					Map<String, Object> typeDef = allTypes.get(dcpContentType);
					indexName.add(ProviderService.extractIndexName(typeDef, dcpContentType));
					indexType.add(ProviderService.extractIndexType(typeDef, dcpContentType));
				}
			}
		}
	}

	@Override
	protected SearchRequestBuilder prepareSearchRequest(Client client) {
		SearchRequestBuilder srb = client.prepareSearch(indexName.toArray(new String[indexName.size()]))
				.setTypes(indexType.toArray(new String[indexType.size()])).addField("_source");
		if (projectCode != null) {
			srb.setFilter(new TermsFilterBuilder(DcpContentObjectFields.DCP_PROJECT, projectCode));
		}
		return srb;
	}

	@Override
	protected void performHitProcessing(Client client, BulkRequestBuilder brb, SearchHit hit) {
		Map<String, Object> content = hit.getSource();
		String id = hit.getId();
		String dcpContentType = (String) content.get(DcpContentObjectFields.DCP_CONTENT_TYPE);
		Map<String, Object> typeDef = providerService.findContentType(dcpContentType);
		if (typeDef == null) {
			writeTaskLog("No type definition found for document id=" + id + " so is skipped");
		} else {
			try {
				// Run preprocessors to normalize mapped fields
				providerService.runPreprocessors(dcpContentType, ProviderService.extractPreprocessors(typeDef, dcpContentType),
						content);
			} catch (InvalidDataException e) {
				writeTaskLog("Data error from preprocessors execution so document " + id + " is skipped: " + e.getMessage());
				return;
			}
			// put content back into search subsystem
			brb.add(client.prepareIndex(ProviderService.extractIndexName(typeDef, dcpContentType),
					ProviderService.extractIndexType(typeDef, dcpContentType), id).setSource(content));
		}
	}

	@Override
	protected void performPostReindexingProcessing(Client client) {
		client.admin().indices().flush(new FlushRequest(indexName.toArray(new String[indexName.size()]))).actionGet();
	}

}
