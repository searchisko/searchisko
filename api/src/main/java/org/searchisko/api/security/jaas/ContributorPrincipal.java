package org.searchisko.api.security.jaas;

import java.util.HashSet;
import java.util.Set;

import org.jasig.cas.client.validation.Assertion;

/**
 * Contributor principal. It's used when contributor is authenticated
 *
 * @author Libor Krzyzanek
 * @see org.jasig.cas.client.jaas.AssertionPrincipal
 */
public class ContributorPrincipal extends org.jasig.cas.client.jaas.AssertionPrincipal implements PrincipalWithRoles {

	protected Set<String> roles;

	/**
	 * Use only for test purposes!
	 *
	 * @param name
	 */
	public ContributorPrincipal(final String name) {
		this(name, null);
	}

	/**
	 * Use in LoginModule
	 *
	 * @param name
	 * @param assertion
	 */
	public ContributorPrincipal(final String name, final Assertion assertion) {
		super(name, assertion);
		this.roles = new HashSet<>();
	}

	@Override
	public Set<String> getRoles() {
		return roles;
	}

	@Override
	public void setRoles(Set<String> roles) {
		this.roles = roles;
	}



	@Override
	public String toString() {
		return "ContributorPrincipal{" +
				"name='" + getName() + '\'' +
				",assertion='" + getAssertion() + '\'' +
				",roles='" + roles + '\'' +
				'}';
	}
}
