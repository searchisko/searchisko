/*
 * JBoss, Home of Professional Open Source
 * Copyright 2012 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 */
package org.jboss.dcp.api.rest;

import java.security.Principal;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.core.UriInfo;

import org.jboss.dcp.api.annotations.security.GuestAllowed;
import org.jboss.dcp.api.annotations.security.ProviderAllowed;
import org.jboss.resteasy.core.ResourceMethod;
import org.jboss.resteasy.core.ServerResponse;
import org.jboss.resteasy.plugins.server.embedded.SimplePrincipal;
import org.jboss.resteasy.spi.HttpRequest;
import org.jboss.resteasy.spi.ResteasyProviderFactory;
import org.jboss.resteasy.util.Base64;
import org.jboss.resteasy.util.HttpResponseCodes;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

/**
 * Unit test for {@link SecurityPreProcessInterceptor}
 * 
 * @author Vlastimil Elias (velias at redhat dot com)
 */
public class SecurityPreProcessInterceptorTest {

	private SecurityPreProcessInterceptor getTested() {
		SecurityPreProcessInterceptor tested = new SecurityPreProcessInterceptor();
		tested.securityContext = Mockito.mock(SecurityContext.class);
		tested.log = Logger.getLogger("testlogger");
		return tested;
	}

	@SuppressWarnings("unchecked")
	@Test
	public void preProcess() throws SecurityException, NoSuchMethodException {
		SecurityPreProcessInterceptor tested = getTested();
		HttpRequest requestMock = Mockito.mock(HttpRequest.class);
		HttpHeaders httpHeadersMock = Mockito.mock(HttpHeaders.class);
		UriInfo uriInfoMock = Mockito.mock(UriInfo.class);
		ResourceMethod methodMock = Mockito.mock(ResourceMethod.class);
		Mockito.when((Class<MethodAnnotationsMock>) methodMock.getResourceClass()).thenReturn(
				MethodAnnotationsMock.class);
		Mockito.when(methodMock.getMethod()).thenReturn(MethodAnnotationsMock.class.getMethod("methodProviderAllowed"));

		Mockito.when(requestMock.getHttpHeaders()).thenReturn(httpHeadersMock);
		Mockito.when(requestMock.getUri()).thenReturn(uriInfoMock);
		MultivaluedMap<String, String> queryParamsMock = Mockito.mock(MultivaluedMap.class);
		Mockito.when(queryParamsMock.getFirst("provider")).thenReturn("uname");
		Mockito.when(queryParamsMock.getFirst("pwd")).thenReturn("pwd");

		// case - no any authentication provided
		{
			ServerResponse res = tested.preProcess(requestMock, methodMock);
			Assert.assertNotNull(res);
			Assert.assertEquals(HttpResponseCodes.SC_UNAUTHORIZED, res.getStatus());
			Assert.assertEquals("Basic realm=\"Insert Provider's username and password\"",
					res.getMetadata().get("WWW-Authenticate").get(0));
		}

		Principal simplePrincipal = new SimplePrincipal("uname");

		// case - basic authentication OK
		{
			Mockito.reset(tested.securityContext, httpHeadersMock, uriInfoMock);
			ResteasyProviderFactory.pushContext(SecurityContext.class, new CustomSecurityContext(simplePrincipal,
					false, true, "BASIC"));
			List<String> value = new ArrayList<String>();
			value.add("NTLM:sdfsdfkjskdfs");
			value.add("Basic:" + Base64.encodeBytes("uname".getBytes()));
			value.add("Basic:" + Base64.encodeBytes("uname:pwd".getBytes()));
			value.add("PPP:sdfsdfkjskdfs");
			Mockito.when(httpHeadersMock.getRequestHeader("Authorization")).thenReturn(value);
			Mockito.when(tested.securityContext.getUserPrincipal()).thenReturn(simplePrincipal);
			ServerResponse res = tested.preProcess(requestMock, methodMock);
			Assert.assertNull(res);
			SecurityContext ctx = ResteasyProviderFactory.getContextData(SecurityContext.class);
			Assert.assertEquals("uname", ctx.getUserPrincipal().getName());
			Assert.assertEquals("BASIC", ctx.getAuthenticationScheme());
		}

		// case - basic authentication FAIL
		{
			Mockito.reset(tested.securityContext, httpHeadersMock, uriInfoMock);
			ResteasyProviderFactory.clearContextData();
			List<String> value = new ArrayList<String>();
			value.add("NTLM:sdfsdfkjskdfs");
			value.add("Basic:" + Base64.encodeBytes("uname:pwd".getBytes()));
			value.add("PPP:sdfsdfkjskdfs");
			Mockito.when(httpHeadersMock.getRequestHeader("Authorization")).thenReturn(value);
			Mockito.when(tested.securityContext.getUserPrincipal()).thenReturn(null);
			ServerResponse res = tested.preProcess(requestMock, methodMock);
			Assert.assertNotNull(res);
			Assert.assertEquals(HttpResponseCodes.SC_UNAUTHORIZED, res.getStatus());
			Assert.assertEquals("Basic realm=\"Insert Provider's username and password\"",
					res.getMetadata().get("WWW-Authenticate").get(0));
			Assert.assertNull(ResteasyProviderFactory.getContextData(SecurityContext.class));
		}

		// case - custom authentication OK
		{
			Mockito.reset(tested.securityContext, httpHeadersMock, uriInfoMock);
			ResteasyProviderFactory.clearContextData();
			ResteasyProviderFactory.pushContext(SecurityContext.class, new CustomSecurityContext(simplePrincipal,
					false, true, "CUSTOM"));
			Mockito.when(uriInfoMock.getQueryParameters()).thenReturn(queryParamsMock);
			Mockito.when(tested.securityContext.getUserPrincipal()).thenReturn(simplePrincipal);
			ServerResponse res = tested.preProcess(requestMock, methodMock);
			Assert.assertNull(res);
			SecurityContext ctx = ResteasyProviderFactory.getContextData(SecurityContext.class);
			Assert.assertEquals("uname", ctx.getUserPrincipal().getName());
			Assert.assertEquals("CUSTOM", ctx.getAuthenticationScheme());
		}

		// case - custom authentication FAIL
		{
			Mockito.reset(tested.securityContext, httpHeadersMock, uriInfoMock);
			ResteasyProviderFactory.clearContextData();
			Mockito.when(uriInfoMock.getQueryParameters()).thenReturn(queryParamsMock);
			Mockito.when(tested.securityContext.getUserPrincipal()).thenReturn(null);
			ServerResponse res = tested.preProcess(requestMock, methodMock);
			Assert.assertNotNull(res);
			Assert.assertEquals(HttpResponseCodes.SC_UNAUTHORIZED, res.getStatus());
			Assert.assertEquals("Basic realm=\"Insert Provider's username and password\"",
					res.getMetadata().get("WWW-Authenticate").get(0));
			Assert.assertNull(ResteasyProviderFactory.getContextData(SecurityContext.class));
		}

		// case - user is not superprovider but superprovider is required by annotation so FAIL
		{
			Mockito.reset(tested.securityContext, httpHeadersMock, uriInfoMock, methodMock);
			ResteasyProviderFactory.clearContextData();
			ResteasyProviderFactory.pushContext(SecurityContext.class, new CustomSecurityContext(simplePrincipal,
					false, true, "CUSTOM"));
			Mockito.when((Class<ClassSuperProviderAllowedMock>) methodMock.getResourceClass()).thenReturn(
					ClassSuperProviderAllowedMock.class);
			Mockito.when(methodMock.getMethod()).thenReturn(
					ClassSuperProviderAllowedMock.class.getMethod("methodNotAnnotated"));
			Mockito.when(uriInfoMock.getQueryParameters()).thenReturn(queryParamsMock);
			Mockito.when(tested.securityContext.getUserPrincipal()).thenReturn(simplePrincipal);
			Mockito.when(tested.securityContext.isUserInRole(CustomSecurityContext.SUPER_ADMIN_ROLE)).thenReturn(false);
			ServerResponse res = tested.preProcess(requestMock, methodMock);
			Assert.assertNotNull(res);
			Assert.assertEquals(HttpResponseCodes.SC_FORBIDDEN, res.getStatus());
			SecurityContext ctx = ResteasyProviderFactory.getContextData(SecurityContext.class);
			Assert.assertEquals("uname", ctx.getUserPrincipal().getName());
			Assert.assertEquals("CUSTOM", ctx.getAuthenticationScheme());
		}

		// case - user is superprovider and superprovider is required by annotation so OK
		{
			Mockito.reset(tested.securityContext, httpHeadersMock, uriInfoMock, methodMock);
			ResteasyProviderFactory.clearContextData();
			ResteasyProviderFactory.pushContext(SecurityContext.class, new CustomSecurityContext(simplePrincipal,
					false, true, "CUSTOM"));
			Mockito.when((Class<ClassSuperProviderAllowedMock>) methodMock.getResourceClass()).thenReturn(
					ClassSuperProviderAllowedMock.class);
			Mockito.when(methodMock.getMethod()).thenReturn(
					ClassSuperProviderAllowedMock.class.getMethod("methodNotAnnotated"));
			Mockito.when(uriInfoMock.getQueryParameters()).thenReturn(queryParamsMock);
			Mockito.when(tested.securityContext.getUserPrincipal()).thenReturn(simplePrincipal);
			Mockito.when(tested.securityContext.isUserInRole(CustomSecurityContext.SUPER_ADMIN_ROLE)).thenReturn(true);
			ServerResponse res = tested.preProcess(requestMock, methodMock);
			Assert.assertNull(res);
			SecurityContext ctx = ResteasyProviderFactory.getContextData(SecurityContext.class);
			Assert.assertEquals("uname", ctx.getUserPrincipal().getName());
			Assert.assertEquals("CUSTOM", ctx.getAuthenticationScheme());
		}
	}

	@Test
	public void getProviderAlowedAnnotation() throws SecurityException, NoSuchMethodException {
		SecurityPreProcessInterceptor tested = getTested();

		Assert.assertNull(tested.getProviderAlowedAnnotation(MethodAnnotationsMock.class,
				MethodAnnotationsMock.class.getMethod("methodNotAnnotated")));
		Assert.assertNull(tested.getProviderAlowedAnnotation(MethodAnnotationsMock.class,
				MethodAnnotationsMock.class.getMethod("methodGuestAllowed")));

		Assert.assertNotNull(tested.getProviderAlowedAnnotation(MethodAnnotationsMock.class,
				MethodAnnotationsMock.class.getMethod("methodProviderAllowed")));
		Assert.assertNotNull(tested.getProviderAlowedAnnotation(ClassProviderAllowedMock.class,
				ClassProviderAllowedMock.class.getMethod("methodNotAnnotated")));
		Assert.assertNotNull(tested.getProviderAlowedAnnotation(SubclassProviderAllowedMock.class,
				SubclassProviderAllowedMock.class.getMethod("methodNotAnnotated")));
	}

	@Test
	public void accept() throws SecurityException, NoSuchMethodException {
		SecurityPreProcessInterceptor tested = getTested();

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

	public static class MethodAnnotationsMock {

		@GuestAllowed
		public void methodGuestAllowed() {

		}

		@ProviderAllowed
		public void methodProviderAllowed() {

		}

		public void methodNotAnnotated() {

		}

	}

	@ProviderAllowed
	public static class ClassProviderAllowedMock {

		public void methodNotAnnotated() {

		}
	}

	@ProviderAllowed(superProviderOnly = true)
	public static class ClassSuperProviderAllowedMock {

		public void methodNotAnnotated() {

		}
	}

	public static class SubclassProviderAllowedMock extends ClassProviderAllowedMock {

	}

}
