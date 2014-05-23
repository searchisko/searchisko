/*
 * JBoss, Home of Professional Open Source
 * Copyright 2014 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 */
package org.searchisko.api.filter;

import java.net.URI;
import java.net.URISyntaxException;

import javax.servlet.http.HttpServletRequest;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

/**
 * TODO #98 Unit test for {@link ESProxyFilter}
 * 
 * @author Vlastimil Elias (velias at redhat dot com)
 */
public class ESProxyFilterTest {

	@Test
	public void rewriteRequestUrl() throws URISyntaxException {
		ESProxyFilter tested = new ESProxyFilter();

		URI targetUri = new URI("http://12.5.6.87:9200");
		HttpServletRequest servletRequest = Mockito.mock(HttpServletRequest.class);

		Assert.assertEquals("http://12.5.6.87:9200", tested.rewriteRequestUrl(servletRequest, targetUri));

		Mockito.when(servletRequest.getRequestURI()).thenReturn("/v1/rest/sys/es/search");
		Assert.assertEquals("http://12.5.6.87:9200", tested.rewriteRequestUrl(servletRequest, targetUri));

		Mockito.when(servletRequest.getRequestURI()).thenReturn("/v1/rest/sys/es/search/");
		Assert.assertEquals("http://12.5.6.87:9200", tested.rewriteRequestUrl(servletRequest, targetUri));

		Mockito.when(servletRequest.getRequestURI()).thenReturn("/v1/rest/sys/es/search/_all");
		Assert.assertEquals("http://12.5.6.87:9200/_all", tested.rewriteRequestUrl(servletRequest, targetUri));

		Mockito.when(servletRequest.getRequestURI()).thenReturn("/v1/rest/sys/es/stats/_all/mytype");
		Assert.assertEquals("http://12.5.6.87:9200/_all/mytype", tested.rewriteRequestUrl(servletRequest, targetUri));

	}

}
