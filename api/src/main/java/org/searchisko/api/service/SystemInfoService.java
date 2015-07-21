/*
 * JBoss, Home of Professional Open Source
 * Copyright 2012 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 */
package org.searchisko.api.service;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.PostConstruct;
import javax.ejb.Lock;
import javax.ejb.LockType;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.inject.Named;
import javax.persistence.EntityManager;
import javax.servlet.http.HttpServletRequest;

import org.elasticsearch.action.admin.cluster.node.info.NodesInfoRequest;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.xcontent.ToXContent;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.searchisko.api.filter.CDIServletRequestProducingListener;
import org.searchisko.api.model.StatsConfiguration;
import org.searchisko.api.util.SearchUtils;
import org.searchisko.persistence.service.JdbcContentPersistenceService;

/**
 * Service used to provide system info. Build info is loaded from {@value #FILENAME} file.
 * 
 * @author Vlastimil Elias (velias at redhat dot com)
 */
@Named
@ApplicationScoped
@Singleton
@Lock(LockType.READ)
@Startup
public class SystemInfoService {

	private static final String FILENAME = "/systeminfo.properties";

	@Inject
	protected Logger log;

	/**
	 * @see CDIServletRequestProducingListener
	 */
	@Inject
	protected HttpServletRequest servletRequest;

	@Inject
	private EntityManager em;

	@Inject
	protected JdbcContentPersistenceService jdbcContentPersistenceService;

	@Inject
	protected AppConfigurationService appConfigurationService;

	@Inject
	protected SearchClientService searchClientService;

	@Inject
	protected StatsClientService statsClientService;

	@Inject
	protected StatsConfiguration statsConfiguration;

	private Properties buildInfo = new Properties();

	@PostConstruct
	public void init() throws IOException {
		InputStream inStream = AppConfigurationService.class.getResourceAsStream(FILENAME);
		if (inStream == null) {
			String msg = "Cannot load build info from " + FILENAME + " because not found";
			log.log(Level.WARNING, msg);
			throw new IOException(msg);
		}
		try {
			buildInfo.load(inStream);
		} catch (IOException e) {
			log.log(Level.WARNING, "Cannot load build info from " + FILENAME + " due error: " + e.getMessage(), e);
			throw e;
		} finally {
			inStream.close();
		}

		log.log(Level.INFO, "Starting Searchisko version {0} built at {1} from commit {2}", new Object[] { getVersion(),
				getBuildTimestamp(), getBuildCommit() });
	}

	public String getVersion() {
		return buildInfo.getProperty("version");
	}

	public String getBuildTimestamp() {
		return buildInfo.getProperty("build-timestamp");
	}

	public String getBuildCommit() {
		return buildInfo.getProperty("build-commit");
	}

	/**
	 * Get build info loaded from {@value #FILENAME}.
	 * 
	 * @return build info
	 */
	public Map<Object, Object> getBuildInfo() {
		return buildInfo;
	}

	/**
	 * Get complete system info.
	 * 
	 * @param fullInfo if true then info contains all informations, some of them may be system sensitive. For false it
	 *          returns only basic info about build.
	 * @return
	 * @throws IOException if something goes wrong
	 */
	public Map<Object, Object> getSystemInfo(boolean fullInfo) throws IOException {

		Map<Object, Object> ret = new LinkedHashMap<Object, Object>();

		Map<Object, Object> p = getBuildInfo();
		if (p != null) {
			ret.put("build", new LinkedHashMap<>(p));
		}

		if (fullInfo) {
			if (servletRequest != null)
				ret.put("servlet-container", servletRequest.getServletContext().getServerInfo());

			getDatabaseInfo(ret);
			getConfigInfo(ret);
			getElasticsearchRuntimeInfo(ret);
			getJpaInfo(ret);
			ret.put("system", new LinkedHashMap<>(System.getProperties()));
		}

		return ret;
	}

	private void getElasticsearchRuntimeInfo(Map<Object, Object> ret) {
		getInfoFromEsClient(searchClientService.getClient(), ret, "es-search");
		if (statsClientService.getClient() != null) {
			getInfoFromEsClient(statsClientService.getClient(), ret, "es-stats");
		}

	}

	private void getInfoFromEsClient(Client client, Map<Object, Object> ret, String name) {
		try {
			XContentBuilder builder = XContentFactory.jsonBuilder();
			builder.startObject();
			builder.field("nodes_info");
			builder.startObject();
			client.admin().cluster().nodesInfo(new NodesInfoRequest()).actionGet()
					.toXContent(builder, ToXContent.EMPTY_PARAMS);
			builder.endObject();
			builder.endObject();
			builder.close();
			ret.put(name, SearchUtils.convertToJsonMap(builder.string()));
		} catch (Exception e) {
			ret.put(name, e.getMessage());
		}
	}

	private void getConfigInfo(Map<Object, Object> ret) {
		Map<Object, Object> map = new LinkedHashMap<>();
		Map<Object, Object> am = new LinkedHashMap<>(appConfigurationService.getAppConfigurationProperties());
		// hide password
		if (am.containsKey(AppConfigurationService.CONTRIBUTORPROFILE_PROVIDER_PASSWORD))
			am.put(AppConfigurationService.CONTRIBUTORPROFILE_PROVIDER_PASSWORD, "*****");
		map.put(AppConfigurationService.FILENAME, am);
		map.put(SearchClientService.CONFIG_FILE, new LinkedHashMap<>(searchClientService.getSettings()));
		if (searchClientService.getTransportSettings() != null)
			map.put(SearchClientService.CONFIG_FILE_TRANSPORT,
					new LinkedHashMap<>(searchClientService.getTransportSettings()));
		map.put(StatsConfiguration.FILE, new LinkedHashMap<>(statsConfiguration.getSettingsProps()));
		if (statsClientService.getSettings() != null) {
			map.put(StatsClientService.CONFIG_FILE, new LinkedHashMap<>(statsClientService.getSettings()));
		}
		if (statsClientService.getTransportSettings() != null) {
			map.put(StatsClientService.CONFIG_FILE_TRANSPORT, new LinkedHashMap<>(statsClientService.getTransportSettings()));
		}
		try {
			Properties sso = SearchUtils.loadProperties("/sso.properties");
			if (sso != null) {
				map.put("/sso.properties", new LinkedHashMap<>(sso));
			}
		} catch (Exception e) {
			// nothing to do
		}
		ret.put("config", map);
	}

	protected void getJpaInfo(Map<Object, Object> ret) {
		if (em != null) {
			Map<Object, Object> jpa = new LinkedHashMap<>();
			jpa.put("entity-manager", new LinkedHashMap<>(em.getProperties()));
			ret.put("jpa", jpa);
		} else {
			ret.put("jpa", "unused");
		}
	}

	protected void getDatabaseInfo(Map<Object, Object> ret) {
		if (jdbcContentPersistenceService != null && jdbcContentPersistenceService.getDataSource() != null) {
			try {
				Connection conn = null;
				try {
					conn = jdbcContentPersistenceService.getDataSource().getConnection();
					Map<String, Object> dbmap = new LinkedHashMap<>();
					DatabaseMetaData dbmd = conn.getMetaData();
					dbmap.put("product-name", dbmd.getDatabaseProductName());
					dbmap.put("product-version", dbmd.getDatabaseProductVersion());
					dbmap.put("driver-name", dbmd.getDriverName());
					dbmap.put("driver-version", dbmd.getDriverVersion());
					dbmap.put("jdbc-version", dbmd.getJDBCMajorVersion() + "." + dbmd.getJDBCMinorVersion());
					dbmap.put("url", dbmd.getURL());
					dbmap.put("username", dbmd.getUserName());
					dbmap.put("transaction-isolation-default", "" + dbmd.getDefaultTransactionIsolation());
					ret.put("db", dbmap);
				} finally {
					if (conn != null)
						conn.close();
				}
			} catch (Exception e) {
				ret.put("db", "Info obtaining error: " + e.getMessage());
			}
		} else {
			ret.put("db", "unused");
		}
	}

}
