/*
 * JBoss, Home of Professional Open Source
 * Copyright 2014 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 */
package org.searchisko.api.rest.search;

/**
 * Parsed configuration of range filter.
 *
 * @author Lukas Vlcek
 * @since 1.0.2
 */
public class SemiParsedRangeFilterConfig extends SemiParsedFilterConfigSupportSuppressed {

	private String lte = null;
	private String gte = null;
	private String processorClassName = null;

	public boolean definesGte() { return gte != null; }
	public boolean definesLte() { return lte != null; }

	public void setGte(String value) { this.gte = value; }
	public String getGte() { return this.gte; }

	public void setLte(String value) { this.lte = value; }
	public String getLte() { return this.lte; }

	public void setProcessor(String processorClassName) { this.processorClassName = processorClassName; }
	public String getProcessor() {
		return this.processorClassName;
	}
}
