/*
 * JBoss, Home of Professional Open Source
 * Copyright 2014 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 */

package org.searchisko.api.security.jaas;

import java.security.Principal;
import java.util.Set;

/**
 * Principal with roles associated with it.
 * JAAS doesn't provide easy way how to get list of roles for princiapl only isUserInRole
 *
 * @author Libor Krzyzanek
 */
public interface PrincipalWithRoles extends Principal {

	public Set<String> getRoles();

	public void setRoles(Set<String> roles);
}
