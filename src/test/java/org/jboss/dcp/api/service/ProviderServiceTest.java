/*
 * JBoss, Home of Professional Open Source
 * Copyright 2012 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 */
package org.jboss.dcp.api.service;

import java.util.HashMap;
import java.util.Map;

import org.elasticsearch.common.settings.SettingsException;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

/**
 * Unit test for {@link ProviderService}
 * 
 * @author Vlastimil Elias (velias at redhat dot com)
 */
public class ProviderServiceTest {

	@Test
	public void getEntityService() {
		ProviderService tested = new ProviderService();
		tested.entityService = Mockito.mock(ElasticsearchEntityService.class);
		Assert.assertEquals(tested.entityService, tested.getEntityService());
	}

	@Test
	public void generateDcpId() {
		ProviderService tested = new ProviderService();
		Assert.assertEquals("mytype-myid", tested.generateDcpId("mytype", "myid"));
		Assert.assertEquals("mytype-null", tested.generateDcpId("mytype", null));
		Assert.assertEquals("null-myid", tested.generateDcpId(null, "myid"));
		Assert.assertEquals("null-null", tested.generateDcpId(null, null));
	}

	@Test
	public void getContentType() {
		Map<String, Object> providerDef = new HashMap<String, Object>();

		// case - type field not defined
		Assert.assertNull(ProviderService.getContentType(providerDef, "mytype"));

		Map<String, Object> types = new HashMap<String, Object>();
		providerDef.put(ProviderService.TYPE, types);
		types.put("typeOk", new HashMap<String, Object>());
		types.put("typeBadStructure", "baad");

		// case - type not defined in type field
		Assert.assertNull(ProviderService.getContentType(providerDef, "mytype"));

		// case - type found
		Assert.assertEquals(types.get("typeOk"), ProviderService.getContentType(providerDef, "typeOk"));

		// case - bad configuration of concrete type
		try {
			ProviderService.getContentType(providerDef, "typeBadStructure");
			Assert.fail("SettingsException expected");
		} catch (SettingsException e) {
			// OK
		}

		// case - bad configuration of type field
		providerDef.put(ProviderService.TYPE, "baaad");
		try {
			ProviderService.getContentType(providerDef, "mytype");
			Assert.fail("SettingsException expected");
		} catch (SettingsException e) {
			// OK
		}

	}

	@Test
	public void getPreprocessors() {
		// TODO UNITTEST
	}

	@Test
	public void getIndexName() {
		// TODO UNITTEST
	}

	@Test
	public void getIndexType() {
		// TODO UNITTEST
	}

	@Test
	public void getDcpType() {
		// TODO UNITTEST
	}

	@Test
	public void runPreprocessors() {
		// TODO UNITTEST
	}

	@Test
	public void findContentType() {
		// TODO UNITTEST
	}

	@Test
	public void findProvider() {
		// TODO UNITTEST
	}

	@Test
	public void isSuperProvider() {
		// TODO UNITTEST
	}

	@Test
	public void authenticate() {
		// TODO UNITTEST
	}
}
