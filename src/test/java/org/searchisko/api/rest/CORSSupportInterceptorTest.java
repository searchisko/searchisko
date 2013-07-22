/*
 * JBoss, Home of Professional Open Source
 * Copyright 2012 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 */
package org.searchisko.api.rest;

import static org.searchisko.api.rest.CORSSupportInterceptor.ACCESS_CONTROL_ALLOW_METHODS;
import static org.searchisko.api.rest.CORSSupportInterceptor.ACCESS_CONTROL_ALLOW_ORIGIN;
import static org.searchisko.api.rest.CORSSupportInterceptor.ACCESS_CONTROL_MAX_AGE;

import java.lang.reflect.Method;

import javax.ws.rs.GET;
import javax.ws.rs.OPTIONS;

import org.searchisko.api.annotations.header.CORSSupport;
import org.jboss.resteasy.core.ServerResponse;
import org.junit.Assert;
import org.junit.Test;

/**
 * Unit test for {@link CORSSupportInterceptor}
 * 
 * @author Lukas Vlcek
 */
public class CORSSupportInterceptorTest {

	private CORSSupportInterceptor getTested() {
		CORSSupportInterceptor tested = new CORSSupportInterceptor();
		return tested;
	}

	@GET
	@CORSSupport()
	private void methodGET() {
	}

	@OPTIONS
	@CORSSupport(allowedMethods = { CORSSupport.PUT, CORSSupport.POST })
	private void methodOPTIONS() {
	}

	@Test
	@SuppressWarnings("rawtypes")
	public void responseHeaderForGETTest() {

		Class noparams[] = {};
		Method aMethod = null;

		try {
			aMethod = CORSSupportInterceptorTest.class.getDeclaredMethod("methodGET", noparams);
		} catch (NoSuchMethodException e) {
			Assert.fail(e.getMessage());
		}

		ServerResponse response = new ServerResponse();
		response.setResourceMethod(aMethod);

		CORSSupportInterceptor.addHeaders(aMethod);
		CORSSupportInterceptor tested = getTested();
		tested.postProcess(response);

		Assert.assertTrue(response.getMetadata().containsKey(ACCESS_CONTROL_ALLOW_ORIGIN));
		Assert.assertEquals("*", response.getMetadata().get(ACCESS_CONTROL_ALLOW_ORIGIN).get(0));

		Assert.assertFalse(response.getMetadata().containsKey(ACCESS_CONTROL_MAX_AGE));
	}

	@Test
	@SuppressWarnings("rawtypes")
	public void responseHeaderForOPTIONSTest() {

		Class noparams[] = {};
		Method aMethod = null;

		try {
			aMethod = CORSSupportInterceptorTest.class.getDeclaredMethod("methodOPTIONS", noparams);
		} catch (NoSuchMethodException e) {
			Assert.fail(e.getMessage());
		}

		ServerResponse response = new ServerResponse();
		response.setResourceMethod(aMethod);

		CORSSupportInterceptor.addHeaders(aMethod);
		CORSSupportInterceptor tested = getTested();
		tested.postProcess(response);

		Assert.assertTrue(response.getMetadata().containsKey(ACCESS_CONTROL_ALLOW_ORIGIN));
		Assert.assertEquals("*", response.getMetadata().get(ACCESS_CONTROL_ALLOW_ORIGIN).get(0));

		Assert.assertTrue(response.getMetadata().containsKey(ACCESS_CONTROL_MAX_AGE));
		Assert.assertEquals("86400", response.getMetadata().get(ACCESS_CONTROL_MAX_AGE).get(0));

		Assert.assertTrue(response.getMetadata().containsKey(ACCESS_CONTROL_ALLOW_METHODS));
		Assert.assertEquals(CORSSupport.PUT, response.getMetadata().get(ACCESS_CONTROL_ALLOW_METHODS).get(0));
		Assert.assertEquals(CORSSupport.POST, response.getMetadata().get(ACCESS_CONTROL_ALLOW_METHODS).get(1));
	}
}
