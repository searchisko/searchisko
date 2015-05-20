/*
 * JBoss, Home of Professional Open Source
 * Copyright 2014 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 */
package org.searchisko.ftest.rest;

import com.jayway.restassured.http.ContentType;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.junit.InSequence;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.searchisko.api.security.Role;
import org.searchisko.ftest.DeploymentHelpers;
import org.searchisko.ftest.ProviderModel;
import org.searchisko.ftest.filter.ESProxyFilterTest;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import static com.jayway.restassured.RestAssured.given;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.iterableWithSize;

/**
 * Integration test for /search/{registered_query} REST API.
 *
 * @author Lukas Vlcek
 * @see org.searchisko.api.rest.SearchRestService
 * @see <a href="https://github.com/searchisko/searchisko/issues/123>See GitHub issue details</a>
 */
@RunWith(Arquillian.class)
public class SearchRegisteredQueriesRestServiceTest {

    public static final String SEARCH_REST_API = DeploymentHelpers.CURRENT_REST_VERSION + "search";
    public static final String QUERY_REST_API = DeploymentHelpers.CURRENT_REST_VERSION + "query";

    @Deployment(testable = false)
    public static WebArchive createDeployment() throws IOException {
        return DeploymentHelpers.createDeployment();
    }

    @ArquillianResource
    URL context;

    public static final String TYPE1 = "provider1_blog";
    public static final String TYPE2 = "provider1_issue";

    static ProviderModel provider1 = new ProviderModel("provider1", "password");

    static final String contentId = "test-id";

    static final String contentId2 = "test-id2";
    static final String contentId3 = "test-id3";
    static final String contentId4 = "test-id4";

    @Test
    @InSequence(10)
    public void setupCreateProviderAndDocumentTypes() throws MalformedURLException {
        String idx1 = provider1.addContentType(TYPE1, "blogpost", true);
        String idx2 = provider1.addContentType(TYPE2, "issue", true, "", Role.PROVIDER);

        ProviderRestServiceTest.createNewProvider(context, provider1);
        ESProxyFilterTest.createSearchESIndex(context, idx1, "{}");
        ESProxyFilterTest.createSearchESIndex(context, idx2, "{}");
        ESProxyFilterTest.createSearchESIndexMapping(context, idx1, TYPE1, "{\"" + TYPE1 + "\":{}}");
        ESProxyFilterTest.createSearchESIndexMapping(context, idx2, TYPE2, "{\"" + TYPE2 + "\":{}}");

        // TODO: implement utility to bring in configuration/mappings/sys_queries/query.json
        ESProxyFilterTest.createSearchESIndex(context, "sys_queries", "{}");
        ESProxyFilterTest.createSearchESIndexMapping(context, "sys_queries", "query",
                "{\n" +
                "    \"query\" : {\n" +
                "        \"_timestamp\" : { \"enabled\" : true },\n" +
                "        \"_all\" : {\"enabled\" : false},\n" +
                "        \"properties\" : {\n" +
                "            \"name\" : {\"type\" : \"string\", \"analyzer\" : \"keyword\"},\n" +
                "            \"description\" : {\"type\" : \"string\", \"index\" : \"no\"},\n" +
                "            \"template\" : {\n" +
                "              \"type\" : \"object\", \"enabled\" : false \n" +
                "            }\n" +
                "        }\n" +
                "    }\n" +
                "}");
    }

    @Test
    @InSequence(39)
    public void setupPushContent() throws MalformedURLException {
        {
            Map<String, Object> content = new HashMap<>();
            content.put("data", "test");
            content.put("data2", "test2");
            content.put("data3", "test3");
            content.put("data4", "test4");
            content.put("data5", "foo");
            ContentRestServiceTest.createOrUpdateContent(context, provider1, TYPE1, contentId, content);
        }

        {
            Map<String, Object> content = new HashMap<>();
            content.put("data", "test");
            content.put("data2", "test2");
            content.put("data3", "test3");
            content.put("data4", "test4");
            content.put("data5", "bar");
            ContentRestServiceTest.createOrUpdateContent(context, provider1, TYPE1, contentId2, content);
        }

        {
            Map<String, Object> content = new HashMap<>();
            content.put("data", "test");
            content.put("data2", "test2");
            content.put("data3", "test3");
            content.put("data4", "test4");
            content.put("data5", "bar");
            ContentRestServiceTest.createOrUpdateContent(context, provider1, TYPE2, contentId3, content);
        }

        {
            Map<String, Object> content = new HashMap<>();
            content.put("data", "test");
            content.put("data2", "test2");
            content.put("data3", "test3");
            content.put("data4", "test4");
            content.put("data5", "bar");
            ContentRestServiceTest.createOrUpdateContent(context, provider1, TYPE2, contentId4, content);
        }
    }

    @Test
    @InSequence(90)
    public void testRegisteredQueryNotFound() throws MalformedURLException {
        given().expect()
               .statusCode(404)
               .when().get(new URL(context, SEARCH_REST_API + "/invalid_query_id").toExternalForm());
    }

    @Test
    @InSequence(100)
    public void testRegisteredQuery() throws MalformedURLException {

        // uncomment if you run only this test method
        // setupCreateProviderAndDocumentTypes();
        // re-index test documents again to make sure we have 4 documents
        // setupPushContent();

        String query = "{\n" +
                "  \"id\": \"query0\",\n" +
                "  \"template\": {\n" +
                "      \"query\": {\n" +
                "         \"term\": {\n" +
                "            \"{{field}}\" : \"{{value}}\" \n" +
                "           }\n" +
                "       }\n" +
                "   }\n" +
                "}";

        given().contentType(ContentType.JSON)
                .auth().basic(DeploymentHelpers.DEFAULT_PROVIDER_NAME, DeploymentHelpers.DEFAULT_PROVIDER_PASSWORD)
                .body(query)
                .expect().statusCode(200)
                .log().ifValidationFails()
                .body("id", is("query0"))
                .when().post(new URL(context, QUERY_REST_API).toExternalForm());

        DeploymentHelpers.refreshES();

        given().auth().preemptive().basic(provider1.name, provider1.password)
                .expect().log().ifValidationFails()
                .statusCode(200).contentType(ContentType.JSON)
                .body("hits.total", is(4))
                .when().get(new URL(context, SEARCH_REST_API + "/query0?field=data&value=test").toExternalForm());

    }

    @Test
    @InSequence(110)
    public void testRegisteredQueryForRoles() throws MalformedURLException {

        // uncomment if you run only this test method
        // setupCreateProviderAndDocumentTypes();
        // re-index test documents again to make sure we have 4 documents
        // setupPushContent();

        String query = "{\n" +
                "  \"id\": \"query1\",\n" +
                "  \"roles\": [ \"provider\" ],\n" +
                "  \"template\": {\n" +
                "      \"query\": {\n" +
                "         \"term\": {\n" +
                "            \"{{field}}\" : \"{{value}}\" \n" +
                "           }\n" +
                "       }\n" +
                "   }\n" +
                "}";

        given().contentType(ContentType.JSON)
                .auth().basic(DeploymentHelpers.DEFAULT_PROVIDER_NAME, DeploymentHelpers.DEFAULT_PROVIDER_PASSWORD)
                .body(query)
                .expect().statusCode(200)
                .log().ifValidationFails()
                .body("id", is("query1"))
                .when().post(new URL(context, QUERY_REST_API).toExternalForm());

        DeploymentHelpers.refreshES();

        String providerUsername = "provider";
        String providerPassword = "provider";

        given().auth().preemptive().basic(providerUsername, providerPassword)
                .expect().log().ifValidationFails()
                .statusCode(200).contentType(ContentType.JSON)
                .body("hits.total", is(4))
                .when().get(new URL(context, SEARCH_REST_API + "/query1?field=data&value=test").toExternalForm());

        String contribUsername = "contributor1";
        String contribPassword = "password1";

        given().auth().preemptive().basic(contribUsername, contribPassword)
                .expect().log().ifValidationFails()
                .statusCode(403)
                .when().get(new URL(context, SEARCH_REST_API + "/query1?field=data&value=test").toExternalForm());

    }

    @Test
    @InSequence(120)
    public void testRegisteredQueryForParamArray() throws MalformedURLException {

        // uncomment if you run only this test method
        // setupCreateProviderAndDocumentTypes();
        // re-index test documents again to make sure we have 4 documents
        // setupPushContent();

        String query = "{\n" +
                "  \"id\": \"query2\",\n" +
                "  \"template\": {\n" +
                "      \"query\": {\n" +
                "         \"terms\": {\n" +
                "            \"{{field}}\" : [ \"{{#value}}\", \"{{.}}\", \"{{/value}}\" ]\n" +
                "           }\n" +
                "       }\n" +
                "   }\n" +
                "}";

        given().contentType(ContentType.JSON)
                .auth().basic(DeploymentHelpers.DEFAULT_PROVIDER_NAME, DeploymentHelpers.DEFAULT_PROVIDER_PASSWORD)
                .body(query)
                .expect().statusCode(200)
                .log().ifValidationFails()
                .body("id", is("query2"))
                .when().post(new URL(context, QUERY_REST_API).toExternalForm());

        DeploymentHelpers.refreshES();

        String providerUsername = "provider";
        String providerPassword = "provider";

        given().auth().preemptive().basic(providerUsername, providerPassword)
                .expect().log().ifValidationFails()
                .statusCode(200).contentType(ContentType.JSON)
                .body("hits.total", is(1))
                .when().get(new URL(context, SEARCH_REST_API + "/query2?field=data5&value=foo")
                .toExternalForm());

        given().auth().preemptive().basic(providerUsername, providerPassword)
                .expect().log().ifValidationFails()
                .statusCode(200).contentType(ContentType.JSON)
                .body("hits.total", is(4))
                .when().get(new URL(context, SEARCH_REST_API + "/query2?field=data5&value=foo&value=bar")
                .toExternalForm());

    }

    @Test
    @InSequence(130)
    public void testRegisteredQueryWithConditionalClause() throws MalformedURLException {

        // uncomment if you run only this test method
        // setupCreateProviderAndDocumentTypes();
        // re-index test documents again to make sure we have 4 documents
        // setupPushContent();

        String query = "{\n" +
                "  \"id\": \"query3\",\n" +
                "  \"template\": \"{" +
                "      \\\"query\\\": {" +
                "         \\\"bool\\\": {" +
                "            \\\"should\\\" : [" +
                "                { \\\"term\\\": { \\\"data5\\\": \\\"foo\\\"} }" +
                "                {{#use_also_second_term_query}}" +
                "                  , { \\\"term\\\": { \\\"data5\\\": \\\"bar\\\"} }" +
                "                {{/use_also_second_term_query}}" +
                "              ]" +
                "           }" +
                "       }" +
                "   }\"\n" +
                "}";

        given().contentType(ContentType.JSON)
                .auth().basic(DeploymentHelpers.DEFAULT_PROVIDER_NAME, DeploymentHelpers.DEFAULT_PROVIDER_PASSWORD)
                .body(query)
                .expect().statusCode(200)
                .log().ifValidationFails()
                .body("id", is("query3"))
                .when().post(new URL(context, QUERY_REST_API).toExternalForm());

        DeploymentHelpers.refreshES();

        String providerUsername = "provider";
        String providerPassword = "provider";

        given().auth().preemptive().basic(providerUsername, providerPassword)
                .expect().log().ifValidationFails()
                .statusCode(200).contentType(ContentType.JSON)
                .body("hits.total", is(1))
                .when().get(new URL(context, SEARCH_REST_API + "/query3")
                // .when().get(new URL(context, SEARCH_REST_API + "/query3?use_also_second_term_query") // works too
                .toExternalForm());

        given().auth().preemptive().basic(providerUsername, providerPassword)
                .expect().log().ifValidationFails()
                .statusCode(200).contentType(ContentType.JSON)
                .body("hits.total", is(4))
                .when().get(new URL(context, SEARCH_REST_API + "/query3?use_also_second_term_query=yes")
                .toExternalForm());
    }

    @Test
    @InSequence(140)
    public void testRegisteredQueryWithScripting() throws MalformedURLException {

        // uncomment if you run only this test method
        // setupCreateProviderAndDocumentTypes();
        // re-index test documents again to make sure we have 4 documents
        // setupPushContent();

        String query = "{\n" +
                "  \"id\": \"query4\",\n" +
                "  \"template\": \"{" +
                "      \\\"size\\\": 0, " +
                "      \\\"query\\\": {" +
                "          \\\"match_all\\\": {}" +
                "      }," +
                "      \\\"aggs\\\": {" +
                "          \\\"scripted\\\": {" +
                "              \\\"terms\\\": {" +
                "                  \\\"script\\\": \\\"_source.data5\\\"" +
                "              }" +
                "          }" +
                "      }" +
                "   }\"\n" +
                "}";

        given().contentType(ContentType.JSON)
                .auth().basic(DeploymentHelpers.DEFAULT_PROVIDER_NAME, DeploymentHelpers.DEFAULT_PROVIDER_PASSWORD)
                .body(query)
                .expect().statusCode(200)
                .log().ifValidationFails()
                .body("id", is("query4"))
                .when().post(new URL(context, QUERY_REST_API).toExternalForm());

        DeploymentHelpers.refreshES();

        String providerUsername = "provider";
        String providerPassword = "provider";

        given().auth().preemptive().basic(providerUsername, providerPassword)
                .expect().log().ifValidationFails()
                .statusCode(200).contentType(ContentType.JSON)
                .body("hits.total", is(4))
                .body("aggregations.scripted.buckets", iterableWithSize(2))
                .body("aggregations.scripted.buckets[0].key", is("bar"))
                .body("aggregations.scripted.buckets[0].doc_count", is(3))
                .body("aggregations.scripted.buckets[1].key", is("foo"))
                .body("aggregations.scripted.buckets[1].doc_count", is(1))
                .when().get(new URL(context, SEARCH_REST_API + "/query4")
                .toExternalForm());
    }

    @Test
    @InSequence(150)
    public void testRegisteredQueryWithDefaultSysType() throws MalformedURLException {

        // uncomment if you run only this test method
        // setupCreateProviderAndDocumentTypes();
        // re-index test documents again to make sure we have 4 documents
        // setupPushContent();

        String query = "{\n" +
                "  \"id\": \"query5\",\n" +
                "  \"default\": {\n" +
                "    \"sys_type\": \"issue\"\n" +
                "   },\n" +
                "  \"template\": {" +
                "    \"query\": { \"match_all\": {}} \n" +
                "   }" +
                "}";

        given().contentType(ContentType.JSON)
                .auth().basic(DeploymentHelpers.DEFAULT_PROVIDER_NAME, DeploymentHelpers.DEFAULT_PROVIDER_PASSWORD)
                .body(query)
                .expect().statusCode(200)
                .log().ifValidationFails()
                .body("id", is("query5"))
                .when().post(new URL(context, QUERY_REST_API).toExternalForm());

        DeploymentHelpers.refreshES();

        String providerUsername = "provider";
        String providerPassword = "provider";

        given().auth().preemptive().basic(providerUsername, providerPassword)
                .expect().log().ifValidationFails()
                .statusCode(200).contentType(ContentType.JSON)
                .body("hits.total", is(2))
                .when().get(new URL(context, SEARCH_REST_API + "/query5")
                .toExternalForm());
    }

    @Test
    @InSequence(160)
    public void testRegisteredQueryWithDefaultSysContentType() throws MalformedURLException {

        // uncomment if you run only this test method
        // setupCreateProviderAndDocumentTypes();
        // re-index test documents again to make sure we have 4 documents
        // setupPushContent();

        String query = "{\n" +
                "  \"id\": \"query6\",\n" +
                "  \"default\": {\n" +
                "    \"sys_content_type\": \"provider1_issue\"\n" +
                "   },\n" +
                "  \"template\": {" +
                "    \"query\": { \"match_all\": {}} \n" +
                "   }" +
                "}";

        given().contentType(ContentType.JSON)
                .auth().basic(DeploymentHelpers.DEFAULT_PROVIDER_NAME, DeploymentHelpers.DEFAULT_PROVIDER_PASSWORD)
                .body(query)
                .expect().statusCode(200)
                .log().ifValidationFails()
                .body("id", is("query6"))
                .when().post(new URL(context, QUERY_REST_API).toExternalForm());

        DeploymentHelpers.refreshES();

        String providerUsername = "provider";
        String providerPassword = "provider";

        given().auth().preemptive().basic(providerUsername, providerPassword)
                .expect().log().ifValidationFails()
                .statusCode(200).contentType(ContentType.JSON)
                .body("hits.total", is(2))
                .when().get(new URL(context, SEARCH_REST_API + "/query6")
                .toExternalForm());
    }
}
