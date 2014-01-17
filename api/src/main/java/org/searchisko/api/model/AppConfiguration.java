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
 *
 */
public class AppConfiguration implements Serializable {

	private static final long serialVersionUID = -7666288282634258445L;

	public enum ClientType {
		TRANSPORT, EMBEDDED
	};

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
}
