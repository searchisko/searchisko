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

	protected Map<String, Object> contentTypes = new HashMap<>();

	public ProviderModel(String name, String password) {
		this.name = name;
		this.password = password;
		this.passwordHash = DigestUtils.shaHex(password + name);
	}

	public void addContentType(String contentType, String sysType, boolean persist) {
		Map<String, Object> data = new HashMap<>();
		data.put(ProviderService.SYS_TYPE, sysType);
		data.put(ProviderService.PERSIST, persist);

		Map<String, Object> index = new HashMap<>();
		index.put("name", "data_" + contentType);
		index.put("type", contentType);
		data.put(ProviderService.INDEX, index);

		contentTypes.put(contentType, data);
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
		data.put(ProviderService.TYPE, contentTypes);
		return data;
	}

	@Override
	public String toString() {
		return "ProviderModel{" +
				"name='" + name + '\'' +
				", password='" + password + '\'' +
				", passwordHash='" + passwordHash + '\'' +
				", contentTypes=" + contentTypes +
				'}';
	}
}
