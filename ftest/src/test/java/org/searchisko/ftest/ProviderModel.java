package org.searchisko.ftest;

import org.apache.commons.codec.digest.DigestUtils;
import org.searchisko.api.service.ProviderService;

import java.util.HashMap;
import java.util.Map;

/**
 * Simple Model for Provider
 *
 * @author Libor Krzyzanek
 */
public class ProviderModel {

	protected String name;

	protected String password;

	protected String passwordHash;

	public ProviderModel(String name, String password) {
		this.name = name;
		this.password = password;
		this.passwordHash = DigestUtils.shaHex(password + name);
	}

	/**
	 * Creates "JSON" Map representation
	 *
	 * @return
	 * @see org.searchisko.api.service.ProviderService#NAME
	 */
	public Map<String, Object> getProviderJSONModel() {
		final Map<String, Object> data = new HashMap<>();
		data.put(ProviderService.NAME, name);
		data.put(ProviderService.PASSWORD_HASH, passwordHash);
		return data;
	}

	@Override
	public String toString() {
		return "ProviderModel{" +
				"name='" + name + '\'' +
				", password='" + password + '\'' +
				'}';
	}
}
