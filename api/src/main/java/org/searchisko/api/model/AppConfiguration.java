/*
 * JBoss, Home of Professional Open Source
 * Copyright 2012 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 */
package org.searchisko.api.model;

import java.io.Serializable;

/**
 * General application configuration
 *
 * @author Libor Krzyzanek
 */
public class AppConfiguration implements Serializable {

	private static final long serialVersionUID = -7666288282634258445L;

	public enum ClientType {
		TRANSPORT, EMBEDDED
	}

	private ClientType clientType;

	/**
	 * Application data path
	 */
	private String appDataPath;

	/**
	 * Flag if it's needed to create INIT provider data
	 */
	private boolean providerCreateInitData;

	/**
	 * Threshold for updating contributor profiles
	 */
	private int contributorProfileUpdateThreshold;

	/**
	 * Contributor Profile Provider configuration
	 */
	private ContributorProfileProviderConfig contributorProfileProviderConfig;

	/**
	 * CAS Configuration
	 */
	private CasConfig casConfig;

	public AppConfiguration(String appDataPath) {
		this.appDataPath = appDataPath;
	}

	public ClientType getClientType() {
		return clientType;
	}

	public void setClientType(ClientType clientType) {
		this.clientType = clientType;
	}

	public String getAppDataPath() {
		return appDataPath;
	}

	public void setAppDataPath(String appDataPath) {
		this.appDataPath = appDataPath;
	}

	public boolean isProviderCreateInitData() {
		return providerCreateInitData;
	}

	public void setProviderCreateInitData(boolean providerCreateInitData) {
		this.providerCreateInitData = providerCreateInitData;
	}

	public int getContributorProfileUpdateThreshold() {
		return contributorProfileUpdateThreshold;
	}

	public void setContributorProfileUpdateThreshold(int contributorProfileUpdateThreshold) {
		this.contributorProfileUpdateThreshold = contributorProfileUpdateThreshold;
	}

	public ContributorProfileProviderConfig getContributorProfileProviderConfig() {
		return contributorProfileProviderConfig;
	}

	public void setContributorProfileProviderConfig(ContributorProfileProviderConfig contributorProfileProviderConfig) {
		this.contributorProfileProviderConfig = contributorProfileProviderConfig;
	}

	public CasConfig getCasConfig() {
		return casConfig;
	}

	public void setCasConfig(CasConfig casConfig) {
		this.casConfig = casConfig;
	}

	public static class ContributorProfileProviderConfig {
		protected String urlbase;
		protected String username;
		protected String password;

		public ContributorProfileProviderConfig(String urlbase) {
			this.urlbase = urlbase;
		}

		public ContributorProfileProviderConfig(String urlbase, String username, String password) {
			this.urlbase = urlbase;
			this.username = username;
			this.password = password;
		}

		public String getUrlbase() {
			return urlbase;
		}

		public void setUrlbase(String urlbase) {
			this.urlbase = urlbase;
		}

		public String getUsername() {
			return username;
		}

		public void setUsername(String username) {
			this.username = username;
		}

		public String getPassword() {
			return password;
		}

		public void setPassword(String password) {
			this.password = password;
		}

		@Override
		public String toString() {
			return "ContributorProfileProviderConfig{" +
					"urlbase='" + urlbase + '\'' +
					", username='" + username + '\'' +
					", password=<SKIPPED>" +
					'}';
		}
	}

	public static class CasConfig {
		protected String serverName;
		protected String ServerUrl;

		public CasConfig(String serverName, String serverUrl) {
			this.serverName = serverName;
			ServerUrl = serverUrl;
		}

		public String getServerName() {
			return serverName;
		}

		public void setServerName(String serverName) {
			this.serverName = serverName;
		}

		public String getServerUrl() {
			return ServerUrl;
		}

		public void setServerUrl(String serverUrl) {
			ServerUrl = serverUrl;
		}

		@Override
		public String toString() {
			return "CasConfig{" +
					"serverName='" + serverName + '\'' +
					", ServerUrl='" + ServerUrl + '\'' +
					'}';
		}
	}

	@Override
	public String toString() {
		return "AppConfiguration{" +
				"clientType=" + clientType +
				", appDataPath='" + appDataPath + '\'' +
				", providerCreateInitData=" + providerCreateInitData +
				", contributorProfileUpdateThreshold=" + contributorProfileUpdateThreshold +
				", contributorProfileProviderConfig=" + contributorProfileProviderConfig +
				", casConfig=" + casConfig +
				'}';
	}
}
