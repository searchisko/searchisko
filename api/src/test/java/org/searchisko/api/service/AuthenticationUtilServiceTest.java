/*
 * JBoss, Home of Professional Open Source
 * Copyright 2014 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 */
package org.searchisko.api.service;

import java.security.Principal;
import java.util.logging.Logger;

import javax.servlet.http.HttpServletRequest;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;
import org.searchisko.api.security.AuthenticatedUserType;
import org.searchisko.api.security.Role;
import org.searchisko.api.security.jaas.ContributorPrincipal;

/**
 * Unit tests for {@link AuthenticationUtilService}
 * 
 * @author Vlastimil Elias (velias at redhat dot com)
 */
public class AuthenticationUtilServiceTest {

	// TODO #142 other unit tests

	@Test
	public void updateAuthenticatedContributorProfile() {
		AuthenticationUtilService tested = getTested();
		// case - nobody is authenticated
		{
			Mockito.when(tested.httpRequest.getUserPrincipal()).thenReturn(null);

			Assert.assertFalse(tested.updateAuthenticatedContributorProfile());
			Mockito.verifyZeroInteractions(tested.contributorProfileService);
		}

		Principal p = Mockito.mock(Principal.class);
		// case - provider is authenticated
		{
			Mockito.reset(tested.contributorProfileService, tested.httpRequest);
			Mockito.when(tested.httpRequest.getUserPrincipal()).thenReturn(p);
			Mockito.when(tested.httpRequest.isUserInRole(Role.PROVIDER)).thenReturn(true);

			Assert.assertFalse(tested.updateAuthenticatedContributorProfile());
			Mockito.verifyZeroInteractions(tested.contributorProfileService);
		}

		// case - contributor is authenticated but username not in principal
		{
			Mockito.reset(tested.contributorProfileService, tested.httpRequest, p);
			Mockito.when(tested.httpRequest.getUserPrincipal()).thenReturn(p);
			Mockito.when(tested.httpRequest.isUserInRole(Role.CONTRIBUTOR)).thenReturn(true);

			Assert.assertFalse(tested.updateAuthenticatedContributorProfile());
			Mockito.verifyZeroInteractions(tested.contributorProfileService);
			Mockito.verify(p).getName();
		}

		// case - contributor is authenticated, username is in principal
		{
			p = new ContributorPrincipal("uname");
			Mockito.reset(tested.contributorProfileService, tested.httpRequest);
			Mockito.when(tested.httpRequest.getUserPrincipal()).thenReturn(p);
			Mockito.when(tested.httpRequest.isUserInRole(Role.CONTRIBUTOR)).thenReturn(true);

			Assert.assertTrue(tested.updateAuthenticatedContributorProfile());
			Mockito.verify(tested.contributorProfileService).createOrUpdateProfile(
					ContributorProfileService.FIELD_TSC_JBOSSORG_USERNAME, "uname", false);
			Mockito.verifyNoMoreInteractions(tested.contributorProfileService);
		}

		// case - contributor is authenticated, unsupported type of principal
		{
			p = Mockito.mock(Principal.class);
			Mockito.reset(tested.contributorProfileService, tested.httpRequest);
			Mockito.when(tested.httpRequest.getUserPrincipal()).thenReturn(p);
			Mockito.when(tested.httpRequest.isUserInRole(Role.CONTRIBUTOR)).thenReturn(true);

			Assert.assertFalse(tested.updateAuthenticatedContributorProfile());
			Mockito.verifyNoMoreInteractions(tested.contributorProfileService);
		}

	}

	@Test
	public void getAuthenticatedUserType() {
		AuthenticationUtilService tested = getTested();

		// case - not authenticated
		{
			Mockito.when(tested.httpRequest.getUserPrincipal()).thenReturn(null);
			Assert.assertNull(tested.getAuthenticatedUserType());
		}

		Principal p = Mockito.mock(Principal.class);

		// case - authenticated as PROVIDER
		{
			Mockito.reset(tested.httpRequest);
			Mockito.when(tested.httpRequest.getUserPrincipal()).thenReturn(p);
			Mockito.when(tested.httpRequest.isUserInRole(Role.PROVIDER)).thenReturn(true);
			Mockito.when(tested.httpRequest.isUserInRole(Role.CONTRIBUTOR)).thenReturn(false);

			Assert.assertEquals(AuthenticatedUserType.PROVIDER, tested.getAuthenticatedUserType());
		}

		// case - authenticated as CONTRIBUTOR
		{
			Mockito.reset(tested.httpRequest);
			Mockito.when(tested.httpRequest.getUserPrincipal()).thenReturn(p);
			Mockito.when(tested.httpRequest.isUserInRole(Role.PROVIDER)).thenReturn(false);
			Mockito.when(tested.httpRequest.isUserInRole(Role.CONTRIBUTOR)).thenReturn(true);
			Assert.assertEquals(AuthenticatedUserType.CONTRIBUTOR, tested.getAuthenticatedUserType());
		}

	}

	@Test
	public void isAuthenticatedUserOfType() {
		AuthenticationUtilService tested = getTested();

		// case - not authenticated
		{
			Mockito.when(tested.httpRequest.getUserPrincipal()).thenReturn(null);
			Assert.assertFalse(tested.isAuthenticatedUserOfType(AuthenticatedUserType.CONTRIBUTOR));
			Assert.assertFalse(tested.isAuthenticatedUserOfType(AuthenticatedUserType.PROVIDER));
		}

		Principal p = Mockito.mock(Principal.class);

		// case - authenticated as PROVIDER
		{
			Mockito.reset(tested.httpRequest);
			Mockito.when(tested.httpRequest.getUserPrincipal()).thenReturn(p);
			Mockito.when(tested.httpRequest.isUserInRole(Role.PROVIDER)).thenReturn(true);
			Mockito.when(tested.httpRequest.isUserInRole(Role.CONTRIBUTOR)).thenReturn(false);
			Assert.assertFalse(tested.isAuthenticatedUserOfType(AuthenticatedUserType.CONTRIBUTOR));
			Assert.assertTrue(tested.isAuthenticatedUserOfType(AuthenticatedUserType.PROVIDER));
		}

		// case - authenticated as CONTRIBUTOR
		{
			Mockito.reset(tested.httpRequest);
			Mockito.when(tested.httpRequest.getUserPrincipal()).thenReturn(p);
			Mockito.when(tested.httpRequest.isUserInRole(Role.PROVIDER)).thenReturn(false);
			Mockito.when(tested.httpRequest.isUserInRole(Role.CONTRIBUTOR)).thenReturn(true);
			Assert.assertTrue(tested.isAuthenticatedUserOfType(AuthenticatedUserType.CONTRIBUTOR));
			Assert.assertFalse(tested.isAuthenticatedUserOfType(AuthenticatedUserType.PROVIDER));
		}
	}

	private AuthenticationUtilService getTested() {
		AuthenticationUtilService tested = new AuthenticationUtilService();
		tested.log = Logger.getLogger("testlogger");
		tested.contributorProfileService = Mockito.mock(ContributorProfileService.class);
		tested.httpRequest = Mockito.mock(HttpServletRequest.class);
		return tested;
	}

}
