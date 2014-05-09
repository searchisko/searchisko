package org.searchisko.api.security.jaas;

import java.security.Principal;

/**
 * Provider principal. It's used when provider is authenticated.
 *
 * @author Libor Krzyzanek
 */
public class ProviderPrincipal implements Principal {

	protected String name;

	public ProviderPrincipal(String name) {
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
				'}';
	}
}
