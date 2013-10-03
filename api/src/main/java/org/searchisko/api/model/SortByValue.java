package org.searchisko.api.model;

import org.searchisko.api.rest.FeedRestService;
import org.searchisko.api.rest.SearchRestService;
import org.searchisko.api.util.SearchUtils;

/**
 * Enum with names available for <code>sortBy</code> search request parameter.
 *
 * @author Vlastimil Elias (velias at redhat dot com)
 * @see QuerySettings
 */
public enum SortByValue {
	/**
	 * Sort by score. Used as default for search. See {@link SearchRestService}.
	 */
	SCORE("score"),
	/**
	 * Newest modified content first.
	 */
	NEW("new"),
	/**
	 * Oldest modified content first.
	 */
	OLD("old"),

	/**
	 * Newest created content first. Used as default for ATOM feeds. See {@link FeedRestService}.
	 */
	NEW_CREATION("new-create");

	/**
	 * Value used as request parameter
	 */
	private String value;

	private SortByValue(String value) {
		this.value = value;
	}

	@Override
	public String toString() {
		return value;
	}

	/**
	 * Convert request parameter to enum item.
	 *
	 * @param requestVal value from request to parse
	 * @return enum item for given request value, null if it is empty
	 * @throws IllegalArgumentException if request value is invalid
	 */
	public static SortByValue parseRequestParameterValue(String requestVal) throws IllegalArgumentException {
		requestVal = SearchUtils.trimToNull(requestVal);
		if (requestVal == null)
			return null;
		for (SortByValue n : SortByValue.values()) {
			if (n.value.equals(requestVal))
				return n;
		}
		throw new IllegalArgumentException(QuerySettings.SORT_BY_KEY);
	}

}