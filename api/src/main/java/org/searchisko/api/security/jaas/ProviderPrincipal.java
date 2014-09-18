package org.searchisko.api.security.jaas;

import java.util.HashSet;
import java.util.Set;

/**
 * Provider principal. It's used when provider is authenticated.
 *
 * @author Libor Krzyzanek
 */
public class ProviderPrincipal implements PrincipalWithRoles {

	protected String name;

	protected Set<String> roles;

	public ProviderPrincipal(String name) {
		this(name, new HashSet<String>());
	}

	public ProviderPrincipal(String name, Set<String> roles) {
		this.name = name;
		this.roles = roles;
	}

	@Override
	public String getName() {
		return name;
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
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		ProviderPrincipal that = (ProviderPrincipal) o;

		if (name != null ? !name.equals(that.name) : that.name != null) return false;

		return true;
	}

	@Override
	public int hashCode() {
		return name != null ? name.hashCode() : 0;
	}

	@Override
	public String toString() {
		return "ProviderPrincipal{" +
				"name='" + name + '\'' +
				"roles='" + roles + '\'' +
				'}';
	}
}
