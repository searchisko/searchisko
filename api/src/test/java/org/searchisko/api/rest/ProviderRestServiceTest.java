/*
 * JBoss, Home of Professional Open Source
 * Copyright 2012 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 */
package org.searchisko.api.rest;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import javax.ejb.ObjectNotFoundException;
import javax.ws.rs.core.Response.Status;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;
import org.searchisko.api.rest.exception.NotAuthorizedException;
import org.searchisko.api.rest.exception.RequiredFieldException;
import org.searchisko.api.security.Role;
import org.searchisko.api.service.AuthenticationUtilService;
import org.searchisko.api.service.ContentManipulationLockService;
import org.searchisko.api.service.ProviderService;
import org.searchisko.api.service.SecurityService;
import org.searchisko.api.testtools.TestUtils;

/**
 * Unit test for {@link ProviderRestService}.
 * 
 * @author Vlastimil Elias (velias at redhat dot com)
 */
public class ProviderRestServiceTest {

	@Test
	public void init() {
		ProviderRestService tested = new ProviderRestService();
		tested.log = Logger.getLogger("testlogger");
		Assert.assertNull(tested.entityService);
		tested.providerService = Mockito.mock(ProviderService.class);
		// EntityService es = Mockito.mock(ProviderService.class);
		Assert.assertNull(tested.entityService);
		tested.init();
		// Assert.assertEquals(es, tested.entityService);
	}

	@Test
	public void getAll() {
		ProviderRestService tested = getTested();

		// case - OK
		ESDataOnlyResponse res = new ESDataOnlyResponse(null);
		Mockito.when(tested.entityService.getAll(10, 12, ProviderRestService.FIELDS_TO_REMOVE)).thenReturn(res);
		Assert.assertEquals(res, tested.getAll(10, 12));
		Mockito.verify(tested.entityService).getAll(10, 12, ProviderRestService.FIELDS_TO_REMOVE);
		Mockito.verifyNoMoreInteractions(tested.entityService);

		// case - OK, null returned
		Mockito.reset(tested.entityService);
		Mockito.when(tested.entityService.getAll(10, 12, ProviderRestService.FIELDS_TO_REMOVE)).thenReturn(null);
		Assert.assertEquals(null, tested.getAll(10, 12));
		Mockito.verify(tested.entityService).getAll(10, 12, ProviderRestService.FIELDS_TO_REMOVE);
		Mockito.verifyNoMoreInteractions(tested.entityService);
	}

	@Test(expected = RuntimeException.class)
	public void getAll_exceptionFromService() {
		ProviderRestService tested = getTested();

		// case - error
		Mockito.reset(tested.entityService);
		Mockito.when(tested.entityService.getAll(10, 12, ProviderRestService.FIELDS_TO_REMOVE)).thenThrow(
				new RuntimeException("my exception"));
		TestUtils.assertResponseStatus(tested.getAll(10, 12), Status.INTERNAL_SERVER_ERROR);
		Mockito.verify(tested.entityService).getAll(10, 12, ProviderRestService.FIELDS_TO_REMOVE);
		Mockito.verifyNoMoreInteractions(tested.entityService);
	}

	@Test(expected = RequiredFieldException.class)
	public void get_inputParamValidation() {
		ProviderRestService tested = getTested();

		// input parameter is bad
		{
			TestUtils.assertResponseStatus(tested.get(""), Status.BAD_REQUEST);
		}
	}

	@Test(expected = RequiredFieldException.class)
	public void changePassword_inputPatamValidation_1() {
		ProviderRestService tested = getTested();
		TestUtils.assertResponseStatus(tested.changePassword(null, "pwd"), Status.BAD_REQUEST);
	}

	@Test(expected = RequiredFieldException.class)
	public void changePassword_inputPatamValidation_2() {
		ProviderRestService tested = getTested();
		TestUtils.assertResponseStatus(tested.changePassword("", "pwd"), Status.BAD_REQUEST);
	}

	@Test(expected = RequiredFieldException.class)
	public void changePassword_inputPatamValidation_3() {
		ProviderRestService tested = getTested();
		TestUtils.assertResponseStatus(tested.changePassword("aa", null), Status.BAD_REQUEST);
	}

	@Test(expected = RequiredFieldException.class)
	public void changePassword_inputPatamValidation_4() {
		ProviderRestService tested = getTested();
		TestUtils.assertResponseStatus(tested.changePassword("aa", ""), Status.BAD_REQUEST);
	}

	@Test(expected = RequiredFieldException.class)
	public void changePassword_inputPatamValidation_5() {
		ProviderRestService tested = getTested();
		TestUtils.assertResponseStatus(tested.changePassword("aa", "   "), Status.BAD_REQUEST);
	}

	@Test(expected = RequiredFieldException.class)
	public void changePassword_inputPatamValidation_6() {
		ProviderRestService tested = getTested();
		TestUtils.assertResponseStatus(tested.changePassword("aa", "\n   \n"), Status.BAD_REQUEST);
	}

	@Test(expected = RequiredFieldException.class)
	public void changePassword_inputPatamValidation_7() {
		ProviderRestService tested = getTested();
		TestUtils.assertResponseStatus(tested.changePassword(null, null), Status.BAD_REQUEST);
	}

	@Test(expected = RequiredFieldException.class)
	public void changePassword_inputPatamValidation_8() {
		ProviderRestService tested = getTested();
		TestUtils.assertResponseStatus(tested.changePassword("", ""), Status.BAD_REQUEST);
	}

	@Test(expected = RequiredFieldException.class)
	public void create_id_inputParamValidation() {
		ProviderRestService tested = getTested();
		// case - invalid id parameter
		Map<String, Object> m = new HashMap<String, Object>();
		m.put(ProviderService.NAME, "myname");
		TestUtils.assertResponseStatus(tested.create(null, m), Status.BAD_REQUEST);
		TestUtils.assertResponseStatus(tested.create("", m), Status.BAD_REQUEST);
	}

	@SuppressWarnings("unchecked")
	@Test
	public void create_id() {
		ProviderRestService tested = getTested();

		// case - invalid name field in input data
		{
			Map<String, Object> m = new HashMap<String, Object>();
			TestUtils.assertResponseStatus(tested.create("myname", m), Status.BAD_REQUEST);
			m.put(ProviderService.NAME, "");
			TestUtils.assertResponseStatus(tested.create("myname", m), Status.BAD_REQUEST);
		}

		// case - name field in data is not same as id parameter
		{
			Map<String, Object> m = new HashMap<String, Object>();
			m.put(ProviderService.NAME, "myanothername");
			TestUtils.assertResponseStatus(tested.create("myname", m), Status.BAD_REQUEST);
		}

		// case - OK, no previously existing entity so new pwd hash used
		{
			Map<String, Object> m = new HashMap<String, Object>();
			m.put(ProviderService.NAME, "myname");
			m.put(ProviderService.PASSWORD_HASH, "pwhs");
			Mockito.when(tested.entityService.get("myname")).thenReturn(null);
			Map<String, Object> ret = (Map<String, Object>) tested.create("myname", m);
			Assert.assertEquals("myname", ret.get("id"));
			Assert.assertEquals("pwhs", m.get(ProviderService.PASSWORD_HASH));
			Assert.assertEquals("myname", m.get(ProviderService.NAME));
			Mockito.verify(tested.providerService).create("myname", m);
			Mockito.verify(tested.providerService).get("myname");
			Mockito.verifyNoMoreInteractions(tested.entityService);
		}

		// case - OK, previously existing entity without pwd hash, so new pwd hash used
		{
			Mockito.reset(tested.providerService);
			Map<String, Object> m = new HashMap<String, Object>();
			m.put(ProviderService.NAME, "myname");
			m.put(ProviderService.PASSWORD_HASH, "pwhs");
			Mockito.when(tested.entityService.get("12")).thenReturn(new HashMap<String, Object>());
			Map<String, Object> ret = (Map<String, Object>) tested.create("myname", m);
			Assert.assertEquals("myname", ret.get("id"));
			Assert.assertEquals("pwhs", m.get(ProviderService.PASSWORD_HASH));
			Assert.assertEquals("myname", m.get(ProviderService.NAME));
			Mockito.verify(tested.providerService).create("myname", m);
			Mockito.verify(tested.providerService).get("myname");
			Mockito.verifyNoMoreInteractions(tested.entityService);
		}

		// case - OK, previously existing entity with pwd hash, so old pwd hash preserved
		{
			Mockito.reset(tested.providerService);
			Map<String, Object> m = new HashMap<String, Object>();
			m.put(ProviderService.NAME, "myname");
			m.put(ProviderService.PASSWORD_HASH, "pwhs");
			Map<String, Object> entityOld = new HashMap<String, Object>();
			entityOld.put(ProviderService.PASSWORD_HASH, "pwhsold");
			entityOld.put(ProviderService.NAME, "myname");
			Mockito.when(tested.providerService.get("myname")).thenReturn(entityOld);
			Map<String, Object> ret = (Map<String, Object>) tested.create("myname", m);
			Assert.assertEquals("myname", ret.get("id"));
			Assert.assertEquals("pwhsold", m.get(ProviderService.PASSWORD_HASH));
			Assert.assertEquals("myname", m.get(ProviderService.NAME));
			Mockito.verify(tested.providerService).create("myname", m);
			Mockito.verify(tested.providerService).get("myname");
			Mockito.verifyNoMoreInteractions(tested.entityService);
		}

		// case - OK, previously existing entity with pwd hash, so old pwd hash preserved. new entity without pwd hash
		{
			Mockito.reset(tested.providerService);
			Map<String, Object> m = new HashMap<String, Object>();
			m.put(ProviderService.NAME, "myname");
			Map<String, Object> entityOld = new HashMap<String, Object>();
			entityOld.put(ProviderService.PASSWORD_HASH, "pwhsold");
			entityOld.put(ProviderService.NAME, "myname");
			Mockito.when(tested.providerService.get("myname")).thenReturn(entityOld);
			Map<String, Object> ret = (Map<String, Object>) tested.create("myname", m);
			Assert.assertEquals("myname", ret.get("id"));
			Assert.assertEquals("pwhsold", m.get(ProviderService.PASSWORD_HASH));
			Assert.assertEquals("myname", m.get(ProviderService.NAME));
			Mockito.verify(tested.providerService).create("myname", m);
			Mockito.verify(tested.providerService).get("myname");
			Mockito.verifyNoMoreInteractions(tested.entityService);
		}

	}

	@Test(expected = RuntimeException.class)
	public void create_id_errorFromService() {
		ProviderRestService tested = getTested();
		Mockito.reset(tested.providerService);
		Map<String, Object> m = new HashMap<String, Object>();
		m.put(ProviderService.NAME, "myname");
		Mockito.doThrow(new RuntimeException("my exception")).when(tested.providerService).create("myname", m);
		tested.create("myname", m);
	}

	@SuppressWarnings("unchecked")
	@Test
	public void create_noid() {
		ProviderRestService tested = getTested();

		// case - invalid name field in input data
		{
			Map<String, Object> m = new HashMap<String, Object>();
			TestUtils.assertResponseStatus(tested.create(m), Status.BAD_REQUEST);
			m.put(ProviderService.NAME, "");
			TestUtils.assertResponseStatus(tested.create(m), Status.BAD_REQUEST);
		}

		// case - OK, no previously existing entity so new pwd hash used
		{
			Map<String, Object> m = new HashMap<String, Object>();
			m.put(ProviderService.NAME, "myname");
			m.put(ProviderService.PASSWORD_HASH, "pwhs");
			Mockito.when(tested.entityService.get("myname")).thenReturn(null);
			Map<String, Object> ret = (Map<String, Object>) tested.create(m);
			Assert.assertEquals("myname", ret.get("id"));
			Assert.assertEquals("pwhs", m.get(ProviderService.PASSWORD_HASH));
			Assert.assertEquals("myname", m.get(ProviderService.NAME));
			Mockito.verify(tested.providerService).create("myname", m);
			Mockito.verify(tested.providerService).get("myname");
			Mockito.verifyNoMoreInteractions(tested.entityService);
		}

		// case - OK, previously existing entity without pwd hash, so new pwd hash used
		{
			Mockito.reset(tested.providerService);
			Map<String, Object> m = new HashMap<String, Object>();
			m.put(ProviderService.NAME, "myname");
			m.put(ProviderService.PASSWORD_HASH, "pwhs");
			Mockito.when(tested.entityService.get("12")).thenReturn(new HashMap<String, Object>());
			Map<String, Object> ret = (Map<String, Object>) tested.create(m);
			Assert.assertEquals("myname", ret.get("id"));
			Assert.assertEquals("pwhs", m.get(ProviderService.PASSWORD_HASH));
			Assert.assertEquals("myname", m.get(ProviderService.NAME));
			Mockito.verify(tested.providerService).create("myname", m);
			Mockito.verify(tested.providerService).get("myname");
			Mockito.verifyNoMoreInteractions(tested.entityService);
		}

		// case - OK, previously existing entity with pwd hash, so old pwd hash preserved
		{
			Mockito.reset(tested.providerService);
			Map<String, Object> m = new HashMap<String, Object>();
			m.put(ProviderService.NAME, "myname");
			m.put(ProviderService.PASSWORD_HASH, "pwhs");
			Map<String, Object> entityOld = new HashMap<String, Object>();
			entityOld.put(ProviderService.PASSWORD_HASH, "pwhsold");
			entityOld.put(ProviderService.NAME, "myname");
			Mockito.when(tested.providerService.get("myname")).thenReturn(entityOld);
			Map<String, Object> ret = (Map<String, Object>) tested.create(m);
			Assert.assertEquals("myname", ret.get("id"));
			Assert.assertEquals("pwhsold", m.get(ProviderService.PASSWORD_HASH));
			Assert.assertEquals("myname", m.get(ProviderService.NAME));
			Mockito.verify(tested.providerService).create("myname", m);
			Mockito.verify(tested.providerService).get("myname");
			Mockito.verifyNoMoreInteractions(tested.entityService);
		}

		// case - OK, previously existing entity with pwd hash, so old pwd hash preserved. new entity without pwd hash
		{
			Mockito.reset(tested.providerService);
			Map<String, Object> m = new HashMap<String, Object>();
			m.put(ProviderService.NAME, "myname");
			Map<String, Object> entityOld = new HashMap<String, Object>();
			entityOld.put(ProviderService.PASSWORD_HASH, "pwhsold");
			entityOld.put(ProviderService.NAME, "myname");
			Mockito.when(tested.providerService.get("myname")).thenReturn(entityOld);
			Map<String, Object> ret = (Map<String, Object>) tested.create(m);
			Assert.assertEquals("myname", ret.get("id"));
			Assert.assertEquals("pwhsold", m.get(ProviderService.PASSWORD_HASH));
			Assert.assertEquals("myname", m.get(ProviderService.NAME));
			Mockito.verify(tested.providerService).create("myname", m);
			Mockito.verify(tested.providerService).get("myname");
			Mockito.verifyNoMoreInteractions(tested.providerService);
		}

	}

	@Test(expected = RuntimeException.class)
	public void create_noid_exceptionFromService() {
		ProviderRestService tested = getTested();
		Mockito.reset(tested.providerService);
		Map<String, Object> m = new HashMap<String, Object>();
		m.put(ProviderService.NAME, "myname");
		Mockito.doThrow(new RuntimeException("my exception")).when(tested.providerService).create("myname", m);
		tested.create(m);
	}

	@Test(expected = RequiredFieldException.class)
	public void contentManipulationLockInfo_inputParamValidation_1() throws ObjectNotFoundException {
		ProviderRestService tested = getTested();
		tested.contentManipulationLockInfo(null);
	}

	@Test(expected = RequiredFieldException.class)
	public void contentManipulationLockInfo_inputParamValidation_2() throws ObjectNotFoundException {
		ProviderRestService tested = getTested();
		tested.contentManipulationLockInfo("");
	}

	@Test(expected = NotAuthorizedException.class)
	public void contentManipulationLockInfo_all_noPermission() throws ObjectNotFoundException {
		ProviderRestService tested = getTested();
		Mockito.when(tested.authenticationUtilService.isUserInRole(Role.ADMIN)).thenReturn(false);
		tested.contentManipulationLockInfo(ContentManipulationLockService.API_ID_ALL);
	}

	@Test
	public void contentManipulationLockInfo_all_ok() throws ObjectNotFoundException {
		ProviderRestService tested = getTested();
		Mockito.when(tested.authenticationUtilService.isUserInRole(Role.ADMIN)).thenReturn(true);

		// case - empty info list
		List<String> infolist = new ArrayList<>();
		Mockito.when(tested.contentManipulationLockService.getLockInfo()).thenReturn(infolist);
		Map<String, Object> ret = tested.contentManipulationLockInfo(ContentManipulationLockService.API_ID_ALL);
		Assert.assertEquals(null, ret.get("content_manipulation_lock"));

		// case - something in info list
		infolist.add("provider1");
		Mockito.when(tested.contentManipulationLockService.getLockInfo()).thenReturn(infolist);
		ret = tested.contentManipulationLockInfo(ContentManipulationLockService.API_ID_ALL);
		Assert.assertEquals(infolist, ret.get("content_manipulation_lock"));
	}

	@Test(expected = ObjectNotFoundException.class)
	public void contentManipulationLockInfo_provider_unknown() throws ObjectNotFoundException {
		ProviderRestService tested = getTested();
		Mockito.when(tested.entityService.get("provider1")).thenReturn(null);
		tested.contentManipulationLockInfo("provider1");
	}

	@Test(expected = NotAuthorizedException.class)
	public void contentManipulationLockInfo_provider_noPermission() throws ObjectNotFoundException {
		ProviderRestService tested = getTested();
		mockEntityGet(tested, "provider1");
		Mockito.doThrow(new NotAuthorizedException("")).when(tested.authenticationUtilService)
				.checkProviderManagementPermission("provider1");

		tested.contentManipulationLockInfo("provider1");
	}

	@Test
	public void contentManipulationLockInfo_provider_nolocklist() throws ObjectNotFoundException {
		ProviderRestService tested = getTested();
		mockEntityGet(tested, "provider1");
		Mockito.when(tested.contentManipulationLockService.getLockInfo()).thenReturn(null);

		Map<String, Object> ret = tested.contentManipulationLockInfo("provider1");
		Assert.assertEquals(null, ret.get("content_manipulation_lock"));
	}

	@Test
	public void contentManipulationLockInfo_provider_nolockinlist() throws ObjectNotFoundException {
		ProviderRestService tested = getTested();
		mockEntityGet(tested, "provider1");
		Mockito.when(tested.contentManipulationLockService.getLockInfo()).thenReturn(
				TestUtils.createListOfStrings("provider2", "provider3"));
		Map<String, Object> ret = tested.contentManipulationLockInfo("provider1");
		Assert.assertEquals(null, ret.get("content_manipulation_lock"));
	}

	@Test
	public void contentManipulationLockInfo_provider_lockinlist() throws ObjectNotFoundException {
		ProviderRestService tested = getTested();
		mockEntityGet(tested, "provider1");
		Mockito.when(tested.contentManipulationLockService.getLockInfo()).thenReturn(
				TestUtils.createListOfStrings("provider1", "provider3"));
		Map<String, Object> ret = tested.contentManipulationLockInfo("provider1");
		Assert.assertEquals(TestUtils.createListOfStrings("provider1"), ret.get("content_manipulation_lock"));
	}

	@Test
	public void contentManipulationLockInfo_provider_alllockinlist() throws ObjectNotFoundException {
		ProviderRestService tested = getTested();
		mockEntityGet(tested, "provider1");
		Mockito.when(tested.contentManipulationLockService.getLockInfo()).thenReturn(
				TestUtils.createListOfStrings(ContentManipulationLockService.API_ID_ALL));
		Map<String, Object> ret = tested.contentManipulationLockInfo("provider1");
		Assert.assertEquals(TestUtils.createListOfStrings(ContentManipulationLockService.API_ID_ALL),
				ret.get("content_manipulation_lock"));
	}

	@Test(expected = RequiredFieldException.class)
	public void contentManipulationLockCreate_inputParamValidation_1() throws ObjectNotFoundException {
		ProviderRestService tested = getTested();
		tested.contentManipulationLockCreate(null);
	}

	@Test(expected = RequiredFieldException.class)
	public void contentManipulationLockCreate_inputParamValidation_2() throws ObjectNotFoundException {
		ProviderRestService tested = getTested();
		tested.contentManipulationLockCreate("");
	}

	@Test(expected = NotAuthorizedException.class)
	public void contentManipulationLockCreate_all_noPermission() throws ObjectNotFoundException {
		ProviderRestService tested = getTested();
		Mockito.when(tested.authenticationUtilService.isUserInRole(Role.ADMIN)).thenReturn(false);
		tested.contentManipulationLockCreate(ContentManipulationLockService.API_ID_ALL);
		Mockito.verifyZeroInteractions(tested.contentManipulationLockService);
	}

	@Test
	public void contentManipulationLockCreate_all_ok() throws ObjectNotFoundException {
		ProviderRestService tested = getTested();
		Mockito.when(tested.authenticationUtilService.isUserInRole(Role.ADMIN)).thenReturn(true);

		Object ret = tested.contentManipulationLockCreate(ContentManipulationLockService.API_ID_ALL);
		TestUtils.assertResponseStatus(ret, Status.OK);
		Mockito.verify(tested.contentManipulationLockService).createLockAll();
		Mockito.verifyZeroInteractions(tested.contentManipulationLockService);
	}

	@Test(expected = ObjectNotFoundException.class)
	public void contentManipulationLockCreate_provider_unknown() throws ObjectNotFoundException {
		ProviderRestService tested = getTested();
		Mockito.when(tested.entityService.get("provider1")).thenReturn(null);
		tested.contentManipulationLockCreate("provider1");
	}

	@Test(expected = NotAuthorizedException.class)
	public void contentManipulationLockCreate_provider_noPermission() throws ObjectNotFoundException {
		ProviderRestService tested = getTested();
		mockEntityGet(tested, "provider1");
		Mockito.doThrow(new NotAuthorizedException("")).when(tested.authenticationUtilService)
				.checkProviderManagementPermission("provider1");
		tested.contentManipulationLockCreate("provider1");
	}

	@Test
	public void contentManipulationLockCreate_provider_ok() throws ObjectNotFoundException {
		ProviderRestService tested = getTested();
		mockEntityGet(tested, "provider1");
		Object ret = tested.contentManipulationLockCreate("provider1");
		TestUtils.assertResponseStatus(ret, Status.OK);
		Mockito.verify(tested.contentManipulationLockService).createLock("provider1");
		Mockito.verifyZeroInteractions(tested.contentManipulationLockService);
	}

	@Test(expected = RequiredFieldException.class)
	public void contentManipulationLockDelete_inputParamValidation_1() throws ObjectNotFoundException {
		ProviderRestService tested = getTested();
		tested.contentManipulationLockDelete(null);
	}

	@Test(expected = RequiredFieldException.class)
	public void contentManipulationLockDelete_inputParamValidation_2() throws ObjectNotFoundException {
		ProviderRestService tested = getTested();
		tested.contentManipulationLockDelete("");
	}

	@Test(expected = NotAuthorizedException.class)
	public void contentManipulationLockDelete_all_noPermission() throws ObjectNotFoundException {
		ProviderRestService tested = getTested();
		Mockito.when(tested.authenticationUtilService.isUserInRole(Role.ADMIN)).thenReturn(false);
		tested.contentManipulationLockDelete(ContentManipulationLockService.API_ID_ALL);
		Mockito.verifyZeroInteractions(tested.contentManipulationLockService);
	}

	@Test
	public void contentManipulationLockDelete_all_ok() throws ObjectNotFoundException {
		ProviderRestService tested = getTested();
		Mockito.when(tested.authenticationUtilService.isUserInRole(Role.ADMIN)).thenReturn(true);

		Object ret = tested.contentManipulationLockDelete(ContentManipulationLockService.API_ID_ALL);
		TestUtils.assertResponseStatus(ret, Status.OK);
		Mockito.verify(tested.contentManipulationLockService).removeLockAll();
		Mockito.verifyZeroInteractions(tested.contentManipulationLockService);
	}

	@Test(expected = ObjectNotFoundException.class)
	public void contentManipulationLockDelete_provider_unknown() throws ObjectNotFoundException {
		ProviderRestService tested = getTested();
		Mockito.when(tested.entityService.get("provider1")).thenReturn(null);
		tested.contentManipulationLockDelete("provider1");
	}

	@Test(expected = NotAuthorizedException.class)
	public void contentManipulationLockDelete_provider_noPermission() throws ObjectNotFoundException {
		ProviderRestService tested = getTested();
		mockEntityGet(tested, "provider1");
		Mockito.doThrow(new NotAuthorizedException("")).when(tested.authenticationUtilService)
				.checkProviderManagementPermission("provider1");
		tested.contentManipulationLockDelete("provider1");
	}

	@Test
	public void contentManipulationLockDelete_provider_noPermissionToRemoveLockAll() throws ObjectNotFoundException {
		ProviderRestService tested = getTested();
		try {
			mockEntityGet(tested, "provider1");
			Mockito.when(tested.contentManipulationLockService.removeLock("provider1")).thenReturn(false);
			tested.contentManipulationLockDelete("provider1");
			Assert.fail("NotAuthorizedException expected");
		} catch (NotAuthorizedException e) {
			Mockito.verify(tested.contentManipulationLockService).removeLock("provider1");
			Mockito.verifyZeroInteractions(tested.contentManipulationLockService);
		}
	}

	@Test
	public void contentManipulationLockDelete_provider_ok() throws ObjectNotFoundException {
		ProviderRestService tested = getTested();
		mockEntityGet(tested, "provider1");
		Mockito.when(tested.contentManipulationLockService.removeLock("provider1")).thenReturn(true);
		Object ret = tested.contentManipulationLockDelete("provider1");
		TestUtils.assertResponseStatus(ret, Status.OK);
		Mockito.verify(tested.contentManipulationLockService).removeLock("provider1");
		Mockito.verifyZeroInteractions(tested.contentManipulationLockService);
	}

	private void mockEntityGet(ProviderRestService tested, String name) {
		Map<String, Object> entity = new HashMap<>();
		entity.put(ProviderService.NAME, name);
		Mockito.when(tested.entityService.get(name)).thenReturn(entity);
	}

	protected ProviderRestService getTested() {
		ProviderRestService tested = new ProviderRestService();
		RestEntityServiceBaseTest.mockLogger(tested);
		tested.providerService = Mockito.mock(ProviderService.class);
		tested.setEntityService(tested.providerService);
		tested.securityService = Mockito.mock(SecurityService.class);
		tested.contentManipulationLockService = Mockito.mock(ContentManipulationLockService.class);
		tested.authenticationUtilService = Mockito.mock(AuthenticationUtilService.class);
		return tested;
	}

}
