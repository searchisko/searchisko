package org.searchisko.api.security;

/**
 * List of Roles in application
 *
 * @author Libor Krzyzanek
 */
public class Role {

	// When adding new roles:
	// 1. Add it to ALL_ROLES
	// 2. Add it to functional tests in searchisko-ftest-users.properties and searchisko-ftest-roles.properties

	public static final String[] ALL_ROLES = {
			Role.ADMIN,
			Role.PROVIDER,
			Role.CONTRIBUTOR
	};

	/**
	 * System Admin. The highest permission. Super Provider has this role
	 */
	public static final String ADMIN = "admin";

	/**
	 * Provider role
	 */
	public static final String PROVIDER = "provider";

	/**
	 * Default role for authenticated contributor
	 */
	public static final String CONTRIBUTOR = "contributor";

}
