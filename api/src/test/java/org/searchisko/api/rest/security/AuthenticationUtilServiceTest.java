/*
 * JBoss, Home of Professional Open Source
 * Copyright 2013 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 */
package org.searchisko.api.rest.security;

import java.util.logging.Logger;

import javax.ws.rs.core.SecurityContext;

import org.jboss.resteasy.plugins.server.embedded.SimplePrincipal;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;
import org.searchisko.api.rest.RestServiceBase;
import org.searchisko.api.rest.exception.NotAuthenticatedException;
import org.searchisko.api.service.ContributorProfileService;

/**
 * Unit test for {@link AuthenticationUtilService}
 * 
 * @author Vlastimil Elias (velias at redhat dot com)
 */
public class AuthenticationUtilServiceTest {

	/**
	 * @return RestServiceBase instance for test with initialized logger
	 */
	protected AuthenticationUtilService getTested() {
		AuthenticationUtilService tested = new AuthenticationUtilService();
		tested.log = Logger.getLogger(RestServiceBase.class.getName());
		return tested;
	}

	@Test
	public void getAuthenticatedProvider() {
		AuthenticationUtilService tested = getTested();

		// CASE - not authenticated - security context is empty
		try {
			tested.getAuthenticatedProvider();
			Assert.fail("Exception must be thrown");
		} catch (NotAuthenticatedException e) {
			// OK
		}

		// CASE - not authenticated - security context is bad type
		{
			SecurityContext scMock = Mockito.mock(SecurityContext.class);
			tested.securityContext = scMock;
			Mockito.when(scMock.getUserPrincipal()).thenReturn(new SimplePrincipal("aa"));
			try {
				tested.getAuthenticatedProvider();
				Assert.fail("Exception must be thrown");
			} catch (NotAuthenticatedException e) {
				// OK
			}
		}

		// CASE - not authenticated - security context is correct type but principal is empty
		{
			SecurityContext scMock = Mockito.mock(ProviderCustomSecurityContext.class);
			tested.securityContext = scMock;
			Mockito.when(scMock.getUserPrincipal()).thenReturn(null);
			try {
				tested.getAuthenticatedProvider();
				Assert.fail("Exception must be thrown");
			} catch (NotAuthenticatedException e) {
				// OK
			}
		}

		// CASE - provider authenticated OK
		{
			SecurityContext scMock = Mockito.mock(ProviderCustomSecurityContext.class);
			tested.securityContext = scMock;
			Mockito.when(scMock.getUserPrincipal()).thenReturn(new SimplePrincipal("aa"));
			Assert.assertEquals("aa", tested.getAuthenticatedProvider());
		}
	}

	@Test
	public void getAuthenticatedContributor() {
		AuthenticationUtilService tested = getTested();

		// CASE - not authenticated - security context is empty
		try {
			tested.getAuthenticatedContributor(false);
			Assert.fail("Exception must be thrown");
		} catch (NotAuthenticatedException e) {
			// OK
		}

		// CASE - not authenticated - security context is bad type
		{
			SecurityContext scMock = Mockito.mock(SecurityContext.class);
			tested.securityContext = scMock;
			Mockito.when(scMock.getUserPrincipal()).thenReturn(new SimplePrincipal("aa"));
			try {
				tested.getAuthenticatedContributor(false);
				Assert.fail("Exception must be thrown");
			} catch (NotAuthenticatedException e) {
				// OK
			}
		}

		// CASE - not authenticated - security context is correct type but principal is empty
		{
			tested.securityContext = new ContributorCustomSecurityContext(null, true, "a");
			try {
				tested.getAuthenticatedContributor(false);
				Assert.fail("Exception must be thrown");
			} catch (NotAuthenticatedException e) {
				// OK
			}
		}

		// CASE - provider authenticated OK
		{
			tested.contributorProfileService = Mockito.mock(ContributorProfileService.class);
			Mockito.when(tested.contributorProfileService.getContributorId("aa")).thenReturn("bb");
			tested.securityContext = new ContributorCustomSecurityContext(new SimplePrincipal("aa"), true, "a");
			Assert.assertEquals("bb", tested.getAuthenticatedContributor(false));
		}
	}

}
