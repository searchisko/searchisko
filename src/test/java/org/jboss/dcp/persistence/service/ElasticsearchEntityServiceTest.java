/*
 * JBoss, Home of Professional Open Source
 * Copyright 2012 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 */
package org.jboss.dcp.persistence.service;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.core.StreamingOutput;

import junit.framework.Assert;

import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.client.Client;
import org.jboss.dcp.api.testtools.ESRealClientTestBase;
import org.jboss.dcp.api.testtools.TestUtils;
import org.jboss.dcp.persistence.service.ElasticsearchEntityService;
import org.junit.Test;
import org.mockito.Mockito;

/**
 * Unit test fo {@link ElasticsearchEntityService}
 * 
 * @author Vlastimil Elias (velias at redhat dot com)
 */
public class ElasticsearchEntityServiceTest extends ESRealClientTestBase {

	private static final String INDEX_TYPE = "index_type";
	private static final String INDEX_NAME = "index_name";

	private ElasticsearchEntityService getTested(Client client) {
		if (client == null)
			client = Mockito.mock(Client.class);
		return new ElasticsearchEntityService(client, INDEX_NAME, INDEX_TYPE, true);
	}

	@Test
	public void constructor() {
		Client clientMock = Mockito.mock(Client.class);
		ElasticsearchEntityService tested = new ElasticsearchEntityService(clientMock, INDEX_NAME, INDEX_TYPE, true);
		Assert.assertEquals(clientMock, tested.client);
		Assert.assertEquals(INDEX_NAME, tested.indexName);
		Assert.assertEquals(INDEX_TYPE, tested.indexType);
		Assert.assertEquals(true, tested.operationThreaded);

		tested = new ElasticsearchEntityService(clientMock, INDEX_NAME, INDEX_TYPE, false);
		Assert.assertEquals(clientMock, tested.client);
		Assert.assertEquals(INDEX_NAME, tested.indexName);
		Assert.assertEquals(INDEX_TYPE, tested.indexType);
		Assert.assertEquals(false, tested.operationThreaded);
	}

	@Test
	public void createSearchRequestBuilder() {
		Client client = prepareESClientForUnitTest();
		ElasticsearchEntityService tested = getTested(client);
		try {
			SearchRequestBuilder rb = tested.createSearchRequestBuilder();
			SearchRequest r = rb.request();
			Assert.assertEquals(INDEX_NAME, r.indices()[0]);
			Assert.assertEquals(INDEX_TYPE, r.types()[0]);
		} finally {
			indexDelete(INDEX_NAME);
			finalizeESClientForUnitTest();
		}
	}

	@Test
	public void getAll() throws IOException, InterruptedException {
		Client client = prepareESClientForUnitTest();
		ElasticsearchEntityService tested = getTested(client);
		try {

			// case - index doesn't exists
			{
				StreamingOutput out = tested.getAll(null, null, null);
				TestUtils.assetStreamingOutputContent("{\"total\":0,\"hits\":[]}", out);
			}

			indexCreate(INDEX_NAME);
			// case - index exists empty
			{
				StreamingOutput out = tested.getAll(null, null, null);
				TestUtils.assetStreamingOutputContent("{\"total\":0,\"hits\":[]}", out);
			}

			indexInsertDocument(INDEX_NAME, INDEX_TYPE, "known-1", "{\"name\":\"test1\",\"idx\":\"1\"}");
			indexInsertDocument(INDEX_NAME, INDEX_TYPE, "known-2", "{\"name\":\"test2\",\"idx\":\"2\"}");
			indexInsertDocument(INDEX_NAME, INDEX_TYPE, "known-3", "{\"name\":\"test3\",\"idx\":\"3\"}");
			indexInsertDocument(INDEX_NAME, INDEX_TYPE, "known-4", "{\"name\":\"test4\",\"idx\":\"4\"}");
			indexFlush(INDEX_NAME);

			// case - no paging nor filtering
			{
				StreamingOutput out = tested.getAll(null, null, null);
				TestUtils
						.assetStreamingOutputContent(
								"{\"total\":4,\"hits\":[{\"id\":\"known-1\",\"data\":{\"idx\":\"1\",\"name\":\"test1\"}},{\"id\":\"known-2\",\"data\":{\"idx\":\"2\",\"name\":\"test2\"}},{\"id\":\"known-3\",\"data\":{\"idx\":\"3\",\"name\":\"test3\"}},{\"id\":\"known-4\",\"data\":{\"idx\":\"4\",\"name\":\"test4\"}}]}",
								out);
			}

			// case - paging, no filtering
			{
				StreamingOutput out = tested.getAll(1, 2, null);
				TestUtils
						.assetStreamingOutputContent(
								"{\"total\":4,\"hits\":[{\"id\":\"known-2\",\"data\":{\"idx\":\"2\",\"name\":\"test2\"}},{\"id\":\"known-3\",\"data\":{\"idx\":\"3\",\"name\":\"test3\"}}]}",
								out);
			}

			// case - paging and filtering
			{
				StreamingOutput out = tested.getAll(1, 2, new String[] { "idx" });
				TestUtils
						.assetStreamingOutputContent(
								"{\"total\":4,\"hits\":[{\"id\":\"known-2\",\"data\":{\"name\":\"test2\"}},{\"id\":\"known-3\",\"data\":{\"name\":\"test3\"}}]}",
								out);
			}

			// case - no paging, but filtering
			{
				StreamingOutput out = tested.getAll(null, null, new String[] { "idx", "name" });
				TestUtils
						.assetStreamingOutputContent(
								"{\"total\":4,\"hits\":[{\"id\":\"known-1\",\"data\":{}},{\"id\":\"known-2\",\"data\":{}},{\"id\":\"known-3\",\"data\":{}},{\"id\":\"known-4\",\"data\":{}}]}",
								out);
			}

		} finally {
			indexDelete(INDEX_NAME);
			finalizeESClientForUnitTest();
		}
	}

	@Test
	public void get() throws InterruptedException {
		Client client = prepareESClientForUnitTest();
		ElasticsearchEntityService tested = getTested(client);
		try {

			// case - index doesn't exists
			{
				Map<String, Object> out = tested.get("10");
				Assert.assertNull(out);
			}

			indexCreate(INDEX_NAME);
			// case - index exists empty
			{
				Map<String, Object> out = tested.get("10");
				Assert.assertNull(out);
			}

			indexInsertDocument(INDEX_NAME, INDEX_TYPE, "10", "{\"name\":\"test1\",\"idx\":\"1\"}");
			indexInsertDocument(INDEX_NAME, INDEX_TYPE, "20", "{\"name\":\"test2\",\"idx\":\"2\"}");
			indexInsertDocument(INDEX_NAME, INDEX_TYPE, "30", "{\"name\":\"test3\",\"idx\":\"3\"}");
			indexFlush(INDEX_NAME);

			// case - index full but object not found
			{
				Map<String, Object> out = tested.get("100");
				Assert.assertNull(out);
			}

			// case - index full but object not found
			{
				Map<String, Object> out = tested.get("10");
				Assert.assertNotNull(out);
				Assert.assertEquals("test1", out.get("name"));
			}
		} finally {
			indexDelete(INDEX_NAME);
			finalizeESClientForUnitTest();
		}
	}

	@Test
	public void create_noid() {
		Client client = prepareESClientForUnitTest();
		ElasticsearchEntityService tested = getTested(client);
		try {

			// case - index doesn't exists
			String id1 = null;
			{
				Map<String, Object> entity = new HashMap<String, Object>();
				entity.put("name", "val");
				id1 = tested.create(entity);

				Assert.assertEquals("val", tested.get(id1).get("name"));
			}

			// case - index exists, create second
			{
				Map<String, Object> entity = new HashMap<String, Object>();
				entity.put("name", "val2");
				String id = tested.create(entity);

				Assert.assertEquals("val2", tested.get(id).get("name"));
				Assert.assertEquals("val", tested.get(id1).get("name"));
			}

		} finally {
			indexDelete(INDEX_NAME);
			finalizeESClientForUnitTest();
		}
	}

	@Test
	public void create_withid() {
		Client client = prepareESClientForUnitTest();
		ElasticsearchEntityService tested = getTested(client);
		try {

			// case - index doesn't exists
			String id1 = "1";
			{
				Map<String, Object> entity = new HashMap<String, Object>();
				entity.put("name", "val");
				tested.create(id1, entity);

				Assert.assertEquals("val", tested.get(id1).get("name"));
			}

			// case - index exists, create second
			String id2 = "2";
			{
				Map<String, Object> entity = new HashMap<String, Object>();
				entity.put("name", "val2");
				tested.create(id2, entity);

				Assert.assertEquals("val2", tested.get(id2).get("name"));
				Assert.assertEquals("val", tested.get(id1).get("name"));
			}

			// case - index exists, update first
			{
				Map<String, Object> entity = new HashMap<String, Object>();
				entity.put("name", "val1_1");
				tested.create(id1, entity);

				Assert.assertEquals("val2", tested.get(id2).get("name"));
				Assert.assertEquals("val1_1", tested.get(id1).get("name"));
			}

		} finally {
			indexDelete(INDEX_NAME);
			finalizeESClientForUnitTest();
		}
	}

	@Test
	public void update() {
		Client client = prepareESClientForUnitTest();
		ElasticsearchEntityService tested = getTested(client);
		try {
			// case - index doesn't exists and record too, so create it
			String id1 = "1";
			{
				Map<String, Object> entity = new HashMap<String, Object>();
				entity.put("name", "val");
				tested.update(id1, entity);

				Assert.assertEquals("val", tested.get(id1).get("name"));
			}

			// case - index exists, create second
			String id2 = "2";
			{
				Map<String, Object> entity = new HashMap<String, Object>();
				entity.put("name", "val2");
				tested.update(id2, entity);

				Assert.assertEquals("val2", tested.get(id2).get("name"));
				Assert.assertEquals("val", tested.get(id1).get("name"));
			}

			// case - index exists, update first
			{
				Map<String, Object> entity = new HashMap<String, Object>();
				entity.put("name", "val1_1");
				tested.update(id1, entity);

				Assert.assertEquals("val2", tested.get(id2).get("name"));
				Assert.assertEquals("val1_1", tested.get(id1).get("name"));
			}

		} finally {
			indexDelete(INDEX_NAME);
			finalizeESClientForUnitTest();
		}
	}

	@Test
	public void delete() throws InterruptedException {
		Client client = prepareESClientForUnitTest();
		ElasticsearchEntityService tested = getTested(client);
		try {

			// case - index doesn't exists
			{
				tested.delete("1");
			}

			indexDelete(INDEX_NAME);
			indexCreate(INDEX_NAME);
			// case - index exists but record not in it
			{
				tested.delete("1");
			}

			indexInsertDocument(INDEX_NAME, INDEX_TYPE, "10", "{\"name\":\"test1\",\"idx\":\"1\"}");
			indexInsertDocument(INDEX_NAME, INDEX_TYPE, "20", "{\"name\":\"test2\",\"idx\":\"2\"}");
			indexInsertDocument(INDEX_NAME, INDEX_TYPE, "30", "{\"name\":\"test3\",\"idx\":\"3\"}");
			indexFlush(INDEX_NAME);
			// case - index exists and record deleted
			{
				tested.delete("10");
				Assert.assertNull(indexGetDocument(INDEX_NAME, INDEX_TYPE, "10"));
				Assert.assertNotNull(indexGetDocument(INDEX_NAME, INDEX_TYPE, "20"));
				Assert.assertNotNull(indexGetDocument(INDEX_NAME, INDEX_TYPE, "30"));

				tested.delete("30");
				Assert.assertNull(indexGetDocument(INDEX_NAME, INDEX_TYPE, "10"));
				Assert.assertNotNull(indexGetDocument(INDEX_NAME, INDEX_TYPE, "20"));
				Assert.assertNull(indexGetDocument(INDEX_NAME, INDEX_TYPE, "30"));
			}

		} finally {
			indexDelete(INDEX_NAME);
			finalizeESClientForUnitTest();
		}
	}

	@Test
	public void getClient() {
		ElasticsearchEntityService tested = getTested(null);
		Assert.assertNotNull(tested.getClient());
		Assert.assertEquals(tested.client, tested.getClient());
	}
}
