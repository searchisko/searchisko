/*
 * JBoss, Home of Professional Open Source
 * Copyright 2014 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 */
package org.searchisko.api.filter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;
import org.searchisko.api.service.ConfigService;

/**
 * Unit test for {@link CORSWithCredentialsFilter}.
 * 
 * @author Vlastimil Elias (velias at redhat dot com)
 */
public class CORSWithCredentialsFilterTest {

	private static final String KEY = "KEY";

	private static final String M_OPTIONS = "OPTIONS";
	private static final String M_GET = "GET";
	private static final String M_POST = "POST";
	private static final String M_PUT = "PUT";
	private static final String M_DELETE = "DELETE";

	private static final String[] METHODS = new String[] { M_GET, M_POST, M_PUT, M_DELETE };

	/**
	 * CORS headers
	 */
	private static final String HEADER_ORIGIN = "Origin";

	private static final String ACCESS_CONTROL_ALLOW_ORIGIN = "Access-Control-Allow-Origin";
	private static final String ACCESS_CONTROL_ALLOW_CREDENTIALS = "Access-Control-Allow-Credentials";

	private static final String ACCESS_CONTROL_ALLOW_HEADERS = "Access-Control-Allow-Headers";
	private static final String ACCESS_CONTROL_ALLOW_METHODS = "Access-Control-Allow-Methods";
	private static final String ACCESS_CONTROL_MAX_AGE = "Access-Control-Max-Age";

	@Test
	public void doFilter_NO_CORS_NO_OPTIONS() throws IOException, ServletException {
		CORSWithCredentialsFilter tested = getTested();

		HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
		HttpServletResponse response = Mockito.mock(HttpServletResponse.class);
		FilterChain chain = Mockito.mock(FilterChain.class);

		for (String m : METHODS) {
			Mockito.reset(request, response, chain);
			Mockito.when(request.getMethod()).thenReturn(m);

			String ho = null;
			if (M_GET.equals(m))
				ho = " ";

			Mockito.when(request.getHeader(HEADER_ORIGIN)).thenReturn(ho);

			tested.doFilter(request, response, chain);

			// nothing called on response as it is not CORS request
			Mockito.verifyZeroInteractions(response);
			// but request is passed to other handling
			Mockito.verify(chain).doFilter(request, response);
		}
	}

	@Test
	public void doFilter_NO_CORS_OPTIONS() throws IOException, ServletException {
		CORSWithCredentialsFilter tested = getTested();

		HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
		HttpServletResponse response = Mockito.mock(HttpServletResponse.class);
		FilterChain chain = Mockito.mock(FilterChain.class);

		Mockito.when(request.getHeader(HEADER_ORIGIN)).thenReturn(null);
		Mockito.when(request.getMethod()).thenReturn(M_OPTIONS);

		tested.doFilter(request, response, chain);

		// nothing called on response as it is not CORS request
		Mockito.verifyZeroInteractions(response);
		// but request is passed to other handling
		Mockito.verify(chain).doFilter(request, response);

	}

	@Test
	public void doFilter_CORS_NO_OPTIONS() throws IOException, ServletException {
		CORSWithCredentialsFilter tested = getTested();

		HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
		HttpServletResponse response = Mockito.mock(HttpServletResponse.class);
		FilterChain chain = Mockito.mock(FilterChain.class);

		for (String m : METHODS) {
			String domain = "http://mydomain.org";
			if (M_POST.equals(m)) {
				// special domain defined in CORS SPEC
				domain = "null";
			}
			Mockito.reset(request, response, chain);
			Mockito.when(request.getMethod()).thenReturn(m);
			Mockito.when(request.getHeader(HEADER_ORIGIN)).thenReturn(domain);

			tested.doFilter(request, response, chain);

			// headers set
			Mockito.verify(response).setHeader(ACCESS_CONTROL_ALLOW_ORIGIN, domain);
			Mockito.verify(response).setHeader(ACCESS_CONTROL_ALLOW_CREDENTIALS, "true");
			Mockito.verifyNoMoreInteractions(response);
			// then request is passed to other handling
			Mockito.verify(chain).doFilter(request, response);
		}
	}

	@Test
	public void doFilter_CORS_OPTIONS() throws IOException, ServletException {
		CORSWithCredentialsFilter tested = getTested();

		HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
		HttpServletResponse response = Mockito.mock(HttpServletResponse.class);
		FilterChain chain = Mockito.mock(FilterChain.class);

		String domain = "http://mydomain.org";
		Mockito.when(request.getMethod()).thenReturn(M_OPTIONS);
		Mockito.when(request.getHeader(HEADER_ORIGIN)).thenReturn(domain);

		tested.doFilter(request, response, chain);

		// headers set
		Mockito.verify(response).setHeader(ACCESS_CONTROL_ALLOW_ORIGIN, domain);
		Mockito.verify(response).setHeader(ACCESS_CONTROL_ALLOW_CREDENTIALS, "true");
		Mockito.verify(response).setHeader(Mockito.eq(ACCESS_CONTROL_MAX_AGE), Mockito.notNull(String.class));
		Mockito.verify(response).setHeader(Mockito.eq(ACCESS_CONTROL_ALLOW_HEADERS), Mockito.notNull(String.class));
		for (String m : METHODS) {
			Mockito.verify(response).addHeader(ACCESS_CONTROL_ALLOW_METHODS, m);
		}
		Mockito.verify(response).setStatus(HttpServletResponse.SC_OK);
		Mockito.verifyNoMoreInteractions(response);

		// request is NOT passed to other handling!
		Mockito.verifyZeroInteractions(chain);
	}

	@Test
	public void getConfigList() {
		CORSWithCredentialsFilter tested = getTested();

		Assert.assertNull(tested.getConfigList(null, KEY));

		Map<String, Object> config = new HashMap<>();
		Assert.assertNull(tested.getConfigList(config, KEY));

		// case - bad type
		config.put(KEY, "aaa");
		Assert.assertNull(tested.getConfigList(config, KEY));

		// case - empty list
		List<String> list = new ArrayList<>();
		config.put(KEY, list);
		Assert.assertNull(tested.getConfigList(config, KEY));

		list.add("aaa");
		Assert.assertEquals(list, tested.getConfigList(config, KEY));

	}

	private CORSWithCredentialsFilter getTested() {
		CORSWithCredentialsFilter tested = new CORSWithCredentialsFilter();
		tested.configService = Mockito.mock(ConfigService.class);
		tested.log = Logger.getLogger("test logger");
		return tested;
	}
}
