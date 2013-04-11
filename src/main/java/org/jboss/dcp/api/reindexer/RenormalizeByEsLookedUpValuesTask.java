/*
 * JBoss, Home of Professional Open Source
 * Copyright 2013 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 */
package org.jboss.dcp.api.reindexer;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.elasticsearch.action.admin.indices.flush.FlushRequest;
import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.settings.SettingsException;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.index.query.FilterBuilder;
import org.elasticsearch.index.query.OrFilterBuilder;
import org.elasticsearch.index.query.TermsFilterBuilder;
import org.elasticsearch.search.SearchHit;
import org.jboss.dcp.api.DcpContentObjectFields;
import org.jboss.dcp.api.service.ProviderService;
import org.jboss.dcp.api.service.SearchClientService;
import org.jboss.dcp.api.tasker.Task;
import org.jboss.elasticsearch.tools.content.ESLookupValuePreprocessor;
import org.jboss.elasticsearch.tools.content.InvalidDataException;
import org.jboss.elasticsearch.tools.content.StructuredContentPreprocessor;
import org.jboss.elasticsearch.tools.content.StructuredContentPreprocessorFactory;

/**
 * Task used to update document in ES search indices by selecting them over values in fields which are sources for
 * normalization from some es lookup value (org.jboss.elasticsearch.tools.content.ESLookupValuePreprocessor used in type
 * configuration). Used for example when some contributor configuration is extended with new identifier which must be
 * bound to this contributor.
 * <p>
 * All documents for given value in given lookup field are loaded from all ES indices (only indices where DCP content is
 * stored), all preprocessors are applied to content, and then it is stored back to the ES index.
 * 
 * @author Vlastimil Elias (velias at redhat dot com)
 */
public class RenormalizeByEsLookedUpValuesTask extends Task {

	protected ProviderService providerService;
	protected SearchClientService searchClientService;
	private static final long ES_SCROLL_KEEPALIVE = 60 * 1000;

	// configuration fields
	protected String lookupIndex;
	protected String lookupType;
	protected String lookupField;
	protected String[] esValues;

	/**
	 * @param providerService
	 * @param searchClientService
	 * @param lookupIndex index over which is value looked up
	 * @param lookupType document type over which is value looked up
	 * @param lookupField name of lookup field in lookupType document
	 * @param esValues values of lookup field to reindex documents for
	 */
	public RenormalizeByEsLookedUpValuesTask(ProviderService providerService, SearchClientService searchClientService,
			String lookupIndex, String lookupType, String lookupField, String[] esValues) {
		this.providerService = providerService;
		this.searchClientService = searchClientService;
		this.lookupIndex = lookupIndex;
		this.lookupType = lookupType;
		this.lookupField = lookupField;
		this.esValues = esValues;
	}

	/**
	 * Constructor for unit tests.
	 */
	protected RenormalizeByEsLookedUpValuesTask() {
	}

	@Override
	public void performTask() throws Exception {

		try {
			Client client = searchClientService.getClient();
			List<Map<String, Object>> providers = providerService.getAll();

			for (Map<String, Object> providerDef : providers) {

				Map<String, Map<String, Object>> allTypes = ProviderService.extractAllContentTypes(providerDef);
				if (allTypes != null) {
					for (String dcpContentType : allTypes.keySet()) {
						if (isCanceledOrInterrupted()) {
							return;
						}
						Map<String, Object> typeDef = providerService.findContentType(dcpContentType);
						try {
							Set<String> esFields = takeLookedUpEsFields(typeDef, dcpContentType);
							if (esFields != null && !esFields.isEmpty()) {
								writeTaskLog("Going to process dcp_content_type " + dcpContentType);

								String indexName = ProviderService.extractIndexName(typeDef, dcpContentType);
								SearchRequestBuilder srb = client.prepareSearch(indexName)
										.setTypes(ProviderService.extractIndexType(typeDef, dcpContentType)).addField("_source");
								Set<FilterBuilder> searchFilters = new HashSet<FilterBuilder>();
								for (String esField : esFields) {
									searchFilters.add(new TermsFilterBuilder(esField, esValues));
								}
								srb.setFilter(new OrFilterBuilder(searchFilters.toArray(new FilterBuilder[searchFilters.size()])));
								srb.setScroll(new TimeValue(ES_SCROLL_KEEPALIVE)).setSearchType(SearchType.SCAN);

								SearchResponse scrollResp = srb.execute().actionGet();
								int i = 0;
								if (scrollResp.hits().totalHits() > 0) {
									scrollResp = executeESScrollSearchNextRequest(client, scrollResp);
									while (scrollResp.hits().hits().length > 0) {
										BulkRequestBuilder brb = client.prepareBulk();
										for (SearchHit hit : scrollResp.getHits()) {
											if (isCanceledOrInterrupted()) {
												writeTaskLog("Processed " + i + " documents then cancelled.");
												return;
											}
											i++;
											performHitProcessing(client, brb, hit);
										}
										brb.execute().actionGet();
										if (isCanceledOrInterrupted()) {
											writeTaskLog("Processed " + i + " documents then cancelled.");
											return;
										}
										scrollResp = executeESScrollSearchNextRequest(client, scrollResp);
									}
									client.admin().indices().flush(new FlushRequest(indexName)).actionGet();
								}
								writeTaskLog("Processed " + i + " documents.");
							}
						} catch (IllegalArgumentException e) {
							writeTaskLog("ERROR: Bad configuration of some 'input_preprocessors' for dcp_content_type="
									+ dcpContentType + ". Cause: " + e.getMessage());
						} catch (ClassCastException e) {
							writeTaskLog("ERROR: Bad configuration structure of some 'input_preprocessors' for dcp_content_type="
									+ dcpContentType + ". Cause: " + e.getMessage());
						}
					}
				}
			}
		} catch (SettingsException e) {
			throw new Exception(e.getMessage());
		}
	}

	protected Set<String> takeLookedUpEsFields(Map<String, Object> typeDef, String dcpContentType) {
		List<Map<String, Object>> preprocessorsDef = ProviderService.extractPreprocessors(typeDef, dcpContentType);
		Set<String> ret = new HashSet<String>();
		if (preprocessorsDef != null) {
			List<StructuredContentPreprocessor> preprocessors = StructuredContentPreprocessorFactory.createPreprocessors(
					preprocessorsDef, searchClientService.getClient());
			for (StructuredContentPreprocessor preprocessor : preprocessors) {
				if (preprocessor instanceof ESLookupValuePreprocessor) {
					ESLookupValuePreprocessor pp = (ESLookupValuePreprocessor) preprocessor;
					if (pp.getSourceField() != null && lookupIndex.equals(pp.getIndexName())
							&& lookupType.equals(pp.getIndexType()) && lookupField.equals(pp.getIdxSearchField())) {
						List<String> sb = pp.getSourceBases();
						if (sb == null || sb.isEmpty()) {
							ret.add(pp.getSourceField());
						} else {
							for (String base : sb) {
								ret.add(base + "." + pp.getSourceField());
							}
						}
					}
				}
			}
		}
		return ret;
	}

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
				writeTaskLog("ERROR: Data error from preprocessors execution so document " + id + " is skipped: "
						+ e.getMessage());
				return;
			}
			// put content back into search subsystem
			brb.add(client.prepareIndex(ProviderService.extractIndexName(typeDef, dcpContentType),
					ProviderService.extractIndexType(typeDef, dcpContentType), id).setSource(content));
		}
	}

	private SearchResponse executeESScrollSearchNextRequest(Client client, SearchResponse scrollResp) {
		return client.prepareSearchScroll(scrollResp.getScrollId()).setScroll(new TimeValue(ES_SCROLL_KEEPALIVE)).execute()
				.actionGet();
	}

}
