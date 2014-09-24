/*
 * JBoss, Home of Professional Open Source
 * Copyright 2014 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 */

package org.searchisko.api.events;

import java.util.Map;

import org.searchisko.api.security.AuthenticatedUserType;

/**
 * CDI Event to notify system that roles has changed for given authenticated entity.
 *
 * @author Libor Krzyzanek
 * @see org.searchisko.api.security.util.ActualRolesService
 */
public class RolesUpdatedEvent {

	private AuthenticatedUserType authenticatedUserType;

	private Map<String, Object> entity;

	public RolesUpdatedEvent(AuthenticatedUserType authenticatedUserType, Map<String, Object> entity) {
		this.authenticatedUserType = authenticatedUserType;
		this.entity = entity;
	}

	public Map<String, Object> getEntity() {
		return entity;
	}

	public AuthenticatedUserType getAuthenticatedUserType() {
		return authenticatedUserType;
	}

	@Override
	public String toString() {
		return "RolesUpdatedEvent{" +
				"authenticatedUserType=" + authenticatedUserType +
				", entity=" + entity +
				'}';
	}
}
