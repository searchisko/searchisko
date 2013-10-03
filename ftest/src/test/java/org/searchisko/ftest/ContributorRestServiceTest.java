/*
 * JBoss, Home of Professional Open Source
 * Copyright 2012 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 */
package org.searchisko.ftest;

import java.io.File;
import java.net.URL;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.resteasy.client.ProxyFactory;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.shrinkwrap.resolver.api.maven.Maven;
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
        final File[] runtimeDeps = Maven.resolver().loadPomFromFile("api/pom.xml").importRuntimeDependencies().resolve()
//        final File[] runtimeDeps = Maven.resolver().loadPomFromFile("../api/pom.xml").importRuntimeDependencies().resolve()
                .withTransitivity().asFile();
        final WebArchive war = ShrinkWrap.create(WebArchive.class, "searchisko-contributorrestservice.war")
                .addPackages(true, "org.searchisko.api")
                .addPackages(true, "org.searchisko.persistence")
                .addAsLibraries(runtimeDeps)
                .addAsResource("META-INF/test-persistence.xml", "META-INF/persistence.xml")
                .addAsWebInfResource(new File("src/test/resources/webapp/WEB-INF/web.xml"), "web.xml")
                .addAsResource("systeminfo.properties", "systeminfo.properties")
                // TODO: Add app.properties
                .addAsWebInfResource(new File("src/test/resources/webapp/WEB-INF/searchisko-ds.xml"), "searchisko-ds.xml")
                .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml");

        return war;
    }

    @Test
    public void assertServiceResponding(@ArquillianResource URL base) {
        ContributorRestService proxy = ProxyFactory.create(ContributorRestService.class, base.toString());

        assertNotNull(proxy.search("pmuir@redhat.com"));
    }

//	@Test
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
//	public void getAll_permissions() {
//		TestUtils.assertPermissionSuperProvider(ContributorRestService.class, "getAll", Integer.class, Integer.class);
//	}
//
//	@Test
//	public void get_permissions() {
//		TestUtils.assertPermissionSuperProvider(ContributorRestService.class, "get", String.class);
//	}
//
//	@Test
//	public void create_permissions() {
//		TestUtils.assertPermissionSuperProvider(ContributorRestService.class, "create", String.class, Map.class);
//		TestUtils.assertPermissionSuperProvider(ContributorRestService.class, "create", Map.class);
//	}
//
//	@Test
//	public void delete_permissions() {
//		TestUtils.assertPermissionSuperProvider(ContributorRestService.class, "delete", String.class);
//	}

}
