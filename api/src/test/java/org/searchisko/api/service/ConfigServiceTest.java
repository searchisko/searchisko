/*
 * JBoss, Home of Professional Open Source
 * Copyright 2012 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 */
package org.searchisko.api.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import javax.ws.rs.core.StreamingOutput;

import junit.framework.Assert;

import org.searchisko.api.rest.ESDataOnlyResponse;
import org.searchisko.api.testtools.ESRealClientTestBase;
import org.searchisko.persistence.service.EntityService;
import org.junit.Test;
import org.mockito.Mockito;

/**
 * Unit test for {@link ConfigService}
 *
 * @author Vlastimil Elias (velias at redhat dot com)
 */
public class ConfigServiceTest extends ESRealClientTestBase {

	private ConfigService getTested() {
		ConfigService ret = new ConfigService();
		ret.entityService = Mockito.mock(EntityService.class);
		ret.log = Logger.getLogger("testlogger");
		return ret;
	}

	@Test
	public void getAll_pager() {
		ConfigService tested = getTested();

		String[] ff = new String[] { "aa" };
		// case - value is returned
		StreamingOutput value = new ESDataOnlyResponse(null);
		Mockito.when(tested.entityService.getAll(10, 20, ff)).thenReturn(value);
		Assert.assertEquals(value, tested.getAll(10, 20, ff));

		// case - null is returned
		Mockito.reset(tested.entityService);
		Mockito.when(tested.entityService.getAll(10, 20, ff)).thenReturn(null);
		Assert.assertEquals(null, tested.getAll(10, 20, ff));
		Mockito.verify(tested.entityService).getAll(10, 20, ff);
		Mockito.verifyNoMoreInteractions(tested.entityService);

		// case - exception is passed
		Mockito.reset(tested.entityService);
		Mockito.when(tested.entityService.getAll(10, 20, ff)).thenThrow(new RuntimeException("testex"));
		try {
			tested.getAll(10, 20, ff);
			Assert.fail("RuntimeException expected");
		} catch (RuntimeException e) {
			// OK
		}
		Mockito.verify(tested.entityService).getAll(10, 20, ff);
		Mockito.verifyNoMoreInteractions(tested.entityService);
	}

	@Test
	public void getAll_raw() {
		ConfigService tested = getTested();

		// case - value is returned
		List<Map<String, Object>> value = new ArrayList<Map<String, Object>>();
		Mockito.when(tested.entityService.getAll()).thenReturn(value);
		Assert.assertEquals(value, tested.getAll());

		// case - null is returned
		Mockito.reset(tested.entityService);
		Mockito.when(tested.entityService.getAll()).thenReturn(null);
		Assert.assertEquals(null, tested.getAll());
		Mockito.verify(tested.entityService).getAll();
		Mockito.verifyNoMoreInteractions(tested.entityService);

		// case - exception is passed
		Mockito.reset(tested.entityService);
		Mockito.when(tested.entityService.getAll()).thenThrow(new RuntimeException("testex"));
		try {
			tested.getAll();
			Assert.fail("RuntimeException expected");
		} catch (RuntimeException e) {
			// OK
		}
		Mockito.verify(tested.entityService).getAll();
		Mockito.verifyNoMoreInteractions(tested.entityService);
	}

	@Test
	public void get() {
		ConfigService tested = getTested();

		// case - value is returned
		Map<String, Object> value = new HashMap<String, Object>();
		Mockito.when(tested.entityService.get("10")).thenReturn(value);
		Assert.assertEquals(value, tested.get("10"));

		// case - null is returned
		Mockito.reset(tested.entityService);
		Mockito.when(tested.entityService.get("10")).thenReturn(null);
		Assert.assertEquals(null, tested.get("10"));
		Mockito.verify(tested.entityService).get("10");
		Mockito.verifyNoMoreInteractions(tested.entityService);

		// case - exception is passed
		Mockito.reset(tested.entityService);
		Mockito.when(tested.entityService.get("10")).thenThrow(new RuntimeException("testex"));
		try {
			tested.get("10");
			Assert.fail("RuntimeException expected");
		} catch (RuntimeException e) {
			// OK
		}
		Mockito.verify(tested.entityService).get("10");
		Mockito.verifyNoMoreInteractions(tested.entityService);
	}

	@Test
	public void create_noid() {
		ConfigService tested = getTested();

		// case - insert to noexisting index
		{
			Map<String, Object> entity = new HashMap<String, Object>();
			entity.put("name", "v1");
			Mockito.when(tested.entityService.create(entity)).thenReturn("1");
			String id = tested.create(entity);
			Assert.assertEquals("1", id);
		}

	}

	@Test
	public void create_id() {
		ConfigService tested = getTested();

		// case - insert noexisting object to noexisting index
		{
			Map<String, Object> entity = new HashMap<String, Object>();
			entity.put("name", "v1");
			tested.create("1", entity);
			Mockito.verify(tested.entityService).create("1", entity);
		}

	}

	@Test
	public void update() {
		ConfigService tested = getTested();

		// case - insert noexisting object to noexisting index
		{
			Mockito.reset(tested.entityService);
			Map<String, Object> entity = new HashMap<String, Object>();
			entity.put("name", "v1");

			tested.update("1", entity);
			Mockito.verify(tested.entityService).update("1", entity);
		}

	}

	@Test
	public void delete() throws InterruptedException {
		ConfigService tested = getTested();
		Mockito.reset(tested.entityService);
		tested.delete("1");
		Mockito.verify(tested.entityService).delete("1");
	}

}
