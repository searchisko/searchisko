/*
 * JBoss, Home of Professional Open Source
 * Copyright 2012 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 */
package org.jboss.dcp.api.testtools;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.elasticsearch.action.admin.indices.create.CreateIndexRequest;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest;
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

			Thread.sleep(100);

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

}
