/*
 * JBoss, Home of Professional Open Source
 * Copyright 2012 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 */
package org.searchisko.api.service;

import java.security.Principal;
import java.util.Collection;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.inject.Named;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang.StringUtils;
import org.searchisko.api.rest.exception.NotAuthenticatedException;
import org.searchisko.api.rest.exception.NotAuthorizedException;
import org.searchisko.api.security.AuthenticatedUserType;
import org.searchisko.api.security.Role;
import org.searchisko.api.security.jaas.ContributorPrincipal;
import org.searchisko.api.security.jaas.PrincipalWithRoles;
import org.searchisko.api.util.SearchUtils;

/**
 * Authentication utility service. Use it in your RestServices and other places if you need info about currently logged
 * in user! Also contains methods for fine grained permission checks. It encapsulates underlying security mechanism
 * based on {@link HttpServletRequest#isUserInRole(String)} and {@link HttpServletRequest#getUserPrincipal()}.
 * 
 * @author Libor Krzyzanek
 * @author Vlastimil Elias (velias at redhat dot com)
 */
@Named
@RequestScoped
public class AuthenticationUtilService {

	@Inject
	protected Logger log;

	@Inject
	protected ContributorProfileService contributorProfileService;

	@Inject
	protected HttpServletRequest httpRequest;

	/**
	 * request scoped cache.
	 */
	protected String cachedContributorId;

	/**
	 * Check if any user is authenticated just now.
	 * 
	 * @return true is any user is authenticated
	 */
	public boolean isAuthenticatedUser() {
		return httpRequest.getUserPrincipal() != null;
	}

	/**
	 * Get authenticated user principal. Return null if no any user is authenticated.
	 * 
	 * @return principal if user is authenticated
	 */
	public Principal getAuthenticatedUserPrincipal() {
		return httpRequest.getUserPrincipal();
	}

	/**
	 * Check is user is in given role.
	 * 
	 * @param role to check permission for
	 * @return true if user is in given role.
	 */
	public boolean isUserInRole(String role) {
		role = StringUtils.trimToNull(role);
		if (role == null)
			return false;
		return httpRequest.isUserInRole(role);
	}

	/**
	 * Check if user is at least in one of defined set of roles.
	 * 
	 * @param acceptAdmin if true and user has {@link Role#ADMIN} then method returns true
	 * @param roles array of roles to check
	 * @return true if user is at least in one of defined roles
	 */
	public boolean isUserInAnyOfRoles(boolean acceptAdmin, String... roles) {
		if (acceptAdmin && isUserInRole(Role.ADMIN))
			return true;
		if (roles == null || roles.length == 0)
			return false;
		for (String role : roles) {
			if (isUserInRole(role))
				return true;
		}
		return false;
	}

	/**
	 * Check if user is at least in one of defined set of roles.
	 * 
	 * @param acceptAdmin if true and user has {@link Role#ADMIN} then method returns true
	 * @param roles array of roles to check
	 * @return true if user is at least in one of defined roles
	 */
	public boolean isUserInAnyOfRoles(boolean acceptAdmin, Collection<String> roles) {
		if (acceptAdmin && isUserInRole(Role.ADMIN))
			return true;
		if (roles == null || roles.isEmpty())
			return false;
		for (String role : roles) {
			if (isUserInRole(role))
				return true;
		}
		return false;
	}

	/**
	 * Get name of authenticated/logged in 'provider' based on security user principal. Can be used only in methods where
	 * roles ${@link org.searchisko.api.security.Role#PROVIDER} is applied.
	 * 
	 * @return name of authenticated provider
	 * @throws NotAuthenticatedException if Provider is not authenticated. Use role PROVIDER to your REST service to
	 *           prevent this exception.
	 */
	public String getAuthenticatedProvider() throws NotAuthenticatedException {
		if (!isAuthenticatedUserOfType(AuthenticatedUserType.PROVIDER)) {
			throw new NotAuthenticatedException(AuthenticatedUserType.PROVIDER);
		}
		return httpRequest.getUserPrincipal().getName();
	}

	/**
	 * Check if logged in user has management permission for passed in provider.
	 * 
	 * @param providerName to check permission for
	 * @throws NotAuthorizedException if user has not the permission
	 */
	public void checkProviderManagementPermission(String providerName) throws NotAuthorizedException {
		if (log.isLoggable(Level.FINE)) {
			log.log(Level.FINE, "Going to check ProviderManage permission for provider {0} and principal {1}", new Object[] {
					providerName, httpRequest.getUserPrincipal() });
		}
		try {
			if (httpRequest.getUserPrincipal() != null && providerName != null
					&& (httpRequest.isUserInRole(Role.ADMIN) || providerName.equalsIgnoreCase(getAuthenticatedProvider()))) {
				return;
			}
		} catch (NotAuthenticatedException e) {
			// let id be as we throw new one later
		}
		throw new NotAuthorizedException("management permission for content provider " + providerName);
	}

	/**
	 * Get 'contributor id' for currently authenticated/logged in user based on security user principal. Can be used only
	 * in methods where {@link org.searchisko.api.security.Role#CONTRIBUTOR} applied.
	 * 
	 * @param forceCreate if <code>true</code> we need contributor id so backend should create it for logged in user if
	 *          not created yet. If <code>false</code> then we do not need it currently, so system can't create it but
	 *          return null instead.
	 * @return contributor id - can be null if <code><forceCreate</code> is false and contributor record do not exists yet
	 *         for current user.
	 * @throws NotAuthenticatedException in case contributor is not authenticated/logged in. This should never happen if
	 *           security interceptor is correctly implemented and configured for this class and
	 *           {@link org.searchisko.api.security.Role#CONTRIBUTOR} is applied.
	 */
	public String getAuthenticatedContributor(boolean forceCreate) throws NotAuthenticatedException {
		log.log(Level.FINEST, "Get Authenticated Contributor, forceCreate: {0}", forceCreate);

		if (!isAuthenticatedUserOfType(AuthenticatedUserType.CONTRIBUTOR)) {
			log.fine("User is not authenticated");
			cachedContributorId = null;
			throw new NotAuthenticatedException(AuthenticatedUserType.CONTRIBUTOR);
		}

		// cache contributor id in request not to call backend service too often
		if (cachedContributorId != null) {
			return cachedContributorId;
		}

		Principal user = httpRequest.getUserPrincipal();

		String cid = SearchUtils.trimToNull(contributorProfileService.getContributorId(
				mapPrincipalToContributorCodeType(user), user.getName(), forceCreate));
		cachedContributorId = cid;

		log.log(Level.FINE, "Contributor ID for authenticated user: {0}", cid);

		return cid;
	}

	/**
	 * Force update of currently logged in contributor profile. No any exception is thrown. Should be called after
	 * contributor authentication.
	 */
	public boolean updateAuthenticatedContributorProfile() {
		if (isAuthenticatedUserOfType(AuthenticatedUserType.CONTRIBUTOR)) {
			try {
				String username = SearchUtils.trimToNull(httpRequest.getUserPrincipal().getName());
				if (username != null) {
					// TODO CONTRIBUTOR_PROFILE we should consider to run update in another thread not to block caller
					contributorProfileService.createOrUpdateProfile(
							mapPrincipalToContributorCodeType(httpRequest.getUserPrincipal()), username, false);
					return true;
				}
			} catch (Exception e) {
				log.log(Level.WARNING, "Contributor profile update failed: " + e.getMessage(), e);
			}
		}
		return false;
	}

	protected String mapPrincipalToContributorCodeType(Principal principal) {
		if (principal == null) {
			throw new NotAuthenticatedException(AuthenticatedUserType.CONTRIBUTOR);
		}
		// CAS uses own principal so we can distinguish authentication source based on it
		if (principal instanceof ContributorPrincipal || principal instanceof org.jasig.cas.client.jaas.AssertionPrincipal) {
			return ContributorProfileService.FIELD_TSC_JBOSSORG_USERNAME;
		} else {
			throw new UnsupportedOperationException("Unsupported Principal Type: " + principal);
		}
	}

	/**
	 * Get user type for currently authenticated user.
	 * 
	 * @return user type or null if unknown or not authenticated
	 */
	public AuthenticatedUserType getAuthenticatedUserType() {
		if (httpRequest.getUserPrincipal() == null) {
			return null;
		}
		if (httpRequest.isUserInRole(Role.CONTRIBUTOR)) {
			return AuthenticatedUserType.CONTRIBUTOR;
		} else if (httpRequest.isUserInRole(Role.PROVIDER)) {
			return AuthenticatedUserType.PROVIDER;
		} else {
			return null;
		}
	}

	/**
	 * Get user roles for actually authenticated user.
	 *
	 * @return null if user is not authenticated or principal is not instance of {@link org.searchisko.api.security.jaas.PrincipalWithRoles}
	 */
	public Set<String> getUserRoles() {
		Principal principal = httpRequest.getUserPrincipal();
		if (httpRequest.getUserPrincipal() == null) {
			return null;
		}
		if (principal instanceof PrincipalWithRoles) {
			return ((PrincipalWithRoles)principal).getRoles();
		}
		return null;
	}

	/**
	 * Check if user of given type is authenticated.
	 * 
	 * @param userType to check
	 * @return true if user of given type is authenticated.
	 */
	public boolean isAuthenticatedUserOfType(AuthenticatedUserType userType) {
		if (log.isLoggable(Level.FINEST)) {
			log.log(Level.FINEST, "Principal: {0}, role to check: {1}", new Object[] { httpRequest.getUserPrincipal(),
					userType });
		}
		if (httpRequest.getUserPrincipal() == null) {
			return false;
		}
		if (userType != null) {
			switch (userType) {
			case PROVIDER:
				return httpRequest.isUserInRole(Role.PROVIDER);
			case CONTRIBUTOR:
				return httpRequest.isUserInRole(Role.CONTRIBUTOR);
			}
		}
		return false;
	}

}
