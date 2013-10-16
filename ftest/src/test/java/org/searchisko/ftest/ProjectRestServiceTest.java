/*
 * JBoss, Home of Professional Open Source
 * Copyright 2012 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 */
package org.searchisko.ftest;

/**
 * Unit test for {@link ProjectRestService}
 *
 * @author Vlastimil Elias (velias at redhat dot com)
 */
public class ProjectRestServiceTest {

//    @Test
//    public void init() {
//        ProjectRestService tested = new ProjectRestService();
//        Assert.assertNull(tested.entityService);
//        tested.projectService = Mockito.mock(EntityService.class);
//        Assert.assertNull(tested.entityService);
//        tested.init();
//        Assert.assertEquals(tested.projectService, tested.entityService);
//    }
//
//    @Test
//    public void getAll() {
//        ProjectRestService tested = getTested();
//
//        // case - OK
//        ESDataOnlyResponse res = new ESDataOnlyResponse(null);
//        Mockito.when(tested.entityService.getAll(10, 12, tested.fieldsToRemove)).thenReturn(res);
//        Assert.assertEquals(res, tested.getAll(10, 12));
//
//        // case - error
//        Mockito.reset(tested.entityService);
//        Mockito.when(tested.entityService.getAll(10, 12, tested.fieldsToRemove)).thenThrow(
//                new RuntimeException("my exception"));
//        TestUtils.assertResponseStatus(tested.getAll(10, 12), Status.INTERNAL_SERVER_ERROR);
//    }
//
//    @Test
//    public void get() {
//        ProjectRestService tested = getTested();
//
//        // case - OK
//        Map<String, Object> m = new HashMap<String, Object>();
//        Mockito.when(tested.entityService.get("10")).thenReturn(m);
//        Assert.assertEquals(m, tested.get("10"));
//
//        // case - error
//        Mockito.reset(tested.entityService);
//        Mockito.when(tested.entityService.get("10")).thenThrow(new RuntimeException("my exception"));
//        TestUtils.assertResponseStatus(tested.get("10"), Status.INTERNAL_SERVER_ERROR);
//    }
//
//    @SuppressWarnings("unchecked")
//    @Test
//    public void create_id() {
//        ProjectRestService tested = getTested();
//
//        // case - invalid id parameter
//        {
//            Map<String, Object> m = new HashMap<String, Object>();
//            m.put(ProjectService.CODE, "myname");
//            TestUtils.assertResponseStatus(tested.create(null, m), Status.BAD_REQUEST);
//            TestUtils.assertResponseStatus(tested.create("", m), Status.BAD_REQUEST);
//        }
//
//        // case - invalid name field in input data
//        {
//            Map<String, Object> m = new HashMap<String, Object>();
//            TestUtils.assertResponseStatus(tested.create("myname", m), Status.BAD_REQUEST);
//            m.put(ProjectService.CODE, "");
//            TestUtils.assertResponseStatus(tested.create("myname", m), Status.BAD_REQUEST);
//        }
//
//        // case - name field in data is not same as id parameter
//        {
//            Map<String, Object> m = new HashMap<String, Object>();
//            m.put(ProjectService.CODE, "myanothername");
//            TestUtils.assertResponseStatus(tested.create("myname", m), Status.BAD_REQUEST);
//        }
//
//        // case - OK
//        {
//            Map<String, Object> m = new HashMap<String, Object>();
//            m.put(ProjectService.CODE, "myname");
//            Map<String, Object> ret = (Map<String, Object>) tested.create("myname", m);
//            Assert.assertEquals("myname", ret.get("id"));
//            Assert.assertEquals("myname", m.get(ProjectService.CODE));
//            Mockito.verify(tested.entityService).create("myname", m);
//            Mockito.verifyNoMoreInteractions(tested.entityService);
//        }
//
//        // case - error
//        {
//            Mockito.reset(tested.entityService);
//            Map<String, Object> m = new HashMap<String, Object>();
//            m.put(ProjectService.CODE, "myname");
//            Mockito.doThrow(new RuntimeException("my exception")).when(tested.entityService).create("myname", m);
//            TestUtils.assertResponseStatus(tested.create("myname", m), Status.INTERNAL_SERVER_ERROR);
//            Mockito.verify(tested.entityService).create("myname", m);
//            Mockito.verifyNoMoreInteractions(tested.entityService);
//        }
//    }
//
//    @SuppressWarnings("unchecked")
//    @Test
//    public void create_noid() {
//        ProjectRestService tested = getTested();
//
//        // case - invalid name field in input data
//        {
//            Map<String, Object> m = new HashMap<String, Object>();
//            TestUtils.assertResponseStatus(tested.create(m), Status.BAD_REQUEST);
//            m.put(ProjectService.CODE, "");
//            TestUtils.assertResponseStatus(tested.create(m), Status.BAD_REQUEST);
//        }
//
//        // case - OK
//        {
//            Map<String, Object> m = new HashMap<String, Object>();
//            m.put(ProjectService.CODE, "myname");
//            Mockito.when(tested.entityService.get("myname")).thenReturn(null);
//            Map<String, Object> ret = (Map<String, Object>) tested.create(m);
//            Assert.assertEquals("myname", ret.get("id"));
//            Assert.assertEquals("myname", m.get(ProjectService.CODE));
//            Mockito.verify(tested.entityService).create("myname", m);
//            Mockito.verifyNoMoreInteractions(tested.entityService);
//        }
//
//        // case - error
//        {
//            Mockito.reset(tested.entityService);
//            Map<String, Object> m = new HashMap<String, Object>();
//            m.put(ProjectService.CODE, "myname");
//            Mockito.doThrow(new RuntimeException("my exception")).when(tested.entityService).create("myname", m);
//            TestUtils.assertResponseStatus(tested.create(m), Status.INTERNAL_SERVER_ERROR);
//            Mockito.verify(tested.entityService).create("myname", m);
//            Mockito.verifyNoMoreInteractions(tested.entityService);
//        }
//    }
//
//    @Test
//    public void getAll_permissions() {
//        TestUtils.assertPermissionGuest(ProjectRestService.class, "getAll", Integer.class, Integer.class);
//    }
//
//    @Test
//    public void get_permissions() {
//        TestUtils.assertPermissionGuest(ProjectRestService.class, "get", String.class);
//    }
//
//    @Test
//    public void create_permissions() {
//        TestUtils.assertPermissionSuperProvider(ProjectRestService.class, "create", String.class, Map.class);
//        TestUtils.assertPermissionSuperProvider(ProjectRestService.class, "create", Map.class);
//    }
//
//    @Test
//    public void delete_permissions() {
//        TestUtils.assertPermissionSuperProvider(ProjectRestService.class, "delete", String.class);
//    }
//
//    protected ProjectRestService getTested() {
//        ProjectRestService tested = new ProjectRestService();
//        RestEntityServiceBaseTest.mockLogger(tested);
//        tested.setEntityService(Mockito.mock(EntityService.class));
//        tested.securityContext = Mockito.mock(SecurityContext.class);
//        return tested;
//    }

}
