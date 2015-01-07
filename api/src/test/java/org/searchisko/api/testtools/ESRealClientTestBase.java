/*
 * JBoss, Home of Professional Open Source
 * Copyright 2012 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 */
package org.searchisko.api.testtools;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.io.FileUtils;
import org.elasticsearch.action.admin.cluster.health.ClusterHealthRequest;
import org.elasticsearch.action.admin.indices.create.CreateIndexRequest;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest;
import org.elasticsearch.action.admin.indices.mapping.put.PutMappingRequest;
import org.elasticsearch.action.get.GetRequest;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.node.Node;
import org.elasticsearch.node.NodeBuilder;
import org.mockito.Mockito;
import org.searchisko.api.service.SearchClientService;

/**
 * Base class for unit tests which need to run some test against ElasticSearch cluster. You can use next pattern in your
 * unit test method to obtain testing client connected to in-memory ES cluster without any data.
 *
 * Note that even if the ES cluster is started in embedded mode it can still form a cluster of more nodes. To make
 * sure you start isolated cluster for your unit test provide unique clusterName value.
 *
 * <pre>
 * try{
 *   Client client = prepareESClientForUnitTest(clusterName);
 * 
 *   ... your unit test code here
 * 
 * } finally {
 *   finalizeESClientForUnitTest();
 * }
 * </pre>
 * 
 * Or you can use
 * 
 * <pre>
 * try{
 *   SearchClientService scService = prepareSearchClientServiceMock(clusterName);
 * 
 *   ... your unit test code here
 * 
 * } finally {
 *   finalizeESClientForUnitTest();
 * }
 * </pre>
 * 
 * @author Vlastimil Elias (velias at redhat dot com)
 */
public abstract class ESRealClientTestBase {

	private Client client;

	private Node node;

	private File tempFolder;

	protected Logger log = Logger.getLogger(ESRealClientTestBase.class.getName());

	/**
	 * Prepare SearchClientService against in-memory EC cluster for unit test. Do not forgot to call
	 * {@link #finalizeESClientForUnitTest()} at the end of test!
	 *
	 * @param clusterName
	 * @return
	 */
	public final SearchClientService prepareSearchClientServiceMock(String clusterName) {
		SearchClientService ret = Mockito.mock(SearchClientService.class);
		if (client == null)
			prepareESClientForUnitTest(clusterName);
		Mockito.when(ret.getClient()).thenReturn(client);
		return ret;
	}

	/**
	 * Prepare ES in-memory client for unit test. Do not forgot to call {@link #finalizeESClientForUnitTest()} at the end
	 * of test!
	 *
	 * @param clusterName
	 * @return
	 * @throws Exception
	 */
	public final Client prepareESClientForUnitTest(String clusterName) {
		if (client != null)
			return client;
		try {
			// For unit tests it is recommended to use local node.
			// This is to ensure that your node will never join existing cluster on the network.

			// path.data location
			// parent folder is system's defined tmp dir instead of actual directory where test runs
			String parent = System.getProperty("java.io.tmpdir");
			tempFolder = new File(parent, "tmp");
			String tempFolderName = tempFolder.getCanonicalPath();
			log.log(Level.INFO, "Temporary folder for ES JUnit Client: {0}", tempFolderName);

			if (tempFolder.exists()) {
				FileUtils.deleteDirectory(tempFolder);
			}
			if (!tempFolder.mkdirs()) {
				throw new IOException("Could not create a temporary folder [" + tempFolderName + "]");
			}

			// Make sure that the index and metadata are not stored on the disk
			// path.data folder is created but we make sure it is removed after test finishes
			Settings settings = org.elasticsearch.common.settings.ImmutableSettings.settingsBuilder()
					.put("index.store.type", "memory").put("gateway.type", "none").put("http.enabled", "false")
					.put("path.data", tempFolderName).put("node.river", "_none_").put("index.number_of_shards", 1).build();

			node = NodeBuilder.nodeBuilder().clusterName(clusterName).settings(settings).local(true).node();

			client = node.client();

			client.admin().cluster().health((new ClusterHealthRequest()).waitForYellowStatus()).actionGet();

			return client;
		} catch (Exception e) {
			finalizeESClientForUnitTest();
			throw new RuntimeException(e.getMessage(), e);
		}
	}

	/**
	 * Close ES client, call in finally block !!!
	 */
	public final void finalizeESClientForUnitTest() {
		if (client != null)
			client.close();
		client = null;
		if (node != null)
			node.close();
		node = null;

		if (tempFolder != null && tempFolder.exists()) {
			try {
				FileUtils.deleteDirectory(tempFolder);
			} catch (IOException e) {
				// nothing to do
			}
		}
		tempFolder = null;
	}

	/**
	 * Delete index from in-memory client.
	 * 
	 * @param indexName
	 */
	public void indexDelete(String indexName) {
		try {
			client.admin().indices().delete(new DeleteIndexRequest(indexName)).actionGet();
		} catch (Exception e) {
			// ignore
		}
	}

	/**
	 * Create index in in-memory client.
	 * Operation is blocking until the cluster status is yellow or better.
	 * 
	 * @param indexName
	 */
	public void indexCreate(String indexName) {
		client.admin().indices().create(new CreateIndexRequest(indexName)).actionGet();
		client.admin().cluster().health((new ClusterHealthRequest(indexName)).waitForYellowStatus()).actionGet();
	}

	/**
	 * Create mapping inside index in in-memory client.
	 * 
	 * @param indexName to add mapping into
	 * @param indexType to add mapping for
	 * @param mappingSource mapping definition
	 */
	public void indexMappingCreate(String indexName, String indexType, String mappingSource) {
		client.admin().indices().putMapping(new PutMappingRequest(indexName).type(indexType).source(mappingSource))
				.actionGet();
	}

	/**
	 * Insert document into in-memory client.
	 * 
	 * @param indexName
	 * @param documentType
	 * @param id
	 * @param source
	 */
	public void indexInsertDocument(String indexName, String documentType, String id, String source) {
		client.index((new IndexRequest(indexName, documentType, id)).source(source)).actionGet();
	}

	/**
	 * Get document from in-memory client.
	 * 
	 * @param indexName
	 * @param documentType
	 * @param id
	 * @return null if document doesn't exist, Map of Maps structure if exists.
	 * 
	 */
	public Map<String, Object> indexGetDocument(String indexName, String documentType, String id) {
		GetResponse r = client.get((new GetRequest(indexName, documentType, id))).actionGet();
		if (r != null && r.isExists()) {
			return r.getSourceAsMap();
		}
		return null;

	}

	/**
	 * Flush and Refresh search index - use it after you insert new documents and before you try to search/get them to be
	 * sure they are in index and visible for search.
	 * 
	 * @param indexName
	 */
	public void indexFlushAndRefresh(String... indexName) {
		client.admin().indices().prepareFlush(indexName).execute().actionGet();
		client.admin().indices().prepareRefresh(indexName).execute().actionGet();
	}
}
