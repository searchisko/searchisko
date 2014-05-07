/*
 * JBoss, Home of Professional Open Source
 * Copyright 2013 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 */
package org.searchisko.api.rest;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;
import org.searchisko.api.rest.exception.NotAuthenticatedException;
import org.searchisko.api.rest.security.AuthenticationUtilService;
import org.searchisko.api.security.AuthenticatedUserType;
import org.searchisko.api.testtools.TestUtils;

import javax.ws.rs.core.SecurityContext;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Unit test for {@link AuthStatusRestService}
 * 
 * @author Vlastimil Elias (velias at redhat dot com)
 */
public class AuthStatusRestServiceTest {

	@Test
	public void authStatus_permissions() throws Exception {
		TestUtils.assertPermissionContributorOptional(AuthStatusRestService.class, "authStatus");
	}

	@Test
	public void authStatus_notAuthenticated() {

		AuthStatusRestService tested = new AuthStatusRestService();
		tested.authenticationUtilService = Mockito.mock(AuthenticationUtilService.class);
		tested.log = Logger.getLogger("testlogger");

		Mockito.when(tested.authenticationUtilService.getAuthenticatedContributor(null, false)).thenThrow(
				new NotAuthenticatedException(AuthenticatedUserType.CONTRIBUTOR));

		Map<String, Object> ret = tested.authStatus();

		Mockito.verify(tested.authenticationUtilService).getAuthenticatedContributor(null, false);
		Assert.assertNotNull(ret);
		Assert.assertEquals(1, ret.size());
		Assert.assertFalse((Boolean) ret.get("authenticated"));
	}

	@Test
	public void authStatus_authenticated() {

		AuthStatusRestService tested = new AuthStatusRestService();
		tested.authenticationUtilService = Mockito.mock(AuthenticationUtilService.class);
		tested.log = Logger.getLogger("testlogger");

		SecurityContext scMock = Mockito.mock(SecurityContext.class);
		tested.securityContext = scMock;
		// case - no contributor id returned (because we do not need it)
		{
			Mockito.when(tested.authenticationUtilService.getAuthenticatedContributor(scMock, false)).thenReturn(null);

			Map<String, Object> ret = tested.authStatus();

			Mockito.verify(tested.authenticationUtilService).getAuthenticatedContributor(scMock, false);
			Assert.assertNotNull(ret);
			Assert.assertEquals(1, ret.size());
			Assert.assertTrue((Boolean) ret.get("authenticated"));
			Mockito.verify(tested.authenticationUtilService).updateAuthenticatedContributorProfile(scMock);
		}

		// case - contributor id returned
		{
			Mockito.reset(tested.authenticationUtilService);
			Mockito.when(tested.authenticationUtilService.getAuthenticatedContributor(scMock, false)).thenReturn(
					"aaa <jb@jk.po>");

			Map<String, Object> ret = tested.authStatus();

			Mockito.verify(tested.authenticationUtilService).getAuthenticatedContributor(scMock, false);
			Mockito.verify(tested.authenticationUtilService).updateAuthenticatedContributorProfile(scMock);
			Assert.assertNotNull(ret);
			Assert.assertEquals(1, ret.size());
			Assert.assertTrue((Boolean) ret.get("authenticated"));
			Mockito.verify(tested.authenticationUtilService).updateAuthenticatedContributorProfile(scMock);
		}

	}

}
