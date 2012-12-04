/*
 * JBoss, Home of Professional Open Source
 * Copyright 2012 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 */
package org.jboss.dcp.api.rest;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.Principal;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.core.StreamingOutput;

import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.common.xcontent.ToXContent;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.jboss.dcp.api.testtools.TestUtils;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

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
	public void getProvider() {
		RestServiceBase tested = getTested();
		SecurityContext scMock = Mockito.mock(SecurityContext.class);
		tested.securityContext = scMock;
		Mockito.when(scMock.getUserPrincipal()).thenReturn(new Principal() {

			@Override
			public String getName() {
				return "aa";
			}
		});
		Assert.assertEquals("aa", tested.getProvider());
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
	public void createResponse_StreamingOutput() throws IOException {
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

		StreamingOutput so = tested.createResponse(srMock);
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		so.write(bos);
		Assert.assertEquals("{\"testfield\":\"testvalue\"}", bos.toString());
	}

	@Test
	public void createRequiredFieldResponse() {
		RestServiceBase tested = getTested();
		Response r = TestUtils.assertResponseStatus(tested.createRequiredFieldResponse("myfield"), Status.BAD_REQUEST);
		Assert.assertEquals("Required parameter 'myfield' not set", r.getEntity());
	}

	@Test
	public void createBadFieldDataResponse() {
		RestServiceBase tested = getTested();
		Response r = TestUtils.assertResponseStatus(tested.createBadFieldDataResponse("myfield"), Status.BAD_REQUEST);
		Assert.assertEquals("Parameter 'myfield' has bad value", r.getEntity());
	}

	@Test
	public void createErrorResponse() {
		RestServiceBase tested = getTested();
		Response r = TestUtils.assertResponseStatus(tested.createErrorResponse(new Exception("my exception")),
				Status.INTERNAL_SERVER_ERROR);
		Assert.assertEquals("Error [java.lang.Exception]: my exception", r.getEntity());
	}
}
