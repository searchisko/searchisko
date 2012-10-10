/*
 * JBoss, Home of Professional Open Source
 * Copyright 2012 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 */
package org.jboss.dcp.api.service;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Named;

import org.apache.commons.codec.digest.DigestUtils;

/**
 * Security service for authentication providers
 * 
 * @author Libor Krzyzanek
 * 
 */
@Named
@ApplicationScoped
public class SecurityService {

	public String createPwdHash(String username, String pwd) {
		return DigestUtils.shaHex(pwd + username);
	}

	public boolean checkPwdHash(String username, String pwd, String hash) {
		return hash.equals(createPwdHash(username, pwd));
	}

}
