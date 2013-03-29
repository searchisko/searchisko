/*
 * JBoss, Home of Professional Open Source
 * Copyright 2012 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 */
package org.jboss.dcp.api.rest;

import static org.jboss.dcp.api.rest.AccessControlPostProcessInterceptor.*;
import org.jboss.dcp.api.annotations.header.CORSSupport;
import org.jboss.resteasy.core.ServerResponse;
import org.junit.Assert;
import org.junit.Test;

import javax.ws.rs.GET;
import javax.ws.rs.OPTIONS;
import java.lang.reflect.Method;

/**
 * Unit test for {@link AccessControlPostProcessInterceptor}
 *
 * @author Lukas Vlcek
 */
public class AccessControlPostProcessInterceptorTest {

    private AccessControlPostProcessInterceptor getTested() {
        AccessControlPostProcessInterceptor tested = new AccessControlPostProcessInterceptor();
        return tested;
    }

    @GET
    @CORSSupport()
    private void methodGET() {}

    @OPTIONS
    @CORSSupport(allowedMethods = {CORSSupport.PUT, CORSSupport.POST})
    private void methodOPTIONS() {}

    @Test
    public void responseHeaderForGETTest() {

        Class noparams[] = {};
        Method aMethod = null;

        try {
            aMethod = AccessControlPostProcessInterceptorTest.class.getDeclaredMethod("methodGET", noparams);
        } catch (NoSuchMethodException e) {
            Assert.fail(e.getMessage());
        }

        ServerResponse response = new ServerResponse();
        response.setResourceMethod(aMethod);

        AccessControlPostProcessInterceptor.addHeaders(aMethod);
        AccessControlPostProcessInterceptor tested = getTested();
        tested.postProcess(response);

        Assert.assertTrue(response.getMetadata().containsKey(ACCESS_CONTROL_ALLOW_ORIGIN));
        Assert.assertEquals("*", response.getMetadata().get(ACCESS_CONTROL_ALLOW_ORIGIN).get(0));
    }

    @Test
    public void responseHeaderForOPTIONSTest() {

        Class noparams[] = {};
        Method aMethod = null;

        try {
            aMethod = AccessControlPostProcessInterceptorTest.class.getDeclaredMethod("methodOPTIONS", noparams);
        } catch (NoSuchMethodException e) {
            Assert.fail(e.getMessage());
        }

        ServerResponse response = new ServerResponse();
        response.setResourceMethod(aMethod);

        AccessControlPostProcessInterceptor.addHeaders(aMethod);
        AccessControlPostProcessInterceptor tested = getTested();
        tested.postProcess(response);

        Assert.assertTrue(response.getMetadata().containsKey(ACCESS_CONTROL_ALLOW_ORIGIN));
        Assert.assertEquals("*", response.getMetadata().get(ACCESS_CONTROL_ALLOW_ORIGIN).get(0));

        Assert.assertTrue(response.getMetadata().containsKey(ACCESS_CONTROL_ALLOW_METHODS));
        Assert.assertEquals(CORSSupport.PUT, response.getMetadata().get(ACCESS_CONTROL_ALLOW_METHODS).get(0));
        Assert.assertEquals(CORSSupport.POST, response.getMetadata().get(ACCESS_CONTROL_ALLOW_METHODS).get(1));
    }
}
