package org.searchisko.api.security.jaas;

import java.security.Principal;

/**
 * Contributor principal. It's used when provider is authenticated although CAS uses own ${@link org.jasig.cas.client.jaas.AssertionPrincipal}
 *
 * @author Libor Krzyzanek
 * @see org.jasig.cas.client.jaas.AssertionPrincipal
 */
public class ContributorPrincipal implements Principal {

	// TODO: Unify ContributorPrincipal with CAS's org.jasig.cas.client.jaas.AssertionPrincipal

	protected String name;

	public ContributorPrincipal(String name) {
		this.name = name;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		ContributorPrincipal that = (ContributorPrincipal) o;

		if (name != null ? !name.equals(that.name) : that.name != null) return false;

		return true;
	}

	@Override
	public int hashCode() {
		return name != null ? name.hashCode() : 0;
	}

	@Override
	public String toString() {
		return "ContributorPrincipal{" +
				"name='" + name + '\'' +
				'}';
	}
}
