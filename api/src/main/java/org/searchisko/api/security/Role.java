package org.searchisko.api.security;

/**
 * List of Roles in application
 *
 * @author Libor Krzyzanek
 */
public class Role {

	// When adding a new role:
	// 1. Add it to ALL_ROLES
	// 2. Add it to functional tests in searchisko-ftest-users.properties and searchisko-ftest-roles.properties

	public static final String[] ALL_ROLES = {
			Role.ADMIN,
			Role.PROVIDER,
			Role.CONTRIBUTOR,
			Role.CONTRIBUTORS_MANAGER,
			Role.PROJECTS_MANAGER,
			Role.TASKS_MANAGER,
			Role.TAGS_MANAGER
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

	/**
	 * User with this role can manage contributors via Contributor REST API
	 *
	 * @see org.searchisko.api.rest.ContributorRestService
	 */
	public static final String CONTRIBUTORS_MANAGER = "contributors_manager";

	/**
	 * User with this role can manage projects via Project REST API
	 *
	 * @see org.searchisko.api.rest.ProjectRestService
	 */
	public static final String PROJECTS_MANAGER = "projects_manager";

	/**
	 * User with this role can manage tasks via Task REST API
	 *
	 * @see org.searchisko.api.rest.TaskRestService
	 */
	public static final String TASKS_MANAGER = "tasks_manager";

	/**
	 * User with this role can manage custom tags via Tagging REST API.
	 *
	 * @see org.searchisko.api.rest.CustomTagRestService
	 */
	public static final String TAGS_MANAGER = "tags_manager";

}
