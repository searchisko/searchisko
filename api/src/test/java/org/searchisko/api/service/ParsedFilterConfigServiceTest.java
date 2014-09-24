/*
 * JBoss, Home of Professional Open Source
 * Copyright 2014 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 */
package org.searchisko.api.service;

import java.util.HashMap;
import java.util.logging.Logger;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

/**
 * Unit test for {@link ParsedFilterConfigService}
 * 
 * @author Vlastimil Elias (velias at redhat dot com)
 */
public class ParsedFilterConfigServiceTest {

	@Test
	public void isCacheInitialized() {
		ParsedFilterConfigService tested = getTested();

		Assert.assertFalse(tested.isCacheInitialized());

		tested.semiParsedFilters = new HashMap<>();
		Assert.assertFalse(tested.isCacheInitialized());

		tested.searchFilters = new HashMap<>();

		Assert.assertTrue(tested.isCacheInitialized());
	}

	@Test(expected = RuntimeException.class)
	public void getFilterConfigsForRequest_notInitialized() {
		ParsedFilterConfigService tested = getTested();
		tested.getFilterConfigsForRequest();
	}

	@Test
	public void getFilterConfigsForRequest_initialized() {
		ParsedFilterConfigService tested = getTested();
		tested.semiParsedFilters = new HashMap<>();
		tested.getFilterConfigsForRequest();
	}

	@Test(expected = RuntimeException.class)
	public void getSearchFiltersForRequest_notInitialized() {
		ParsedFilterConfigService tested = getTested();
		tested.getSearchFiltersForRequest();
	}

	@Test
	public void getSearchFiltersForRequest_initialized() {
		ParsedFilterConfigService tested = getTested();
		tested.searchFilters = new HashMap<>();
		tested.getSearchFiltersForRequest();
	}

	@Test(expected = RuntimeException.class)
	public void getRangeFiltersIntervals_notInitialized() {
		ParsedFilterConfigService tested = getTested();
		tested.getRangeFiltersIntervals();
	}

	@Test
	public void getRangeFiltersIntervals_initialized() {
		ParsedFilterConfigService tested = getTested();
		tested.rangeFiltersIntervals = new HashMap<>();
		tested.getRangeFiltersIntervals();
	}

	private ParsedFilterConfigService getTested() {
		ParsedFilterConfigService tested = new ParsedFilterConfigService();
		tested.log = Logger.getLogger("test logger");
		tested.configService = Mockito.mock(ConfigService.class);
		return tested;
	}

}
