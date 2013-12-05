package org.searchisko.api.rest.security;

import javax.ws.rs.core.SecurityContext;

/**
 * Types of possible authenticated users - each part of API may require another type of user to be authenticated.
 * Separate set of REST interceptors may be used to implement authentication and authorization for given type of user.
 * Info about authenticated user is stored as {@link SecurityContext} of distinct type reflecting type.
 * 
 * @author Vlastimil Elias (velias at redhat dot com)
 */
public enum AuthenticatedUserTypes {
	PROVIDER, CONTRIBUTOR;
}