package org.searchisko.api.security.jaas;

import org.jasig.cas.client.validation.Assertion;

/**
 * Contributor principal. It's used when contributor is authenticated
 *
 * @author Libor Krzyzanek
 * @see org.jasig.cas.client.jaas.AssertionPrincipal
 */
public class ContributorPrincipal extends org.jasig.cas.client.jaas.AssertionPrincipal {

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
	}

	@Override
	public String toString() {
		return "ContributorPrincipal{" +
				"name='" + getName() + '\'' +
				",assertion='" + getAssertion() + '\'' +
				'}';
	}
}
