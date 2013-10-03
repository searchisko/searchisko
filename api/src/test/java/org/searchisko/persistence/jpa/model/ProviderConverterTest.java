/*
 * JBoss, Home of Professional Open Source
 * Copyright 2012 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 */
package org.searchisko.persistence.jpa.model;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.searchisko.api.service.ProviderService;
import org.junit.Test;

/**
 * @author Libor Krzyzanek
 *
 */
public class ProviderConverterTest {

	@Test
	public void testConvertToModel() throws IOException {
		ProviderConverter converter = new ProviderConverter();
		Map<String, Object> data = new HashMap<String, Object>();
		data.put(ProviderService.NAME, "jbossorg");
		data.put(ProviderService.SUPER_PROVIDER, true);

		Provider p = converter.convertToModel("jbossorg", data);

		assertEquals("jbossorg", p.getName());
		assertEquals("{\"" + ProviderService.NAME + "\":\"jbossorg\",\"" + ProviderService.SUPER_PROVIDER + "\":true}",
				p.getValue());
	}
}
