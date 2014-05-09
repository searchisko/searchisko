package org.searchisko.api.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jboss.elasticsearch.tools.content.PreprocessChainContext;

public final class PreprocessChainContextImpl implements PreprocessChainContext {

	public static final String WD_PREPROC_NAME = "preprocessor";
	public static final String WD_WARNING = "warning";

	public List<Map<String, String>> warnings = null;

	@Override
	public void addDataWarning(String preprocessorName, String warningMessage) throws IllegalArgumentException {
		if (warnings == null) {
			warnings = new ArrayList<>();
		}
		Map<String, String> wd = new HashMap<String, String>();
		wd.put(WD_PREPROC_NAME, preprocessorName);
		wd.put(WD_WARNING, warningMessage);
		warnings.add(wd);
	}

}