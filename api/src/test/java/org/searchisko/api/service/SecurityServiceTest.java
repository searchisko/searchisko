/*
 * JBoss, Home of Professional Open Source
 * Copyright 2012 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 */
package org.searchisko.api.service;

import org.junit.Assert;
import org.junit.Test;

/**
 * Unit test for {@link SecurityService}
 *
 * @author Vlastimil Elias (velias at redhat dot com)
 */
public class SecurityServiceTest {

	@Test(expected = IllegalArgumentException.class)
	public void main_noArguments() {
		SecurityService.main(null);
	}

	@Test(expected = IllegalArgumentException.class)
	public void main_badArguments() {
		SecurityService.main(new String[] { "aa" });
	}


	@Test
	public void main() {
		SecurityService.main(new String[] { "provider1", "pwd" });
		SecurityService.main(new String[] { "provider2", "pwd" });
	}

	@Test
	public void createAndCheckPasswordHash() {
		SecurityService tested = new SecurityService();
		Assert.assertTrue(tested.checkPwdHash("uname", "pwd", tested.createPwdHash("uname", "pwd")));
	}
}
