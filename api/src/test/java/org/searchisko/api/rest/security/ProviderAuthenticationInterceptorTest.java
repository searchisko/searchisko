/*
 * JBoss, Home of Professional Open Source
 * Copyright 2012 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 */
package org.searchisko.api.rest.security;

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
import org.searchisko.api.rest.security.ProviderSecurityPreProcessInterceptorTest.ClassProviderAllowedMock;
import org.searchisko.api.rest.security.ProviderSecurityPreProcessInterceptorTest.ClassSuperProviderAllowedMock;
import org.searchisko.api.rest.security.ProviderSecurityPreProcessInterceptorTest.MethodAnnotationsMock;
import org.searchisko.api.rest.security.ProviderSecurityPreProcessInterceptorTest.SubclassProviderAllowedMock;
import org.searchisko.api.service.ProviderService;

/**
 * Unit test for {@link ProviderAuthenticationInterceptor}
 * 
 * @author Libor Krzyzanek
 * @author Vlastimil Elias (velias at redhat dot com)
 * 
 */
public class ProviderAuthenticationInterceptorTest {

	private ProviderAuthenticationInterceptor getTested() {
		ProviderAuthenticationInterceptor tested = new ProviderAuthenticationInterceptor();
		tested.providerService = Mockito.mock(ProviderService.class);
		tested.log = Logger.getLogger("testlogger");
		return tested;
	}

	@Test
	public void testNotAuthenticated() {
		ProviderAuthenticationInterceptor tested = getTested();

		HttpRequest requestMock = getHttpRequestMock();
		ResourceMethod methodMock = Mockito.mock(ResourceMethod.class);

		ServerResponse res = tested.preProcess(requestMock, methodMock);
		Assert.assertNull(res);
		Mockito.verify(tested.providerService, Mockito.times(0)).authenticate(Mockito.anyString(), Mockito.anyString());
	}

	@Test
	public void testBasicAuthentication() {
		ProviderAuthenticationInterceptor tested = getTested();

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
		Assert.assertEquals(ProviderCustomSecurityContext.class, ctx.getClass());
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

	@Test
	public void acceptTest() throws SecurityException, NoSuchMethodException {
		ProviderAuthenticationInterceptor tested = getTested();

		Assert.assertFalse(tested.accept(MethodAnnotationsMock.class,
				MethodAnnotationsMock.class.getMethod("methodNotAnnotated")));
		Assert.assertFalse(tested.accept(MethodAnnotationsMock.class,
				MethodAnnotationsMock.class.getMethod("methodGuestAllowed")));

		Assert.assertTrue(tested.accept(MethodAnnotationsMock.class,
				MethodAnnotationsMock.class.getMethod("methodProviderAllowed")));
		Assert.assertTrue(tested.accept(ClassProviderAllowedMock.class,
				ClassProviderAllowedMock.class.getMethod("methodNotAnnotated")));
		Assert.assertTrue(tested.accept(SubclassProviderAllowedMock.class,
				SubclassProviderAllowedMock.class.getMethod("methodNotAnnotated")));
		Assert.assertTrue(tested.accept(ClassSuperProviderAllowedMock.class,
				ClassSuperProviderAllowedMock.class.getMethod("methodNotAnnotated")));
	}

}
