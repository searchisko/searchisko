/*
 * JBoss, Home of Professional Open Source
 * Copyright 2014 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 */
package org.searchisko.api.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ejb.Lock;
import javax.ejb.LockType;
import javax.ejb.Singleton;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.inject.Named;

/**
 * Business logic service for 'Content manipulation API' locks.
 * 
 * @author Vlastimil Elias (velias at redhat dot com)
 */
@Named
@ApplicationScoped
@Singleton
@Lock(LockType.READ)
public class ContentManipulationLockService {

	@Inject
	protected ConfigService configService;

	public static final String API_ID_ALL = "_all";

	/**
	 * Check if lock for given provider exists.
	 * 
	 * @param providerName to check
	 * @return true if lock exist (all lock also returns true here)
	 */
	public boolean isLockedForProvider(String providerName) {
		List<String> locks = getCurrentLocks();
		return locks != null && (locks.contains(API_ID_ALL) || locks.contains(providerName));
	}

	/**
	 * Get info about all currently existing locks.
	 * 
	 * @return list of all current locks. Contains {@link #API_ID_ALL} value if all providers are locked. Can be null if
	 *         no locks are there.
	 * @see #createLockAll()
	 * @see #removeLockAll()
	 */
	public List<String> getLockInfo() {
		return getCurrentLocks();
	}

	/**
	 * Create lock for one provider.
	 * 
	 * @param providerName - name of provider to create lock for. It is not validated inside of this method for existence!
	 * @return true if lock has been created, false if exists already
	 */
	public boolean createLock(String providerName) {
		synchronized (CFGFILE_NAME) {
			List<String> locks = getCurrentLocks();
			if (locks != null) {
				if (locks.contains(API_ID_ALL) || locks.contains(providerName))
					return false;
			} else {
				locks = new ArrayList<>();
			}
			locks.add(providerName);
			storeCurrentLocks(locks);
			return true;
		}
	}

	/**
	 * Create lock for all providers
	 */
	public void createLockAll() {
		synchronized (CFGFILE_NAME) {
			List<String> locks = new ArrayList<>();
			locks.add(API_ID_ALL);
			storeCurrentLocks(locks);
		}
	}

	/**
	 * Remove lock for named provider.
	 * 
	 * @param providerName - remove lock for given provider
	 * @return false in case if it is not possible to remove lock (it means _all is locked), true if lock has been removed
	 *         or not existed
	 */
	public boolean removeLock(String providerName) {
		synchronized (CFGFILE_NAME) {
			List<String> locks = getCurrentLocks();
			if (locks != null && !locks.isEmpty()) {
				if (locks.contains(API_ID_ALL))
					return false;
				locks.remove(providerName);
				if (locks.isEmpty())
					locks = null;
				storeCurrentLocks(locks);
			}
			return true;
		}
	}

	/**
	 * Remove lock for all providers.
	 */
	public void removeLockAll() {
		synchronized (CFGFILE_NAME) {
			storeCurrentLocks(null);
		}
	}

	protected static final String CFGFILE_NAME = "sys_content_manipulation_locks";

	@SuppressWarnings("unchecked")
	protected List<String> getCurrentLocks() {
		Map<String, Object> lfile = configService.get(CFGFILE_NAME);
		if (lfile != null)
			return (List<String>) lfile.get(CFGFILE_NAME);
		return null;
	}

	protected void storeCurrentLocks(List<String> locks) {
		if (locks == null) {
			configService.delete(CFGFILE_NAME);
		} else {
			Map<String, Object> entity = new HashMap<>();
			entity.put(CFGFILE_NAME, locks);
			configService.create(CFGFILE_NAME, entity);
		}
	}

}
