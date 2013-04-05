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

import javax.ejb.LocalBean;
import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.core.StreamingOutput;

import org.elasticsearch.common.settings.SettingsException;
import org.jboss.dcp.api.cache.IndexNamesCache;
import org.jboss.dcp.api.cache.ProviderCache;
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
@Stateless
@LocalBean
public class ProviderService implements EntityService {

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
	public static final String INPUT_PREPROCESSORS = "input_preprocessors";
	/** Configuration Key for Elastic Search indices **/
	public static final String SEARCH_INDICES = "search_indices";

	public static final String SEARCH_ALL_EXCLUDED = "search_all_excluded";

	public static final String PERSIST = "persist";

	public static final String DCP_CONTENT_CONTENT_TYPE = "dcp_content_content-type";

	@Inject
	protected Logger log;

	@Inject
	@Named("providerServiceBackend")
	protected EntityService entityService;

	@Inject
	protected SecurityService securityService;

	@Inject
	protected SearchClientService searchClientService;

	@Inject
	protected IndexNamesCache indexNamesCache;

	@Inject
	protected ProviderCache providerCache;

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
	 * Find 'provider content type' configuration based on its identifier (called <code>dcp_content_type</code>). Each
	 * 'provider content type' identifier have to be DCP wide unique (so must be defined only for one provider)!
	 * 
	 * @param typeName <code>dcp_content_type</code> to look for
	 * @return content type configuration structure or <code>null</code> if not found
	 */
	public Map<String, Object> findContentType(String typeName) {
		// we do not cache here because listAllProviders() caches.
		List<Map<String, Object>> allProviders = getAll();
		if (allProviders != null) {
			for (Map<String, Object> providerDef : allProviders) {
				Map<String, Object> ct = extractContentType(providerDef, typeName);
				if (ct != null) {
					return ct;
				}
			}
		}
		return null;
	}

	/**
	 * Find provider based on its name (called <code>dcp_content_provider</code>). Values are cached here with timeout so
	 * may provide rather obsolete data sometimes!
	 * 
	 * @param providerName of provider - DCP wide unique
	 * @return provider configuration
	 */
	public Map<String, Object> findProvider(String providerName) {
		if (providerName == null)
			return null;
		Map<String, Object> ret = providerCache.get(providerName);
		if (ret == null) {
			ret = get(providerName);
			if (ret != null)
				providerCache.put(providerName, ret);
		}
		return ret;
	}

	/**
	 * Cache of all providers list.
	 * 
	 * @see #getAll()
	 */
	protected List<Map<String, Object>> cacheAllProviders;

	/**
	 * validity timestamp for cache of all providers list.
	 * 
	 * @see #getAll()
	 */
	protected long cacheAllProvidersValidTo = 0;

	/**
	 * Time to live to compute validity timestamp for cache of all providers list.
	 * 
	 * @see #getAll()
	 */
	protected long cacheAllProvidersTTL = 10000;

	@Override
	public String create(Map<String, Object> entity) {
		String id = entityService.create(entity);
		flushCaches();
		return id;
	}

	@Override
	public void create(String id, Map<String, Object> entity) {
		entityService.create(id, entity);
		flushCaches();
	}

	/**
	 * List configuration for all providers. Value is cached here with timeout so may provide rather obsolete data
	 * sometimes!
	 * 
	 * @return list with configurations for all providers
	 * @see ProviderService#cacheAllProvidersTTL
	 * 
	 */
	@Override
	public List<Map<String, Object>> getAll() {
		if (cacheAllProviders == null || cacheAllProvidersValidTo < System.currentTimeMillis()) {
			cacheAllProviders = entityService.getAll();
			cacheAllProvidersValidTo = System.currentTimeMillis() + cacheAllProvidersTTL;
		}
		return cacheAllProviders;
	}

	@Override
	public StreamingOutput getAll(Integer from, Integer size, String[] fieldsToRemove) {
		return entityService.getAll(from, size, fieldsToRemove);
	}

	@Override
	public Map<String, Object> get(String id) {
		return entityService.get(id);
	}

	@Override
	public void update(String id, Map<String, Object> entity) {
		entityService.update(id, entity);
		flushCaches();
	}

	@Override
	public void delete(String id) {
		entityService.delete(id);
		flushCaches();
	}

	/**
	 * Flush all caches containing data extracted from Provider definitions.
	 */
	public void flushCaches() {
		cacheAllProvidersValidTo = 0;
		if (indexNamesCache != null)
			indexNamesCache.flush();
		if (providerCache != null)
			providerCache.flush();
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
	 * Get configuration for all <code>dcp_content_type</code> from provider configuration.
	 * 
	 * @param providerDef provider configuration structure
	 * @return map with all type configurations or null if doesn't exist. Key in map is type name, value is type
	 *         configuration.
	 * @throws SettingsException for incorrect configuration structure
	 */
	@SuppressWarnings("unchecked")
	public static Map<String, Map<String, Object>> extractAllContentTypes(Map<String, Object> providerDef) {
		try {
			Map<String, Map<String, Object>> types = (Map<String, Map<String, Object>>) providerDef.get(TYPE);
			return types;
		} catch (ClassCastException e) {
			throw new SettingsException("Incorrect configuration for provider '" + providerDef.get(NAME)
					+ "' when trying to retrieve all dcp_provider_type configurations. Contact administrators please.");
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
			return (List<Map<String, Object>>) typeDef.get(INPUT_PREPROCESSORS);
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
	 * Get <code>dcp_content_content-type</code> value from one <code>dcp_content_type</code> configuration structure.
	 * 
	 * @param typeDef <code>dcp_content_type</code> configuration structure
	 * @param typeName <code>dcp_content_type</code> name to be used for error messages
	 * @return <code>dcp_content_content-type</code> value
	 * @throws SettingsException if value is not present in configuration or is invalid
	 */
	public static String extractDcpContentContentType(Map<String, Object> typeDef, String typeName) {
		String ret = null;
		if (typeDef.get(DCP_CONTENT_CONTENT_TYPE) != null)
			ret = typeDef.get(DCP_CONTENT_CONTENT_TYPE).toString();

		if (ret == null || ret.trim().isEmpty())
			throw new SettingsException("dcp_content_content-type is not defined correctly for dcp_provider_type=" + typeName
					+ ". Contact administrators please.");

		return ret;
	}

	/**
	 * Get {@value ProviderService#SEARCH_ALL_EXCLUDED} value from one <code>dcp_content_type</code> configuration
	 * structure. Handle all cases if not in structure etc.
	 * 
	 * @param typeDef <code>dcp_content_type</code> configuration structure
	 * @return SEARCH_ALL_EXCLUDED value from configuration
	 */
	public static boolean extractSearchAllExcluded(Map<String, Object> typeDef) {
		return extractBoolean(typeDef, SEARCH_ALL_EXCLUDED);
	}

	/**
	 * Get {@value ProviderService#PERSIST} value from one <code>dcp_content_type</code> configuration structure. Handle
	 * all cases if not in structure etc.
	 * 
	 * @param typeDef <code>dcp_content_type</code> configuration structure
	 * @return PERSIST value from configuration
	 */
	public static boolean extractPersist(Map<String, Object> typeDef) {
		return extractBoolean(typeDef, PERSIST);
	}

	private static boolean extractBoolean(Map<String, Object> typeDef, String fieldName) {
		Object p = typeDef.get(fieldName);
		if (p != null) {
			if (p instanceof Boolean)
				return (Boolean) p;
			else
				return Boolean.parseBoolean(p.toString());
		} else {
			return false;
		}
	}

}
