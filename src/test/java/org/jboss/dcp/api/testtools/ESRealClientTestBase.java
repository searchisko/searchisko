/*
 * JBoss, Home of Professional Open Source
 * Copyright 2012 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 */
package org.jboss.dcp.api.testtools;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.elasticsearch.action.admin.cluster.health.ClusterHealthRequest;
import org.elasticsearch.action.admin.indices.create.CreateIndexRequest;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest;
import org.elasticsearch.action.admin.indices.flush.FlushRequest;
import org.elasticsearch.action.admin.indices.mapping.put.PutMappingRequest;
import org.elasticsearch.action.get.GetRequest;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.node.Node;
import org.elasticsearch.node.NodeBuilder;
import org.jboss.dcp.api.service.SearchClientService;
import org.mockito.Mockito;

/**
 * Base class for unit tests which need to run some test against ElasticSearch cluster. You can use next pattern in your
 * unit test method to obtain testing client connected to inmemory ES cluster without any data.
 * 
 * <pre>
 * try{
 *   Client client = prepareESClientForUnitTest();
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
 *   SearchClientService scService = prepareSearchClientServiceMock();
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

	/**
	 * Prepare SearchClientService against inmemory EC cluster for unit test. Do not forgot to call
	 * {@link #finalizeESClientForUnitTest()} at the end of test!
	 * 
	 * @return
	 */
	public final SearchClientService prepareSearchClientServiceMock() {
		SearchClientService ret = Mockito.mock(SearchClientService.class);
		if (client == null)
			prepareESClientForUnitTest();
		Mockito.when(ret.getClient()).thenReturn(client);
		return ret;
	}

	/**
	 * Prepare ES inmemory client for unit test. Do not forgot to call {@link #finalizeESClientForUnitTest()} at the end
	 * of test!
	 * 
	 * @return
	 * @throws Exception
	 */
	public final Client prepareESClientForUnitTest() {
		if (client != null)
			return client;
		try {
			// For unit tests it is recommended to use local node.
			// This is to ensure that your node will never join existing cluster on the network.

			// path.data location
			tempFolder = new File("tmp");
			String tempFolderName = tempFolder.getCanonicalPath();

			if (tempFolder.exists()) {
				FileUtils.deleteDirectory(tempFolder);
			}
			if (!tempFolder.mkdir()) {
				throw new IOException("Could not create a temporary folder [" + tempFolderName + "]");
			}

			// Make sure that the index and metadata are not stored on the disk
			// path.data folder is created but we make sure it is removed after test finishes
			Settings settings = org.elasticsearch.common.settings.ImmutableSettings.settingsBuilder()
					.put("index.store.type", "memory").put("gateway.type", "none").put("http.enabled", "false")
					.put("path.data", tempFolderName).put("node.river", "_none_").build();

			node = NodeBuilder.nodeBuilder().settings(settings).local(true).node();

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
	 * Delete index from inmemory client.
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
	 * Create index in inmemory client.
	 * 
	 * @param indexName
	 */
	public void indexCreate(String indexName) {
		client.admin().indices().create(new CreateIndexRequest(indexName)).actionGet();
		client.admin().cluster().health((new ClusterHealthRequest(indexName)).waitForYellowStatus()).actionGet();
	}

	/**
	 * Create mapping inside index in inmemory client.
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
	 * Insert document into inmemory client.
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
	 * Get document from inmemory client.
	 * 
	 * @param indexName
	 * @param documentType
	 * @param id
	 * @return null if document doesn't exist, Map of Maps structure if exists.
	 * 
	 */
	public Map<String, Object> indexGetDocument(String indexName, String documentType, String id) {
		GetResponse r = client.get((new GetRequest(indexName, documentType, id))).actionGet();
		if (r != null && r.exists()) {
			return r.sourceAsMap();
		}
		return null;

	}

	/**
	 * Flush search index - use it after you insert new documents and before you try to search/get them to be sure they
	 * are in index.
	 * 
	 * @param indexName
	 */
	public void indexFlush(String indexName) {
		client.admin().indices().flush(new FlushRequest(indexName)).actionGet();
	}
}
