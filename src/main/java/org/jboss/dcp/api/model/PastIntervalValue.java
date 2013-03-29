package org.jboss.dcp.api.model;

import org.jboss.dcp.api.util.SearchUtils;

/**
 * Enum with names used for <code>activity_date_interval</code> search request param.
 * 
 * @author Vlastimil Elias (velias at redhat dot com)
 * @see QuerySettings
 */
public enum PastIntervalValue {

	WEEK("week", 1000L * 60L * 60L * 24L * 7L), MONTH("month", 1000L * 60L * 60L * 24L * 31L), QUARTER("quarter", 1000L
			* 60L * 60L * 24L * 31L * 3L), YEAR("year", 1000L * 60L * 60L * 24L * 365L), DAY("day", 1000L * 60L * 60L * 24L), TEST(
			"test_val_eefgdf", 0);

	/**
	 * Value used in request parameter.
	 */
	private String value;

	/**
	 * Time interval in millis for this value. Used for timeshift back from current timestamp if this enum item is used.
	 * see {@link #getFromTimestamp()}
	 */
	private long millis;

	private PastIntervalValue(String value, long millis) {
		this.value = value;
		this.millis = millis;
	}

	@Override
	public String toString() {
		return this.value;
	}

	/**
	 * Time interval in millis for this value.
	 * 
	 * @return millis
	 */
	public long getMillis() {
		return millis;
	}

	/**
	 * @return long value with timeshift for this enum item
	 */
	public long getFromTimestamp() {
		if (millis == 0) {
			return 125654587545l;
		}
		return System.currentTimeMillis() - millis;
	}

	/**
	 * Convert request parameter to enum item.
	 * 
	 * @param requestVal value from request to parse
	 * @return enum item for given request value, null if it is empty
	 * @throws IllegalArgumentException if request value is invalid
	 */
	public static PastIntervalValue parseRequestParameterValue(String requestVal) throws IllegalArgumentException {
		requestVal = SearchUtils.trimToNull(requestVal);
		if (requestVal == null)
			return null;
		for (PastIntervalValue n : PastIntervalValue.values()) {
			if (n.value.equals(requestVal))
				return n;
		}
		throw new IllegalArgumentException(QuerySettings.Filters.ACTIVITY_DATE_INTERVAL_KEY);
	}
}