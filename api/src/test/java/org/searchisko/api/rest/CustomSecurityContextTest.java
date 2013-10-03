/*
 * JBoss, Home of Professional Open Source
 * Copyright 2012 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 */
package org.searchisko.api.rest;

import java.security.Principal;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

/**
 * Unit test for {@link CustomSecurityContext}.
 *
 * @author Vlastimil Elias (velias at redhat dot com)
 */
public class CustomSecurityContextTest {

	@Test
	public void getUserPrincipal() {
		Principal p = Mockito.mock(Principal.class);
		CustomSecurityContext tested = new CustomSecurityContext(p, false, false, null);
		Assert.assertEquals(p, tested.getUserPrincipal());
	}

	@Test
	public void isUserInRole() {
		CustomSecurityContext tested = new CustomSecurityContext(null, false, false, null);
		Assert.assertFalse(tested.isUserInRole(null));
		Assert.assertFalse(tested.isUserInRole(""));
		Assert.assertFalse(tested.isUserInRole("unknown"));
		Assert.assertFalse(tested.isUserInRole(CustomSecurityContext.SUPER_ADMIN_ROLE));

		tested = new CustomSecurityContext(null, true, false, null);
		Assert.assertFalse(tested.isUserInRole(null));
		Assert.assertFalse(tested.isUserInRole(""));
		Assert.assertFalse(tested.isUserInRole("unknown"));
		Assert.assertTrue(tested.isUserInRole(CustomSecurityContext.SUPER_ADMIN_ROLE));
	}

	@Test
	public void isSecure() {
		CustomSecurityContext tested = new CustomSecurityContext(null, false, false, null);
		Assert.assertFalse(tested.isSecure());
		tested = new CustomSecurityContext(null, false, true, null);
		Assert.assertTrue(tested.isSecure());
	}

	@Test
	public void getAuthenticationScheme() {
		CustomSecurityContext tested = new CustomSecurityContext(null, false, false, null);
		Assert.assertEquals(null, tested.getAuthenticationScheme());

		tested = new CustomSecurityContext(null, false, false, "aus");
		Assert.assertEquals("aus", tested.getAuthenticationScheme());
	}

}
