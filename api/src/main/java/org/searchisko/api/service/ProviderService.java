/*
 * JBoss, Home of Professional Open Source
 * Copyright 2012 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 */
package org.searchisko.api.service;

import java.util.Collection;
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
import org.jboss.elasticsearch.tools.content.InvalidDataException;
import org.jboss.elasticsearch.tools.content.StructuredContentPreprocessor;
import org.jboss.elasticsearch.tools.content.StructuredContentPreprocessorFactory;
import org.searchisko.api.cache.IndexNamesCache;
import org.searchisko.api.cache.ProviderCache;
import org.searchisko.api.rest.exception.PreprocessorInvalidDataException;
import org.searchisko.api.util.PreprocessChainContextImpl;
import org.searchisko.api.util.SearchUtils;
import org.searchisko.persistence.service.EntityService;
import org.searchisko.persistence.service.ListRequest;

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
	 * Key for provider's name which is unique identifier - can be same as provider's ID. Also used during Configuration
	 * read to get index name.
	 */
	public static final String NAME = "name";

	/** Configuration file Key for password hash **/
	public static final String PASSWORD_HASH = "pwd_hash";

	/** Configuration Key for flag if provider is super provider (system administrator) **/
	public static final String SUPER_PROVIDER = "super_provider";

	/** Configuration Key for content type **/
	public static final String TYPE = "type";
	/** Configuration Key for index settings **/
	public static final String INDEX = "index";
	/** Configuration Key for indexer settings **/
	public static final String INDEXER = "indexer";

	/** Configuration Key for sys_type setting **/
	public static final String SYS_TYPE = "sys_type";
	/** Configuration Key for sys_visible_for_roles setting **/
	public static final String SYS_VISIBLE_FOR_ROLES = "sys_visible_for_roles";
	/** Configuration Key for preprocessors setting **/
	public static final String INPUT_PREPROCESSORS = "input_preprocessors";
	/** Configuration Key for Elasticsearch indices **/
	public static final String SEARCH_INDICES = "search_indices";
	public static final String SEARCH_ALL_EXCLUDED = "search_all_excluded";
	public static final String PERSIST = "persist";

	public static final String SYS_CONTENT_CONTENT_TYPE = "sys_content_content-type";

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
	 * Check if requested provider is super_provider.
	 * 
	 * @param providerName name of provider to check
	 * @return true if it is super_provider
	 */
	public boolean isSuperProvider(String providerName) {
		Map<String, Object> providerData = findProvider(providerName);

		if (providerData == null)
			return false;

		Object superProviderFlag = providerData.get(SUPER_PROVIDER);

		return (superProviderFlag != null && Boolean.parseBoolean(superProviderFlag.toString()));
	}

	/**
	 * Find 'provider content type' configuration based on its identifier (called <code>sys_content_type</code>). Each
	 * 'provider content type' identifier has to be system wide unique (so must be defined only for one provider)!
	 * 
	 * @param typeName <code>sys_content_type</code> to look for
	 * @return content type configuration info or <code>null</code> if not found
	 * 
	 * @see #parseTypeNameFromSysId(String)
	 */
	public ProviderContentTypeInfo findContentType(String typeName) {
		if (SearchUtils.isBlank(typeName))
			return null;
		// we do not cache here because getAll() caches.
		List<Map<String, Object>> allProviders = getAll();
		if (allProviders != null) {
			for (Map<String, Object> providerDef : allProviders) {
				Map<String, Object> ct = extractContentType(providerDef, typeName);
				if (ct != null) {
					return new ProviderContentTypeInfo(providerDef, typeName);
				}
			}
		}
		return null;
	}

	/**
	 * Info about found provider content type.
	 * 
	 * @see ProviderService#findContentType(String)
	 */
	public static final class ProviderContentTypeInfo {
		private Map<String, Object> providerDef;
		private Map<String, Object> typeDef;
		private String typeName;

		public ProviderContentTypeInfo(Map<String, Object> providerDef, String typeName) {
			if (providerDef == null)
				throw new IllegalArgumentException("providerDef can't be null");

			if (SearchUtils.isBlank(typeName)) {
				throw new IllegalArgumentException("typeName can't be null nor empty");
			}

			typeDef = extractContentType(providerDef, typeName);
			if (typeDef == null)
				throw new IllegalArgumentException(typeName + " type must be in provider info passed in");

			this.providerDef = providerDef;
			this.typeName = typeName;
		}

		/**
		 * Get complete provider configuration for provider the type is for.
		 * 
		 * @return provider info structure, never null
		 */
		public Map<String, Object> getProviderDef() {
			return providerDef;
		}

		/**
		 * Get name of provider the type is for.
		 * 
		 * @return provider name
		 */
		public String getProviderName() {
			return (String) providerDef.get(NAME);
		}

		/**
		 * Get name of type
		 * 
		 * @return name of type, never null or empty
		 */
		public String getTypeName() {
			return typeName;
		}

		/**
		 * Get type configuration structure
		 * 
		 * @return type info, never null
		 */
		public Map<String, Object> getTypeDef() {
			return typeDef;
		}

		@Override
		public String toString() {
			return "ProviderContentTypeInfo [typeName=" + typeName + ", providerDef=" + providerDef + ", typeDef=" + typeDef
					+ "]";
		}

	}

	/**
	 * Find provider based on its name (called <code>sys_content_provider</code>). Values are cached here with timeout so
	 * may provide rather obsolete data sometimes!
	 * 
	 * @param providerName of provider - system wide unique
	 * @return provider configuration
	 */
	public Map<String, Object> findProvider(String providerName) {
		if (SearchUtils.trimToNull(providerName) == null)
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
	 * @param typeName <code>sys_content_type</code> name we run preprocessors for to be used for error messages
	 * @param preprocessorsDef definition of preprocessors - see {@link #extractPreprocessors(Map, String)}
	 * @param content to run preprocessors on
	 * @return list of warnings from preprocessors, may be null
	 * @throws PreprocessorInvalidDataException wrapping {@link InvalidDataException} if it is thrown by preprocessors.
	 */
	public List<Map<String, String>> runPreprocessors(String typeName, List<Map<String, Object>> preprocessorsDef,
			Map<String, Object> content) throws PreprocessorInvalidDataException {
		try {
			List<StructuredContentPreprocessor> preprocessors = StructuredContentPreprocessorFactory.createPreprocessors(
					preprocessorsDef, searchClientService.getClient());
			PreprocessChainContextImpl context = new PreprocessChainContextImpl();
			for (StructuredContentPreprocessor preprocessor : preprocessors) {
				content = preprocessor.preprocessData(content, context);
			}
			return context.warnings;
		} catch (InvalidDataException e) {
			// #188 - we must remap this exception to something else which do not extends RuntimeException
			throw new PreprocessorInvalidDataException(e);
		} catch (IllegalArgumentException e) {
			throw new SettingsException("Bad configuration of some 'input_preprocessors' for sys_content_type=" + typeName
					+ ". Cause: " + e.getMessage(), e);
		} catch (ClassCastException e) {
			throw new SettingsException("Bad configuration structure of some 'input_preprocessors' for sys_content_type="
					+ typeName + ". Cause: " + e.getMessage(), e);
		}
	}

	/**
	 * Generate system wide unique <code>sys_id</code> value from <code>sys_content_type</code> and
	 * <code>sys_content_id</code>.
	 * 
	 * @param type <code>sys_content_type</code> value which is system wide unique
	 * @param contentId <code>sys_content_id</code> value which is unique for <code>sys_content_type</code>
	 * @return system wide unique <code>sys_id</code> value
	 */
	public String generateSysId(String type, String contentId) {
		return type + "-" + contentId;
	}

	/**
	 * Parse <code>sys_content_type</code> name from system wide unique content system id (<code>sys_id</code>) value.
	 * Type is not validated for existence inside of this method!
	 * 
	 * @param contentSysId to get type from
	 * @return <code>sys_content_type</code> from id. never null
	 * @throws IllegalArgumentException if passed in contentSysId is null or has invalid format
	 */
	public String parseTypeNameFromSysId(String contentSysId) throws IllegalArgumentException {
		if (contentSysId == null)
			throw new IllegalArgumentException("sys_id can't be null");
		contentSysId = contentSysId.trim();
		int idx = contentSysId.indexOf("-");
		if (idx < 1 || idx >= contentSysId.length() - 1)
			throw new IllegalArgumentException("Invalid format of sys_id");
		return contentSysId.substring(0, idx);
	}

	/**
	 * Get configuration for one <code>sys_content_type</code> from provider configuration.
	 * 
	 * @param providerDef provider configuration structure
	 * @param typeName name of <code>sys_content_type</code> to get configuration for
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
					+ "' when trying to find sys_provider_type=" + typeName + ".");
		}
	}

	/**
	 * Get configuration for all <code>sys_content_type</code> from provider configuration.
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
					+ "' when trying to retrieve all sys_provider_type configurations.");
		}
	}

	/**
	 * Get preprocessors configuration from one <code>sys_content_type</code> configuration structure.
	 * 
	 * @param typeDef <code>sys_content_type</code> configuration structure
	 * @param typeName <code>sys_content_type</code> name to be used for error messages
	 * @return list of preprocessor configurations
	 */
	@SuppressWarnings("unchecked")
	public static List<Map<String, Object>> extractPreprocessors(Map<String, Object> typeDef, String typeName) {
		try {
			return (List<Map<String, Object>>) typeDef.get(INPUT_PREPROCESSORS);
		} catch (ClassCastException e) {
			throw new SettingsException("Incorrect configuration of 'input_preprocessors' for sys_provider_type=" + typeName
					+ ".");
		}
	}

	/**
	 * Get preprocessors configuration from one <code>sys_content_type</code> configuration.
	 * 
	 * @param typeDef <code>sys_content_type</code> configuration
	 * @param typeName <code>sys_content_type</code> name to be used for error messages
	 * @return list of preprocessor configurations
	 */
	public static List<Map<String, Object>> extractPreprocessors(ProviderContentTypeInfo typeInfo, String typeName) {
		return extractPreprocessors(typeInfo.getTypeDef(), typeName);
	}

	/**
	 * Get search subsystem index name from one <code>sys_content_type</code> configuration structure.
	 * 
	 * @param typeDef <code>sys_content_type</code> configuration structure
	 * @param typeName <code>sys_content_type</code> name to be used for error messages
	 * @return search index name
	 */
	@SuppressWarnings("unchecked")
	public static String extractIndexName(Map<String, Object> typeDef, String typeName) {
		try {
			String ret = null;
			if (typeDef.get(INDEX) != null)
				ret = (String) ((Map<String, Object>) typeDef.get(INDEX)).get(NAME);

			if (ret == null || ret.trim().isEmpty())
				throw new SettingsException("Incorrect configuration of 'index.name' for sys_provider_type='" + typeName
						+ "'. SearchBackend index name is not defined.");
			return ret;

		} catch (ClassCastException e) {
			throw new SettingsException("Incorrect structure of 'index' configuration for sys_provider_type='" + typeName
					+ "'.");
		}
	}

	/**
	 * Get search subsystem index name from one <code>sys_content_type</code> configuration.
	 * 
	 * @param typeInfo <code>sys_content_type</code> configuration info
	 * @param typeName <code>sys_content_type</code> name to be used for error messages
	 * @return search index name
	 */
	public static String extractIndexName(ProviderContentTypeInfo typeInfo, String typeName) {
		return extractIndexName(typeInfo.getTypeDef(), typeName);
	}

	/**
	 * Get array of names of search indices in search subsystem used for searching values for given
	 * <code>sys_content_type</code>. Array or string with indices name is get from
	 * {@value ProviderService#SEARCH_INDICES} config value if exists, if not then main index name is used, see
	 * {@link #extractIndexName(Map, String)}.
	 * 
	 * @param typeDef <code>sys_content_type</code> configuration structure
	 * @param typeName <code>sys_content_type</code> name to be used for error messages
	 * @return search index name
	 */
	@SuppressWarnings("unchecked")
	public static String[] extractSearchIndices(Map<String, Object> typeDef, String typeName) {
		try {
			if (typeDef.get(INDEX) == null) {
				throw new SettingsException("Missing 'index' section in configuration for sys_provider_type='" + typeName
						+ "'.");
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
							+ "' for sys_provider_type='" + typeName + "'. Value must be string or array of strings.");
				}
			}
		} catch (ClassCastException e) {
			throw new SettingsException("Incorrect structure of 'index' configuration for sys_provider_type='" + typeName
					+ "'.");
		}
	}

	/**
	 * Get array of names of search indices in search subsystem used for searching values for given
	 * <code>sys_content_type</code>. Array or string with indices name is get from
	 * {@value ProviderService#SEARCH_INDICES} config value if exists, if not then main index name is used, see
	 * {@link #extractIndexName(Map, String)}.
	 * 
	 * @param typeInfo <code>sys_content_type</code> configuration
	 * @param typeName <code>sys_content_type</code> name to be used for error messages
	 * @return search index name
	 */
	public static String[] extractSearchIndices(ProviderContentTypeInfo typeInfo, String typeName) {
		return extractSearchIndices(typeInfo.getTypeDef(), typeName);
	}

	/**
	 * Get set of user role given <code>sys_content_type</code> is visible for. Array or string with roles is get from
	 * {@value ProviderService#SYS_VISIBLE_FOR_ROLES} config value if exists.
	 * 
	 * @param typeInfo <code>sys_content_type</code> configuration
	 * @param typeName <code>sys_content_type</code> name to be used for error messages
	 * @return collection of user roles this content is available for. null means it is available for everybody.
	 */
	public static Collection<String> extractTypeVisibilityRoles(ProviderContentTypeInfo typeInfo, String typeName) {
		return extractTypeVisibilityRoles(typeInfo.getTypeDef(), typeName);
	}

	/**
	 * Get set of user role given <code>sys_content_type</code> is visible for. Array or string with roles is get from
	 * {@value ProviderService#SYS_VISIBLE_FOR_ROLES} config value if exists.
	 * 
	 * @param typeInfo <code>sys_content_type</code> configuration
	 * @param typeName <code>sys_content_type</code> name to be used for error messages
	 * @return collection of user roles this content is available for. null means it is available for everybody.
	 */
	public static Collection<String> extractTypeVisibilityRoles(Map<String, Object> typeDef, String typeName) {
		try {
			return SearchUtils.getListOfStringsFromJsonMap(typeDef, SYS_VISIBLE_FOR_ROLES);
		} catch (SettingsException e) {
			throw new SettingsException("Incorrect structure of '" + SYS_VISIBLE_FOR_ROLES
					+ "' configuration for sys_provider_type='" + typeName + "'.");
		}
	}

	/**
	 * Get search subsystem type name from one <code>sys_content_type</code> configuration structure.
	 * 
	 * @param typeDef <code>sys_content_type</code> configuration structure
	 * @param typeName <code>sys_content_type</code> name to be used for error messages
	 * @return search type name
	 */
	@SuppressWarnings("unchecked")
	public static String extractIndexType(Map<String, Object> typeDef, String typeName) {
		try {
			String ret = null;
			if (typeDef.get(INDEX) != null)
				ret = (String) ((Map<String, Object>) typeDef.get(INDEX)).get(TYPE);

			if (ret == null || ret.trim().isEmpty())
				throw new SettingsException("Incorrect configuration of 'index.type' for sys_provider_type='" + typeName
						+ "'. SearchBackend index type is not defined.");
			return ret;

		} catch (ClassCastException e) {
			throw new SettingsException("Incorrect structure of 'index' configuration for sys_provider_type='" + typeName
					+ "'.");
		}
	}

	/**
	 * Get search subsystem type name from one <code>sys_content_type</code> configuration.
	 * 
	 * @param typeInfo <code>sys_content_type</code> configuration
	 * @param typeName <code>sys_content_type</code> name to be used for error messages
	 * @return search type name
	 */
	public static String extractIndexType(ProviderContentTypeInfo typeInfo, String typeName) {
		return extractIndexType(typeInfo.getTypeDef(), typeName);
	}

	/**
	 * Get <code>sys_type</code> value from one <code>sys_content_type</code> configuration structure.
	 * 
	 * @param typeDef <code>sys_content_type</code> configuration structure
	 * @param typeName <code>sys_content_type</code> name to be used for error messages
	 * @return <code>sys_type</code> value
	 * @throws SettingsException if value is not present in configuration or is invalid
	 */
	public static String extractSysType(Map<String, Object> typeDef, String typeName) {
		String ret = null;
		if (typeDef.get(SYS_TYPE) != null)
			ret = typeDef.get(SYS_TYPE).toString();

		if (ret == null || ret.trim().isEmpty())
			throw new SettingsException("sys_type is not defined correctly for sys_provider_type=" + typeName + ".");

		return ret;
	}

	/**
	 * Get <code>indexer</code> configuration from one <code>sys_content_type</code> configuration structure.
	 * 
	 * @param typeDef <code>sys_content_type</code> configuration structure
	 * @param typeName <code>sys_content_type</code> name to be used for error messages
	 * @return indexer configuration structure or null if not defined
	 * @throws SettingsException if configuration is invalid
	 */
	@SuppressWarnings("unchecked")
	public static Map<String, Object> extractIndexerConfiguration(Map<String, Object> typeDef, String typeName) {
		try {
			return (Map<String, Object>) typeDef.get(INDEXER);
		} catch (ClassCastException e) {
			throw new SettingsException("Incorrect configuration of 'indexer' for sys_provider_type=" + typeName + ".");
		}
	}

	/**
	 * Get <code>sys_content_content-type</code> value from one <code>sys_content_type</code> configuration structure.
	 * 
	 * @param typeDef <code>sys_content_type</code> configuration structure
	 * @param typeName <code>sys_content_type</code> name to be used for error messages
	 * @return <code>sys_content_content-type</code> value
	 * @throws SettingsException if value is not present in configuration or is invalid
	 */
	public static String extractSysContentContentType(Map<String, Object> typeDef, String typeName) {
		String ret = null;
		if (typeDef.get(SYS_CONTENT_CONTENT_TYPE) != null)
			ret = typeDef.get(SYS_CONTENT_CONTENT_TYPE).toString();

		if (ret == null || ret.trim().isEmpty())
			throw new SettingsException("sys_content_content-type is not defined correctly for sys_provider_type=" + typeName
					+ ".");

		return ret;
	}

	/**
	 * Get {@value ProviderService#SEARCH_ALL_EXCLUDED} value from one <code>sys_content_type</code> configuration
	 * structure. Handle all cases if not in structure etc.
	 * 
	 * @param typeDef <code>sys_content_type</code> configuration structure
	 * @return SEARCH_ALL_EXCLUDED value from configuration
	 */
	public static boolean extractSearchAllExcluded(Map<String, Object> typeDef) {
		return extractBoolean(typeDef, SEARCH_ALL_EXCLUDED);
	}

	/**
	 * Get {@value ProviderService#PERSIST} value from one <code>sys_content_type</code> configuration structure. Handle
	 * all cases if not in structure etc.
	 * 
	 * @param typeDef <code>sys_content_type</code> configuration structure
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

	@Override
	public ListRequest listRequestInit() {
		return entityService.listRequestInit();
	}

	@Override
	public ListRequest listRequestNext(ListRequest previous) {
		return entityService.listRequestNext(previous);
	}

}
