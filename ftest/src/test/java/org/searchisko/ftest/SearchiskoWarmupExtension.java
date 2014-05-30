/*
 * JBoss, Home of Professional Open Source
 * Copyright 2014 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 */

package org.searchisko.ftest;

import org.jboss.arquillian.core.spi.LoadableExtension;

/**
 * Arquillian Extension registers {@link SearchiskoWarmupExecutor}
 *
 * @author Libor Krzyzanek
 */
public class SearchiskoWarmupExtension implements LoadableExtension {


	@Override
	public void register(ExtensionBuilder builder) {
		builder.observer(SearchiskoWarmupExecutor.class);
	}

}
