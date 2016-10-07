/*
 * JBoss, Home of Professional Open Source
 * Copyright 2012 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 */
package org.searchisko.api.util;

import java.io.IOException;
import java.io.InputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.lang.StringUtils;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.type.TypeReference;
import org.elasticsearch.common.joda.time.LocalDateTime;
import org.elasticsearch.common.joda.time.format.ISODateTimeFormat;
import org.elasticsearch.common.settings.SettingsException;

import javax.ws.rs.core.MultivaluedMap;

/**
 * Distinct utility methods.
 * 
 * @author Libor Krzyzanek
 * @author Vlastimil Elias (velias at redhat dot com)
 * @author Lukas Vlcek
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
		if (values != null) {
			for (String value : values) {
				String sv = trimToNull(value);
				if (sv != null) {
					if (safeValues == null) {
						safeValues = new ArrayList<>();
					}
					safeValues.add(sv);
				}
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
	 * Check if object is blank, which mean null Object, or blank String, or empty {@link Collection} or {@link Map}.
	 * 
	 * @param value to check
	 * @return true if value is blank (so null or empty or whitespaces only string)
	 */
	public static boolean isBlank(Object value) {
		return value == null
				|| ((value instanceof String) && isBlank((String) value))
				|| ((value instanceof Collection) && ((Collection<?>) value).isEmpty() || ((value instanceof Map) && ((Map<?, ?>) value)
						.isEmpty()));
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
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXX");
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
	 * @param date date to be tested. Can be String in ISO format or java.util.date
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

			if (log.isLoggable(Level.FINEST)) {
				log.log(Level.FINEST, "date to test again: {0}, threshold: {1}", new Object[] { test, thresholdInMinutes });
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
	 * @throws NumberFormatException if value from map is not convertible to integer number
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
	 * Get list of Strings from given key on given map. If it contains simple String then List is created with it.
	 * {@link #safeList(List)} is used inside to filter list.
	 * 
	 * @param map to get value from
	 * @param key in map to get value from
	 * @return list of strings or null.
	 * @throws SettingsException if value in json map is invalid
	 */
	@SuppressWarnings("unchecked")
	public static List<String> getListOfStringsFromJsonMap(Map<String, Object> map, String key) throws SettingsException {
		if (map == null)
			return null;
		try {
			Object o = map.get(key);
			if (o instanceof String) {
				String v = StringUtils.trimToNull((String) o);
				if (v != null) {
					List<String> l = new ArrayList<>();
					l.add(v);
					return l;
				}
				return null;
			}
			return safeList((List<String>) o);
		} catch (ClassCastException e) {
			throw new SettingsException("No String or Array of strings present in field '" + key);
		}
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

	/**
	 * Convert URL params format to type that is required by internal Elasticsearch API.
	 *
	 * @param params URL parameters
	 * @return
	 */
	public static Map<String, Object> collapseURLParams(MultivaluedMap<String, String> params) {
		Map output = new HashMap<>();
		for (String key: params.keySet()) {
			List values = params.get(key);
			if (values != null) {
				output.put(key, values.size() == 1 ? values.get(0) : values.toArray());
			}
		}
		return output;
	}

	/**
	 * Determine if database product name is mysql
	 * @param databaseProductName
	 * @return true if database i mysql
	 * @see java.sql.Connection#getMetaData()#getDatabaseProductName()
	 */
	public static boolean isMysqlDialect(String databaseProductName) {
		return StringUtils.containsIgnoreCase(databaseProductName, "mysql");
	}

}
