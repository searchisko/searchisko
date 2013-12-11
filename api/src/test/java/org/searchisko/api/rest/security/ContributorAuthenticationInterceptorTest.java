/*
 * JBoss, Home of Professional Open Source
 * Copyright 2012 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 */
package org.searchisko.api.rest.security;

import java.util.logging.Logger;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.core.UriInfo;

import org.jasig.cas.client.authentication.AttributePrincipalImpl;
import org.jasig.cas.client.util.AbstractCasFilter;
import org.jasig.cas.client.validation.Assertion;
import org.jboss.resteasy.core.ResourceMethod;
import org.jboss.resteasy.core.ServerResponse;
import org.jboss.resteasy.spi.HttpRequest;
import org.jboss.resteasy.spi.ResteasyProviderFactory;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;
import org.searchisko.api.rest.security.ContributorSecurityPreProcessInterceptorTest.ClassContributorAllowedMock;
import org.searchisko.api.rest.security.ContributorSecurityPreProcessInterceptorTest.ClassOptionalContributorAllowedMock;
import org.searchisko.api.rest.security.ContributorSecurityPreProcessInterceptorTest.MethodAnnotationsMock;
import org.searchisko.api.rest.security.ContributorSecurityPreProcessInterceptorTest.SubclassContributorAllowedMock;

/**
 * Unit test for {@link ContributorAuthenticationInterceptor}
 * 
 * @author Vlastimil Elias (velias at redhat dot com)
 */
public class ContributorAuthenticationInterceptorTest {

	private ContributorAuthenticationInterceptor getTested() {
		ContributorAuthenticationInterceptor tested = new ContributorAuthenticationInterceptor();
		tested.log = Logger.getLogger("testlogger");
		return tested;
	}

	@Test(expected = Exception.class)
	public void testNotHttpServletRequestException() {
		ContributorAuthenticationInterceptor tested = getTested();

		HttpRequest requestMock = getHttpRequestMock();
		ResourceMethod methodMock = Mockito.mock(ResourceMethod.class);

		ServerResponse res = tested.preProcess(requestMock, methodMock);
		Assert.assertNull(res);
	}

	@Test
	public void testNoHttpSession() {
		ContributorAuthenticationInterceptor tested = getTested();

		tested.servletRequest = Mockito.mock(HttpServletRequest.class);
		Mockito.when(tested.servletRequest.getSession(false)).thenReturn(null);

		HttpRequest requestMock = getHttpRequestMock();
		ResourceMethod methodMock = Mockito.mock(ResourceMethod.class);

		ServerResponse res = tested.preProcess(requestMock, methodMock);
		Assert.assertNull(res);
		Mockito.verify(tested.servletRequest).getSession(false);
	}

	@Test
	public void testCASAuthentication_noAssertionInSession() {
		ContributorAuthenticationInterceptor tested = getTested();

		tested.servletRequest = Mockito.mock(HttpServletRequest.class);
		HttpSession sessionMock = Mockito.mock(HttpSession.class);
		Mockito.when(tested.servletRequest.getSession(false)).thenReturn(sessionMock);
		Mockito.when(sessionMock.getAttribute(AbstractCasFilter.CONST_CAS_ASSERTION)).thenReturn(null);

		HttpRequest requestMock = getHttpRequestMock();
		ResourceMethod methodMock = Mockito.mock(ResourceMethod.class);

		ServerResponse res = tested.preProcess(requestMock, methodMock);
		Assert.assertNull(res);
		Mockito.verify(tested.servletRequest).getSession(false);
	}

	@Test
	public void testCASAuthentication_noPrincipalInAssertion() {
		ContributorAuthenticationInterceptor tested = getTested();

		tested.servletRequest = Mockito.mock(HttpServletRequest.class);
		HttpSession sessionMock = Mockito.mock(HttpSession.class);
		Mockito.when(tested.servletRequest.getSession(false)).thenReturn(sessionMock);
		Assertion assertionMock = Mockito.mock(Assertion.class);
		Mockito.when(sessionMock.getAttribute(AbstractCasFilter.CONST_CAS_ASSERTION)).thenReturn(assertionMock);
		Mockito.when(assertionMock.getPrincipal()).thenReturn(null);

		HttpRequest requestMock = getHttpRequestMock();
		ResourceMethod methodMock = Mockito.mock(ResourceMethod.class);

		ServerResponse res = tested.preProcess(requestMock, methodMock);
		Assert.assertNull(res);
		Mockito.verify(tested.servletRequest).getSession(false);
	}

	@Test
	public void testCASAuthentication_OK() {
		ContributorAuthenticationInterceptor tested = getTested();

		String uname = "username";

		tested.servletRequest = Mockito.mock(HttpServletRequest.class);
		HttpSession sessionMock = Mockito.mock(HttpSession.class);
		Mockito.when(tested.servletRequest.getSession(false)).thenReturn(sessionMock);
		Assertion assertionMock = Mockito.mock(Assertion.class);
		Mockito.when(sessionMock.getAttribute(AbstractCasFilter.CONST_CAS_ASSERTION)).thenReturn(assertionMock);
		Mockito.when(assertionMock.getPrincipal()).thenReturn(new AttributePrincipalImpl(uname));

		HttpRequest requestMock = getHttpRequestMock();
		ResourceMethod methodMock = Mockito.mock(ResourceMethod.class);

		ServerResponse res = tested.preProcess(requestMock, methodMock);
		Assert.assertNull(res);

		SecurityContext ctx = ResteasyProviderFactory.getContextData(SecurityContext.class);
		Assert.assertEquals(ContributorCustomSecurityContext.class, ctx.getClass());
		Assert.assertEquals(uname, ctx.getUserPrincipal().getName());
		Assert.assertEquals(ContributorAuthenticationInterceptor.AUTH_METHOD_CAS, ctx.getAuthenticationScheme());
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
		ContributorAuthenticationInterceptor tested = getTested();

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
		// accept optional annotation!
		Assert.assertTrue(tested.accept(ClassOptionalContributorAllowedMock.class,
				ClassOptionalContributorAllowedMock.class.getMethod("methodNotAnnotated")));
	}

}
