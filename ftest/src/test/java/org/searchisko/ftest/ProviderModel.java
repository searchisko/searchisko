package org.searchisko.ftest;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.codec.digest.DigestUtils;
import org.searchisko.api.service.ProviderService;

/**
 * Simple Model for Provider
 * 
 * @author Libor Krzyzanek
 */
public class ProviderModel {

	public String name;

	public String password;

	public String passwordHash;

	public Map<String, Map<String, Object>> contentTypes = new HashMap<>();

	public ProviderModel(String name, String password) {
		this.name = name;
		this.password = password;
		this.passwordHash = DigestUtils.shaHex(password + name);
	}

	/**
	 * Add content type into provider.
	 * 
	 * @param contentType name of content type (sys_content_type)
	 * @param sysType name of sys_type
	 * @param persist true if content is marked as persistent
	 * @return name of ES index for this type
	 */
	public String addContentType(String contentType, String sysType, boolean persist) {
		return addContentType(contentType, sysType, persist, null);
	}

	/**
	 * Add content type into provider.
	 * 
	 * @param contentType name of content type (sys_content_type)
	 * @param sysType name of sys_type
	 * @param persist true if content is marked as persistent
	 * @param contentContentType {@value ProviderService#SYS_CONTENT_CONTENT_TYPE} value, can be null
	 * @param roles user roles this type is restricted to
	 * @return name of ES index for this type
	 */
	public String addContentType(String contentType, String sysType, boolean persist, String contentContentType,
			String... roles) {
		Map<String, Object> data = new HashMap<>();
		data.put(ProviderService.SYS_TYPE, sysType);
		data.put(ProviderService.PERSIST, persist);
		if (contentContentType != null) {
			data.put(ProviderService.SYS_CONTENT_CONTENT_TYPE, contentContentType);
		}

		if (roles != null) {
			data.put(ProviderService.SYS_VISIBLE_FOR_ROLES, Arrays.asList(roles));
		}

		Map<String, Object> index = new HashMap<>();
		String indexName = "data_" + contentType;
		index.put("name", indexName);
		index.put("type", contentType);
		data.put(ProviderService.INDEX, index);

		contentTypes.put(contentType, data);

		return indexName;
	}

	/**
	 * Get content type definition from provider
	 * 
	 * @param contentType to ger
	 * @return type or null if not defined
	 */
	public Map<String, Object> getContentType(String contentType) {
		return contentTypes.get(contentType);
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
		return "ProviderModel{" + "name='" + name + '\'' + ", password='" + password + '\'' + ", passwordHash='"
				+ passwordHash + '\'' + ", contentTypes=" + contentTypes + '}';
	}
}
