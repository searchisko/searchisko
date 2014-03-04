/*
 * JBoss, Home of Professional Open Source
 * Copyright 2014 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 */
package org.searchisko.api.rest.search;

import java.util.Collections;
import java.util.List;

/**
 * Parsed configuration of filter which supports "_suppress" config option.
 *
 * @author Lukas Vlcek
 * @since 1.0.2
 */
public class SemiParsedFilterConfigSupportSuppressed extends SemiParsedFilterConfig {

	List<String> suppress = Collections.emptyList();

	public List<String> getSuppressed() { return this.suppress; }
	public void setSuppressed(List<String> suppress) { this.suppress = suppress; }
}
