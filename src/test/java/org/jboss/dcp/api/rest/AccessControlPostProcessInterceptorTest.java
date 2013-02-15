/*
 * JBoss, Home of Professional Open Source
 * Copyright 2012 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 */
package org.jboss.dcp.api.rest;

import junit.framework.Assert;
import org.jboss.resteasy.core.ServerResponse;
import org.junit.Test;

import static org.jboss.dcp.api.rest.AccessControlPostProcessInterceptor.*;

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

    @Test
    public void responseHeaderTest() {

        AccessControlPostProcessInterceptor tested = getTested();

        ServerResponse response = new ServerResponse();
        tested.postProcess(response);

        Assert.assertTrue(response.getMetadata().containsKey(ACCESS_CONTROL_ALLOW_ORIGIN));
        Assert.assertEquals("*", response.getMetadata().get(ACCESS_CONTROL_ALLOW_ORIGIN).get(0));
    }
}
