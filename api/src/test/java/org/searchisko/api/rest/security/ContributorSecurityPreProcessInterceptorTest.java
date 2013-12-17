/*
 * JBoss, Home of Professional Open Source
 * Copyright 2012 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 */
package org.searchisko.api.rest.security;

import java.security.Principal;
import java.util.logging.Logger;

import org.jboss.resteasy.core.ResourceMethod;
import org.jboss.resteasy.core.ServerResponse;
import org.jboss.resteasy.plugins.server.embedded.SimplePrincipal;
import org.jboss.resteasy.util.HttpResponseCodes;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;
import org.searchisko.api.annotations.security.ContributorAllowed;
import org.searchisko.api.annotations.security.GuestAllowed;

/**
 * Unit test for {@link ContributorSecurityPreProcessInterceptor}
 * 
 * @author Vlastimil Elias (velias at redhat dot com)
 */
public class ContributorSecurityPreProcessInterceptorTest {

	Principal principal = new SimplePrincipal("uname");

	private ContributorSecurityPreProcessInterceptor getTested() {
		ContributorSecurityPreProcessInterceptor tested = new ContributorSecurityPreProcessInterceptor();
		tested.log = Logger.getLogger("testlogger");
		return tested;
	}

	@Test
	public void notAuthenticatedTest_noSecurityContext() {
		ContributorSecurityPreProcessInterceptor tested = getTested();
		ServerResponse res = tested.preProcess(null, null);

		Assert.assertNotNull(res);
		Assert.assertEquals(HttpResponseCodes.SC_FORBIDDEN, res.getStatus());
	}

	@Test
	public void notAuthenticatedTest_badSecurityContext() {
		ContributorSecurityPreProcessInterceptor tested = getTested();

		tested.securityContext = new ProviderCustomSecurityContext(null, false, true, "a");

		ServerResponse res = tested.preProcess(null, null);

		Assert.assertNotNull(res);
		Assert.assertEquals(HttpResponseCodes.SC_FORBIDDEN, res.getStatus());
	}

	@Test
	public void notAuthenticatedTest_emptySecurityContext() {
		ContributorSecurityPreProcessInterceptor tested = getTested();

		tested.securityContext = new ContributorCustomSecurityContext(null, true, "a");

		ServerResponse res = tested.preProcess(null, null);

		Assert.assertNotNull(res);
		Assert.assertEquals(HttpResponseCodes.SC_FORBIDDEN, res.getStatus());
	}

	@SuppressWarnings("unchecked")
	@Test
	public void authenticatedTest() throws NoSuchMethodException, SecurityException {
		ContributorSecurityPreProcessInterceptor tested = getTested();

		// we create subclass here to check it is evaluated OK die proxying in CDI
		tested.securityContext = new ContributorCustomSecurityContext(principal, true, "a") {
		};

		ResourceMethod methodMock = Mockito.mock(ResourceMethod.class);
		Mockito.when((Class<MethodAnnotationsMock>) methodMock.getResourceClass()).thenReturn(MethodAnnotationsMock.class);
		Mockito.when(methodMock.getMethod()).thenReturn(MethodAnnotationsMock.class.getMethod("methodContributorAllowed"));

		ServerResponse res = tested.preProcess(null, methodMock);

		Assert.assertNull(res);
	}

	@Test
	public void getProviderAllowedAnnotationTest() throws SecurityException, NoSuchMethodException {

		Assert.assertNull(ContributorSecurityPreProcessInterceptor.getContributorAllowedAnnotation(
				MethodAnnotationsMock.class, MethodAnnotationsMock.class.getMethod("methodNotAnnotated")));
		Assert.assertNull(ContributorSecurityPreProcessInterceptor.getContributorAllowedAnnotation(
				MethodAnnotationsMock.class, MethodAnnotationsMock.class.getMethod("methodGuestAllowed")));

		Assert.assertNotNull(ContributorSecurityPreProcessInterceptor.getContributorAllowedAnnotation(
				MethodAnnotationsMock.class, MethodAnnotationsMock.class.getMethod("methodContributorAllowed")));
		Assert.assertNotNull(ContributorSecurityPreProcessInterceptor.getContributorAllowedAnnotation(
				ClassContributorAllowedMock.class, ClassContributorAllowedMock.class.getMethod("methodNotAnnotated")));
		Assert.assertNotNull(ContributorSecurityPreProcessInterceptor.getContributorAllowedAnnotation(
				SubclassContributorAllowedMock.class, SubclassContributorAllowedMock.class.getMethod("methodNotAnnotated")));
	}

	@Test
	public void acceptTest() throws SecurityException, NoSuchMethodException {
		ContributorSecurityPreProcessInterceptor tested = getTested();

		Assert.assertFalse(tested.accept(MethodAnnotationsMock.class,
				MethodAnnotationsMock.class.getMethod("methodNotAnnotated")));
		Assert.assertFalse(tested.accept(MethodAnnotationsMock.class,
				MethodAnnotationsMock.class.getMethod("methodGuestAllowed")));

		Assert.assertTrue(tested.accept(MethodAnnotationsMock.class,
				MethodAnnotationsMock.class.getMethod("methodContributorAllowed")));
		Assert.assertTrue(tested.accept(ClassContributorAllowedMock.class,
				ClassContributorAllowedMock.class.getMethod("methodNotAnnotated")));
		Assert.assertTrue(tested.accept(SubclassContributorAllowedMock.class,
				SubclassContributorAllowedMock.class.getMethod("methodNotAnnotated")));

		// optional allowed methos is not accepted so authorization is not performed by this interceptor
		Assert.assertFalse(tested.accept(ClassOptionalContributorAllowedMock.class,
				ClassOptionalContributorAllowedMock.class.getMethod("methodNotAnnotated")));
	}

	@Test
	public void isGuestAllowed() throws SecurityException, NoSuchMethodException {
		Assert.assertFalse(ContributorSecurityPreProcessInterceptor.isGuestAllowed(MethodAnnotationsMock.class
				.getMethod("methodNotAnnotated")));
		Assert.assertFalse(ContributorSecurityPreProcessInterceptor.isGuestAllowed(MethodAnnotationsMock.class
				.getMethod("methodContributorAllowed")));
		Assert.assertTrue(ContributorSecurityPreProcessInterceptor.isGuestAllowed(MethodAnnotationsMock.class
				.getMethod("methodGuestAllowed")));
		Assert.assertTrue(ContributorSecurityPreProcessInterceptor.isGuestAllowed(ClassContributorAllowedMock.class
				.getMethod("methodGuestAllowed")));
	}

	public static class MethodAnnotationsMock {

		@GuestAllowed
		public void methodGuestAllowed() {

		}

		@ContributorAllowed
		public void methodContributorAllowed() {

		}

		public void methodNotAnnotated() {

		}

	}

	@ContributorAllowed
	public static class ClassContributorAllowedMock {

		public void methodNotAnnotated() {

		}

		@GuestAllowed
		public void methodGuestAllowed() {

		}
	}

	@ContributorAllowed(optional = true)
	public static class ClassOptionalContributorAllowedMock {

		public void methodNotAnnotated() {

		}
	}

	public static class SubclassContributorAllowedMock extends ClassContributorAllowedMock {

	}

}
