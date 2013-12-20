/*
 * JBoss, Home of Professional Open Source
 * Copyright 2012 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 */
package org.searchisko.api.service;

import static org.elasticsearch.common.settings.ImmutableSettings.settingsBuilder;

import java.io.File;
import java.io.IOException;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.PreDestroy;
import javax.inject.Inject;

import org.elasticsearch.action.admin.cluster.health.ClusterHealthResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.Requests;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.node.Node;
import org.elasticsearch.node.NodeBuilder;
import org.searchisko.api.model.AppConfiguration;

/**
 * Common service for Elasticsearch client. It handles closing client (and node in embedded mode) so child doesn't need
 * to care of it.
 * 
 * @author Libor Krzyzanek
 * @author Vlastimil Elias (velias at redhat dot com)
 * 
 */
public class ElasticsearchClientService {

	@Inject
	protected Logger log;

	/**
	 * Node used only in embedded mode
	 */
	protected Node node;

	protected Client client;

	@Inject
	protected AppConfigurationService appConfigurationService;

	/**
	 * Prepare ES embedded Node.<br/>
	 * Inspired by: https://github.com/jbossorg/elasticsearch-river-jira/blob/master/src/test
	 * /java/org/jboss/elasticsearch/river/jira/testtools/ESRealClientTestBase.java
	 * 
	 * @return Embedded node
	 * @throws Exception
	 */
	public Node createEmbeddedNode(String name, Properties settings) throws Exception {
		Node node = null;
		try {
			String nodePath = appConfigurationService.getAppConfiguration().getAppDataPath();

			if (nodePath != null) {
				nodePath = nodePath + "/" + name;
			} else {
				// default location within AS
				nodePath = name;
			}
			File pathFolder = new File(nodePath);
			String pathFolderName = pathFolder.getCanonicalPath();

			if (!pathFolder.exists()) {
				if (!pathFolder.mkdirs()) {
					throw new IOException("Could not create a folder for Elasticsearch node [" + pathFolderName + "]");
				}
			}

			// HTTP is completely disabled
			Settings s = org.elasticsearch.common.settings.ImmutableSettings.settingsBuilder().put(settings)
					.put("path.data", pathFolderName).build();

			node = NodeBuilder.nodeBuilder().settings(s).local(true).node();
			log.log(Level.INFO, "New Embedded Node instance created, {0}, location: {1}",
					new Object[] { node, pathFolderName });
			return node;
		} catch (Exception e) {
			if (node != null) {
				node.close();
			}
			throw e;
		}
	}

	protected TransportClient createTransportClient(Properties transportAddresses, Properties settings) {
		final TransportClient transportClient = new TransportClient(settingsBuilder().put(settings)
				.put("client.transport.sniff", true).build());
		log.log(Level.INFO, "New TransportClient instance created, {0}", client);

		for (final String host : transportAddresses.stringPropertyNames()) {
			int port = Integer.parseInt(transportAddresses.getProperty(host));
			log.info("Adding transport address: " + host + " port: " + port);
			transportClient.addTransportAddress(new InetSocketTransportAddress(host, port));
		}
		return transportClient;
	}

	public Client getClient() {
		return client;
	}

	@PreDestroy
	public void destroy() {
		try {
			if (client != null) {
				client.close();
				client = null;
				log.info("Elasticsearch Search Client is closed");
			}
		} finally {
			if (node != null) {
				node.close();
				node = null;
				log.info("Elasticsearch Search Node is closed");
			}
		}
	}

	public void checkHealthOfCluster(Client client) {
		try {
			if (log.isLoggable(Level.FINE)
					&& appConfigurationService.getAppConfiguration().getClientType()
							.compareTo(AppConfiguration.ClientType.EMBEDDED) != 0) {
				log.fine("== Cluster health response ==");
				ClusterHealthResponse healthResponse = client.admin().cluster().health(Requests.clusterHealthRequest("_all"))
						.actionGet(TimeValue.timeValueSeconds(10));
				log.log(Level.FINE, "number of nodes: {0}", healthResponse.getNumberOfNodes());
				log.log(Level.FINE, "number of data nodes: {0}", healthResponse.getNumberOfDataNodes());
				log.log(Level.FINE, "active shards: {0}", healthResponse.getActiveShards());
				log.log(Level.FINE, "relocating shards: {0}", healthResponse.getRelocatingShards());
				log.log(Level.FINE, "active primary shards: {0}", healthResponse.getActivePrimaryShards());
				log.log(Level.FINE, "initializing shards: {0}", healthResponse.getInitializingShards());
				log.log(Level.FINE, "unassigned shards: {0}", healthResponse.getUnassignedShards());

			}
		} catch (Throwable e) {
			log.log(Level.SEVERE, "Ping request failed.", e);
		}
	}

}
