/*
 * JBoss, Home of Professional Open Source
 * Copyright 2012 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 */
package org.jboss.dcp.api.rest;

import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.StreamingOutput;

import junit.framework.Assert;

import org.elasticsearch.action.search.SearchResponse;
import org.jboss.dcp.api.service.ContributorService;
import org.jboss.dcp.api.testtools.TestUtils;
import org.junit.Test;
import org.mockito.Mockito;

/**
 * Unit test for {@link ContributorRestService}.
 * 
 * @author Vlastimil Elias (velias at redhat dot com)
 */
public class ContributorRestServiceTest {

	@Test
	public void init() {
		ContributorRestService tested = new ContributorRestService();
		Assert.assertNull(tested.entityService);
		tested.contributorService = Mockito.mock(ContributorService.class);
		Assert.assertNull(tested.entityService);
		tested.init();
		Assert.assertEquals(tested.contributorService, tested.entityService);
	}

	@Test
	public void search() throws Exception {
		ContributorRestService tested = new ContributorRestService();
		tested.contributorService = Mockito.mock(ContributorService.class);
		RestEntityServiceBaseTest.mockLogger(tested);

		// case - return from service OK, one result
		{
			SearchResponse sr = ESDataOnlyResponseTest.mockSearchResponse("ve", "email@em", null, null);
			Mockito.when(tested.contributorService.search("email@em")).thenReturn(sr);
			StreamingOutput ret = (StreamingOutput) tested.search("email@em");
			ESDataOnlyResponseTest.assetStreamingOutputContent(
					"{\"total\":1,\"hits\":[{\"id\":\"ve\",\"data\":{\"dcp_id\":\"ve\",\"dcp_name\":\"email@em\"}}]}", ret);
		}

		// case - return from service OK, no result
		{
			Mockito.reset(tested.contributorService);
			SearchResponse sr = ESDataOnlyResponseTest.mockSearchResponse(null, null, null, null);
			Mockito.when(tested.contributorService.search("email@em")).thenReturn(sr);
			StreamingOutput ret = (StreamingOutput) tested.search("email@em");
			ESDataOnlyResponseTest.assetStreamingOutputContent("{\"total\":0,\"hits\":[]}", ret);
		}

		// case - Exception from service
		{
			Mockito.reset(tested.contributorService);
			Mockito.when(tested.contributorService.search("email@em")).thenThrow(new RuntimeException("test exception"));
			TestUtils.assertResponseStatus(tested.search("email@em"), Status.INTERNAL_SERVER_ERROR);
		}
	}

}
