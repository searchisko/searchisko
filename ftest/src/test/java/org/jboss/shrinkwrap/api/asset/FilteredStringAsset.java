package org.jboss.shrinkwrap.api.asset;

import org.apache.commons.lang3.StringUtils;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Map;

/**
 * Simple filtered Asset
 *
 * @author Libor Krzyzanek
 */
public class FilteredStringAsset implements Asset {

	/**
	 * Underlying content.
	 */
	private final String content;

	/**
	 * Creates a new instance backed by the specified String but filtered
	 *
	 * @param content           The content represented as a String
	 * @param replaceDefinition definition of replacement
	 * @throws IllegalArgumentException If the contents were not specified
	 */
	public FilteredStringAsset(String content, Map<String, String> replaceDefinition) {
		for (Map.Entry<String, String> def : replaceDefinition.entrySet()) {
			content = StringUtils.replace(content, def.getKey(), def.getValue());
		}
		this.content = content;
	}


	@Override
	public InputStream openStream() {
		return new ByteArrayInputStream(content.getBytes());
	}
}
