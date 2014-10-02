/*
 * JBoss, Home of Professional Open Source
 * Copyright 2013 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 */
package org.searchisko.api.rest;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;
import org.searchisko.api.rest.exception.NotAuthenticatedException;
import org.searchisko.api.security.AuthenticatedUserType;
import org.searchisko.api.service.AuthenticationUtilService;

/**
 * Unit test for {@link AuthStatusRestService}
 * 
 * @author Vlastimil Elias (velias at redhat dot com)
 */
public class AuthStatusRestServiceTest {

	@Test
	public void authStatus_notAuthenticated() {

		AuthStatusRestService tested = new AuthStatusRestService();
		tested.authenticationUtilService = Mockito.mock(AuthenticationUtilService.class);
		tested.log = Logger.getLogger("testlogger");

		Mockito.when(tested.authenticationUtilService.getAuthenticatedContributor(false)).thenThrow(
				new NotAuthenticatedException(AuthenticatedUserType.CONTRIBUTOR));

		Map<String, Object> ret = tested.authStatus(null);

		Mockito.verify(tested.authenticationUtilService).getAuthenticatedContributor(false);
		Assert.assertNotNull(ret);
		Assert.assertEquals(1, ret.size());
		Assert.assertFalse((Boolean) ret.get("authenticated"));
	}

	@Test
	public void authStatus_authenticated() {

		AuthStatusRestService tested = new AuthStatusRestService();
		tested.authenticationUtilService = Mockito.mock(AuthenticationUtilService.class);
		tested.log = Logger.getLogger("testlogger");

		// case - no contributor id returned (because we do not need it), no roles available but requested
		{
			Mockito.when(tested.authenticationUtilService.getAuthenticatedContributor(false)).thenReturn(null);

			Map<String, Object> ret = tested.authStatus("y");

			Mockito.verify(tested.authenticationUtilService).getAuthenticatedContributor(false);
			Assert.assertNotNull(ret);
			Assert.assertEquals(1, ret.size());
			Assert.assertTrue((Boolean) ret.get("authenticated"));
			Mockito.verify(tested.authenticationUtilService).updateAuthenticatedContributorProfile();
			Mockito.verify(tested.authenticationUtilService).getUserRoles();
			Mockito.verifyNoMoreInteractions(tested.authenticationUtilService);
		}

		// case - contributor id returned, roles available and requested
		{
			Mockito.reset(tested.authenticationUtilService);
			Mockito.when(tested.authenticationUtilService.getAuthenticatedContributor(false)).thenReturn("aaa <jb@jk.po>");
			Set<String> roles = new HashSet<>();
			roles.add("role1");
			Mockito.when(tested.authenticationUtilService.getUserRoles()).thenReturn(roles);

			Map<String, Object> ret = tested.authStatus("y");

			Mockito.verify(tested.authenticationUtilService).getAuthenticatedContributor(false);
			Mockito.verify(tested.authenticationUtilService).updateAuthenticatedContributorProfile();
			Assert.assertNotNull(ret);
			Assert.assertEquals(2, ret.size());
			Assert.assertTrue((Boolean) ret.get("authenticated"));
			Assert.assertEquals(roles, ret.get("roles"));
			Mockito.verify(tested.authenticationUtilService).updateAuthenticatedContributorProfile();
			Mockito.verify(tested.authenticationUtilService).getUserRoles();
			Mockito.verifyNoMoreInteractions(tested.authenticationUtilService);
		}

		// case - contributor id returned, no roles requested
		{
			Mockito.reset(tested.authenticationUtilService);
			Mockito.when(tested.authenticationUtilService.getAuthenticatedContributor(false)).thenReturn("aaa <jb@jk.po>");

			Map<String, Object> ret = tested.authStatus(null);

			Mockito.verify(tested.authenticationUtilService).getAuthenticatedContributor(false);
			Mockito.verify(tested.authenticationUtilService).updateAuthenticatedContributorProfile();
			Assert.assertNotNull(ret);
			Assert.assertEquals(1, ret.size());
			Assert.assertTrue((Boolean) ret.get("authenticated"));
			Mockito.verify(tested.authenticationUtilService).updateAuthenticatedContributorProfile();
			Mockito.verifyNoMoreInteractions(tested.authenticationUtilService);
		}

	}

}
