/*
 * JBoss, Home of Professional Open Source
 * Copyright 2012 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 */
package org.searchisko.persistence.jpa.model;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;
import org.searchisko.api.service.ProviderService;

import static org.junit.Assert.assertEquals;
import org.searchisko.api.testtools.TestUtils;

/**
 * Unit test for {@link ProviderConverter}.
 * 
 * @author Libor Krzyzanek
 * 
 */
public class ProviderConverterTest {

	@Test
	public void convertToModel() throws IOException {
		ProviderConverter converter = new ProviderConverter();
		Map<String, Object> data = new HashMap<>();
		data.put(ProviderService.NAME, "jbossorg");
		data.put(ProviderService.SUPER_PROVIDER, true);

		Provider p = converter.convertToModel("jbossorg", data);

		assertEquals("jbossorg", p.getName());
		TestUtils.assertJsonContent("{\"" + ProviderService.NAME + "\":\"jbossorg\",\"" + ProviderService.SUPER_PROVIDER + "\":true}",
				p.getValue());
	}
}
