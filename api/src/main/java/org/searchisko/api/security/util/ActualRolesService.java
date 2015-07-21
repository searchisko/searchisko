/*
 * JBoss, Home of Professional Open Source
 * Copyright 2014 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 */

package org.searchisko.api.security.util;

import java.security.Principal;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.PostConstruct;
import javax.ejb.Lock;
import javax.ejb.LockType;
import javax.ejb.Singleton;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;
import javax.inject.Named;

import org.infinispan.Cache;
import org.infinispan.manager.EmbeddedCacheManager;
import org.searchisko.api.events.RolesUpdatedEvent;
import org.searchisko.api.security.AuthenticatedUserType;
import org.searchisko.api.security.Role;
import org.searchisko.api.service.ContributorProfileService;
import org.searchisko.api.service.ContributorService;

/**
 * Utility Service that keeps actual roles in cache. It observes {@link org.searchisko.api.events.RolesUpdatedEvent} and
 * put updated roles into the cache.
 * 
 * @author Libor Krzyzanek
 */
@Named
@ApplicationScoped
@Singleton
@Lock(LockType.READ)
public class ActualRolesService {

	@Inject
	protected Logger log;

	@Inject
	protected EmbeddedCacheManager container;

	public static final String CACHE_NAME = "searchisko-user-roles";

	protected Cache<Object, Set<String>> actualRoles;

	@PostConstruct
	public void init() {
		actualRoles = container.getCache(CACHE_NAME);
	}

	public void rolesUpdatedEventHandler(@Observes RolesUpdatedEvent event) {
		log.log(Level.FINE, "event: {0}", event);

		if (AuthenticatedUserType.CONTRIBUTOR.equals(event.getAuthenticatedUserType())) {
			Map<String, Object> entity = event.getEntity();
			String username = ContributorService.getContributorTypeSpecificCodeFirst(entity,
					ContributorProfileService.FIELD_TSC_JBOSSORG_USERNAME);

			String key = getCacheKey(AuthenticatedUserType.CONTRIBUTOR, username);
			Set<String> roles = ContributorService.extractRoles(entity);
			roles.add(Role.CONTRIBUTOR);
			actualRoles.put(key, roles);
		}
	}

	public static String getCacheKey(AuthenticatedUserType authenticatedUserType, String username) {
		return authenticatedUserType.name() + "-" + username;
	}

	public Set<String> getActualRolesRemoveFromCache(Principal principal) {
		String key = getCacheKey(AuthenticatedUserType.CONTRIBUTOR, principal.getName());

		if (!actualRoles.containsKey(key)) {
			return null;
		}
		return actualRoles.remove(key);
	}

}
