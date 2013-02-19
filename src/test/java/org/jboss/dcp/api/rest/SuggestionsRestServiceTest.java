/*
 * JBoss, Home of Professional Open Source
 * Copyright 2012 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 */
package org.jboss.dcp.api.rest;

import junit.framework.Assert;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.jboss.dcp.api.service.ProjectService;
import org.jboss.dcp.api.testtools.TestUtils;
import org.junit.Test;
import org.mockito.Mockito;

import java.io.IOException;
import java.util.logging.Logger;

/**
 * Unit test for {@link SuggestionsRestService}
 *
 * @author Lukas Vlcek
 */
public class SuggestionsRestServiceTest {

    @Test
    public void handleSuggestionsProject() throws IOException {
        SuggestionsRestService tested = new SuggestionsRestService();
        tested.projectService = Mockito.mock(ProjectService.class);
        tested.log = Logger.getLogger("testlogger");

        Assert.assertTrue(true);

        SearchRequestBuilder srbMock = new SearchRequestBuilder(null);
        srbMock = tested.getProjectSearchRequestBuilder(srbMock, "test");

        TestUtils.assertJsonContentFromClasspathFile("/suggestions/project_suggestions.json", srbMock.toString());
    }
}
