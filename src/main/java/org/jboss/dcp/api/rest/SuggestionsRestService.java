/*
 * JBoss, Home of Professional Open Source
 * Copyright 2012 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 */
package org.jboss.dcp.api.rest;

import org.elasticsearch.action.search.*;
import org.elasticsearch.client.Client;
import org.elasticsearch.index.query.QueryBuilders;
import org.jboss.dcp.api.annotations.header.AccessControlAllowOrigin;
import org.jboss.dcp.api.annotations.security.GuestAllowed;
import org.jboss.dcp.api.model.QuerySettings;
import org.jboss.dcp.api.service.SearchClientService;
import org.jboss.dcp.api.util.SearchUtils;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.UriInfo;
import java.util.UUID;
import java.util.logging.Logger;

/**
 * Suggestions REST API.
 *
 * @author Lukas Vlcek
 */
@RequestScoped
@Path("/suggestions")
@Produces(MediaType.APPLICATION_JSON)
public class SuggestionsRestService extends RestServiceBase {

    public static final String SEARCH_INDEX_NAME = "data_project_info";

    public static final String SEARCH_INDEX_TYPE = "jbossorg_project_info";

    public static final Integer DEFAULT_SIZE = 5;

    @Inject
    protected Logger log;

    @Inject
    protected SearchClientService searchClientService;

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
    public Object project(@QueryParam(QuerySettings.QUERY_KEY) String query, @QueryParam(QuerySettings.Filters.SIZE_KEY) Integer size) {

        try {

            if (query == null) {
                throw new IllegalArgumentException(QuerySettings.QUERY_KEY);
            }

            if (size == null || size < 1) {
                size = DEFAULT_SIZE;
            } else if (size > 200) {
                size = 200; // Max
            }

            final Client client = searchClientService.getClient();
            final MultiSearchRequestBuilder msrb = getProjectMultiSearchRequestBuilder(
                    client.prepareMultiSearch(),
                    getProjectSearchNGramRequestBuilder(client.prepareSearch().setIndices(SEARCH_INDEX_NAME).setTypes(SEARCH_INDEX_TYPE), query, size),
                    getProjectSearchFuzzyRequestBuilder(client.prepareSearch().setIndices(SEARCH_INDEX_NAME).setTypes(SEARCH_INDEX_TYPE), query, size)
            );

            String responseUuid = UUID.randomUUID().toString();

            final MultiSearchResponse searchResponse = msrb.execute().actionGet();

            return createResponse(searchResponse, responseUuid);
        } catch (IllegalArgumentException e) {
            return createBadFieldDataResponse(e.getMessage());
        } catch (Exception e) {
            return createErrorResponse(e);
        }
    }

    /**
     *
     * @param srb
     * @param query
     * @return
     */
    protected SearchRequestBuilder getProjectSearchNGramRequestBuilder(SearchRequestBuilder srb, String query, int size) {
        return srb.addFields("dcp_project", "dcp_project_name")
            .setSize(size)
            .setSearchType(SearchType.QUERY_AND_FETCH)
            .setQuery(
                    QueryBuilders.queryString(query)
                            .analyzer("whitespace")
                            .field("dcp_project_name")
                            .field("dcp_project_name.edgengram")
                            .field("dcp_project_name.ngram")
            )
            .addHighlightedField("dcp_project_name", 1, 0)
            .addHighlightedField("dcp_project_name.ngram", 1, 0)
            .addHighlightedField("dcp_project_name.edgengram", 1, 0)
            ;
    }

    /**
     *
     * @param srb
     * @param query
     * @return
     */
    protected SearchRequestBuilder getProjectSearchFuzzyRequestBuilder(SearchRequestBuilder srb, String query, int size) {
        return srb.addFields("dcp_project","dcp_project_name")
                .setSize(size)
                .setSearchType(SearchType.QUERY_AND_FETCH)
                .setQuery(
                        QueryBuilders.fuzzyLikeThisQuery("dcp_project_name", "dcp_project_name.ngram")
                                .analyzer("whitespace")
                                .maxQueryTerms(10)
                                .likeText(query)
                )
                .addHighlightedField("dcp_project_name", 1, 0)
                .addHighlightedField("dcp_project_name.ngram", 1, 0)
                .addHighlightedField("dcp_project_name.edgengram", 1, 0)
                ;
    }

    /**
     * @param msrb
     * @param srbNGram
     * @param srbFuzzy
     * @return
     */
    protected MultiSearchRequestBuilder getProjectMultiSearchRequestBuilder(MultiSearchRequestBuilder msrb,
                                                                            SearchRequestBuilder srbNGram,
                                                                            SearchRequestBuilder srbFuzzy) {
        return msrb.add(srbNGram).add(srbFuzzy);
    }

}
