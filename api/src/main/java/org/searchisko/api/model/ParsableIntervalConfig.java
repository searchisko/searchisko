/*
 * JBoss, Home of Professional Open Source
 * Copyright 20142 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 */
package org.searchisko.api.model;

/**
 * Every class that can be referenced in range filter config via "_processor" field must implement this interface.
 *
 * Each implementation must:
 * - must implement this interface
 * - be an enum Java type
 * - must implement method "parseRequestParameterValue(String requestVal)" method returning instance of ParsableIntervalConfig
 *
 * The contract allows to get some date (represented as a long value) related to reference date (again represented as
 * long value) which is passed into both "lte" and "gte" functions.
 *
 * TODO: relax enum condition. Implementing classes should not have to be enums!
 * This interface is experimental and is subject to change in the future. Existing design has some problems:
 * - it is not possible to declare static methods in the interface but we are using "parseRequestParameterValue" method
 *   in static context
 *
 * @see PastIntervalValue
 * @author Lukas Vlcek
 * @since 1.0.2
 */
public interface ParsableIntervalConfig {

	/**
	 * Return value used for "gte".
	 *
	 * @param value
	 * @return
	 */
	public long getLteValue(long value);

	/**
	 * Return value used for "lte".
	 *
	 * @param value
	 * @return
	 */
	public long getGteValue(long value);
}
