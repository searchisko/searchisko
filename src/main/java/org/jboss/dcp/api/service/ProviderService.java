/*
 * JBoss, Home of Professional Open Source
 * Copyright 2012 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 */
package org.jboss.dcp.api.service;

import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.inject.Named;

import org.elasticsearch.common.settings.SettingsException;
import org.jboss.dcp.persistence.service.EntityService;
import org.jboss.elasticsearch.tools.content.StructuredContentPreprocessor;
import org.jboss.elasticsearch.tools.content.StructuredContentPreprocessorFactory;

/**
 * Service related to Content Provider, mainly reading provider configuration files.
 * 
 * @author Libor Krzyzanek
 * @author Vlastimil Elias (velias at redhat dot com)
 * 
 */
@Named
@ApplicationScoped
public class ProviderService {

	/**
	 * Key for provider's name which is unique identifier - can be same as provdier's ID. Also used during Configuration
	 * read to get index name.
	 */
	public static final String NAME = "name";

	/** Configuration file Key for password hash **/
	public static final String PASSWORD_HASH = "pwd_hash";

	/** Configuration Key for flag if provider is super provider (administrator of DCP) **/
	public static final String SUPER_PROVIDER = "super_provider";

	/** Configuration Key for content type **/
	public static final String TYPE = "type";
	/** Configuration Key for index settings **/
	public static final String INDEX = "index";
	/** Configuration Key for dcp_type setting **/
	public static final String DCP_TYPE = "dcp_type";
	/** Configuration Key for Elastic Search indices **/
	public static final String SEARCH_INDICES = "search_indices";

	public static final String SEARCH_ALL_EXCLUDED = "search_all_excluded";

	@Inject
	protected Logger log;

	@Inject
	@Named("providerServiceBackend")
	protected EntityService entityService;

	@Inject
	protected SecurityService securityService;

	@Inject
	protected SearchClientService searchClientService;

	/**
	 * Check if password matches for given provider.
	 * 
	 * @param providerName name of provider
	 * @param password password to check
	 * @return true if provider name and password matches so it's authenticated
	 */
	public boolean authenticate(String providerName, String password) {
		if (providerName == null || password == null) {
			return false;
		}

		Map<String, Object> providerData = findProvider(providerName);
		if (providerData == null) {
			return false;
		}
		Object hash = providerData.get(PASSWORD_HASH);
		if (hash == null) {
			log.log(Level.SEVERE, "Provider {0} doesn't have any password hash defined.", providerName);
			return false;
		} else {
			return securityService.checkPwdHash(providerName, password, hash.toString());
		}
	}

	/**
	 * Check if requested provider is superprovider.
	 * 
	 * @param providerName name of provider to check
	 * @return true if it is superprovider
	 */
	public boolean isSuperProvider(String providerName) {
		Map<String, Object> providerData = findProvider(providerName);

		if (providerData == null)
			return false;

		Object superProviderFlag = providerData.get(SUPER_PROVIDER);

		return (superProviderFlag != null && Boolean.parseBoolean(superProviderFlag.toString()));
	}

	/**
	 * Find provider based on its name (called <code>dcp_content_provider</code>).
	 * 
	 * @param providerName of provider - DCP wide unique
	 * @return provider configuration
	 */
	public Map<String, Object> findProvider(String providerName) {
		return getEntityService().get(providerName);
	}

	/**
	 * Find 'provider content type' configuration based on its identifier (called <code>dcp_content_type</code>). Each
	 * 'provider content type' identifier have to be DCP wide unique (so must be defined only for one provider)!
	 * 
	 * @param typeName <code>dcp_content_type</code> to look for
	 * @return content type configuration structure or <code>null</code> if not found
	 */
	public Map<String, Object> findContentType(String typeName) {
		List<Map<String, Object>> allProviders = listAllProviders();
		if (allProviders != null) {
			for (Map<String, Object> providerDef : allProviders) {
				Map<String, Object> ct = extractContentType(providerDef, typeName);
				if (ct != null) {
					// TODO PROVIDERSERVICE cache
					return ct;
				}
			}
		}
		return null;
	}

	/**
	 * List configuration for all providers.
	 * 
	 * @return list with configurations for all providers
	 */
	public List<Map<String, Object>> listAllProviders() {
		return entityService.getAll();
	}

	/**
	 * Run defined content preprocessors on passed in content.
	 * 
	 * @param typeName <code>dcp_content_type</code> name we run preprocessors for to be used for error messages
	 * @param preprocessorsDef definition of preprocessors - see {@link #getPreprocessors(Map)}
	 * @param content to run preprocessors on
	 */
	public void runPreprocessors(String typeName, List<Map<String, Object>> preprocessorsDef, Map<String, Object> content) {
		try {
			List<StructuredContentPreprocessor> preprocessors = StructuredContentPreprocessorFactory.createPreprocessors(
					preprocessorsDef, searchClientService.getClient());
			for (StructuredContentPreprocessor preprocessor : preprocessors) {
				content = preprocessor.preprocessData(content);
			}
		} catch (IllegalArgumentException e) {
			throw new SettingsException("Bad configuration of some 'input_preprocessors' for dcp_content_type=" + typeName
					+ ". Contact administrators please. Cause: " + e.getMessage(), e);
		} catch (ClassCastException e) {
			throw new SettingsException("Bad configuration structure of some 'input_preprocessors' for dcp_content_type="
					+ typeName + ". Contact administrators please. Cause: " + e.getMessage(), e);
		}
	}

	/**
	 * Generate DCP wide unique <code>dcp_id</code> value from <code>dcp_content_type</code> and
	 * <code>dcp_content_id</code>.
	 * 
	 * @param type <code>dcp_content_type</code> value which is DCP wide unique
	 * @param contentId <code>dcp_content_id</code> value which is unique for <code>dcp_content_type</code>
	 * @return DCP wide unique <code>dcp_id</code> value
	 */
	public String generateDcpId(String type, String contentId) {
		return type + "-" + contentId;
	}

	/**
	 * Get configuration for one <code>dcp_content_type</code> from provider configuration.
	 * 
	 * @param providerDef provider configuration structure
	 * @param typeName name of <code>dcp_content_type</code> to get configuration for
	 * @return type configuration or null if doesn't exist
	 * @throws SettingsException for incorrect configuration structure
	 */
	@SuppressWarnings("unchecked")
	public static Map<String, Object> extractContentType(Map<String, Object> providerDef, String typeName) {
		try {
			Map<String, Object> types = (Map<String, Object>) providerDef.get(TYPE);
			if (types != null) {
				if (types.containsKey(typeName)) {
					return (Map<String, Object>) types.get(typeName);
				}
			}
			return null;
		} catch (ClassCastException e) {
			throw new SettingsException("Incorrect configuration for provider '" + providerDef.get(NAME)
					+ "' when trying to find dcp_provider_type=" + typeName + ". Contact administrators please.");
		}
	}

	/**
	 * Get preprocessors configuration from one <code>dcp_content_type</code> configuration structure.
	 * 
	 * @param typeDef <code>dcp_content_type</code> configuration structure
	 * @param typeName <code>dcp_content_type</code> name to be used for error messages
	 * @return list of preprocessor configurations
	 */
	@SuppressWarnings("unchecked")
	public static List<Map<String, Object>> extractPreprocessors(Map<String, Object> typeDef, String typeName) {
		try {
			return (List<Map<String, Object>>) typeDef.get("input_preprocessors");
		} catch (ClassCastException e) {
			throw new SettingsException("Incorrect configuration of 'input_preprocessors' for dcp_provider_type=" + typeName
					+ ". Contact administrators please.");
		}
	}

	/**
	 * Get search subsystem index name from one <code>dcp_content_type</code> configuration structure.
	 * 
	 * @param typeDef <code>dcp_content_type</code> configuration structure
	 * @param typeName <code>dcp_content_type</code> name to be used for error messages
	 * @return search index name
	 */
	@SuppressWarnings("unchecked")
	public static String extractIndexName(Map<String, Object> typeDef, String typeName) {
		try {
			String ret = null;
			if (typeDef.get(INDEX) != null)
				ret = (String) ((Map<String, Object>) typeDef.get(INDEX)).get(NAME);

			if (ret == null || ret.trim().isEmpty())
				throw new SettingsException("Incorrect configuration of 'index.name' for dcp_provider_type='" + typeName
						+ "'. SearchBackend index name is not defined. Contact administrators please.");
			return ret;

		} catch (ClassCastException e) {
			throw new SettingsException("Incorrect structure of 'index' configuration for dcp_provider_type='" + typeName
					+ "'. Contact administrators please.");
		}
	}

	/**
	 * Get array of names of search indices in search subsystem used for searching values for given
	 * <code>dcp_content_type</code>. Array or string with indices name is get from
	 * {@value ProviderService#SEARCH_INDICES} config value if exists, if not then main index name is used, see
	 * {@link #extractIndexName(Map, String)}.
	 * 
	 * @param typeDef <code>dcp_content_type</code> configuration structure
	 * @param typeName <code>dcp_content_type</code> name to be used for error messages
	 * @return search index name
	 */
	@SuppressWarnings("unchecked")
	public static String[] extractSearchIndices(Map<String, Object> typeDef, String typeName) {
		try {
			if (typeDef.get(INDEX) == null) {
				throw new SettingsException("Missing 'index' section in configuration for dcp_provider_type='" + typeName
						+ "'. Contact administrators please.");
			}
			Object val = ((Map<String, Object>) typeDef.get(INDEX)).get(SEARCH_INDICES);
			if (val == null) {
				return new String[] { extractIndexName(typeDef, typeName) };
			} else {
				if (val instanceof String) {
					return new String[] { (String) val };
				} else if (val instanceof List) {
					return (String[]) ((List<String>) val).toArray(new String[((List<String>) val).size()]);
				} else {
					throw new SettingsException("Incorrect configuration of 'index." + SEARCH_INDICES
							+ "' for dcp_provider_type='" + typeName
							+ "'. Value must be string or array of strings. Contact administrators please.");
				}
			}
		} catch (ClassCastException e) {
			throw new SettingsException("Incorrect structure of 'index' configuration for dcp_provider_type='" + typeName
					+ "'. Contact administrators please.");
		}
	}

	/**
	 * Get search subsystem type name from one <code>dcp_content_type</code> configuration structure.
	 * 
	 * @param typeDef <code>dcp_content_type</code> configuration structure
	 * @param typeName <code>dcp_content_type</code> name to be used for error messages
	 * @return search type name
	 */
	@SuppressWarnings("unchecked")
	public static String extractIndexType(Map<String, Object> typeDef, String typeName) {
		try {
			String ret = null;
			if (typeDef.get(INDEX) != null)
				ret = (String) ((Map<String, Object>) typeDef.get(INDEX)).get(TYPE);

			if (ret == null || ret.trim().isEmpty())
				throw new SettingsException("Incorrect configuration of 'index.type' for dcp_provider_type='" + typeName
						+ "'. SearchBackend index type is not defined. Contact administrators please.");
			return ret;

		} catch (ClassCastException e) {
			throw new SettingsException("Incorrect structure of 'index' configuration for dcp_provider_type='" + typeName
					+ "'. Contact administrators please.");
		}
	}

	/**
	 * Get <code>dcp_type</code> value from one <code>dcp_content_type</code> configuration structure.
	 * 
	 * @param typeDef <code>dcp_content_type</code> configuration structure
	 * @param typeName <code>dcp_content_type</code> name to be used for error messages
	 * @return <code>dcp_type</code> value
	 * @throws SettingsException if value is not present in configuration or is invalid
	 */
	public static String extractDcpType(Map<String, Object> typeDef, String typeName) {
		String ret = null;
		if (typeDef.get(DCP_TYPE) != null)
			ret = typeDef.get(DCP_TYPE).toString();

		if (ret == null || ret.trim().isEmpty())
			throw new SettingsException("dcp_type is not defined correctly for dcp_provider_type=" + typeName
					+ ". Contact administrators please.");

		return ret;
	}

	/**
	 * Get {@value ProviderService#SEARCH_ALL_EXCLUDED} value from one <code>dcp_content_type</code> configuration
	 * structure. Handle all cases if not in structure etc.
	 * 
	 * @param typeDef <code>dcp_content_type</code> configuration structure
	 * @return SEARCH_ALL_EXCLUDED value from configuration
	 * @throws SettingsException if value is not present in configuration or is invalid
	 */
	public static boolean extractSearchAllExcluded(Map<String, Object> typeDef) {
		if (typeDef.containsKey(SEARCH_ALL_EXCLUDED))
			return Boolean.parseBoolean(typeDef.get(SEARCH_ALL_EXCLUDED).toString());
		else
			return false;
	}

	public EntityService getEntityService() {
		return entityService;
	}

}
