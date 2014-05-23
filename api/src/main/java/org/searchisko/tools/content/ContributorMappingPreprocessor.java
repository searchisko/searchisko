/*
 * JBoss, Home of Professional Open Source
 * Copyright 2014 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 */
package org.searchisko.tools.content;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.elasticsearch.client.Client;
import org.elasticsearch.common.settings.SettingsException;
import org.jboss.elasticsearch.tools.content.ESLookupValuePreprocessor;
import org.searchisko.api.ContentObjectFields;
import org.searchisko.api.service.ContributorService;

/**
 * {@link ESLookupValuePreprocessor} preconfigured for Contributor normalization. You have to define
 * <code>source_field</code>/<code>source_value</code> and <code>idx_search_field</code> config params in settings only
 * (and optionally <code>source_bases</code>). You can change <code>result_mapping</code> if you want, but default
 * config which fills {@value ContentObjectFields#SYS_CONTRIBUTORS}.
 * 
 * Example of configuration for this preprocessor for lookup of multiple values of same structure:
 * 
 * <pre>
 * { 
 *     "name"     : "Author mapper",
 *     "class"    : "org.searchisko.tools.content.ContributorMappingPreprocessor",
 *     "settings" : {
 *         "source_field"      : "author.email",
 *         "idx_search_field"  : "email"
 *     } 
 * }
 * </pre>
 * 
 * @author Vlastimil Elias (velias at redhat dot com)
 */
public class ContributorMappingPreprocessor extends ESLookupValuePreprocessor {

	protected static final List<Map<String, String>> resultMapping = new ArrayList<>();
	static {
		Map<String, String> rm_code = new HashMap<>();
		rm_code.put(CFG_idx_result_field, ContributorService.FIELD_CODE);
		rm_code.put(CFG_target_field, ContentObjectFields.SYS_CONTRIBUTORS);
		resultMapping.add(rm_code);
	}

	@Override
	public void init(String name, Client client, Map<String, Object> settings) throws SettingsException {
		settings.put(CFG_index_name, ContributorService.SEARCH_INDEX_NAME);
		settings.put(CFG_index_type, ContributorService.SEARCH_INDEX_TYPE);

		if (!settings.containsKey(CFG_result_mapping)) {
			settings.put(CFG_result_mapping, resultMapping);
		}
		super.init(name, client, settings);
	}
}
