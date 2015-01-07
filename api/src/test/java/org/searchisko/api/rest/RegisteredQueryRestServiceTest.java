/*
 * JBoss, Home of Professional Open Source
 * Copyright 2015 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 */
package org.searchisko.api.rest;

import org.junit.Test;
import org.searchisko.api.service.RegisteredQueryService;
import org.searchisko.api.testtools.ESRealClientTestBase;

/**
 * Unit test for {@link org.searchisko.api.rest.RegisteredQueryRestService}.
 *
 * TODO
 *
 * @author Lukas Vlcek
 */
public class RegisteredQueryRestServiceTest extends ESRealClientTestBase {

    @SuppressWarnings("unchecked")
    @Test
    public void processFieldSysVisibleForRoles() {

        finalizeESClientForUnitTest();
    }

    protected RegisteredQueryRestService getTested() {
        RegisteredQueryRestService tested = new RegisteredQueryRestService();
        tested.registeredQueryService = new RegisteredQueryService();
//        tested.registeredQueryService.searchClientService
//        tested.searchClientService = prepareSearchClientServiceMock();
        return tested;
    }
}
