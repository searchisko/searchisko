/*
 * JBoss, Home of Professional Open Source
 * Copyright 2012 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 */
package org.searchisko.api.rest;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.core.UriInfo;

import org.jboss.resteasy.core.ResourceMethod;
import org.jboss.resteasy.core.ServerResponse;
import org.jboss.resteasy.spi.HttpRequest;
import org.jboss.resteasy.spi.ResteasyProviderFactory;
import org.jboss.resteasy.util.Base64;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;
import org.searchisko.api.service.ProviderService;

/**
 * @author Libor Krzyzanek
 * @author Vlastimil Elias (velias at redhat dot com)
 *
 */
public class AuthenticationInterceptorTest {

	String uname = "uname";
	String pwd = "pwd";

	private AuthenticationInterceptor getTested() {
		AuthenticationInterceptor tested = new AuthenticationInterceptor();
		tested.providerService = Mockito.mock(ProviderService.class);
		tested.log = Logger.getLogger("testlogger");
		return tested;
	}

	/**
	 * Test method for
	 * {@link org.searchisko.api.rest.AuthenticationInterceptor#preProcess(org.jboss.resteasy.spi.HttpRequest, org.jboss.resteasy.core.ResourceMethod)}
	 * .
	 */
	@Test
	public void testNotAuthenticated() {
		AuthenticationInterceptor tested = getTested();

		HttpRequest requestMock = getHttpRequestMock();
		ResourceMethod methodMock = Mockito.mock(ResourceMethod.class);

		ServerResponse res = tested.preProcess(requestMock, methodMock);
		Assert.assertNull(res);
		Mockito.verify(tested.providerService, Mockito.times(0)).authenticate(Mockito.anyString(), Mockito.anyString());
	}

	/**
	 * Test method for
	 * {@link org.searchisko.api.rest.AuthenticationInterceptor#preProcess(org.jboss.resteasy.spi.HttpRequest, org.jboss.resteasy.core.ResourceMethod)}
	 * .
	 */
	@Test
	public void testBasicAuthentication() {
		AuthenticationInterceptor tested = getTested();

		HttpRequest requestMock = getHttpRequestMock();
		ResourceMethod methodMock = Mockito.mock(ResourceMethod.class);

		String uname = "uname";
		String pwd = "pwd";

		List<String> value = new ArrayList<String>();
		value.add("NTLM:sdfsdfkjskdfs");
		value.add("Basic:" + Base64.encodeBytes(uname.getBytes()));
		value.add("Basic:" + Base64.encodeBytes((uname + ":" + pwd).getBytes()));
		value.add("PPP:sdfsdfkjskdfs");
		Mockito.when(requestMock.getHttpHeaders().getRequestHeader("Authorization")).thenReturn(value);

		Mockito.when(tested.providerService.authenticate(uname, pwd)).thenReturn(true);

		ServerResponse res = tested.preProcess(requestMock, methodMock);
		Assert.assertNull(res);

		SecurityContext ctx = ResteasyProviderFactory.getContextData(SecurityContext.class);
		Assert.assertEquals(uname, ctx.getUserPrincipal().getName());
		Assert.assertEquals("BASIC", ctx.getAuthenticationScheme());
	}

	public HttpRequest getHttpRequestMock() {
		HttpRequest requestMock = Mockito.mock(HttpRequest.class);
		HttpHeaders httpHeadersMock = Mockito.mock(HttpHeaders.class);
		UriInfo uriInfoMock = Mockito.mock(UriInfo.class);

		Mockito.when(requestMock.getHttpHeaders()).thenReturn(httpHeadersMock);
		Mockito.when(requestMock.getUri()).thenReturn(uriInfoMock);

		return requestMock;
	}

}
