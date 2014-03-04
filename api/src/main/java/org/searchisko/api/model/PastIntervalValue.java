/*
 * JBoss, Home of Professional Open Source
 * Copyright 2012 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 */
package org.searchisko.api.model;

import org.searchisko.api.util.SearchUtils;

/**
 * Enum representing past interval names.
 * This processor is used to calculate predefined intervals (week, month, ...) before reference date.
 * It allows to configure the range filter to be used in cases like:
 * "give me documents that have been update within last month"
 * ... or ...
 * "give me documents that have been update in the past but not within last week".
 *
 * @author Vlastimil Elias (velias at redhat dot com)
 * @author Lukas Vlcek
 * @see QuerySettings
 */
public enum PastIntervalValue implements ParsableIntervalConfig {

	WEEK("week", 1000L * 60L * 60L * 24L * 7L), MONTH("month", 1000L * 60L * 60L * 24L * 31L),
	QUARTER("quarter", 1000L * 60L * 60L * 24L * 31L * 3L), YEAR("year", 1000L * 60L * 60L * 24L * 365L),
	DAY("day", 1000L * 60L * 60L * 24L), NULL("null",null), UNDEFINED("undefined",null), TEST("test_val_eefgdf", 0L);

	/**
	 * Value used in request parameter.
	 */
	private String value;

	/**
	 * Time interval in millis for this value.
	 */
	private Long millis;

	private PastIntervalValue(String value, Long millis) {
		this.value = value;
		this.millis = millis;
	}

	@Override
	public String toString() {
		return this.value;
	}

	public long getLteValue(long value) {
		if (this.equals(TEST)) {
			return 125654587545L;
		}
		return value - this.millis;
	}

	public long getGteValue(long value) {
		if (this.equals(TEST)) {
			return 125654587545L;
		}
		return value - this.millis;
	}

	/**
	 * Convert request parameter to enum item.
	 *
	 * @param requestVal value from request to parse
	 * @return enum item for given request value, {@link org.searchisko.api.model.PastIntervalValue#NULL} if it is empty
	 *         or {@link org.searchisko.api.model.PastIntervalValue#UNDEFINED} is the value it unknown.
	 * @throws IllegalArgumentException if request value is invalid
	 */
	public static PastIntervalValue parseRequestParameterValue(String requestVal) {
		requestVal = SearchUtils.trimToNull(requestVal);
		if (requestVal == null)
			return NULL;
		for (PastIntervalValue n : PastIntervalValue.values()) {
			if (n.value.equals(requestVal))
				return n;
		}
		return UNDEFINED;
	}
}