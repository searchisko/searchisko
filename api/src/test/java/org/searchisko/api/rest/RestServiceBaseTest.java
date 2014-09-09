/*
 * JBoss, Home of Professional Open Source
 * Copyright 2012 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 */
package org.searchisko.api.rest;

import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Logger;

import javax.ws.rs.core.StreamingOutput;

import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.common.xcontent.ToXContent;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.json.JSONException;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.searchisko.api.testtools.TestUtils;

/**
 * Unit test for {@link RestServiceBase}.
 * 
 * @author Vlastimil Elias (velias at redhat dot com)
 */
public class RestServiceBaseTest {

	/**
	 * @return RestServiceBase instance for test with initialized logger
	 */
	protected RestServiceBase getTested() {
		RestServiceBase tested = new RestServiceBase();
		tested.log = Logger.getLogger(RestServiceBase.class.getName());
		return tested;
	}

	@Test
	public void createResponse_Map() {
		RestServiceBase tested = getTested();
		GetResponse rMock = Mockito.mock(GetResponse.class);
		Map<String, Object> m = new HashMap<String, Object>();
		Mockito.when(rMock.getSource()).thenReturn(m);
		Assert.assertEquals(m, tested.createResponse(rMock));
	}

	@Test
	public void createResponse_StreamingOutput() throws IOException, JSONException {
		RestServiceBase tested = getTested();
		SearchResponse srMock = Mockito.mock(SearchResponse.class);
		Mockito.doAnswer(new Answer<Object>() {

			@Override
			public Object answer(InvocationOnMock invocation) throws Throwable {
				Object[] args = invocation.getArguments();
				XContentBuilder resp = (XContentBuilder) args[0];
				resp.field("testfield", "testvalue");
				return null;
			}
		}).when(srMock).toXContent(Mockito.any(XContentBuilder.class), Mockito.eq(ToXContent.EMPTY_PARAMS));

		Map<String, String> af = new LinkedHashMap<String, String>();
		TestUtils.assetJsonStreamingOutputContent("{\"testfield\":\"testvalue\"}", tested.createResponse(srMock, af));

		af.put("uuid", "myid");
		af.put("ag", "qag");
		af.put("oo", null);
		StreamingOutput so = tested.createResponse(srMock, af);
		TestUtils.assetJsonStreamingOutputContent("{\"uuid\":\"myid\",\"ag\":\"qag\",\"oo\":null,\"testfield\":\"testvalue\"}",
				so);

		TestUtils.assetJsonStreamingOutputContent("{\"testfield\":\"testvalue\"}", tested.createResponse(srMock, null));
	}
}
