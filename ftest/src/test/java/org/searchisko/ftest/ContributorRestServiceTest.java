/*
 * JBoss, Home of Professional Open Source
 * Copyright 2012 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 */
package org.searchisko.ftest;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import com.jayway.restassured.http.ContentType;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.junit.InSequence;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.searchisko.api.rest.ContributorRestService;

import static com.jayway.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

/**
 * Unit test for {@link ContributorRestService}.
 *
 * @author Vlastimil Elias (velias at redhat dot com), Jason Porter (jporter@redhat.com)
 */
@RunWith(Arquillian.class)
public class ContributorRestServiceTest {

    @Deployment(testable = false)
    public static WebArchive createDeployment() {
        return DeploymentHelpers.createDeployment();
    }

    private final String restVersion = "v1/rest/";

    private static String contributorId;

    @Test @InSequence(0)
    public void assertServiceRespondingNoHits(@ArquillianResource URL context) throws MalformedURLException {
        given().
                param("email", "no-email@redhat.com").
        expect().
                log().ifError().
                statusCode(200).
                body("total", is(0)).
                body("hits", is(empty())).
        when().
                get(new URL(context, restVersion + "contributor/search").toExternalForm());
    }

    @Test @InSequence(1)
    public void assertPostWorksAuthenticated(@ArquillianResource URL context) throws MalformedURLException {
        final Map<String, Object> typeSpecificCode = new HashMap<>();
        typeSpecificCode.put("jbossorg_username", "LightGuard");
        typeSpecificCode.put("jbossorg_blog", "");
        typeSpecificCode.put("github_username", "LightGuard");

        final Map<String, Object> params = new HashMap<>();
        params.put("code", "Jason Porter <jporter@redhat.com>");
        params.put("email", "jporter@redhat.com");
        params.put("type_specific_code", typeSpecificCode);

        contributorId = given().
                contentType(ContentType.JSON).
                auth().basic("jbossorg", "jbossorgjbossorg").
                body(params).
        expect().
                contentType(ContentType.JSON).
                statusCode(200).
                body("id", is(not(empty()))).
        when().
                post(new URL(context, restVersion + "contributor").toExternalForm()).
                andReturn().body().jsonPath().get("id");
    }

    @Test @InSequence(2)
    public void assertUnauthenticatedPostErrors(@ArquillianResource URL context) throws MalformedURLException {
        final Map<String, Object> params = new HashMap<>();
        params.put("code", "Jason Porter <jporter@redhat.com>");
        params.put("email", "jporter@redhat.com");
        params.put("type_specific_code", "");

        given().
                contentType(ContentType.JSON).
                parameters(params).
        expect().
                statusCode(401).
        when().
                post(new URL(context, restVersion + "contributor").toExternalForm());
    }

    @Test @InSequence(3)
    public void assertAuthenticatedGetWithId(@ArquillianResource URL context) throws MalformedURLException {
        given().
                pathParam("id", contributorId).
                auth().basic("jbossorg", "jbossorgjbossorg").
        expect().
                log().ifError().
                statusCode(200).
                contentType(ContentType.JSON).
                body("email", is("jporter@redhat.com")).
                body("code", is("Jason Porter <jporter@redhat.com>")).
                body("type_specific_code.github_username", is("LightGuard")).
        when().
                get(new URL(context, restVersion + "contributor/{id}").toExternalForm());
    }

    @Test @InSequence(4)
    public void assertNonAuthenticatedGetWithId(@ArquillianResource URL context) throws MalformedURLException {
        given().
                pathParam("id", contributorId).
        expect().
                statusCode(401).
        when().
                get(new URL(context, restVersion + "contributor/{id}").toExternalForm());
    }

    @Test @InSequence(5)
    public void assertNonAuthenticatedGet(@ArquillianResource URL context) throws MalformedURLException {
        given().
        expect().
                statusCode(401).
        when().
                get(new URL(context, restVersion + "contributor/").toExternalForm());
    }

    @Test @InSequence(6)
    public void assertAuthenticatedUpdate(@ArquillianResource URL context) throws Exception {
        final Map<String, Object> typeSpecificCode = new HashMap<>();
        typeSpecificCode.put("jbossorg_blog", "seam.Jason Porter");

        final Map<String, Object> params = new HashMap<>();
        params.put("type_specific_code", typeSpecificCode);

        given().
                pathParam("id", contributorId).
                contentType(ContentType.JSON).
                body(params).
                auth().basic("jbossorg", "jbossorgjbossorg").
                log().all(true).
        expect().
                statusCode(200).
        when().
                post(new URL(context, restVersion + "contributor/{id}").toExternalForm());
    }

    @Test @InSequence(7)
    public void assertAuthenticatedGet(@ArquillianResource URL context) throws MalformedURLException {
        given().
                parameters("from", 0, "to", 100).
                auth().basic("jbossorg", "jbossorgjbossorg").
        expect().
                statusCode(200).
                contentType(ContentType.JSON).
                body("total", is(1)).
                body("hits[0].id", is(contributorId)).
                body("hits[0].data.jbossorg_blog", hasItem("seam.Jason Porter")).
        when().
                get(new URL(context, restVersion + "contributor/").toExternalForm());
    }

//
//	@Test
//	public void search() throws Exception {
//		ContributorRestService tested = new ContributorRestService();
//		tested.contributorService = Mockito.mock(ContributorService.class);
//		RestEntityServiceBaseTest.mockLogger(tested);
//
//		// case - return from service OK, one result
//		{
//			SearchResponse sr = ESDataOnlyResponseTest.mockSearchResponse("ve", "email@em", null, null);
//			Mockito.when(tested.contributorService.search("email@em")).thenReturn(sr);
//			StreamingOutput ret = (StreamingOutput) tested.search("email@em");
//			TestUtils.assetStreamingOutputContent(
//					"{\"total\":1,\"hits\":[{\"id\":\"ve\",\"data\":{\"sys_name\":\"email@em\",\"sys_id\":\"ve\"}}]}", ret);
//		}
//
//		// case - return from service OK, no result
//		{
//			Mockito.reset(tested.contributorService);
//			SearchResponse sr = ESDataOnlyResponseTest.mockSearchResponse(null, null, null, null);
//			Mockito.when(tested.contributorService.search("email@em")).thenReturn(sr);
//			StreamingOutput ret = (StreamingOutput) tested.search("email@em");
//			TestUtils.assetStreamingOutputContent("{\"total\":0,\"hits\":[]}", ret);
//		}
//
//		// case - Exception from service
//		{
//			Mockito.reset(tested.contributorService);
//			Mockito.when(tested.contributorService.search("email@em")).thenThrow(new RuntimeException("test exception"));
//			TestUtils.assertResponseStatus(tested.search("email@em"), Status.INTERNAL_SERVER_ERROR);
//		}
//	}
//
//    	@Test
    // TODO: This should be in a unit test, but has been covered in this file
//	public void search_permissions() throws Exception {
//		TestUtils.assertPermissionGuest(ContributorRestService.class, "search", String.class);
//	}
//	@Test
    // TODO: This should be in a Unit test
//	public void getAll_permissions() {
//		TestUtils.assertPermissionSuperProvider(ContributorRestService.class, "getAll", Integer.class, Integer.class);
//	}
//
//	@Test
    // TODO: This should be in a Unit test, but has been covered in this file
//	public void get_permissions() {
//		TestUtils.assertPermissionSuperProvider(ContributorRestService.class, "get", String.class);
//	}
//
//	@Test
    // TODO: This should be in a Unit test
//	public void create_permissions() {
//		TestUtils.assertPermissionSuperProvider(ContributorRestService.class, "create", String.class, Map.class);
//		TestUtils.assertPermissionSuperProvider(ContributorRestService.class, "create", Map.class);
//	}
//
//	@Test
    // TODO: This should be in a Unit test
//	public void delete_permissions() {
//		TestUtils.assertPermissionSuperProvider(ContributorRestService.class, "delete", String.class);
//	}

}
