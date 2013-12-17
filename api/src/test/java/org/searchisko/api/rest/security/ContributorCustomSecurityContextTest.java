/*
 * JBoss, Home of Professional Open Source
 * Copyright 2012 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 */
package org.searchisko.api.rest.security;

import java.security.Principal;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

/**
 * Unit test for {@link ContributorCustomSecurityContext}.
 * 
 * @author Vlastimil Elias (velias at redhat dot com)
 */
public class ContributorCustomSecurityContextTest {

	@Test
	public void getUserPrincipal() {
		Principal p = Mockito.mock(Principal.class);
		ContributorCustomSecurityContext tested = new ContributorCustomSecurityContext(p, false, null);
		Assert.assertEquals(p, tested.getUserPrincipal());
	}

	@Test
	public void isUserInRole() {
		ContributorCustomSecurityContext tested = new ContributorCustomSecurityContext(null, false, null);
		Assert.assertFalse(tested.isUserInRole(null));
		Assert.assertFalse(tested.isUserInRole(""));
		Assert.assertFalse(tested.isUserInRole("unknown"));
		Assert.assertFalse(tested.isUserInRole(AuthenticatedUserType.PROVIDER.roleName()));
		Assert.assertTrue(tested.isUserInRole(AuthenticatedUserType.CONTRIBUTOR.roleName()));
	}

	@Test
	public void isSecure() {
		ContributorCustomSecurityContext tested = new ContributorCustomSecurityContext(null, false, null);
		Assert.assertFalse(tested.isSecure());
		tested = new ContributorCustomSecurityContext(null, true, null);
		Assert.assertTrue(tested.isSecure());
	}

	@Test
	public void getAuthenticationScheme() {
		ContributorCustomSecurityContext tested = new ContributorCustomSecurityContext(null, false, null);
		Assert.assertEquals(null, tested.getAuthenticationScheme());

		tested = new ContributorCustomSecurityContext(null, false, "aus");
		Assert.assertEquals("aus", tested.getAuthenticationScheme());
	}

}
