/*
 * JBoss, Home of Professional Open Source
 * Copyright 2014 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 */

package org.searchisko.api.security.jaas;

import java.security.Principal;
import java.util.Enumeration;

import javax.security.auth.login.LoginException;

import org.jboss.security.auth.spi.UsersRolesLoginModule;

/**
 * Login module extends standard UsersRolesLoginModule and populate roles to PrincipalWithRoles object
 *
 * @author Libor Krzyzanek
 * @see org.searchisko.api.security.jaas.PrincipalWithRoles
 */
public class UsersRolesForPrincipalWithRolesLoginModule extends UsersRolesLoginModule {

	@Override
	public boolean commit() throws LoginException {
		boolean success = super.commit();

		if (success) {
			PrincipalWithRoles principal = (PrincipalWithRoles) getIdentity();

			Enumeration<? extends Principal> roles =  getRoleSets()[0].members();
			if (roles != null) {
				while (roles.hasMoreElements()) {
					principal.getRoles().add(roles.nextElement().getName());
				}
			}
		}
		return success;
	}

}
