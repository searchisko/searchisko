/*
 * JBoss, Home of Professional Open Source
 * Copyright 2012 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 */
package org.searchisko.api.rest;

import java.security.Principal;
import java.util.logging.Logger;

import javax.ws.rs.core.SecurityContext;

import org.searchisko.api.annotations.security.GuestAllowed;
import org.searchisko.api.annotations.security.ProviderAllowed;
import org.jboss.resteasy.core.ResourceMethod;
import org.jboss.resteasy.core.ServerResponse;
import org.jboss.resteasy.plugins.server.embedded.SimplePrincipal;
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

	Principal principal = new SimplePrincipal("uname");

	private SecurityPreProcessInterceptor getTested() {
		SecurityPreProcessInterceptor tested = new SecurityPreProcessInterceptor();
		tested.securityContext = Mockito.mock(SecurityContext.class);
		tested.log = Logger.getLogger("testlogger");
		return tested;
	}

	@Test
	public void notAuthenticatedTest() {
		SecurityPreProcessInterceptor tested = getTested();

		Mockito.when(tested.securityContext.getUserPrincipal()).thenReturn(null);

		ServerResponse res = tested.preProcess(null, null);

		Assert.assertNotNull(res);
		Assert.assertEquals(HttpResponseCodes.SC_UNAUTHORIZED, res.getStatus());
		Assert.assertEquals("Basic realm=\"Insert Provider's username and password\"",
				res.getMetadata().get("WWW-Authenticate").get(0));
	}

	@SuppressWarnings("unchecked")
	@Test
	public void authenticatedTest() throws NoSuchMethodException, SecurityException {
		SecurityPreProcessInterceptor tested = getTested();

		Mockito.when(tested.securityContext.getUserPrincipal()).thenReturn(principal);
		ResourceMethod methodMock = Mockito.mock(ResourceMethod.class);
		Mockito.when((Class<MethodAnnotationsMock>) methodMock.getResourceClass()).thenReturn(MethodAnnotationsMock.class);
		Mockito.when(methodMock.getMethod()).thenReturn(MethodAnnotationsMock.class.getMethod("methodProviderAllowed"));

		ServerResponse res = tested.preProcess(null, methodMock);

		Assert.assertNull(res);
	}

	@SuppressWarnings("unchecked")
	@Test
	public void superProviderTest() throws NoSuchMethodException, SecurityException {
		SecurityPreProcessInterceptor tested = getTested();

		ResourceMethod methodMock = Mockito.mock(ResourceMethod.class);
		Mockito.when((Class<ClassSuperProviderAllowedMock>) methodMock.getResourceClass()).thenReturn(
				ClassSuperProviderAllowedMock.class);
		Mockito.when(methodMock.getMethod()).thenReturn(MethodAnnotationsMock.class.getMethod("methodNotAnnotated"));

		// case - user is not super provider but annotation requires super provider

		{
			Mockito.when(tested.securityContext.getUserPrincipal()).thenReturn(principal);
			Mockito.when(tested.securityContext.isUserInRole(CustomSecurityContext.SUPER_ADMIN_ROLE)).thenReturn(false);

			ServerResponse res = tested.preProcess(null, methodMock);

			Assert.assertNotNull(res);
			Assert.assertEquals(HttpResponseCodes.SC_FORBIDDEN, res.getStatus());
		}
		// case - user is super provider and annotation requires super provider

		{
			Mockito.reset(tested.securityContext);
			Mockito.when(tested.securityContext.getUserPrincipal()).thenReturn(principal);
			Mockito.when(tested.securityContext.isUserInRole(CustomSecurityContext.SUPER_ADMIN_ROLE)).thenReturn(true);

			ServerResponse res = tested.preProcess(null, methodMock);

			Assert.assertNull(res);
		}
	}

	@Test
	public void getProviderAllowedAnnotationTest() throws SecurityException, NoSuchMethodException {

		Assert.assertNull(SecurityPreProcessInterceptor.getProviderAllowedAnnotation(MethodAnnotationsMock.class,
                MethodAnnotationsMock.class.getMethod("methodNotAnnotated")));
		Assert.assertNull(SecurityPreProcessInterceptor.getProviderAllowedAnnotation(MethodAnnotationsMock.class,
                MethodAnnotationsMock.class.getMethod("methodGuestAllowed")));

		Assert.assertNotNull(SecurityPreProcessInterceptor.getProviderAllowedAnnotation(MethodAnnotationsMock.class,
                MethodAnnotationsMock.class.getMethod("methodProviderAllowed")));
		Assert.assertNotNull(SecurityPreProcessInterceptor.getProviderAllowedAnnotation(ClassProviderAllowedMock.class,
                ClassProviderAllowedMock.class.getMethod("methodNotAnnotated")));
		Assert.assertNotNull(SecurityPreProcessInterceptor.getProviderAllowedAnnotation(SubclassProviderAllowedMock.class,
                SubclassProviderAllowedMock.class.getMethod("methodNotAnnotated")));
	}

	@Test
	public void acceptTest() throws SecurityException, NoSuchMethodException {
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

	@Test
	public void isGuestAllowed() throws SecurityException, NoSuchMethodException {
		Assert.assertFalse(SecurityPreProcessInterceptor.isGuestAllowed(MethodAnnotationsMock.class
				.getMethod("methodNotAnnotated")));
		Assert.assertFalse(SecurityPreProcessInterceptor.isGuestAllowed(MethodAnnotationsMock.class
				.getMethod("methodProviderAllowed")));
		Assert.assertTrue(SecurityPreProcessInterceptor.isGuestAllowed(MethodAnnotationsMock.class
				.getMethod("methodGuestAllowed")));
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
