package org.searchisko.api.security;

/**
 * Types of possible authenticated users - each part of API may require another type of user to be authenticated.
 * Separate set of Login Modules may be used to implement authentication and authorization for given type of user.
 *
 * @author Vlastimil Elias (velias at redhat dot com)
 */
public enum AuthenticatedUserType {

	PROVIDER,

	CONTRIBUTOR

}