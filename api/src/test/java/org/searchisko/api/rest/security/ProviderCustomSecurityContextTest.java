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
import org.searchisko.api.rest.security.ProviderCustomSecurityContext;

/**
 * Unit test for {@link ProviderCustomSecurityContext}.
 *
 * @author Vlastimil Elias (velias at redhat dot com)
 */
public class ProviderCustomSecurityContextTest {

	@Test
	public void getUserPrincipal() {
		Principal p = Mockito.mock(Principal.class);
		ProviderCustomSecurityContext tested = new ProviderCustomSecurityContext(p, false, false, null);
		Assert.assertEquals(p, tested.getUserPrincipal());
	}

	@Test
	public void isUserInRole() {
		ProviderCustomSecurityContext tested = new ProviderCustomSecurityContext(null, false, false, null);
		Assert.assertFalse(tested.isUserInRole(null));
		Assert.assertFalse(tested.isUserInRole(""));
		Assert.assertFalse(tested.isUserInRole("unknown"));
		Assert.assertFalse(tested.isUserInRole(ProviderCustomSecurityContext.SUPER_ADMIN_ROLE));

		tested = new ProviderCustomSecurityContext(null, true, false, null);
		Assert.assertFalse(tested.isUserInRole(null));
		Assert.assertFalse(tested.isUserInRole(""));
		Assert.assertFalse(tested.isUserInRole("unknown"));
		Assert.assertTrue(tested.isUserInRole(ProviderCustomSecurityContext.SUPER_ADMIN_ROLE));
	}

	@Test
	public void isSecure() {
		ProviderCustomSecurityContext tested = new ProviderCustomSecurityContext(null, false, false, null);
		Assert.assertFalse(tested.isSecure());
		tested = new ProviderCustomSecurityContext(null, false, true, null);
		Assert.assertTrue(tested.isSecure());
	}

	@Test
	public void getAuthenticationScheme() {
		ProviderCustomSecurityContext tested = new ProviderCustomSecurityContext(null, false, false, null);
		Assert.assertEquals(null, tested.getAuthenticationScheme());

		tested = new ProviderCustomSecurityContext(null, false, false, "aus");
		Assert.assertEquals("aus", tested.getAuthenticationScheme());
	}

}
