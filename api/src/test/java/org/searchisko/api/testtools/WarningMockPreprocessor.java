/*
 * JBoss, Home of Professional Open Source
 * Copyright 2014 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 */
package org.searchisko.api.testtools;

import java.util.Map;

import org.elasticsearch.common.settings.SettingsException;
import org.jboss.elasticsearch.tools.content.PreprocessChainContext;
import org.jboss.elasticsearch.tools.content.StructuredContentPreprocessorBase;

/**
 * Mock preprocessor which generates warning if data are null.
 * 
 * @author Vlastimil Elias (velias at redhat dot com)
 */
public class WarningMockPreprocessor extends StructuredContentPreprocessorBase {

	@Override
	public Map<String, Object> preprocessData(Map<String, Object> data, PreprocessChainContext chainContext) {
		if (data == null)
			addDataWarning(chainContext, "warning message because null data");
		return data;
	}

	@Override
	public void init(Map<String, Object> settings) throws SettingsException {
	}

}
