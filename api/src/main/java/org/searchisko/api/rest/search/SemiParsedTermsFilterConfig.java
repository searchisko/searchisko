/*
 * JBoss, Home of Professional Open Source
 * Copyright 2014 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 */
package org.searchisko.api.rest.search;

/**
 * Parsed configuration of terms filter.
 *
 * @author Lukas Vlcek
 * @since 1.0.2
 */
public class SemiParsedTermsFilterConfig extends SemiParsedFilterConfig {

	private boolean lowercase = false;

	public boolean isLowercase() { return lowercase; }
	public void setLowercase(boolean value) { this.lowercase = value; }
}
