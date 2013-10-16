/*
 * JBoss, Home of Professional Open Source
 * Copyright 2012 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 */
package org.searchisko.ftest;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.UriBuilder;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.searchisko.api.rest.ContributorRestService;

import static org.junit.Assert.assertNotNull;

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

    @Test
    public void assertServiceResponding(@ArquillianResource URL base) throws NoSuchMethodException, URISyntaxException {

        final URI baseUri = new URI(base.toString() + "v1/rest/");
        final Client client = ClientBuilder.newBuilder().build();
        final WebTarget target = client.target(UriBuilder.fromUri(baseUri).clone().path(ContributorRestService.class)
                .path(ContributorRestService.class, "search"));
        final String response = target.queryParam("email", new String[] { "pmuir@redhat.com" }).request().get(String.class);

        assertNotNull(response);
    }

//	@Test
    // TODO: This should be in a unit test
//	public void search_permissions() throws Exception {
//		TestUtils.assertPermissionGuest(ContributorRestService.class, "search", String.class);
//	}
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
//	@Test
    // TODO: This should be in a Unit test
//	public void getAll_permissions() {
//		TestUtils.assertPermissionSuperProvider(ContributorRestService.class, "getAll", Integer.class, Integer.class);
//	}
//
//	@Test
    // TODO: This should be in a Unit test
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
