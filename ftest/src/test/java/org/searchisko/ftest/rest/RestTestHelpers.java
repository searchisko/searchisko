/*
 * JBoss, Home of Professional Open Source
 * Copyright 2014 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 */

package org.searchisko.ftest.rest;

import com.jayway.restassured.RestAssured;
import com.jayway.restassured.http.ContentType;
import com.jayway.restassured.specification.RequestSpecification;
import org.searchisko.ftest.DeploymentHelpers;

/**
 * Rest Tests helpers
 *
 * @author Libor Krzyzanek
 */
public class RestTestHelpers {

	private static void defaultSettings() {
		RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
	}

	static {
		defaultSettings();
	}

	/**
	 * Helper method to get RequestSpecification with defined: <br/>
	 * 1. Log both requests and response if validation fails
	 *
	 * @return
	 * @see com.jayway.restassured.RestAssured#enableLoggingOfRequestAndResponseIfValidationFails()
	 */
	public static RequestSpecification givenLogIfFails() {
		RestAssured.reset();
		RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
		return RestAssured.given();
	}


	/**
	 * Helper method to get RequestSpecification with defined: <br/>
	 * 1. JSON as request and response as defaults <br/>
	 * 2. Log both requests and response if validation fails
	 *
	 * @return
	 */
	public static RequestSpecification givenJsonAndLogIfFails() {
		return givenLogIfFails().request().contentType(ContentType.JSON);
	}

	/**
	 * Helper method to get RequestSpecification same as {@link #givenJsonAndLogIfFails()} plus:
	 * Basic authentication as Default Provider
	 *
	 * @return
	 */
	public static RequestSpecification givenJsonAndLogIfFailsAndAuthDefaultProvider() {
		return givenJsonAndLogIfFails().auth().basic(DeploymentHelpers.DEFAULT_PROVIDER_NAME, DeploymentHelpers.DEFAULT_PROVIDER_PASSWORD);
	}

	/**
	 * Helper method to get RequestSpecification same as {@link #givenJsonAndLogIfFails()} plus:
	 * Basic preemptive authentication for give username and password or without it if username and password is null
	 *
	 * @param username username or null if no auth. is needed
	 * @param password password or null if no auth. is needed
	 * @return
	 */
	public static RequestSpecification givenJsonAndLogIfFailsAndAuthPreemptive(String username, String password) {
		if (username == null && password == null) {
			return givenJsonAndLogIfFails();
		}
		return givenJsonAndLogIfFails().auth().preemptive().basic(username, password);
	}
}