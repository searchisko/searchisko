/*
 * JBoss, Home of Professional Open Source
 * Copyright 2012 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 */
package org.jboss.dcp.api.rest;

import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.index.query.QueryBuilders;
import org.jboss.dcp.api.annotations.header.AccessControlAllowOrigin;
import org.jboss.dcp.api.annotations.security.GuestAllowed;
import org.jboss.dcp.api.model.QuerySettings;
import org.jboss.dcp.api.service.ProjectService;
import org.jboss.dcp.api.util.SearchUtils;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.UriInfo;
import java.util.logging.Logger;

/**
 * Suggestions REST API.
 *
 * @author Lukas Vlcek
 */
@RequestScoped
@Path("/search")
@Produces(MediaType.APPLICATION_JSON)
public class SuggestionsRestService extends RestServiceBase {

    @Inject
    protected Logger log;

    @Inject
    protected ProjectService projectService;

    @GET
    @Path("/query_string")
    @Produces(MediaType.APPLICATION_JSON)
    @GuestAllowed
    @AccessControlAllowOrigin
    public Object queryString(@Context UriInfo uriInfo) {

        try {
            MultivaluedMap<String, String> params = uriInfo.getQueryParameters();
            String query = SearchUtils.trimToNull(params.getFirst(QuerySettings.QUERY_KEY));
            if (query == null) {
                throw new IllegalArgumentException(QuerySettings.QUERY_KEY);
            }

            throw new Exception("Method not implemented yet!");

        } catch (IllegalArgumentException e) {
            return createBadFieldDataResponse(e.getMessage());
        } catch (Exception e) {
            return createErrorResponse(e);
        }
    }

    @GET
    @Path("/project")
    @Produces(MediaType.APPLICATION_JSON)
    @GuestAllowed
    @AccessControlAllowOrigin
    public Object project(@Context UriInfo uriInfo) {

        try {
            MultivaluedMap<String, String> params = uriInfo.getQueryParameters();
            String query = SearchUtils.trimToNull(params.getFirst(QuerySettings.QUERY_KEY));
            if (query == null) {
                throw new IllegalArgumentException(QuerySettings.QUERY_KEY);
            }

            final SearchRequestBuilder srb = getProjectSearchRequestBuilder(projectService.getSearchRequestBuilder(), query);

            final SearchResponse searchResponse = srb.execute().actionGet();

            return createResponse(searchResponse, null); // Do we need uuid in this case?
        } catch (IllegalArgumentException e) {
            return createBadFieldDataResponse(e.getMessage());
        } catch (Exception e) {
            return createErrorResponse(e);
        }
    }

    /**
     *
     * @param query
     * @return
     */
    protected SearchRequestBuilder getProjectSearchRequestBuilder(SearchRequestBuilder srb, String query) {
        return srb.addFields("data.name","data.code")
           .setQuery(QueryBuilders.fieldQuery("data.name",query))
           .setSearchType(SearchType.QUERY_AND_FETCH);
    }

}
