/*
 * JBoss, Home of Professional Open Source
 * Copyright 2012 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 */
package org.searchisko.api.service;

import java.util.logging.Logger;

import org.elasticsearch.client.Client;
import org.elasticsearch.node.Node;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

/**
 * Unit test for {@link ElasticsearchClientService}
 *
 * @author Vlastimil Elias (velias at redhat dot com)
 */
public class ElasticsearchClientServiceTest {

	@Test
	public void createTransportClient() {
		// not unit testable
	}

	@Test
	public void createEmbeddedNode() {
		// not unit testable
	}

	@Test
	public void getClient() {
		ElasticsearchClientService tested = getTested();

		Assert.assertNull(tested.getClient());

		tested.client = Mockito.mock(Client.class);
		Assert.assertNotNull(tested.getClient());
		Assert.assertEquals(tested.client, tested.getClient());
	}

	@Test
	public void destroy() {
		ElasticsearchClientService tested = getTested();

		Client clientMock = Mockito.mock(Client.class);
		Node nodeMock = Mockito.mock(Node.class);

		// case - no NPE if both client and node empty
		tested.destroy();

		// case - no NPE on empty node
		tested.client = clientMock;
		tested.node = null;
		tested.destroy();
		Mockito.verify(clientMock).close();

		// case - no NPE on empty client
		Mockito.reset(nodeMock, clientMock);
		tested.client = null;
		tested.node = nodeMock;
		tested.destroy();
		Mockito.verify(nodeMock).close();

		// case - both not null
		Mockito.reset(nodeMock, clientMock);
		tested.client = clientMock;
		tested.node = nodeMock;
		tested.destroy();
		Mockito.verify(nodeMock).close();
		Mockito.verify(clientMock).close();

		// case - node closed when exception from client close
		Mockito.reset(nodeMock, clientMock);
		tested.client = clientMock;
		tested.node = nodeMock;
		Mockito.doThrow(new RuntimeException()).when(clientMock).close();
		try {
			tested.destroy();
		} catch (Exception e) {
			// OK
		}
		Mockito.verify(nodeMock).close();
		Mockito.verify(clientMock).close();

		// case - client closed when exception from node close
		Mockito.reset(nodeMock, clientMock);
		tested.client = clientMock;
		tested.node = nodeMock;
		Mockito.doThrow(new RuntimeException()).when(nodeMock).close();
		try {
			tested.destroy();
		} catch (Exception e) {
			// OK
		}
		Mockito.verify(nodeMock).close();
		Mockito.verify(clientMock).close();

	}

	/**
	 * @return
	 */
	protected ElasticsearchClientService getTested() {
		ElasticsearchClientService tested = new ElasticsearchClientService();
		tested.log = Logger.getLogger("testlogger");
		return tested;
	}
}
