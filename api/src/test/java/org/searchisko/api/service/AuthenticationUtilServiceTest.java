/*
 * JBoss, Home of Professional Open Source
 * Copyright 2014 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 */
package org.searchisko.api.service;

import java.security.Principal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.logging.Logger;

import javax.servlet.http.HttpServletRequest;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;
import org.searchisko.api.rest.exception.NotAuthenticatedException;
import org.searchisko.api.rest.exception.NotAuthorizedException;
import org.searchisko.api.security.AuthenticatedUserType;
import org.searchisko.api.security.Role;
import org.searchisko.api.security.jaas.ContributorPrincipal;
import org.searchisko.api.security.jaas.ProviderPrincipal;

/**
 * Unit tests for {@link AuthenticationUtilService}
 * 
 * @author Vlastimil Elias (velias at redhat dot com)
 */
public class AuthenticationUtilServiceTest {

	private static final String ROLE1 = "role1";
	private static final String ROLE2 = "role2";
	private static final String ROLE3 = "role3";
	private static final String ROLE4 = "role4";

	@Test
	public void isAuthenticatedUser() {
		AuthenticationUtilService tested = getTested();

		// case - not authenticated
		{
			Mockito.when(tested.httpRequest.getUserPrincipal()).thenReturn(null);
			Assert.assertFalse(tested.isAuthenticatedUser());
		}

		// case - authenticated
		{
			Mockito.reset(tested.httpRequest);
			Principal p = Mockito.mock(Principal.class);
			Mockito.when(tested.httpRequest.getUserPrincipal()).thenReturn(p);
			Assert.assertTrue(tested.isAuthenticatedUser());
		}
	}

	@Test
	public void getAuthenticatedUserPrincipal() {
		AuthenticationUtilService tested = getTested();

		// case - not authenticated
		{
			Mockito.when(tested.httpRequest.getUserPrincipal()).thenReturn(null);
			Assert.assertNull(tested.getAuthenticatedUserPrincipal());
		}

		// case - authenticated
		{
			Mockito.reset(tested.httpRequest);
			Principal p = Mockito.mock(Principal.class);
			Mockito.when(tested.httpRequest.getUserPrincipal()).thenReturn(p);
			Assert.assertEquals(p, tested.getAuthenticatedUserPrincipal());
		}
	}

	@Test
	public void isUserInRole() {
		AuthenticationUtilService tested = getTested();

		Mockito.when(tested.httpRequest.isUserInRole(ROLE1)).thenReturn(false);
		Mockito.when(tested.httpRequest.isUserInRole(ROLE2)).thenReturn(true);

		{
			Assert.assertFalse(tested.isUserInRole(null));
			Mockito.verifyZeroInteractions(tested.httpRequest);
		}
		{
			Assert.assertFalse(tested.isUserInRole(""));
			Mockito.verifyZeroInteractions(tested.httpRequest);
		}
		{
			Assert.assertFalse(tested.isUserInRole("  "));
			Mockito.verifyZeroInteractions(tested.httpRequest);
		}
		{
			Assert.assertFalse(tested.isUserInRole(ROLE1));
			Mockito.verify(tested.httpRequest).isUserInRole(ROLE1);
		}
		{
			Assert.assertTrue(tested.isUserInRole(ROLE2));
			Mockito.verify(tested.httpRequest).isUserInRole(ROLE2);
		}
	}

	@Test
	public void isUserInAnyOfRoles_Array() {
		AuthenticationUtilService tested = getTested();

		Mockito.when(tested.httpRequest.isUserInRole(ROLE1)).thenReturn(false);
		Mockito.when(tested.httpRequest.isUserInRole(ROLE2)).thenReturn(true);
		Mockito.when(tested.httpRequest.isUserInRole(ROLE3)).thenReturn(false);
		Mockito.when(tested.httpRequest.isUserInRole(ROLE4)).thenReturn(true);
		Mockito.when(tested.httpRequest.isUserInRole(Role.ADMIN)).thenReturn(true);

		Assert.assertFalse(tested.isUserInAnyOfRoles(false, (String[]) null));
		Assert.assertFalse(tested.isUserInAnyOfRoles(false, new String[] {}));
		Assert.assertFalse(tested.isUserInAnyOfRoles(false, new String[] { ROLE1 }));
		Assert.assertFalse(tested.isUserInAnyOfRoles(false, new String[] { ROLE1, ROLE3 }));
		Assert.assertTrue(tested.isUserInAnyOfRoles(false, new String[] { ROLE2 }));
		Assert.assertTrue(tested.isUserInAnyOfRoles(false, new String[] { ROLE1, ROLE3, ROLE2, ROLE4 }));

		// acceptAdmin param test
		Assert.assertTrue(tested.isUserInAnyOfRoles(true, (String[]) null));
		Assert.assertTrue(tested.isUserInAnyOfRoles(true, new String[] {}));
		Assert.assertTrue(tested.isUserInAnyOfRoles(true, new String[] { ROLE1 }));
		Assert.assertTrue(tested.isUserInAnyOfRoles(true, new String[] { ROLE1, ROLE3 }));

		Mockito.when(tested.httpRequest.isUserInRole(Role.ADMIN)).thenReturn(false);
		Assert.assertFalse(tested.isUserInAnyOfRoles(true, (String[]) null));
		Assert.assertFalse(tested.isUserInAnyOfRoles(true, new String[] {}));
		Assert.assertFalse(tested.isUserInAnyOfRoles(true, new String[] { ROLE1 }));
		Assert.assertFalse(tested.isUserInAnyOfRoles(true, new String[] { ROLE1, ROLE3 }));

	}

	@Test
	public void isUserInAnyOfRoles_Collection() {
		AuthenticationUtilService tested = getTested();

		Mockito.when(tested.httpRequest.isUserInRole(ROLE1)).thenReturn(false);
		Mockito.when(tested.httpRequest.isUserInRole(ROLE2)).thenReturn(true);
		Mockito.when(tested.httpRequest.isUserInRole(ROLE3)).thenReturn(false);
		Mockito.when(tested.httpRequest.isUserInRole(ROLE4)).thenReturn(true);
		Mockito.when(tested.httpRequest.isUserInRole(Role.ADMIN)).thenReturn(true);

		Assert.assertFalse(tested.isUserInAnyOfRoles(false, (Collection<String>) null));
		List<String> collection = new ArrayList<>();
		Assert.assertFalse(tested.isUserInAnyOfRoles(false, collection));

		collection.add(ROLE1);
		Assert.assertFalse(tested.isUserInAnyOfRoles(false, collection));

		collection.add(ROLE3);
		Assert.assertFalse(tested.isUserInAnyOfRoles(false, collection));

		collection.clear();
		collection.add(ROLE2);
		Assert.assertTrue(tested.isUserInAnyOfRoles(false, collection));

		collection.clear();
		collection.add(ROLE1);
		collection.add(ROLE3);
		collection.add(ROLE2);
		collection.add(ROLE4);
		Assert.assertTrue(tested.isUserInAnyOfRoles(false, collection));

		// acceptAdmin param test
		collection.clear();
		Assert.assertTrue(tested.isUserInAnyOfRoles(true, (Collection<String>) null));
		Assert.assertTrue(tested.isUserInAnyOfRoles(true, collection));
		collection.add(ROLE1);
		Assert.assertTrue(tested.isUserInAnyOfRoles(true, collection));
		collection.add(ROLE3);
		Assert.assertTrue(tested.isUserInAnyOfRoles(true, collection));

		Mockito.when(tested.httpRequest.isUserInRole(Role.ADMIN)).thenReturn(false);
		collection.clear();
		Assert.assertFalse(tested.isUserInAnyOfRoles(true, (Collection<String>) null));
		Assert.assertFalse(tested.isUserInAnyOfRoles(true, collection));
		collection.add(ROLE1);
		Assert.assertFalse(tested.isUserInAnyOfRoles(true, collection));
		collection.add(ROLE3);
		Assert.assertFalse(tested.isUserInAnyOfRoles(true, collection));

	}

	@Test
	public void getAuthenticatedProvider() {
		AuthenticationUtilService tested = getTested();

		// case - nobody is authenticated
		try {
			Mockito.reset(tested.contributorProfileService, tested.httpRequest);
			Mockito.when(tested.httpRequest.getUserPrincipal()).thenReturn(null);

			tested.getAuthenticatedProvider();
			Assert.fail("NotAuthenticatedException expected");
		} catch (NotAuthenticatedException e) {
			// OK
			Mockito.verifyZeroInteractions(tested.contributorProfileService);
		}

		// case - CONTRIBUTOR is authenticated
		try {
			Mockito.reset(tested.contributorProfileService, tested.httpRequest);
			Principal p = new ContributorPrincipal("uname");
			Mockito.when(tested.httpRequest.getUserPrincipal()).thenReturn(p);
			Mockito.when(tested.httpRequest.isUserInRole(Role.CONTRIBUTOR)).thenReturn(true);

			tested.getAuthenticatedProvider();
			Assert.fail("NotAuthenticatedException expected");
		} catch (NotAuthenticatedException e) {
			// OK
			Mockito.verifyZeroInteractions(tested.contributorProfileService);
		}

		// case - PROVIDER is authenticated
		{
			Mockito.reset(tested.contributorProfileService, tested.httpRequest);
			Principal p = new ProviderPrincipal("uname");
			Mockito.when(tested.httpRequest.getUserPrincipal()).thenReturn(p);
			Mockito.when(tested.httpRequest.isUserInRole(Role.PROVIDER)).thenReturn(true);

			Assert.assertEquals("uname", tested.getAuthenticatedProvider());

			Mockito.verifyNoMoreInteractions(tested.contributorProfileService);
		}
	}

	@Test
	public void checkProviderManagementPermission() {
		AuthenticationUtilService tested = getTested();

		// case - nobody authenticated
		try {
			Mockito.when(tested.httpRequest.getUserPrincipal()).thenReturn(null);
			tested.checkProviderManagementPermission("provider1");
			Assert.fail("NotAuthorizedException expected");
		} catch (NotAuthorizedException e) {
			// OK
		}

		// case - no provider passed in
		try {
			Principal p = Mockito.mock(Principal.class);
			Mockito.when(tested.httpRequest.getUserPrincipal()).thenReturn(p);
			Mockito.when(tested.httpRequest.isUserInRole(Role.ADMIN)).thenReturn(true);
			tested.checkProviderManagementPermission(null);
			Assert.fail("NotAuthorizedException expected");
		} catch (NotAuthorizedException e) {
			// OK
		}

		// case - provider logged in, provider passed in, match by role
		{
			Principal p = new ProviderPrincipal("provider2");
			Mockito.when(tested.httpRequest.getUserPrincipal()).thenReturn(p);
			Mockito.when(tested.httpRequest.isUserInRole(Role.ADMIN)).thenReturn(true);
			Mockito.when(tested.httpRequest.isUserInRole(Role.PROVIDER)).thenReturn(true);
			tested.checkProviderManagementPermission("provider1");
		}

		// case - contributor logged in, provider passed in, match by role
		{
			Principal p = new ContributorPrincipal("contributor");
			Mockito.when(tested.httpRequest.getUserPrincipal()).thenReturn(p);
			Mockito.when(tested.httpRequest.isUserInRole(Role.ADMIN)).thenReturn(true);
			Mockito.when(tested.httpRequest.isUserInRole(Role.CONTRIBUTOR)).thenReturn(true);
			tested.checkProviderManagementPermission("provider1");
		}

		// case - provider logged in, match by provider name
		{
			Principal p = new ProviderPrincipal("provider1");
			Mockito.when(tested.httpRequest.getUserPrincipal()).thenReturn(p);
			Mockito.when(tested.httpRequest.isUserInRole(Role.ADMIN)).thenReturn(false);
			Mockito.when(tested.httpRequest.isUserInRole(Role.PROVIDER)).thenReturn(true);
			tested.checkProviderManagementPermission("provider1");
		}

		// case - provider logged in, but not match by provider name
		try {
			Principal p = new ProviderPrincipal("provider2");
			Mockito.when(tested.httpRequest.getUserPrincipal()).thenReturn(p);
			Mockito.when(tested.httpRequest.isUserInRole(Role.ADMIN)).thenReturn(false);
			Mockito.when(tested.httpRequest.isUserInRole(Role.PROVIDER)).thenReturn(true);
			tested.checkProviderManagementPermission("provider1");
			Assert.fail("NotAuthorizedException expected");
		} catch (NotAuthorizedException e) {
			// OK
		}

		// case - name is same but contributor is logged in, not provider
		try {
			Principal p = new ContributorPrincipal("provider1");
			Mockito.when(tested.httpRequest.getUserPrincipal()).thenReturn(p);
			Mockito.when(tested.httpRequest.isUserInRole(Role.ADMIN)).thenReturn(false);
			Mockito.when(tested.httpRequest.isUserInRole(Role.PROVIDER)).thenReturn(false);
			Mockito.when(tested.httpRequest.isUserInRole(Role.CONTRIBUTOR)).thenReturn(true);
			tested.checkProviderManagementPermission("provider1");
			Assert.fail("NotAuthorizedException expected");
		} catch (NotAuthorizedException e) {
			// OK
		}
	}

	@Test
	public void getAuthenticatedContributor() {
		AuthenticationUtilService tested = getTested();

		// case - nobody is authenticated
		try {
			Mockito.reset(tested.contributorProfileService, tested.httpRequest);
			Mockito.when(tested.httpRequest.getUserPrincipal()).thenReturn(null);

			tested.cachedContributorId = "aaa";
			tested.getAuthenticatedContributor(false);
			Assert.fail("NotAuthenticatedException expected");
		} catch (NotAuthenticatedException e) {
			// OK
			Assert.assertNull(tested.cachedContributorId);
			Mockito.verifyZeroInteractions(tested.contributorProfileService);
		}

		// case - PROVIDER is authenticated
		try {
			Mockito.reset(tested.contributorProfileService, tested.httpRequest);
			Principal p = Mockito.mock(Principal.class);
			Mockito.when(tested.httpRequest.getUserPrincipal()).thenReturn(p);
			Mockito.when(tested.httpRequest.isUserInRole(Role.PROVIDER)).thenReturn(true);

			tested.cachedContributorId = "aaa";
			tested.getAuthenticatedContributor(false);
			Assert.fail("NotAuthenticatedException expected");
		} catch (NotAuthenticatedException e) {
			// OK
			Assert.assertNull(tested.cachedContributorId);
			Mockito.verifyZeroInteractions(tested.contributorProfileService);
		}

		// case - CONTRIBUTOR is authenticated, known contributor, not forced
		{
			Mockito.reset(tested.contributorProfileService, tested.httpRequest);
			Principal p = new ContributorPrincipal("uname");
			Mockito.when(tested.httpRequest.getUserPrincipal()).thenReturn(p);
			Mockito.when(tested.httpRequest.isUserInRole(Role.CONTRIBUTOR)).thenReturn(true);

			boolean isForced = false;

			Mockito.when(
					tested.contributorProfileService.getContributorId(ContributorProfileService.FIELD_TSC_JBOSSORG_USERNAME,
							"uname", isForced)).thenReturn("cidd");

			tested.cachedContributorId = null;

			Assert.assertEquals("cidd", tested.getAuthenticatedContributor(isForced));

			Mockito.verify(tested.contributorProfileService).getContributorId(
					ContributorProfileService.FIELD_TSC_JBOSSORG_USERNAME, "uname", isForced);
			Mockito.verifyNoMoreInteractions(tested.contributorProfileService);
			Assert.assertEquals("cidd", tested.cachedContributorId);

			// second call uses cache, no more call of service

			Assert.assertEquals("cidd", tested.getAuthenticatedContributor(isForced));
			Mockito.verifyNoMoreInteractions(tested.contributorProfileService);
		}

		// case - CONTRIBUTOR is authenticated, unknown contributor, forced
		{
			Mockito.reset(tested.contributorProfileService, tested.httpRequest);
			Principal p = new ContributorPrincipal("uname");
			Mockito.when(tested.httpRequest.getUserPrincipal()).thenReturn(p);
			Mockito.when(tested.httpRequest.isUserInRole(Role.CONTRIBUTOR)).thenReturn(true);

			boolean isForced = true;

			Mockito.when(
					tested.contributorProfileService.getContributorId(ContributorProfileService.FIELD_TSC_JBOSSORG_USERNAME,
							"uname", isForced)).thenReturn("");

			tested.cachedContributorId = null;

			Assert.assertEquals(null, tested.getAuthenticatedContributor(isForced));

			Mockito.verify(tested.contributorProfileService).getContributorId(
					ContributorProfileService.FIELD_TSC_JBOSSORG_USERNAME, "uname", isForced);
			Mockito.verifyNoMoreInteractions(tested.contributorProfileService);
			Assert.assertEquals(null, tested.cachedContributorId);

		}

	}

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

		// case - authenticated as any other
		{
			Mockito.reset(tested.httpRequest);
			Mockito.when(tested.httpRequest.getUserPrincipal()).thenReturn(p);
			Mockito.when(tested.httpRequest.isUserInRole(Role.PROVIDER)).thenReturn(false);
			Mockito.when(tested.httpRequest.isUserInRole(Role.CONTRIBUTOR)).thenReturn(false);
			Assert.assertEquals(null, tested.getAuthenticatedUserType());
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

		// case - authenticated as other only
		{
			Mockito.reset(tested.httpRequest);
			Mockito.when(tested.httpRequest.getUserPrincipal()).thenReturn(p);
			Mockito.when(tested.httpRequest.isUserInRole(Role.PROVIDER)).thenReturn(false);
			Mockito.when(tested.httpRequest.isUserInRole(Role.CONTRIBUTOR)).thenReturn(false);
			Assert.assertFalse(tested.isAuthenticatedUserOfType(AuthenticatedUserType.CONTRIBUTOR));
			Assert.assertFalse(tested.isAuthenticatedUserOfType(AuthenticatedUserType.PROVIDER));
			Assert.assertFalse(tested.isAuthenticatedUserOfType(null));
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
