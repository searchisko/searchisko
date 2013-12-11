/*
 * JBoss, Home of Professional Open Source
 * Copyright 2013 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 */
package org.searchisko.api.rest;

import java.util.Map;

import junit.framework.Assert;

import org.junit.Test;
import org.mockito.Mockito;
import org.searchisko.api.rest.exception.NotAuthenticatedException;
import org.searchisko.api.rest.security.AuthenticatedUserTypes;
import org.searchisko.api.rest.security.AuthenticationUtilService;

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
		Mockito.when(tested.authenticationUtilService.getAuthenticatedContributor(false)).thenThrow(
				new NotAuthenticatedException(AuthenticatedUserTypes.CONTRIBUTOR));

		Map<String, Object> ret = tested.authStatus();

		Mockito.verify(tested.authenticationUtilService).getAuthenticatedContributor(false);
		Assert.assertNotNull(ret);
		Assert.assertEquals(1, ret.size());
		Assert.assertFalse((Boolean) ret.get("authenticated"));
	}

	@Test
	public void authStatus_authenticated() {

		AuthStatusRestService tested = new AuthStatusRestService();
		tested.authenticationUtilService = Mockito.mock(AuthenticationUtilService.class);

		// case - no contributor id returned (because we do not need it)
		{
			Mockito.when(tested.authenticationUtilService.getAuthenticatedContributor(false)).thenReturn(null);

			Map<String, Object> ret = tested.authStatus();

			Mockito.verify(tested.authenticationUtilService).getAuthenticatedContributor(false);
			Assert.assertNotNull(ret);
			Assert.assertEquals(1, ret.size());
			Assert.assertTrue((Boolean) ret.get("authenticated"));
			Mockito.verify(tested.authenticationUtilService).updateAuthenticatedContributorProfile();
		}

		// case - contributor id returned
		{
			Mockito.reset(tested.authenticationUtilService);
			Mockito.when(tested.authenticationUtilService.getAuthenticatedContributor(false)).thenReturn("aaa <jb@jk.po>");

			Map<String, Object> ret = tested.authStatus();

			Mockito.verify(tested.authenticationUtilService).getAuthenticatedContributor(false);
			Mockito.verify(tested.authenticationUtilService).updateAuthenticatedContributorProfile();
			Assert.assertNotNull(ret);
			Assert.assertEquals(1, ret.size());
			Assert.assertTrue((Boolean) ret.get("authenticated"));
			Mockito.verify(tested.authenticationUtilService).updateAuthenticatedContributorProfile();
		}

	}

}
