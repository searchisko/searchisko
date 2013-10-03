/*
 * JBoss, Home of Professional Open Source
 * Copyright 2012 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 */
package org.searchisko.api.service;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Named;

import org.apache.commons.codec.digest.DigestUtils;

/**
 * Security service for authentication providers
 *
 * @author Libor Krzyzanek
 * @author Vlastimil Elias (velias at redhat dot com)
 *
 */
@Named
@ApplicationScoped
public class SecurityService {

	/**
	 * Method which allows to generate hash from commandline.
	 *
	 * @param args - commandline arguments, username (provider name) as first parameter and password as second one.
	 */
	public static void main(String[] args) {
		if (args == null || args.length < 2) {
			System.out
					.println("You have to pass username (provider name) as first parameter and password as second one to obtain hash");
			return;
		}
		SecurityService s = new SecurityService();
		System.out.println("Hash for username (provider name) '" + args[0] + "' is: " + s.createPwdHash(args[0], args[1]));
	}

	/**
	 * Create password hash.
	 *
	 * @param username
	 * @param pwd
	 * @return hash from username and password
	 */
	public String createPwdHash(String username, String pwd) {
		return DigestUtils.shaHex(pwd + username);
	}

	/**
	 * Check if given hash matches username and password
	 *
	 * @param username
	 * @param pwd
	 * @param hash
	 * @return true if hash matches
	 */
	public boolean checkPwdHash(String username, String pwd, String hash) {
		return hash.equals(createPwdHash(username, pwd));
	}

}
