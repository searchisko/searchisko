/*
 * JBoss, Home of Professional Open Source
 * Copyright 2012 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 */
package org.searchisko.api.util;

import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.type.TypeReference;
import org.elasticsearch.common.joda.time.LocalDateTime;
import org.elasticsearch.common.joda.time.format.ISODateTimeFormat;

import java.io.IOException;
import java.io.InputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Distinct utility methods.
 *
 * @author Libor Krzyzanek
 * @author Vlastimil Elias (velias at redhat dot com)
 */
public class SearchUtils {

	private static final Logger log = Logger.getLogger(SearchUtils.class.getName());

	/**
	 * Load properties from defined path e.g. "/app.properties"
	 *
	 * @param path
	 * @return newly initialized {@link Properties}
	 * @throws IOException
	 * @see {@link Class#getResourceAsStream(String)}
	 */
	public static Properties loadProperties(String path) throws IOException {
		Properties prop = new Properties();
		InputStream inStream = SearchUtils.class.getResourceAsStream(path);
		prop.load(inStream);
		inStream.close();

		return prop;
	}

	/**
	 * Trim string and return null if empty.
	 *
	 * @param value to trim
	 * @return trimmed value or null if empty
	 */
	public static String trimToNull(String value) {
		if (value != null) {
			value = value.trim();
			if (value.isEmpty())
				value = null;
		}
		return value;
	}

	/**
	 * Return a new list which contains non-null and trimmed non-empty items from input list.
	 *
	 * @param values list of strings (for example taken form URL parameters)
	 * @return safe list or null (never empty list)
	 */
	public static List<String> safeList(List<String> values) {
		List<String> safeValues = null;
		for (String value : values) {
			String sv = trimToNull(value);
			if (sv != null) {
				if (safeValues == null) {
					safeValues = new ArrayList<>();
				}
				safeValues.add(sv);
			}
		}
		return safeValues;
	}

	/**
	 * Check if String is blank.
	 *
	 * @param value to check
	 * @return true if value is blank (so null or empty or whitespaces only string)
	 */
	public static boolean isBlank(String value) {
		return value == null || value.trim().isEmpty();
	}

	/**
	 * Convert JSON Map structure into String with JSON content.
	 *
	 * @param jsonMapValue to convert
	 * @return
	 * @throws IOException
	 */
	public static String convertJsonMapToString(Map<String, Object> jsonMapValue) throws IOException {
		if (jsonMapValue == null)
			return "";
		ObjectMapper mapper = new ObjectMapper();
		mapper.setDateFormat(getISODateFormat());
		return mapper.writeValueAsString(jsonMapValue);
	}

	/**
	 * Get ISO date time formatter.
	 *
	 * @return DateFormat instance for ISO format
	 */
	public static DateFormat getISODateFormat() {
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
		sdf.setTimeZone(TimeZone.getTimeZone("GMT"));
		sdf.setLenient(false);
		return sdf;
	}

	/**
	 * Parse ISO date time formatted string into {@link Date} instance.
	 *
	 * @param string ISO formatted date string to parse
	 * @param silent if true then null is returned instead of {@link IllegalArgumentException} thrown
	 * @return parsed date or null
	 * @throws IllegalArgumentException in case of bad format
	 */
	public static Date dateFromISOString(String string, boolean silent) throws IllegalArgumentException {
		if (string == null)
			return null;
		try {
			return ISODateTimeFormat.dateTimeParser().parseDateTime(string).toDate();
		} catch (IllegalArgumentException e) {
			if (!silent)
				throw e;
			else
				return null;
		}
	}

	/**
	 * Test if Date is after date computed as current date minus threshold
	 *
	 * @param date               date to be tested. Can be String in ISO format or java.util.date
	 * @param thresholdInMinutes threshold in minutes
	 * @return false if sysUpdated is newer otherwise true
	 */
	public static boolean isDateAfter(Object date, int thresholdInMinutes) {
		LocalDateTime d = null;
		if (date instanceof Date) {
			d = new LocalDateTime(((Date) date).getTime());
		} else if (date instanceof String) {
			try {
				d = new LocalDateTime(SearchUtils.dateFromISOString((String) date, true));
			} catch (Exception e) {
				// should never happen. See dateFromISOString method
			}
		}
		log.log(Level.FINEST, "date to check: {0}", d);

		if (d != null) {
			LocalDateTime now = new LocalDateTime();
			LocalDateTime test = now.minusMinutes(thresholdInMinutes);

			if (log.isLoggable(Level.FINEST)){
				log.log(Level.FINEST, "date to test again: {0}, threshold: {1}", new Object[] {test, thresholdInMinutes});
			}

			if (d.isBefore(test)) {
				return false;
			}
		}

		return true;
	}

	/**
	 * Convert String with JSON content into JSON Map structure.
	 *
	 * @param jsonData string to convert
	 * @return JSON MAP structure
	 * @throws IOException
	 */
	public static Map<String, Object> convertToJsonMap(String jsonData) throws IOException {
		if (jsonData == null)
			return null;
		ObjectMapper mapper = new ObjectMapper();
		return mapper.readValue(jsonData, new TypeReference<Map<String, Object>>() {
		});
	}

	/**
	 * Get Integer value from value in Json Map. Can convert from {@link String} and {@link Number} values.
	 *
	 * @param map to get Integer value from
	 * @param key in map to get value from
	 * @return Integer value if found. <code>null</code> if value is not present in map.
	 * @throws NumberFormatException if valu from map is not convertible to integer number
	 */
	public static Integer getIntegerFromJsonMap(Map<String, Object> map, String key) throws NumberFormatException {
		if (map == null)
			return null;
		Object o = map.get(key);
		if (o == null)
			return null;
		if (o instanceof Integer)
			return (Integer) o;
		else if (o instanceof Number)
			return ((Number) o).intValue();
		else if (o instanceof String)
			return Integer.valueOf((String) o);
		else
			throw new NumberFormatException();
	}

	/**
	 * Merge values from source into target JSON Map. Target Map is more important during merge, so in case of some
	 * conflicts target Map wins and original value is preserved. Lists (used for JSON Array) merging do not create
	 * duplication (uses <code>equals()</code> to detect them). Structure inside source Map is not changed any way.
	 *
	 * @param source Map to merge values from
	 * @param target Map to merge values into
	 */
	public static void mergeJsonMaps(Map<String, Object> source, Map<String, Object> target) {
		mergeJsonMaps(source, target, null);
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private static void mergeJsonMaps(Map<String, Object> source, Map<String, Object> target, String keyBase) {
		if (source == null || target == null || source.isEmpty())
			return;
		if (keyBase == null || keyBase.trim().isEmpty())
			keyBase = "";
		else {
			keyBase = keyBase.trim() + ".";
		}
		for (String key : source.keySet()) {
			Object sourceValue = source.get(key);
			if (sourceValue == null)
				continue;
			Object targetValue = target.get(key);
			if (targetValue == null) {
				target.put(key, sourceValue);
			} else {
				if (targetValue instanceof List) {
					if (sourceValue instanceof List) {
						if (!((List) sourceValue).isEmpty()) {
							// merge without duplicities
							if (((List) targetValue).isEmpty()) {
								((List) targetValue).addAll((List) sourceValue);
							} else {
								LinkedHashSet nv = new LinkedHashSet();
								nv.addAll((List) targetValue);
								nv.addAll((List) sourceValue);
								((List) targetValue).clear();
								((List) targetValue).addAll(nv);
							}
						}
					} else {
						if (!((List) targetValue).contains(sourceValue))
							((List) targetValue).add(sourceValue);
					}
				} else if (targetValue instanceof Map) {
					if (sourceValue instanceof Map) {
						mergeJsonMaps((Map<String, Object>) sourceValue, (Map<String, Object>) targetValue, keyBase + key);
					} else {
						log.fine("Can't merge value for key " + keyBase + key
								+ " because target is Map but source is not Map. Keeping source value there.");
					}
				} else {
					if (sourceValue instanceof List) {
						List newList = new ArrayList();
						newList.addAll((List) sourceValue);
						if (!newList.contains(targetValue))
							newList.add(targetValue);
						target.put(key, newList);
					} else if (sourceValue instanceof Map) {
						log.fine("Can't merge value for key " + keyBase + key
								+ " because source is Map but target is simple value. Keeping source value.");
					} else {
						// merge values into list if not same
						if (!sourceValue.equals(targetValue)) {
							List newList = new ArrayList<>();
							newList.add(sourceValue);
							newList.add(targetValue);
							target.put(key, newList);
						}
					}
				}
			}
		}
	}

}
