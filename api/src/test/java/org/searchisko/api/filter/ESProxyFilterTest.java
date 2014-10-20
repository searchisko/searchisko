/*
 * JBoss, Home of Professional Open Source
 * Copyright 2014 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 */
package org.searchisko.api.filter;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.Principal;
import java.util.logging.Logger;

import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.http.HttpHeaders;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;
import org.searchisko.api.security.Role;
import org.searchisko.api.service.SearchClientService;
import org.searchisko.api.service.StatsClientService;

/**
 * Unit test for {@link ESProxyFilter}
 * 
 * @author Vlastimil Elias (velias at redhat dot com)
 */
public class ESProxyFilterTest {

	@Test
	public void init() throws ServletException {
		ESProxyFilter tested = getTested();

		FilterConfig filterConfig = Mockito.mock(FilterConfig.class);

		Mockito.when(filterConfig.getInitParameter(ESProxyFilter.CFG_USE_STATS_CLIENT)).thenReturn(null);
		tested.init(filterConfig);
		Assert.assertFalse(tested.useStatsClient);
		Assert.assertNotNull(tested.proxyClient);
		Mockito.verify(filterConfig).getInitParameter(ESProxyFilter.CFG_USE_STATS_CLIENT);
		tested.destroy();
		Assert.assertNull(tested.proxyClient);

		Mockito.reset(filterConfig);
		Mockito.when(filterConfig.getInitParameter(ESProxyFilter.CFG_USE_STATS_CLIENT)).thenReturn("");
		tested.init(filterConfig);
		Assert.assertFalse(tested.useStatsClient);
		tested.destroy();

		Mockito.reset(filterConfig);
		Mockito.when(filterConfig.getInitParameter(ESProxyFilter.CFG_USE_STATS_CLIENT)).thenReturn("  ");
		tested.init(filterConfig);
		Assert.assertFalse(tested.useStatsClient);
		tested.destroy();

		Mockito.reset(filterConfig);
		Mockito.when(filterConfig.getInitParameter(ESProxyFilter.CFG_USE_STATS_CLIENT)).thenReturn("yes");
		tested.init(filterConfig);
		Assert.assertTrue(tested.useStatsClient);
		tested.destroy();

		Mockito.reset(filterConfig);
		Mockito.when(filterConfig.getInitParameter(ESProxyFilter.CFG_USE_STATS_CLIENT)).thenReturn("true");
		tested.init(filterConfig);
		Assert.assertTrue(tested.useStatsClient);
		tested.destroy();
	}

	@Test
	public void getElasticsearchClientService() {
		ESProxyFilter tested = getTested();
		tested.searchClientService = Mockito.mock(SearchClientService.class);
		tested.statsClientService = Mockito.mock(StatsClientService.class);

		Assert.assertEquals(tested.searchClientService, tested.getElasticsearchClientService());

		tested.useStatsClient = true;
		Assert.assertEquals(tested.statsClientService, tested.getElasticsearchClientService());

		tested.useStatsClient = false;
		Assert.assertEquals(tested.searchClientService, tested.getElasticsearchClientService());
	}

	@Test(expected = ServletException.class)
	public void getEsNodeURI() throws ServletException {
		ESProxyFilter tested = getTested();
		tested.searchClientService = Mockito.mock(SearchClientService.class);
		Mockito.when(tested.searchClientService.getClient()).thenReturn(null);

		// case - no NPE if ES client not available (as is if stats cluster is disabled)
		tested.getEsNodeURI();

		// case - other tests are hard to do as ES API is used here
	}

	@Test
	public void rewriteRequestUrl() throws URISyntaxException {
		ESProxyFilter tested = getTested();

		URI targetUri = new URI("http://12.5.6.87:9200");
		HttpServletRequest servletRequest = Mockito.mock(HttpServletRequest.class);

		Assert.assertEquals("http://12.5.6.87:9200", tested.rewriteRequestUrl(servletRequest, targetUri));

		Mockito.when(servletRequest.getRequestURI()).thenReturn("/v2/rest/sys/es/search");
		Assert.assertEquals("http://12.5.6.87:9200", tested.rewriteRequestUrl(servletRequest, targetUri));

		Mockito.when(servletRequest.getRequestURI()).thenReturn("/v2/rest/sys/es/search/");
		Assert.assertEquals("http://12.5.6.87:9200", tested.rewriteRequestUrl(servletRequest, targetUri));

		Mockito.when(servletRequest.getRequestURI()).thenReturn("/v2/rest/sys/es/search/_all");
		Assert.assertEquals("http://12.5.6.87:9200/_all", tested.rewriteRequestUrl(servletRequest, targetUri));

		Mockito.when(servletRequest.getRequestURI()).thenReturn("/v2/rest/sys/es/stats/_all/mytype");
		Assert.assertEquals("http://12.5.6.87:9200/_all/mytype", tested.rewriteRequestUrl(servletRequest, targetUri));

		// case - some context is added
		Mockito.when(servletRequest.getRequestURI()).thenReturn("/context/v2/rest/sys/es/stats/_all/mytype");
		Assert.assertEquals("http://12.5.6.87:9200/_all/mytype", tested.rewriteRequestUrl(servletRequest, targetUri));
	}

	@Test
	public void doFilter() throws IOException, ServletException {
		ESProxyFilter tested = Mockito.mock(ESProxyFilter.class);

		HttpServletRequest requestMock = Mockito.mock(HttpServletRequest.class);
		HttpServletResponse responseMock = Mockito.mock(HttpServletResponse.class);
		FilterChain filterChainMock = Mockito.mock(FilterChain.class);

		// case - admin role so proxy is performed
		Mockito
				.doCallRealMethod()
				.when(tested)
				.doFilter(Mockito.any(ServletRequest.class), Mockito.any(ServletResponse.class), Mockito.any(FilterChain.class));
		Mockito.when(requestMock.isUserInRole(Role.ADMIN)).thenReturn(true);

		tested.doFilter(requestMock, responseMock, filterChainMock);

		Mockito.verify(tested).doFilter(requestMock, responseMock, filterChainMock);
		Mockito.verify(tested).doProxyCall(requestMock, responseMock);
		Mockito.verify(requestMock).isUserInRole(Role.ADMIN);
		Mockito.verifyNoMoreInteractions(requestMock, responseMock, filterChainMock);

		// case - no admin role and not authenticated user
		Mockito.reset(tested, requestMock, responseMock, filterChainMock);
		Mockito
				.doCallRealMethod()
				.when(tested)
				.doFilter(Mockito.any(ServletRequest.class), Mockito.any(ServletResponse.class), Mockito.any(FilterChain.class));
		Mockito.when(requestMock.isUserInRole(Role.ADMIN)).thenReturn(false);
		Mockito.when(requestMock.getUserPrincipal()).thenReturn(null);

		tested.doFilter(requestMock, responseMock, filterChainMock);

		Mockito.verify(tested).doFilter(requestMock, responseMock, filterChainMock);
		Mockito.verify(requestMock).isUserInRole(Role.ADMIN);
		Mockito.verify(requestMock).getUserPrincipal();
		Mockito.verify(responseMock).addHeader(Mockito.eq("WWW-Authenticate"), Mockito.anyString());
		Mockito.verify(responseMock).sendError(HttpServletResponse.SC_UNAUTHORIZED);
		Mockito.verifyNoMoreInteractions(requestMock, responseMock, filterChainMock);

		// case - authenticated user but no admin role
		Mockito.reset(tested, requestMock, responseMock, filterChainMock);
		Mockito
				.doCallRealMethod()
				.when(tested)
				.doFilter(Mockito.any(ServletRequest.class), Mockito.any(ServletResponse.class), Mockito.any(FilterChain.class));
		Mockito.when(requestMock.isUserInRole(Role.ADMIN)).thenReturn(false);
		Principal pm = Mockito.mock(Principal.class);
		Mockito.when(requestMock.getUserPrincipal()).thenReturn(pm);

		tested.doFilter(requestMock, responseMock, filterChainMock);

		Mockito.verify(tested).doFilter(requestMock, responseMock, filterChainMock);
		Mockito.verify(requestMock).isUserInRole(Role.ADMIN);
		Mockito.verify(requestMock).getUserPrincipal();
		Mockito.verify(responseMock).sendError(HttpServletResponse.SC_FORBIDDEN);
		Mockito.verifyNoMoreInteractions(requestMock, responseMock, filterChainMock);
	}

	@Test
	public void handleNotModifiedResponse() throws ServletException, IOException {
		ESProxyFilter tested = getTested();

		HttpServletResponse responseMock = Mockito.mock(HttpServletResponse.class);
		Assert.assertTrue(tested.handleNotModifiedResponse(responseMock, HttpServletResponse.SC_NOT_MODIFIED));
		Mockito.verify(responseMock).setStatus(HttpServletResponse.SC_NOT_MODIFIED);
		Mockito.verify(responseMock).setIntHeader(HttpHeaders.CONTENT_LENGTH, 0);

		Mockito.reset(responseMock);
		Assert.assertFalse(tested.handleNotModifiedResponse(responseMock, HttpServletResponse.SC_OK));
		Mockito.verifyZeroInteractions(responseMock);
	}

	private ESProxyFilter getTested() {
		ESProxyFilter tested = new ESProxyFilter();
		tested.log = Logger.getLogger("testlogger");
		return tested;
	}

}
