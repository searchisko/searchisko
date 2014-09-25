/*
 * JBoss, Home of Professional Open Source
 * Copyright 2014 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 */
package org.searchisko.api.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.searchisko.api.testtools.TestUtils;

/**
 * Unit test for {@link ContentManipulationLockService}
 * 
 * @author Vlastimil Elias (velias at redhat dot com)
 */
public class ContentManipulationLockServiceTest {

	private static final String PROVIDER_1 = "provider1";
	private static final String PROVIDER_2 = "provider2";
	private static final String PROVIDER_3 = "provider3";

	private static final Map<String, Object> createCfgFileAll() {
		return createCfgFile(ContentManipulationLockService.API_ID_ALL);
	}

	private static final Map<String, Object> createCfgFileP1P2() {
		return createCfgFile(PROVIDER_1, PROVIDER_2);
	}

	private static final Map<String, Object> createCfgFile(String... providers) {
		Map<String, Object> cf = new HashMap<>();
		cf.put(ContentManipulationLockService.CFGFILE_NAME, TestUtils.createListOfStrings(providers));
		return cf;
	}

	@Test
	public void isLockedForProvider() {
		ContentManipulationLockService tested = getTested();

		// case - nothing locked
		Assert.assertFalse(tested.isLockedForProvider(PROVIDER_1));

		Mockito.when(tested.configService.get(ContentManipulationLockService.CFGFILE_NAME)).thenReturn(
				new HashMap<String, Object>());
		Assert.assertFalse(tested.isLockedForProvider(PROVIDER_1));

		// case - all locked
		Mockito.when(tested.configService.get(ContentManipulationLockService.CFGFILE_NAME)).thenReturn(createCfgFileAll());
		Assert.assertTrue(tested.isLockedForProvider(PROVIDER_1));
		Assert.assertTrue(tested.isLockedForProvider(PROVIDER_2));
		Assert.assertTrue(tested.isLockedForProvider(PROVIDER_3));

		// case - only some locked
		Mockito.when(tested.configService.get(ContentManipulationLockService.CFGFILE_NAME)).thenReturn(createCfgFileP1P2());
		Assert.assertTrue(tested.isLockedForProvider(PROVIDER_1));
		Assert.assertTrue(tested.isLockedForProvider(PROVIDER_2));
		Assert.assertFalse(tested.isLockedForProvider(PROVIDER_3));

	}

	@Test
	public void getLockInfo() {
		ContentManipulationLockService tested = getTested();

		Assert.assertNull(tested.getLockInfo());

		Mockito.when(tested.configService.get(ContentManipulationLockService.CFGFILE_NAME)).thenReturn(
				new HashMap<String, Object>());
		Assert.assertNull(tested.getLockInfo());

		{
			Mockito.when(tested.configService.get(ContentManipulationLockService.CFGFILE_NAME))
					.thenReturn(createCfgFileAll());
			List<String> li = tested.getLockInfo();
			Assert.assertNotNull(li);
			Assert.assertEquals(1, li.size());
			Assert.assertTrue(li.contains(ContentManipulationLockService.API_ID_ALL));
		}

		{
			Mockito.when(tested.configService.get(ContentManipulationLockService.CFGFILE_NAME)).thenReturn(
					createCfgFileP1P2());
			List<String> li = tested.getLockInfo();
			Assert.assertNotNull(li);
			Assert.assertEquals(2, li.size());
			Assert.assertTrue(li.contains(PROVIDER_1));
			Assert.assertTrue(li.contains(PROVIDER_2));
		}

	}

	@SuppressWarnings("unchecked")
	@Test
	public void createLock() {
		ContentManipulationLockService tested = getTested();

		// no any lock exists, so new is created
		Mockito.when(tested.configService.get(ContentManipulationLockService.CFGFILE_NAME)).thenReturn(null);
		Mockito.doAnswer(new StoreValidationgAnswer(PROVIDER_1)).when(tested.configService)
				.create(Mockito.eq(ContentManipulationLockService.CFGFILE_NAME), Mockito.anyMap());
		Assert.assertTrue(tested.createLock(PROVIDER_1));
		Mockito.verify(tested.configService).get(ContentManipulationLockService.CFGFILE_NAME);
		Mockito.verify(tested.configService).create(Mockito.eq(ContentManipulationLockService.CFGFILE_NAME),
				Mockito.anyMap());
		Mockito.verifyNoMoreInteractions(tested.configService);

		// another lock exists so new one is added
		Mockito.reset(tested.configService);
		Mockito.when(tested.configService.get(ContentManipulationLockService.CFGFILE_NAME)).thenReturn(createCfgFileP1P2());
		Mockito.doAnswer(new StoreValidationgAnswer(PROVIDER_1, PROVIDER_2, PROVIDER_3)).when(tested.configService)
				.create(Mockito.eq(ContentManipulationLockService.CFGFILE_NAME), Mockito.anyMap());
		Assert.assertTrue(tested.createLock(PROVIDER_3));
		Mockito.verify(tested.configService).get(ContentManipulationLockService.CFGFILE_NAME);
		Mockito.verify(tested.configService).create(Mockito.eq(ContentManipulationLockService.CFGFILE_NAME),
				Mockito.anyMap());
		Mockito.verifyNoMoreInteractions(tested.configService);

		// given lock exists so no new one is added
		Mockito.reset(tested.configService);
		Mockito.when(tested.configService.get(ContentManipulationLockService.CFGFILE_NAME)).thenReturn(createCfgFileP1P2());
		Assert.assertFalse(tested.createLock(PROVIDER_2));
		Mockito.verify(tested.configService).get(ContentManipulationLockService.CFGFILE_NAME);
		Mockito.verifyNoMoreInteractions(tested.configService);

		// all lock exists so no new one is added
		Mockito.reset(tested.configService);
		Mockito.when(tested.configService.get(ContentManipulationLockService.CFGFILE_NAME)).thenReturn(createCfgFileAll());
		Assert.assertFalse(tested.createLock(PROVIDER_2));
		Mockito.verify(tested.configService).get(ContentManipulationLockService.CFGFILE_NAME);
		Mockito.verifyNoMoreInteractions(tested.configService);

	}

	@SuppressWarnings("unchecked")
	@Test
	public void createLockAll() {
		ContentManipulationLockService tested = getTested();

		Mockito.reset(tested.configService);
		Mockito.doAnswer(new StoreValidationgAnswer(ContentManipulationLockService.API_ID_ALL)).when(tested.configService)
				.create(Mockito.eq(ContentManipulationLockService.CFGFILE_NAME), Mockito.anyMap());
		tested.createLockAll();
		Mockito.verify(tested.configService).create(Mockito.eq(ContentManipulationLockService.CFGFILE_NAME),
				Mockito.anyMap());
		Mockito.verifyNoMoreInteractions(tested.configService);

	}

	@Test
	public void removeLockAll() {
		ContentManipulationLockService tested = getTested();

		tested.removeLockAll();
		Mockito.verify(tested.configService).delete(Mockito.eq(ContentManipulationLockService.CFGFILE_NAME));
		Mockito.verifyNoMoreInteractions(tested.configService);
	}

	@SuppressWarnings("unchecked")
	@Test
	public void removeLock() {
		ContentManipulationLockService tested = getTested();

		// case - no any lock exists
		Mockito.when(tested.configService.get(ContentManipulationLockService.CFGFILE_NAME)).thenReturn(null);
		Assert.assertTrue(tested.removeLock(PROVIDER_1));
		Mockito.verify(tested.configService).get(ContentManipulationLockService.CFGFILE_NAME);
		Mockito.verifyNoMoreInteractions(tested.configService);

		// case - all lock exists so nothing removed
		Mockito.reset(tested.configService);
		Mockito.when(tested.configService.get(ContentManipulationLockService.CFGFILE_NAME)).thenReturn(createCfgFileAll());
		Assert.assertFalse(tested.removeLock(PROVIDER_1));
		Mockito.verify(tested.configService).get(ContentManipulationLockService.CFGFILE_NAME);
		Mockito.verifyNoMoreInteractions(tested.configService);

		// case - p1 removed from two
		Mockito.reset(tested.configService);
		Mockito.when(tested.configService.get(ContentManipulationLockService.CFGFILE_NAME)).thenReturn(createCfgFileP1P2());
		Mockito.doAnswer(new StoreValidationgAnswer(PROVIDER_2)).when(tested.configService)
				.create(Mockito.eq(ContentManipulationLockService.CFGFILE_NAME), Mockito.anyMap());
		Assert.assertTrue(tested.removeLock(PROVIDER_1));
		Mockito.verify(tested.configService).get(ContentManipulationLockService.CFGFILE_NAME);
		Mockito.verify(tested.configService).create(Mockito.eq(ContentManipulationLockService.CFGFILE_NAME),
				Mockito.anyMap());
		Mockito.verifyNoMoreInteractions(tested.configService);

		// case - last one removed
		Mockito.reset(tested.configService);
		Mockito.when(tested.configService.get(ContentManipulationLockService.CFGFILE_NAME)).thenReturn(
				createCfgFile(PROVIDER_1));
		Assert.assertTrue(tested.removeLock(PROVIDER_1));
		Mockito.verify(tested.configService).get(ContentManipulationLockService.CFGFILE_NAME);
		Mockito.verify(tested.configService).delete(Mockito.eq(ContentManipulationLockService.CFGFILE_NAME));
		Mockito.verifyNoMoreInteractions(tested.configService);

	}

	private static final class StoreValidationgAnswer implements Answer<Object> {

		private List<String> expectedLocks;

		public StoreValidationgAnswer(String... expectedLocks) {
			this.expectedLocks = TestUtils.createListOfStrings(expectedLocks);
		}

		@SuppressWarnings("unchecked")
		@Override
		public Object answer(InvocationOnMock invocation) throws Throwable {
			Map<String, Object> map = (Map<String, Object>) invocation.getArguments()[1];
			List<String> providers = (List<String>) map.get(ContentManipulationLockService.CFGFILE_NAME);

			Assert.assertEquals(expectedLocks, providers);

			return null;
		}

	}

	private ContentManipulationLockService getTested() {
		ContentManipulationLockService tested = new ContentManipulationLockService();
		tested.configService = Mockito.mock(ConfigService.class);
		return tested;
	}

}
