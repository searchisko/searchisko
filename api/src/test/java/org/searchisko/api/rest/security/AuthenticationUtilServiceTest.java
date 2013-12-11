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

		// CASE - provider authenticated OK - contributor id required and returned
		{
			boolean forceCreate = true;
			tested.contributorProfileService = Mockito.mock(ContributorProfileService.class);
			Mockito.when(tested.contributorProfileService.getContributorId("a", "aa", forceCreate)).thenReturn("bb");
			tested.securityContext = new ContributorCustomSecurityContext(new SimplePrincipal("aa"), true, "a");
			Assert.assertEquals("bb", tested.getAuthenticatedContributor(forceCreate));
			Mockito.verify(tested.contributorProfileService, Mockito.times(1)).getContributorId(Mockito.anyString(),
					Mockito.anyString(), Mockito.anyBoolean());

			// second run uses cache
			Assert.assertEquals("bb", tested.getAuthenticatedContributor(forceCreate));
			Mockito.verify(tested.contributorProfileService, Mockito.times(1)).getContributorId(Mockito.anyString(),
					Mockito.anyString(), Mockito.anyBoolean());
		}

		// CASE - provider authenticated OK - contributor id is not required so not returned
		{
			boolean forceCreate = false;
			tested.contributorProfileService = Mockito.mock(ContributorProfileService.class);
			Mockito.when(tested.contributorProfileService.getContributorId("a", "aa", forceCreate)).thenReturn(null);
			tested.securityContext = new ContributorCustomSecurityContext(new SimplePrincipal("aa"), true, "a");
			Assert.assertEquals(null, tested.getAuthenticatedContributor(forceCreate));
			Mockito.verify(tested.contributorProfileService, Mockito.times(1)).getContributorId(Mockito.anyString(),
					Mockito.anyString(), Mockito.anyBoolean());

			// second run uses cache
			Assert.assertEquals(null, tested.getAuthenticatedContributor(forceCreate));
			Mockito.verify(tested.contributorProfileService, Mockito.times(2)).getContributorId(Mockito.anyString(),
					Mockito.anyString(), Mockito.anyBoolean());
		}

		// CASE - provider authenticated OK - contributor id is not required and returned first time, but is required and
		// returned second time
		{
			boolean forceCreate = false;
			boolean forceCreate2 = true;
			tested.contributorProfileService = Mockito.mock(ContributorProfileService.class);
			Mockito.when(tested.contributorProfileService.getContributorId("a", "aa", forceCreate)).thenReturn(null);
			Mockito.when(tested.contributorProfileService.getContributorId("a", "aa", forceCreate2)).thenReturn("bb");
			tested.securityContext = new ContributorCustomSecurityContext(new SimplePrincipal("aa"), true, "a");
			Assert.assertEquals(null, tested.getAuthenticatedContributor(forceCreate));
			Mockito.verify(tested.contributorProfileService, Mockito.times(1)).getContributorId(Mockito.anyString(),
					Mockito.anyString(), Mockito.anyBoolean());

			// second run uses cache
			Assert.assertEquals("bb", tested.getAuthenticatedContributor(forceCreate2));
			Mockito.verify(tested.contributorProfileService, Mockito.times(2)).getContributorId(Mockito.anyString(),
					Mockito.anyString(), Mockito.anyBoolean());
		}

	}

	@Test
	public void updateAuthenticatedContributorProfile() {
		AuthenticationUtilService tested = getTested();
		tested.contributorProfileService = Mockito.mock(ContributorProfileService.class);

		// CASE - not authenticated - no call to service
		{
			Mockito.reset(tested.contributorProfileService);
			tested.updateAuthenticatedContributorProfile();
			Mockito.verifyZeroInteractions(tested.contributorProfileService);
		}
		{
			Mockito.reset(tested.contributorProfileService);
			SecurityContext scMock = Mockito.mock(SecurityContext.class);
			tested.securityContext = scMock;
			Mockito.when(scMock.getUserPrincipal()).thenReturn(new SimplePrincipal("aa"));
			tested.updateAuthenticatedContributorProfile();
			Mockito.verifyZeroInteractions(tested.contributorProfileService);
		}

		tested.securityContext = new ContributorCustomSecurityContext(new SimplePrincipal("aa"), true, "a");
		// case - service call OK
		{
			Mockito.reset(tested.contributorProfileService);
			tested.updateAuthenticatedContributorProfile();
			Mockito.verify(tested.contributorProfileService).createOrUpdateProfile("a", "aa");
		}

		// case - service call exception is not propagated
		{
			Mockito.reset(tested.contributorProfileService);
			Mockito.doThrow(new RuntimeException("Test exception from profile update"))
					.when(tested.contributorProfileService).createOrUpdateProfile(Mockito.anyString(), Mockito.anyString());
			tested.updateAuthenticatedContributorProfile();
			Mockito.verify(tested.contributorProfileService).createOrUpdateProfile("a", "aa");
		}

	}

}
