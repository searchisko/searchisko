/*
 * JBoss, Home of Professional Open Source
 * Copyright 2012 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 */
package org.jboss.dcp.api.service;

import java.util.ArrayList;
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
		Map<String, Object> typeDef = new HashMap<String, Object>();

		// case - preprocessors field not defined
		Assert.assertNull(ProviderService.getPreprocessors(typeDef, "mytype"));

		// case - preprocessors field OK
		typeDef.put("input_preprocessors", new ArrayList<Map<String, Object>>());
		Assert.assertEquals(typeDef.get("input_preprocessors"), ProviderService.getPreprocessors(typeDef, "mytype"));

		// case - preprocessors field with bad element type
		try {
			typeDef.put("input_preprocessors", "badstructureelement");
			ProviderService.getPreprocessors(typeDef, "mytype");
			Assert.fail("SettingsException expected");
		} catch (SettingsException e) {
			// OK
		}
		try {
			typeDef.put("input_preprocessors", new HashMap<String, Object>());
			ProviderService.getPreprocessors(typeDef, "mytype");
			Assert.fail("SettingsException expected");
		} catch (SettingsException e) {
			// OK
		}
	}

	@Test
	public void getIndexName() {
		Map<String, Object> typeDef = new HashMap<String, Object>();

		// case - index field not defined
		try {
			ProviderService.getIndexName(typeDef, "mytype");
			Assert.fail("SettingsException expected");
		} catch (SettingsException e) {
			// OK
		}

		Map<String, Object> indexElement = new HashMap<String, Object>();
		typeDef.put(ProviderService.INDEX, indexElement);
		// case - index field defined but name field is empty
		try {
			ProviderService.getIndexName(typeDef, "mytype");
			Assert.fail("SettingsException expected");
		} catch (SettingsException e) {
			// OK
		}

		indexElement.put(ProviderService.NAME, "myindex");
		// case - index name found correct found
		Assert.assertEquals(indexElement.get(ProviderService.NAME), ProviderService.getIndexName(typeDef, "mytype"));

		// case - empty value for name element
		indexElement.put(ProviderService.NAME, "");
		try {
			ProviderService.getIndexName(typeDef, "mytype");
			Assert.fail("SettingsException expected");
		} catch (SettingsException e) {
			// OK
		}

		// case - empty value for name element
		indexElement.put(ProviderService.NAME, "   ");
		try {
			ProviderService.getIndexName(typeDef, "mytype");
			Assert.fail("SettingsException expected");
		} catch (SettingsException e) {
			// OK
		}

		// case - bad type of value for name element
		indexElement.put(ProviderService.NAME, new Integer(10));
		try {
			ProviderService.getIndexName(typeDef, "mytype");
			Assert.fail("SettingsException expected");
		} catch (SettingsException e) {
			// OK
		}

		// case - bad type of value for index element
		typeDef.put(ProviderService.INDEX, "baaad");
		try {
			ProviderService.getIndexName(typeDef, "mytype");
			Assert.fail("SettingsException expected");
		} catch (SettingsException e) {
			// OK
		}
	}

	@Test
	public void getIndexType() {
		Map<String, Object> typeDef = new HashMap<String, Object>();

		// case - index field not defined
		try {
			ProviderService.getIndexType(typeDef, "mytype");
			Assert.fail("SettingsException expected");
		} catch (SettingsException e) {
			// OK
		}

		Map<String, Object> indexElement = new HashMap<String, Object>();
		typeDef.put(ProviderService.INDEX, indexElement);
		// case - index field defined but type field is empty
		try {
			ProviderService.getIndexType(typeDef, "mytype");
			Assert.fail("SettingsException expected");
		} catch (SettingsException e) {
			// OK
		}

		// case - index type found correct found
		indexElement.put(ProviderService.TYPE, "myidxtype");
		Assert.assertEquals(indexElement.get(ProviderService.TYPE), ProviderService.getIndexType(typeDef, "mytype"));

		// case - empty value for type element
		indexElement.put(ProviderService.TYPE, "");
		try {
			ProviderService.getIndexType(typeDef, "mytype");
			Assert.fail("SettingsException expected");
		} catch (SettingsException e) {
			// OK
		}

		// case - empty value for type element
		indexElement.put(ProviderService.TYPE, "  ");
		try {
			ProviderService.getIndexType(typeDef, "mytype");
			Assert.fail("SettingsException expected");
		} catch (SettingsException e) {
			// OK
		}

		// case - bad type of value for type element
		indexElement.put(ProviderService.TYPE, new Integer(10));
		try {
			ProviderService.getIndexType(typeDef, "mytype");
			Assert.fail("SettingsException expected");
		} catch (SettingsException e) {
			// OK
		}

		// case - bad type of value for index element
		typeDef.put(ProviderService.INDEX, "baaad");
		try {
			ProviderService.getIndexType(typeDef, "mytype");
			Assert.fail("SettingsException expected");
		} catch (SettingsException e) {
			// OK
		}
	}

	@Test
	public void getDcpType() {
		Map<String, Object> typeDef = new HashMap<String, Object>();

		// case - dcp_type field not defined
		try {
			ProviderService.getDcpType(typeDef, "mytype");
			Assert.fail("SettingsException expected");
		} catch (SettingsException e) {
			// OK
		}

		// case - dcp_type field defined but empty
		typeDef.put(ProviderService.DCP_TYPE, "");
		try {
			ProviderService.getDcpType(typeDef, "mytype");
			Assert.fail("SettingsException expected");
		} catch (SettingsException e) {
			// OK
		}

		// case - dcp_type field defined but empty
		typeDef.put(ProviderService.DCP_TYPE, "  ");
		try {
			ProviderService.getDcpType(typeDef, "mytype");
			Assert.fail("SettingsException expected");
		} catch (SettingsException e) {
			// OK
		}

		// case - index type found correct found
		typeDef.put(ProviderService.DCP_TYPE, "mydcptype");
		Assert.assertEquals(typeDef.get(ProviderService.DCP_TYPE), ProviderService.getDcpType(typeDef, "mytype"));
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
