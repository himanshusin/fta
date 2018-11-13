/*
 * Copyright 2017-2018 Tim Segall
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.cobber.fta;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.text.DateFormatSymbols;
import java.text.DecimalFormatSymbols;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;

import com.cobber.fta.DateTimeParser.DateResolutionMode;

/**
 * Analyze Text data to determine type information and other key metrics
 * associated with a text stream. A key objective of the analysis is that it
 * should be sufficiently fast to be in-line (i.e. as the data is input from
 * some source it should be possible to stream the data through this class
 * without undue performance degradation).
 *
 * <p>
 * Typical usage is:
 * </p>
 *
 * <pre>
 * {@code
 * 		TextAnalyzer analysis = new TextAnalyzer("Age");
 *
 * 		analysis.train("12");
 * 		analysis.train("62");
 * 		analysis.train("21");
 * 		analysis.train("37");
 * 		...
 *
 * 		TextAnalysisResult result = analysis.getResult();
 * }
 * </pre>
 */
public class TextAnalyzer {

	/** The default value for the number of samples to collect before making a type determination. */
	public static final int SAMPLE_DEFAULT = 20;
	private int samples = SAMPLE_DEFAULT;

	/** Should we collect statistics (min, max, sum) as we parse the data stream. */
	private boolean collectStatistics = true;

	/** The default value for the maximum Cardinality tracked. */
	public static final int MAX_CARDINALITY_DEFAULT = 500;
	private int maxCardinality = MAX_CARDINALITY_DEFAULT;

	private static final int MIN_SAMPLES_FOR_KEY = 1000;

	/** The default value for the maximum # of outliers tracked. */
	public static final int MAX_OUTLIERS_DEFAULT = 50;
	private int maxOutliers = MAX_OUTLIERS_DEFAULT;

	private static final int REFLECTION_SAMPLES = 30;
	private int reflectionSamples = REFLECTION_SAMPLES;

	private String name;
	private DateResolutionMode resolutionMode = DateResolutionMode.None;
	private char decimalSeparator;
	private char monetaryDecimalSeparator;
	private char groupingSeparator;
	private char minusSign;
	private long sampleCount;
	private long nullCount;
	private long blankCount;
	private Map<String, Integer> cardinality = new HashMap<String, Integer>();
	private final Map<String, Integer> outliers = new HashMap<String, Integer>();
	private List<String> raw; // 0245-11-98
	// 0: d{4}-d{2}-d{2} 1: d{+}-d{+}-d{+} 2: d{+}-d{+}-d{+}
	// 0: d{4} 1: d{+} 2: [-]d{+}
	// input "hello world" 0: a{5} a{5} 1: a{+} a{+} 2: a{+}
	private List<StringBuilder>[] levels = new ArrayList[3];

	private PatternInfo.Type matchType;
	private long matchCount;
	private String matchPattern;
	private PatternInfo matchPatternInfo;

	private boolean trainingStarted;

	private boolean leadingWhiteSpace = false;
	private boolean trailingWhiteSpace = false;

	private double minDouble = Double.MAX_VALUE;
	private double maxDouble = -Double.MAX_VALUE;
	private long negativeDoubles = 0;
	private BigDecimal sumBD = BigDecimal.ZERO;

	private long minLong = Long.MAX_VALUE;
	private long maxLong = Long.MIN_VALUE;
	private long negativeLongs = 0;
	private BigInteger sumBI = BigInteger.ZERO;

	private String minString;
	private String maxString;

	private String minBoolean;
	private String maxBoolean;

	private LocalTime minLocalTime;
	private LocalTime maxLocalTime;

	private LocalDate minLocalDate;
	private LocalDate maxLocalDate;

	private LocalDateTime minLocalDateTime;
	private LocalDateTime maxLocalDateTime;

	private ZonedDateTime minZonedDateTime;
	private ZonedDateTime maxZonedDateTime;

	private OffsetDateTime minOffsetDateTime;
	private OffsetDateTime maxOffsetDateTime;

	private int minRawLength = Integer.MAX_VALUE;
	private int maxRawLength = Integer.MIN_VALUE;

	private int minTrimmedLength = Integer.MAX_VALUE;
	private int maxTrimmedLength = Integer.MIN_VALUE;

	private int possibleDateTime;
	private long totalLongs;
	private long totalLeadingZeros;
	private int possibleEmails;
	private int possibleZips;
	private int possibleURLs;
	private int possibleAddresses;

	private static Map<String, PatternInfo> patternInfo;
	private static Map<String, PatternInfo> typeInfo;
	private static Map<String, String> promotion;

	private static Set<String> zips = new HashSet<String>();
	private static Set<String> usStates = new HashSet<String>();
	private static Set<String> caProvinces = new HashSet<String>();
	private static Set<String> countries = new HashSet<String>();
	private static Set<String> addressMarkers = new HashSet<String>();
	private static Set<String> gender = new HashSet<String>();
	private static Set<String> monthAbbr = new HashSet<String>();

	public static final String PATTERN_ANY = ".";
	public static final String PATTERN_ANY_VARIABLE = ".+";
	public static final String PATTERN_ALPHA = "\\p{Alpha}";
	public static final String PATTERN_ALPHA_VARIABLE = PATTERN_ALPHA + "+";
	public static final String PATTERN_ALPHA_2 = PATTERN_ALPHA + "{2}";
	public static final String PATTERN_ALPHA_3 = PATTERN_ALPHA + "{3}";

	public static final String PATTERN_ALPHANUMERIC = "\\p{Alnum}";
	public static final String PATTERN_ALPHANUMERIC_VARIABLE = PATTERN_ALPHANUMERIC + "+";
	public static final String PATTERN_ALPHANUMERIC_2 = PATTERN_ALPHANUMERIC + "{2}";
	public static final String PATTERN_ALPHANUMERIC_3 = PATTERN_ALPHANUMERIC + "{3}";

	public static final String PATTERN_BOOLEAN = "(?i)(true|false)";
	public static final String PATTERN_YESNO = "(?i)(yes|no)";

	public static final String PATTERN_LONG = "\\d+";
	public static final String PATTERN_SIGNED_LONG = "-?\\d+";
	public static final String PATTERN_DOUBLE = PATTERN_LONG + "|" + "(\\d+)?\\.\\d+";
	public static final String PATTERN_SIGNED_DOUBLE = PATTERN_SIGNED_LONG + "|" + "-?(\\d+)?\\.\\d+";
	public static final String PATTERN_DOUBLE_WITH_EXPONENT = PATTERN_LONG + "|" + "(\\d+)?\\.\\d+(?:[eE]([-+]?\\d+))?";
	public static final String PATTERN_SIGNED_DOUBLE_WITH_EXPONENT = PATTERN_SIGNED_LONG + "|" + "-?(\\d+)?\\.\\d+(?:[eE]([-+]?\\d+))?";

	private final Map<String, DateTimeFormatter> formatterCache = new HashMap<String, DateTimeFormatter>();

	private static void addPattern(final Map<String, PatternInfo> map, final boolean patternIsKey, final String regexp, final PatternInfo.Type type,
			final String typeQualifier, final int minLength, final int maxLength, final String generalPattern, final String format) {
		map.put(patternIsKey ? regexp : (type.toString() + "." + typeQualifier), new PatternInfo(regexp, type, typeQualifier, minLength, maxLength, generalPattern, format));
	}

	static {
		patternInfo = new HashMap<String, PatternInfo>();
		typeInfo = new HashMap<String, PatternInfo>();
		promotion = new HashMap<String, String>();

		addPattern(patternInfo, true, PATTERN_BOOLEAN, PatternInfo.Type.BOOLEAN, null, 4, 5, null, "");
		addPattern(patternInfo, true, PATTERN_YESNO, PatternInfo.Type.BOOLEAN, null, 2, 3, null, "");
		addPattern(patternInfo, true, "[0|1]", PatternInfo.Type.BOOLEAN, null, -1, -1, null, null);

		addPattern(patternInfo, true, PATTERN_ANY_VARIABLE, PatternInfo.Type.STRING, null, 1, -1, null, "");
		addPattern(patternInfo, true, PATTERN_ALPHA_VARIABLE, PatternInfo.Type.STRING, null, 1, -1, null, "");
		addPattern(patternInfo, true, PATTERN_ALPHANUMERIC_VARIABLE, PatternInfo.Type.STRING, null, 1, -1, null, "");
		addPattern(patternInfo, true, PATTERN_ALPHA_2, PatternInfo.Type.STRING, null, 2, 2, null, "");
		addPattern(patternInfo, true, PATTERN_ALPHA_3, PatternInfo.Type.STRING, null, 3, 3, null, "");
		addPattern(patternInfo, true, PATTERN_ALPHANUMERIC_2, PatternInfo.Type.STRING, null, 2, 2, null, "");
		addPattern(patternInfo, true, PATTERN_ALPHANUMERIC_3, PatternInfo.Type.STRING, null, 3, 3, null, "");

		addPattern(patternInfo, true, PATTERN_LONG, PatternInfo.Type.LONG, null, 1, -1, null, "");
		addPattern(patternInfo, true, PATTERN_SIGNED_LONG, PatternInfo.Type.LONG, "SIGNED", 1, -1, null, "");
		addPattern(patternInfo, true, PATTERN_DOUBLE, PatternInfo.Type.DOUBLE, null, -1, -1, null, "");
		addPattern(patternInfo, true, PATTERN_SIGNED_DOUBLE, PatternInfo.Type.DOUBLE, "SIGNED", -1, -1, null, "");
		addPattern(patternInfo, true, PATTERN_DOUBLE_WITH_EXPONENT, PatternInfo.Type.DOUBLE, null, -1, -1, null, "");
		addPattern(patternInfo, true, PATTERN_SIGNED_DOUBLE_WITH_EXPONENT, PatternInfo.Type.DOUBLE, "SIGNED", -1, -1, null, "");

		// Logical Types
		addPattern(typeInfo, false, "[NULL]", PatternInfo.Type.STRING, "NULL", -1, -1, null, null);
		addPattern(typeInfo, false, "\\p{javaWhitespace}*", PatternInfo.Type.STRING, "BLANKORNULL", -1, -1, null, null);
		addPattern(typeInfo, false, "\\p{javaWhitespace}*", PatternInfo.Type.STRING, "BLANK", -1, -1, null, null);
		addPattern(typeInfo, false, "\\d{5}", PatternInfo.Type.LONG, "ZIP", -1, -1, null, null);
		addPattern(typeInfo, false, PATTERN_ANY_VARIABLE, PatternInfo.Type.STRING, "ADDRESS", -1, -1, null, null);
		addPattern(typeInfo, false, PATTERN_ALPHA_2, PatternInfo.Type.STRING, "NA_STATE", -1, -1, null, null);
		addPattern(typeInfo, false, PATTERN_ALPHA_2, PatternInfo.Type.STRING, "US_STATE", -1, -1, null, null);
		addPattern(typeInfo, false, PATTERN_ALPHA_2, PatternInfo.Type.STRING, "CA_PROVINCE", -1, -1, null, null);
		addPattern(typeInfo, false, ".+", PatternInfo.Type.STRING, "COUNTRY", -1, -1, null, null);
		addPattern(typeInfo, false, PATTERN_ALPHA_VARIABLE, PatternInfo.Type.STRING, "GENDER", -1, -1, null, null);
		addPattern(typeInfo, false, PATTERN_ALPHA_3, PatternInfo.Type.STRING, "MONTHABBR", -1, -1, null, null);

		promotion.put(PATTERN_LONG + "---" + PATTERN_SIGNED_LONG, PATTERN_SIGNED_LONG);
		promotion.put(PATTERN_LONG + "---" + PATTERN_DOUBLE, PATTERN_DOUBLE);
		promotion.put(PATTERN_LONG + "---" + PATTERN_SIGNED_DOUBLE, PATTERN_SIGNED_DOUBLE);
		promotion.put(PATTERN_LONG + "---" + PATTERN_DOUBLE_WITH_EXPONENT, PATTERN_DOUBLE_WITH_EXPONENT);
		promotion.put(PATTERN_LONG + "---" + PATTERN_SIGNED_DOUBLE_WITH_EXPONENT, PATTERN_SIGNED_DOUBLE_WITH_EXPONENT);

		promotion.put(PATTERN_SIGNED_LONG + "---" + PATTERN_LONG, PATTERN_SIGNED_LONG);
		promotion.put(PATTERN_SIGNED_LONG + "---" + PATTERN_DOUBLE, PATTERN_DOUBLE);
		promotion.put(PATTERN_SIGNED_LONG + "---" + PATTERN_SIGNED_DOUBLE, PATTERN_SIGNED_DOUBLE);
		promotion.put(PATTERN_SIGNED_LONG + "---" + PATTERN_DOUBLE_WITH_EXPONENT, PATTERN_DOUBLE_WITH_EXPONENT);
		promotion.put(PATTERN_SIGNED_LONG + "---" + PATTERN_SIGNED_DOUBLE_WITH_EXPONENT, PATTERN_SIGNED_DOUBLE_WITH_EXPONENT);

		promotion.put(PATTERN_DOUBLE + "---" + PATTERN_LONG, PATTERN_DOUBLE);
		promotion.put(PATTERN_DOUBLE + "---" + PATTERN_SIGNED_LONG, PATTERN_DOUBLE);
		promotion.put(PATTERN_DOUBLE + "---" + PATTERN_SIGNED_DOUBLE, PATTERN_SIGNED_DOUBLE);
		promotion.put(PATTERN_DOUBLE + "---" + PATTERN_DOUBLE_WITH_EXPONENT, PATTERN_DOUBLE_WITH_EXPONENT);
		promotion.put(PATTERN_DOUBLE + "---" + PATTERN_SIGNED_DOUBLE_WITH_EXPONENT, PATTERN_SIGNED_DOUBLE_WITH_EXPONENT);

		promotion.put(PATTERN_SIGNED_DOUBLE + "---" + PATTERN_LONG, PATTERN_SIGNED_DOUBLE);
		promotion.put(PATTERN_SIGNED_DOUBLE + "---" + PATTERN_SIGNED_LONG, PATTERN_SIGNED_DOUBLE);
		promotion.put(PATTERN_SIGNED_DOUBLE + "---" + PATTERN_DOUBLE, PATTERN_SIGNED_DOUBLE);
		promotion.put(PATTERN_SIGNED_DOUBLE + "---" + PATTERN_DOUBLE_WITH_EXPONENT, PATTERN_DOUBLE_WITH_EXPONENT);
		promotion.put(PATTERN_SIGNED_DOUBLE + "---" + PATTERN_SIGNED_DOUBLE_WITH_EXPONENT, PATTERN_SIGNED_DOUBLE_WITH_EXPONENT);

		promotion.put(PATTERN_DOUBLE_WITH_EXPONENT + "---" + PATTERN_LONG, PATTERN_DOUBLE_WITH_EXPONENT);
		promotion.put(PATTERN_DOUBLE_WITH_EXPONENT + "---" + PATTERN_SIGNED_LONG, PATTERN_DOUBLE_WITH_EXPONENT);
		promotion.put(PATTERN_DOUBLE_WITH_EXPONENT + "---" + PATTERN_DOUBLE, PATTERN_DOUBLE_WITH_EXPONENT);
		promotion.put(PATTERN_DOUBLE_WITH_EXPONENT + "---" + PATTERN_SIGNED_DOUBLE, PATTERN_DOUBLE_WITH_EXPONENT);
		promotion.put(PATTERN_DOUBLE_WITH_EXPONENT + "---" + PATTERN_SIGNED_DOUBLE_WITH_EXPONENT, PATTERN_SIGNED_DOUBLE_WITH_EXPONENT);

		promotion.put(PATTERN_SIGNED_DOUBLE_WITH_EXPONENT + "---" + PATTERN_LONG, PATTERN_SIGNED_DOUBLE_WITH_EXPONENT);
		promotion.put(PATTERN_SIGNED_DOUBLE_WITH_EXPONENT + "---" + PATTERN_SIGNED_LONG, PATTERN_SIGNED_DOUBLE_WITH_EXPONENT);
		promotion.put(PATTERN_SIGNED_DOUBLE_WITH_EXPONENT + "---" + PATTERN_DOUBLE, PATTERN_SIGNED_DOUBLE_WITH_EXPONENT);
		promotion.put(PATTERN_SIGNED_DOUBLE_WITH_EXPONENT + "---" + PATTERN_SIGNED_DOUBLE, PATTERN_SIGNED_DOUBLE_WITH_EXPONENT);
		promotion.put(PATTERN_SIGNED_DOUBLE_WITH_EXPONENT + "---" + PATTERN_DOUBLE_WITH_EXPONENT, PATTERN_SIGNED_DOUBLE_WITH_EXPONENT);

		try {
			BufferedReader reader = null;

			reader = new BufferedReader(new InputStreamReader(TextAnalyzer.class.getResourceAsStream("/reference/us_zips.csv")));
			String line = null;
			while ((line = reader.readLine()) != null) {
				zips.add(line);
			}

			reader = new BufferedReader(new InputStreamReader(TextAnalyzer.class.getResourceAsStream("/reference/us_states.csv")));
			while ((line = reader.readLine()) != null) {
				usStates.add(line);
			}

			reader = new BufferedReader(new InputStreamReader(TextAnalyzer.class.getResourceAsStream("/reference/ca_provinces.csv")));
			while ((line = reader.readLine()) != null) {
				caProvinces.add(line);
			}

			reader = new BufferedReader(new InputStreamReader(TextAnalyzer.class.getResourceAsStream("/reference/countries.csv")));
			while ((line = reader.readLine()) != null) {
				countries.add(line);
			}

			reader = new BufferedReader(new InputStreamReader(TextAnalyzer.class.getResourceAsStream("/reference/address_markers.csv")));
			while ((line = reader.readLine()) != null) {
				addressMarkers.add(line);
			}

			gender.add("MALE");
			gender.add("FEMALE");

			// Setup the Monthly abbreviations
			final String[] shortMonths = new DateFormatSymbols().getShortMonths();
			for (final String shortMonth : shortMonths) {
				monthAbbr.add(shortMonth.toUpperCase(Locale.ROOT));
			}
		}
		catch (IOException e) {
			throw new InternalErrorException("Failed to initialize", e);
		}
	}

	/**
	 * Construct a Text Analyzer for the named data stream with the supplied DateResolutionMode.
	 *
	 * @param name The name of the data stream (e.g. the column of the CSV file)
	 * @param resolutionMode Determines what to do when the Date field is ambiguous (i.e. we cannot determine which
	 *   of the fields is the day or the month.  If resolutionMode is DayFirst, then assume day is first, if resolutionMode is
	 *   MonthFirst then assume month is first, if it is None then the pattern returned may have '?' in to represent
	 *   this ambiguity.
	 * @throws IOException
	 *             If an internal error occurred.
	 */
	public TextAnalyzer(final String name, final DateResolutionMode resolutionMode) throws IOException {
		this.name = name;
		this.resolutionMode = resolutionMode;
		final DecimalFormatSymbols formatSymbols = new DecimalFormatSymbols();
		decimalSeparator = formatSymbols.getDecimalSeparator();
		monetaryDecimalSeparator = formatSymbols.getMonetaryDecimalSeparator();
		groupingSeparator = formatSymbols.getGroupingSeparator();
		minusSign = formatSymbols.getMinusSign();
	}

	/**
	 * Construct a Text Analyzer for the named data stream.  Note: The resolution mode will be 'None'.
	 *
	 * @param name The name of the data stream (e.g. the column of the CSV file)
	 *
	 * @throws IOException
	 *             If an internal error occurred.
	 */
	public TextAnalyzer(final String name) throws IOException {
		this(name, DateResolutionMode.None);
	}

	/**
	 * Construct an anonymous Text Analyzer for a data stream.  Note: The resolution mode will be 'None'.
	 *
	 * @throws IOException
	 *             If an internal error occurred.
	 */
	public TextAnalyzer() throws IOException {
		this("anonymous", DateResolutionMode.None);
	}

	/**
	 * Indicate whether to collect statistics or not.
	 *
     * @param collectStatistics
     *            A boolean indicating the desired state
	 * @return The previous value of this parameter.
	 */
	public boolean setCollectStatistics(final boolean collectStatistics) {
		if (trainingStarted)
			throw new IllegalArgumentException("Cannot adjust statistics collection once training has started");

		final boolean ret = collectStatistics;
		this.collectStatistics = collectStatistics;
		return ret;
	}

	/**
	 * Indicate whether to collect statistics or not.
	 *
	 * @return Whether Statistics collection is enabled.
	 */
	public boolean getCollectStatistics() {
		return collectStatistics;
	}

    /**
     * Set the number of Samples to collect before attempting to determine the
     * type. Note: It is not possible to change the Sample Size once training
     * has started.
     * Indicate whether to collect statistics or not.
     *
     * @param samples
     *            The number of samples to collect
     * @return The previous value of this parameter.
	*/
	public int setSampleSize(final int samples) {
		if (trainingStarted)
			throw new IllegalArgumentException("Cannot change sample size once training has started");
		if (samples < SAMPLE_DEFAULT)
			throw new IllegalArgumentException("Cannot set sample size below " + SAMPLE_DEFAULT);

		final int ret = samples;
		this.samples = samples;

		// Never want the Sample Size to be greater than the Reflection point
		if (samples >= reflectionSamples)
			reflectionSamples = samples + 1;
		return ret;
	}

	/**
	 * Get the number of Samples used to collect before attempting to determine
	 * the type.
	 *
	 * @return The current size of the sample window.
	 */
	public int getSampleSize() {
		return samples;
	}

	/**
	 * Get the number of Samples required before we will 'reflect' on the analysis and
	 * potentially change determination.
	 *
	 * @return The current size of the sample window.
	 */
	public int getReflectionSampleSize() {
		return reflectionSamples;
	}

	/**
	 * Set the maximum cardinality that will be tracked. Note: It is not
	 * possible to change the cardinality once training has started.
	 *
	 * @param newCardinality
	 *            The maximum Cardinality that will be tracked (0 implies no
	 *            tracking)
	 * @return The previous value of this parameter.
	 */
	public int setMaxCardinality(final int newCardinality) {
		if (trainingStarted)
			throw new IllegalArgumentException("Cannot change maxCardinality once training has started");
		if (newCardinality < 0)
			throw new IllegalArgumentException("Invalid value for maxCardinality " + newCardinality);

		final int ret = maxCardinality;
		maxCardinality = newCardinality;
		return ret;
	}

	/**
	 * Get the maximum cardinality that will be tracked. See
	 * {@link #setMaxCardinality(int) setMaxCardinality()} method.
	 *
	 * @return The maximum cardinality.
	 */
	public int getMaxCardinality() {
		return maxCardinality;
	}

	/**
	 * Set the maximum number of outliers that will be tracked. Note: It is not
	 * possible to change the outlier count once training has started.
	 *
	 * @param newMaxOutliers
	 *            The maximum number of outliers that will be tracked (0 implies
	 *            no tracking)
	 * @return The previous value of this parameter.
	 */
	public int setMaxOutliers(final int newMaxOutliers) {
		if (trainingStarted)
			throw new IllegalArgumentException("Cannot change outlier count once training has started");
		if (newMaxOutliers < 0)
			throw new IllegalArgumentException("Invalid value for outlier count " + newMaxOutliers);

		final int ret = maxOutliers;
		maxOutliers = newMaxOutliers;
		return ret;
	}

	/**
	 * Get the maximum number of outliers that will be tracked. See
	 * {@link #setMaxOutliers(int) setMaxOutliers()} method.
	 *
	 * @return The maximum cardinality.
	 */
	public int getMaxOutliers() {
		return maxOutliers;
	}

	private boolean trackLong(final String rawInput, final boolean register) {
		final String input = rawInput.trim();

		// Track String facts - just in case we end up backing out.
		if (minString == null || minString.compareTo(input) > 0) {
			minString = input;
		}
		if (maxString == null || maxString.compareTo(input) < 0) {
			maxString = input;
		}

		long l;

		try {
			l = Long.parseLong(input);
		} catch (NumberFormatException e) {
			return false;
		}

		if (register) {
			totalLongs++;
			if (input.charAt(0) == '0')
				totalLeadingZeros++;
		}

		if (l < 0)
			negativeLongs++;

		if (collectStatistics) {
			if (l < minLong) {
				minLong = l;
			}
			if (l > maxLong) {
				maxLong = l;
			}
			final int digits = l < 0 ? input.length() - 1 : input.length();
			if (digits < minTrimmedLength)
				minTrimmedLength = digits;
			if (digits > maxTrimmedLength)
				maxTrimmedLength = digits;

			sumBI = sumBI.add(BigInteger.valueOf(l));
		}

		if ("ZIP".equals(matchPatternInfo.typeQualifier)) {
			return zips.contains(rawInput);
		}

		return true;
	}

	private boolean trackBoolean(final String input) {
		final String trimmedLower = input.trim().toLowerCase(Locale.ROOT);

		final boolean isTrue = "true".equals(trimmedLower) || "yes".equals(trimmedLower);
		final boolean isFalse = !isTrue && ("false".equals(trimmedLower) || "no".equals(trimmedLower));

		if (isTrue) {
			if (minBoolean == null)
				minBoolean = trimmedLower;
			if (maxBoolean == null || "false".equals(maxBoolean) || "no".equals(maxBoolean))
				maxBoolean = trimmedLower;
		} else if (isFalse) {
			if (maxBoolean == null)
				maxBoolean = trimmedLower;
			if (minBoolean == null || "true".equals(minBoolean) || "yes".equals(maxBoolean))
				minBoolean = trimmedLower;
		}

		return isTrue || isFalse;
	}

	private boolean trackString(final String input, final boolean register) {
		String cleaned = input;

		if (matchPatternInfo.typeQualifier == null) {
			if (matchPatternInfo.isAlphabetic() && !cleaned.trim().chars().allMatch(Character::isAlphabetic))
				return false;
		}
		else {
			if ("EMAIL".equals(matchPatternInfo.typeQualifier)) {
				// Address lists commonly have ;'s as separators as opposed to the
				// ','
				if (cleaned.indexOf(';') != -1)
					cleaned = cleaned.replaceAll(";", ",");
				try {
					return InternetAddress.parse(cleaned).length != 0;
				} catch (AddressException e) {
					return false;
				}
			} else if ("URL".equals(matchPatternInfo.typeQualifier)) {
				try {
					final URL url = new URL(cleaned);
					url.toURI();
					return true;
				} catch (MalformedURLException | URISyntaxException exception) {
					return false;
				}
			} else if ("ADDRESS".equals(matchPatternInfo.typeQualifier)) {
				int spaceIndex = cleaned.lastIndexOf(' ');
				if (spaceIndex == -1 || !addressMarkers.contains(input.substring(spaceIndex + 1).toUpperCase(Locale.ROOT)))
					return false;
			}

		}

		return updateStats(cleaned);
	}

	private boolean updateStats(final String cleaned) {
		final int len = cleaned.trim().length();

		if (matchPatternInfo.minLength != -1 && len < matchPatternInfo.minLength)
			return false;
		if (matchPatternInfo.maxLength != -1 && len > matchPatternInfo.maxLength)
			return false;

		if (minString == null || minString.compareTo(cleaned) > 0) {
			minString = cleaned;
		}
		if (maxString == null || maxString.compareTo(cleaned) < 0) {
			maxString = cleaned;
		}


		if (len < minTrimmedLength)
			minTrimmedLength = len;
		if (len > maxTrimmedLength)
			maxTrimmedLength = len;

		return true;
	}

	private boolean trackDouble(final String input) {
		double d;

		try {
			d = Double.parseDouble(input.trim());
		} catch (NumberFormatException e) {
			return false;
		}

		// If it is NaN/Infinity then we are all done
		if (Double.isNaN(d) || Double.isInfinite(d))
			return true;

		if (d < 0.0)
			negativeDoubles++;

		if (collectStatistics) {
			if (d < minDouble) {
				minDouble = d;
			}
			if (d > maxDouble) {
				maxDouble = d;
			}

			sumBD = sumBD.add(BigDecimal.valueOf(d));
		}

		return true;
	}

	private void trackDateTime(final String dateFormat, final String input) throws DateTimeParseException {
		final DateTimeParserResult result = DateTimeParserResult.asResult(dateFormat, resolutionMode);
		if (result == null) {
			throw new InternalErrorException("NULL result for " + dateFormat);
		}

		// Grab the cached Formatter
		final String formatString = result.getFormatString();
		DateTimeFormatter formatter = formatterCache.get(formatString);
		if (formatter == null) {
			formatter = DateTimeFormatter.ofPattern(formatString);
			formatterCache.put(formatString, formatter);
		}

		final String trimmed = input.trim();

		switch (result.getType()) {
		case LOCALTIME:
			final LocalTime localTime = LocalTime.parse(trimmed, formatter);
			if (collectStatistics) {
				if (minLocalTime == null || localTime.compareTo(minLocalTime) < 0)
					minLocalTime = localTime;
				if (maxLocalTime == null || localTime.compareTo(maxLocalTime) > 0)
					maxLocalTime = localTime;
			}
			break;

		case LOCALDATE:
			final LocalDate localDate = LocalDate.parse(trimmed, formatter);
			if (collectStatistics) {
				if (minLocalDate == null || localDate.compareTo(minLocalDate) < 0)
					minLocalDate = localDate;
				if (maxLocalDate == null || localDate.compareTo(maxLocalDate) > 0)
					maxLocalDate = localDate;
			}
			break;

		case LOCALDATETIME:
			final LocalDateTime localDateTime = LocalDateTime.parse(trimmed, formatter);
			if (collectStatistics) {
				if (minLocalDateTime == null || localDateTime.compareTo(minLocalDateTime) < 0)
					minLocalDateTime = localDateTime;
				if (maxLocalDateTime == null || localDateTime.compareTo(maxLocalDateTime) > 0)
					maxLocalDateTime = localDateTime;
			}
			break;

		case ZONEDDATETIME:
			final ZonedDateTime zonedDataTime = ZonedDateTime.parse(trimmed, formatter);
			if (collectStatistics) {
				if (minZonedDateTime == null || zonedDataTime.compareTo(minZonedDateTime) < 0)
					minZonedDateTime = zonedDataTime;
				if (maxZonedDateTime == null || zonedDataTime.compareTo(maxZonedDateTime) > 0)
					maxZonedDateTime = zonedDataTime;
			}
			break;

		case OFFSETDATETIME:
			final OffsetDateTime offsetDateTime = OffsetDateTime.parse(trimmed, formatter);
			if (collectStatistics) {
				if (minOffsetDateTime == null || offsetDateTime.compareTo(minOffsetDateTime) < 0)
					minOffsetDateTime = offsetDateTime;
				if (maxOffsetDateTime == null || offsetDateTime.compareTo(maxOffsetDateTime) > 0)
					maxOffsetDateTime = offsetDateTime;
			}
			break;

		default:
			throw new InternalErrorException("Expected Date/Time type.");
		}
	}

	/**
	 * Train is the core entry point used to supply input to the Text Analyzer.
	 *
	 * @param rawInput
	 *            The raw input as a String
	 * @return A boolean indicating if the resultant type is currently known.
	 */
	public boolean train(final String rawInput) {
		// Initialize if we have not already done so
		if (!trainingStarted) {
			trainingStarted = true;
			raw = new ArrayList<String>(samples);
			levels[0] = new ArrayList<StringBuilder>(samples);
			levels[1] = new ArrayList<StringBuilder>(samples);
			levels[2] = new ArrayList<StringBuilder>(samples);
		}

		sampleCount++;

		if (rawInput == null) {
			nullCount++;
			return matchType != null;
		}

		final String input = rawInput.trim();

		final int length = input.length();

		if (length == 0) {
			blankCount++;
			trackLength(rawInput);
			return matchType != null;
		}

		trackResult(rawInput);

		// If we have determined a type, no need to further train
		if (matchType != null)
			return true;

		raw.add(rawInput);

		final StringBuilder l0 = new StringBuilder(length);

		// Walk the string
		boolean numericSigned = false;
		int numericDecimalSeparators = 0;
		boolean couldBeNumeric = true;
		int possibleExponentSeen = -1;
		int digitsSeen = 0;
		int alphasSeen = 0;
		int commas = 0;
		int semicolons = 0;
		int atSigns = 0;
		int spaceIndex = -1;

		for (int i = 0; i < length; i++) {
			char ch = input.charAt(i);
			if (i == 0 && ch == minusSign) {
				numericSigned = true;
			} else if (Character.isDigit(ch)) {
				l0.append('d');
				digitsSeen++;
			} else if (ch == decimalSeparator) {
				l0.append('D');
				numericDecimalSeparators++;
				if (decimalSeparator == ',')
					commas++;
			} else if (ch == groupingSeparator) {
				l0.append('G');
				if (groupingSeparator == ',')
					commas++;
			} else if (Character.isAlphabetic(ch)) {
				l0.append('a');
				alphasSeen++;
				if (couldBeNumeric && (ch == 'e' || ch == 'E')) {
					if (possibleExponentSeen != -1 || i < 3 || i + 1 >= length)
						couldBeNumeric = false;
					else
						possibleExponentSeen = i;
				}
				else
					couldBeNumeric = false;
			} else {
				if (ch == '@')
					atSigns++;
				else if (ch == ',')
					commas++;
				else if (ch == ';')
					semicolons++;
				else if (ch == ' ')
					spaceIndex = i;
				l0.append(ch);
				// If the last character was an exponentiation symbol then this better be a sign if it is going to be numeric
				if (possibleExponentSeen != -1 && possibleExponentSeen == i - 1) {
					if (ch != minusSign && ch != '+')
						couldBeNumeric = false;
				}
				else
					couldBeNumeric = false;
			}
		}

		final StringBuilder compressedl0 = new StringBuilder(length);
		if (alphasSeen != 0 && digitsSeen != 0 && alphasSeen + digitsSeen == length) {
			compressedl0.append(PATTERN_ALPHANUMERIC).append('{').append(String.valueOf(length)).append('}');

		} else if ("true".equalsIgnoreCase(input) || "false".equalsIgnoreCase(input)) {
			compressedl0.append(PATTERN_BOOLEAN);
		} else if ("yes".equalsIgnoreCase(input) || "no".equalsIgnoreCase(input)) {
			compressedl0.append(PATTERN_YESNO);
		} else {
			// Walk the new level0 to create the new level1
			final String l0withSentinel = l0.toString() + "|";
			char last = l0withSentinel.charAt(0);
			int repetitions = 1;
			for (int i = 1; i < l0withSentinel.length(); i++) {
				final char ch = l0withSentinel.charAt(i);
				if (ch == last) {
					repetitions++;
				} else {
					if (last == 'd' || last == 'a') {
						compressedl0.append(last == 'd' ? "\\d" : PATTERN_ALPHA);
						compressedl0.append('{').append(String.valueOf(repetitions)).append('}');
					} else {
						for (int j = 0; j < repetitions; j++) {
							compressedl0.append(last);
						}
					}
					last = ch;
					repetitions = 1;
				}
			}
		}
		levels[0].add(compressedl0);

		if (DateTimeParser.determineFormatString(input, resolutionMode) != null)
			possibleDateTime++;
		if (atSigns - 1 == commas || atSigns - 1 == semicolons)
			possibleEmails++;
		if (length == 5 && digitsSeen == 5)
			possibleZips++;
		if (input.indexOf("://") != -1)
			possibleURLs++;
		if (spaceIndex != -1 && addressMarkers.contains(input.substring(spaceIndex + 1, length).toUpperCase(Locale.ROOT)))
			possibleAddresses++;

		// Create the level 1 and 2
		if (digitsSeen > 0 && couldBeNumeric && numericDecimalSeparators <= 1) {
			StringBuilder l1 = null;
			StringBuilder l2 = null;
			if (numericDecimalSeparators == 1) {
				if (possibleExponentSeen == -1) {
					l1 = new StringBuilder(numericSigned ? PATTERN_SIGNED_DOUBLE : PATTERN_DOUBLE);
					l2 = new StringBuilder(PATTERN_SIGNED_DOUBLE);
				}
				else {
					l1 = new StringBuilder(numericSigned ? PATTERN_SIGNED_DOUBLE_WITH_EXPONENT : PATTERN_DOUBLE_WITH_EXPONENT);
					l2 = new StringBuilder(PATTERN_SIGNED_DOUBLE_WITH_EXPONENT);
				}
			}
			else {
				l1 = new StringBuilder(numericSigned ? PATTERN_SIGNED_LONG : PATTERN_LONG);
				l2 = new StringBuilder(PATTERN_SIGNED_LONG);
			}
			levels[1].add(l1);
			levels[2].add(l2);
		} else {
			// Fast version of replaceAll("\\{\\d*\\}", "+"), e.g. replace \d{5} with \d+
			final StringBuilder collapsed = new StringBuilder(compressedl0);
			for (int i = 0; i < collapsed.length(); i++) {
				if (collapsed.charAt(i) == '{' && Character.isDigit(collapsed.charAt(i + 1))) {
					final int start = i++;
					while (collapsed.charAt(++i) != '}')
						/* EMPTY */;
					collapsed.replace(start, i + 1, "+");
				}
			}

			// Level 1 is the collapsed version e.g. convert \d{4}-\d{2}-\d{2] to
			// \d+-\d+-\d+
			final PatternInfo found = patternInfo.get(compressedl0.toString());
			if (found != null && found.generalPattern != null) {
				levels[1].add(new StringBuilder(found.generalPattern));
				levels[2].add(new StringBuilder(collapsed));
			} else {
				levels[1].add(new StringBuilder(collapsed));
				levels[2].add(new StringBuilder(PATTERN_ANY_VARIABLE));
			}

		}

		return matchType != null;
	}

	private Map.Entry<String, Integer> getBest(final int levelIndex) {
		final List<StringBuilder> level = levels[levelIndex];
		if (level.isEmpty())
			return null;

		Map<String, Integer> frequency = new HashMap<String, Integer>();

		// Calculate the frequency of every element
		for (final StringBuilder s : level) {
			final String key = s.toString();
			final Integer seen = frequency.get(key);
			if (seen == null) {
				frequency.put(key, 1);
			} else {
				frequency.put(key, seen + 1);
			}
		}

		// Sort the results
		frequency = Utils.sortByValue(frequency);

		// Grab the best and the second best based on frequency
		Map.Entry<String, Integer> best = null;
		Map.Entry<String, Integer> secondBest = null;
		Map.Entry<String, Integer> thirdBest = null;
		PatternInfo bestPattern = null;
		PatternInfo secondBestPattern = null;
		PatternInfo thirdBestPattern = null;
		String newKey = null;

		// Handle numeric promotion
		for (final Map.Entry<String, Integer> entry : frequency.entrySet()) {

			if (best == null) {
				best = entry;
				bestPattern = patternInfo.get(best.getKey());
			}
			else if (secondBest == null) {
				secondBest = entry;
				secondBestPattern = patternInfo.get(secondBest.getKey());
				if (levelIndex != 0 && bestPattern != null && secondBestPattern != null &&
						bestPattern.isNumeric() && secondBestPattern.isNumeric()) {
					newKey = promotion.get(bestPattern.regexp + "---" + secondBestPattern.regexp);
					best = new AbstractMap.SimpleEntry<String, Integer>(newKey, best.getValue() + secondBest.getValue());
				}
			}
			else if (thirdBest == null) {
				thirdBest = entry;
				thirdBestPattern = patternInfo.get(thirdBest.getKey());
				if (levelIndex != 0 && bestPattern != null && secondBestPattern != null && thirdBestPattern != null &&
						bestPattern.isNumeric() && secondBestPattern.isNumeric() && thirdBestPattern.isNumeric()) {
					newKey = promotion.get(newKey + "---" + thirdBestPattern.regexp);
					best = new AbstractMap.SimpleEntry<String, Integer>(newKey, best.getValue() + thirdBest.getValue());
				}
			}
		}

		if (bestPattern != null && secondBestPattern != null && PatternInfo.Type.STRING.equals(bestPattern.type)) {
			// Promote anything to STRING
			best = new AbstractMap.SimpleEntry<String, Integer>(best.getKey(),
					best.getValue() + secondBest.getValue());
		}

		return best;
	}

	/**
	 * This is the core routine for determining the type of the field. It is
	 * responsible for setting: - matchPattern - matchPatternInfo - matchCount -
	 * type
	 */
	private void determineType() {
		// If we have fewer than 6 samples do not even pretend
		if (sampleCount == 0) {
			matchPattern = PATTERN_ANY_VARIABLE;
			matchPatternInfo = patternInfo.get(matchPattern);
			matchType = matchPatternInfo.type;
			return;
		}

		int level0value = 0, level1value = 0, level2value = 0;
		String level0pattern = null, level1pattern = null, level2pattern = null;
		PatternInfo level0patternInfo = null, level1patternInfo = null, level2patternInfo = null;
		String pattern = null;
		final Map.Entry<String, Integer> level0 = getBest(0);
		final Map.Entry<String, Integer> level1 = getBest(1);
		final Map.Entry<String, Integer> level2 = getBest(2);
		Map.Entry<String, Integer> best = level0;

		if (level0 != null) {
			level0pattern = level0.getKey();
			level0value = level0.getValue();
			level0patternInfo = patternInfo.get(level0pattern);
		}
		if (level1 != null) {
			level1pattern = level1.getKey();
			level1value = level1.getValue();
			level1patternInfo = patternInfo.get(level1pattern);
		}
		if (level2 != null) {
			level2pattern = level2.getKey();
			level2value = level2.getValue();
			level2patternInfo = patternInfo.get(level2pattern);
		}

		if (best != null) {
			pattern = level0pattern;
			matchPatternInfo = level0patternInfo;

			// Take any level 1 with something we recognize or a better count
			if (level1 != null && (level0patternInfo == null || level1value > level0value)) {
				best = level1;
				pattern = level1pattern;
				matchPatternInfo = level1patternInfo;
			}

			// Take a level 2 if
			// - we have something we recognize (and we had nothing)
			// - we have the same key but a better count
			// - we have different keys but same type (signed vs. not-signed)
			// - we have different keys, different types but an improvement of
			// at least 10%
			if (level2 != null && (matchPatternInfo == null
					|| (best.getKey().equals(level2pattern) && level2value > best.getValue())
					|| (!best.getKey().equals(level2pattern) && level2patternInfo != null
							&& matchPatternInfo.type.equals(level2patternInfo.type)
							&& level2.getValue() > best.getValue())
					|| (!best.getKey().equals(level2pattern) && level2.getValue() > best.getValue() + samples / 10))) {
				best = level2;
				pattern = level2pattern;
				matchPatternInfo = level2patternInfo;
			}

			matchType = matchPatternInfo.type;

			if (possibleDateTime == raw.size()) {
				final DateTimeParser det = new DateTimeParser(resolutionMode);
				for (final String sample : raw)
					det.train(sample);

				final DateTimeParserResult result = det.getResult();
				final String formatString = result.getFormatString();
				matchPatternInfo = new PatternInfo(result.getRegExp(), result.getType(), formatString, -1, -1, null,
						formatString);
				matchType = matchPatternInfo.type;
				pattern = matchPatternInfo.regexp;
			}

			// Do we have a set of possible emails?
			if (possibleEmails == raw.size()) {
				final PatternInfo save = matchPatternInfo;
				matchPatternInfo = new PatternInfo(matchPattern, PatternInfo.Type.STRING, "EMAIL", -1, -1, null, null);
				int emails = 0;
				for (final String sample : raw) {
					if (trackString(sample, false))
						emails++;
				}
				// if at least 90% of them looked like a genuine email then
				// stay with email, otherwise back out to simple String
				if (emails < .9 * raw.size())
					matchPatternInfo = save;
			}

			// Do we have a set of possible URLs?
			if (possibleURLs == raw.size()) {
				final PatternInfo save = matchPatternInfo;
				matchPatternInfo = new PatternInfo(matchPattern, PatternInfo.Type.STRING, "URL", -1, -1, null, null);
				int countURLs = 0;
				for (final String sample : raw)
					if (trackString(sample, false))
						countURLs++;
				// if at least 90% of them looked like a genuine URL then
				// stay with URL, otherwise back out to simple String
				if (countURLs < .9 * raw.size())
					matchPatternInfo = save;
			}

			// Do we have a set of possible zip codes?
			if (possibleZips == raw.size()) {
				final PatternInfo save = matchPatternInfo;
				matchPatternInfo = typeInfo.get(PatternInfo.Type.LONG.toString() + "." + "ZIP");
				pattern = matchPatternInfo.regexp;

				int zipCount = 0;
				for (final String sample : raw)
					if (trackLong(sample, false))
						zipCount++;
				// if at least 90% of them looked like a genuine zip
				// then stay with zip, otherwise back out to simple Long
				if (zipCount < .9 * raw.size()) {
					matchPatternInfo = save;
					pattern = save.regexp;
				}
				matchType = matchPatternInfo.type;
			}

			// Do we have a set of possible Addresses?
			if (possibleAddresses >= .90 * raw.size()) {
				matchPatternInfo = new PatternInfo(matchPattern, PatternInfo.Type.STRING, "ADDRESS", -1, -1, null, null);
			}

			for (final String sample : raw)
				trackResult(sample);

			matchCount = best.getValue();
			matchPattern = pattern;
		}
	}

	private void addValid(final String input) {
		final Integer seen = cardinality.get(input);
		if (seen == null) {
			if (cardinality.size() < maxCardinality)
				cardinality.put(input, 1);
		} else
			cardinality.put(input, seen + 1);
	}

	private int outlier(final String input) {
		final Integer seen = outliers.get(input);
		if (seen == null) {
			if (outliers.size() < maxOutliers)
				outliers.put(input, 1);
		} else {
			outliers.put(input, seen + 1);
		}

		return outliers.size();
	}

	private boolean conditionalBackoutToPattern(final long realSamples, PatternInfo current) {
		int alphas = 0;
		int digits = 0;
		int spaces = 0;
		int other = 0;
		int doubles = 0;
		boolean negative = false;
		boolean exponent = false;

		// Sweep the current outliers
		for (final Map.Entry<String, Integer> entry : outliers.entrySet()) {
			String key = entry.getKey();
			Integer value = entry.getValue();
			if (PatternInfo.Type.LONG.equals(current.type) && trackDouble(key)) {
				doubles++;
				if (!negative)
					negative = key.charAt(0) == '-';
				if (!exponent)
					exponent = key.indexOf('e') != -1 || key.indexOf('E') != -1;
			}
			boolean foundAlpha = false;
			boolean foundDigit = false;
			boolean foundSpace = false;
			boolean foundOther = false;
			for (int c : key.codePoints().toArray()) {
			    if (Character.isAlphabetic(c))
			    	foundAlpha = true;
			    else if (Character.isDigit(c))
			    	foundDigit = true;
			    else if (Character.isWhitespace(c))
			    	foundSpace = true;
			    else
			    	foundOther = true;
			}
			if (foundAlpha)
				alphas += value;
			if (foundDigit)
				digits += value;
			if (foundSpace)
				spaces += value;
			if (foundOther)
				other += value;
		}

		int badCharacters = current.isAlphabetic() ? digits : alphas;
		if (badCharacters != 0 && spaces == 0 && other == 0 && current.isAlphabetic()) {
			if (outliers.size() == maxOutliers || digits > .01 * realSamples) {
				backoutToPattern(realSamples, current.regexp.replace("Alpha", "Alnum"));
				return true;
			}
		}
		else if (badCharacters != 0 && spaces == 0 && other == 0 && PatternInfo.Type.LONG.equals(current.type)) {
			if (outliers.size() == maxOutliers || alphas > .01 * realSamples) {
				backoutToPattern(realSamples, "\\p{Alnum}" + Utils.lengthQualifier(minRawLength, maxRawLength));
				return true;
			}
		}
		else if (outliers.size() == doubles && PatternInfo.Type.LONG.equals(current.type)) {
			backoutToPattern(realSamples, doublePattern(negative, exponent));
			return true;
		}
		else {
			if (outliers.size() == maxOutliers || (badCharacters + spaces + other) > .01 * realSamples) {
				backoutToPattern(realSamples, PATTERN_ANY_VARIABLE);
				return true;
			}
		}

		return false;
	}

	private String doublePattern(boolean negative, boolean exponent) {
		if (exponent)
			return negative ? PATTERN_SIGNED_DOUBLE_WITH_EXPONENT : PATTERN_DOUBLE_WITH_EXPONENT;

		return negative ? PATTERN_SIGNED_DOUBLE : PATTERN_DOUBLE;
	}

	private void backoutToPattern(final long realSamples, String newPattern) {
		matchPattern = newPattern;
		matchCount = realSamples;
		matchType = PatternInfo.Type.STRING;
		matchPatternInfo = patternInfo.get(matchPattern);

		// If it is not one of our known types then construct a suitable PatternInfo
		if (matchPatternInfo == null)
			matchPatternInfo = new PatternInfo(matchPattern, matchType, null, -1, -1, null, null);

		// All outliers are now part of the cardinality set and there are now no outliers
		cardinality.putAll(outliers);

		// Need to update stats to reflect any outliers we previously ignored
		for (final String key : outliers.keySet()) {
			updateStats(key);
		}

		outliers.clear();
	}

	private void backoutZip(final long realSamples) {
		if (totalLongs > .95 * realSamples) {
			matchPattern = PATTERN_LONG;
			matchCount = totalLongs;

			final Map<String, Integer> outliersCopy = new HashMap<String, Integer>(outliers);
			// Sweep the current outliers and check they are part of the set
			for (final Map.Entry<String, Integer> entry : outliersCopy.entrySet()) {
				boolean isLong = true;
				try {
					Long.parseLong(entry.getKey());
				} catch (NumberFormatException e) {
					isLong = false;
				}

				if (isLong) {
					if (cardinality.size() < maxCardinality)
						cardinality.put(entry.getKey(), entry.getValue());
					outliers.remove(entry.getKey(), entry.getValue());
				}
			}

			matchPatternInfo = patternInfo.get(matchPattern);
		} else {
			backoutToPattern(realSamples, PATTERN_ANY_VARIABLE);
		}
	}

	private void trackLength(final String input) {
		// We always want to track basic facts for the field
		final int length = input.length();

		if (length != 0 && length < minRawLength)
			minRawLength = length;
		if (length > maxRawLength)
			maxRawLength = length;
	}

	private void trackTrimmedLengthAndWhiteSpace(final String input) {
		// We always want to track basic facts for the field
		final int length = input.length();

		// Determine if there is leading or trailing White space (if not done previously)
		if (length != 0 && (!leadingWhiteSpace || !trailingWhiteSpace)) {
			leadingWhiteSpace = Character.isSpaceChar(input.charAt(0));
			if (length >= 2 && !trailingWhiteSpace) {
				boolean maybe = Character.isSpaceChar(input.charAt(length - 1));
				if (maybe) {
					int i = length - 2;
					while (i >= 0) {
						if (!Character.isSpaceChar(input.charAt(i))) {
							trailingWhiteSpace = true;
							break;
						}
						i--;
					}
				}
			}
		}

		final int trimmedLength = input.trim().length();

		if (trimmedLength < minRawLength)
			minRawLength = trimmedLength;
		if (trimmedLength > maxRawLength)
			maxRawLength = trimmedLength;
	}

	/**
	 * Track the supplied raw input, once we have enough samples attempt to determine the type.
	 * @param input The raw input string
	 */
	private void trackResult(final String input) {

		trackLength(input);

		// If the cache is full and we have not determined a type compute one
		if (matchType == null && sampleCount - (nullCount + blankCount) > samples) {
			determineType();
		}

		if (matchType == null) {
			return;
		}

		final long realSamples = sampleCount - (nullCount + blankCount);
		boolean valid = false;

		switch (matchType) {
		case BOOLEAN:
			if (trackBoolean(input)) {
				matchCount++;
				addValid(input);
				valid = true;
			}
			break;

		case LONG:
			if (trackLong(input, true)) {
				matchCount++;
				addValid(input);
				valid = true;
			}
			else {
				// Do a sanity check once we have enough samples
				if (realSamples == reflectionSamples && (double) matchCount / realSamples < 0.9 && "ZIP".equals(matchPatternInfo.typeQualifier))
					backoutZip(realSamples);
			}
			break;

		case DOUBLE:
			if (trackDouble(input)) {
				matchCount++;
				addValid(input);
				valid = true;
			}
			break;

		case STRING:
			if (trackString(input, true)) {
				matchCount++;
				addValid(input);
				valid = true;
			}
			else {
				if (realSamples == reflectionSamples && (double) matchCount / realSamples < 0.95 &&
						("URL".equals(matchPatternInfo.typeQualifier) || "EMAIL".equals(matchPatternInfo.typeQualifier))) {
					backoutToPattern(realSamples, PATTERN_ANY_VARIABLE);
					valid = true;
				}
			}
			break;

		case LOCALDATE:
		case LOCALTIME:
		case LOCALDATETIME:
		case OFFSETDATETIME:
		case ZONEDDATETIME:
			try {
				trackDateTime(matchPatternInfo.format, input);
				matchCount++;
				addValid(input);
				valid = true;
			}
			catch (DateTimeParseException reale) {
				DateTimeParserResult result = DateTimeParserResult.asResult(matchPatternInfo.format, resolutionMode);
				try {
					result.parse(input);
				}
				catch (DateTimeParseException e) {
					if ("Insufficient digits in input (d)".equals(e.getMessage()) || "Insufficient digits in input (M)".equals(e.getMessage())) {
						try {
							final String formatString = new StringBuffer(matchPatternInfo.format).deleteCharAt(e.getErrorIndex()).toString();
							result = DateTimeParserResult.asResult(formatString, resolutionMode);
							matchPatternInfo = new PatternInfo(result.getRegExp(), matchPatternInfo.type, formatString, -1, -1, null,
									formatString);

							trackDateTime(matchPatternInfo.format, input);
							matchCount++;
							addValid(input);
							valid = true;
						}
						catch (DateTimeParseException e2) {
							// Ignore and record as outlier below
						}
					}
				}
			}
			break;
		}

		if (valid)
			trackTrimmedLengthAndWhiteSpace(input);
		else {
			if (matchType == PatternInfo.Type.STRING && matchPatternInfo.typeQualifier == null &&
					matchPatternInfo.isAlphabetic() && outliers.size() == maxOutliers) {
				// Need to evaluate if we got this wrong
				conditionalBackoutToPattern(realSamples, matchPatternInfo);
			}
			else {
				outlier(input);
			}
		}
	}

	/**
	 * Parse a String regexp to determine length.
	 *
	 * @param input
	 *            String input that must be either a variable length string
	 *            (\\p{Alpha}+) or fixed length, e.g. \\p{Alpha}{3}
	 * @return The length of the input string or -1 if length is variable
	 */
	private int determineLength(final String input) {
		final int len = input.length();
		if (len > 0 && (input.charAt(len - 1) == '+' || input.charAt(len - 1) == '*') || input.indexOf(',') != -1)
			return -1;
		final int lengthInformation = input.lastIndexOf('{');
		if (lengthInformation == -1)
			return -1;
		final String lengthString = input.substring(lengthInformation + 1, len - 1);
		return Integer.parseInt(lengthString);
	}

	/**
	 * Determine if the current dataset reflects a logical type (of uniform length) as defined by the provided set.
	 * @param uniformSet The set of items with a uniform length that reflect this logical type
	 * @param type The type that along with the qualifier identifies the logical type that this set represents
	 * @param qualifier The qualifier that along with the type identifies the logical type that this set represents
	 * @return True if we believe that this data set is defined by the provided set
	 */
	private boolean checkUniformLengthSet(final Set<String> uniformSet, final PatternInfo.Type type, final String qualifier) {
		final long realSamples = sampleCount - (nullCount + blankCount);
		long misses = 0;					// count of number of groups that are misses
		long missCount = 0;				// count of number of misses

		// Sweep the current outliers and check they are part of the set
		for (final Map.Entry<String, Integer> entry : outliers.entrySet()) {
			misses++;
			missCount += entry.getValue();
			// Break out early if we know we are going to fail
			if ((double) missCount / realSamples > .05)
				return false;
		}

		// Sweep the balance and check they are part of the set
		long validCount = 0;
		final Map<String, Integer> newOutliers = new HashMap<String, Integer>();
		if ((double) missCount / realSamples <= .05) {
			for (final Map.Entry<String, Integer> entry : cardinality.entrySet()) {
				if (uniformSet.contains(entry.getKey().toUpperCase(Locale.ROOT)))
					validCount += entry.getValue();
				else {
					misses++;
					missCount += entry.getValue();
					newOutliers.put(entry.getKey(), entry.getValue());
					// Break out early if we know we are going to fail
					if ((double) missCount / realSamples > .05)
						return false;
				}
			}
		}

		// To declare success we need fewer than 5% failures by count and additionally fewer than 4 groups
		if ((double) missCount / realSamples > .05 || misses >= 4)
			return false;

		matchCount = validCount;
		matchPatternInfo = typeInfo.get(type.toString() + "." + qualifier);
		matchPattern = matchPatternInfo.regexp;
		outliers.putAll(newOutliers);
		cardinality.keySet().removeAll(newOutliers.keySet());

		return true;
	}

	/**
	 * Determine if the current dataset reflects a logical type (of variable length) as defined by the provided set.
	 * @param cardinalityUpper The cardinality set but reduced to ignore case
	 * @param variableSet The set of items that reflect this logical type
	 * @param type The type that along with the qualifier identifies the logical type that this set represents
	 * @param qualifier The qualifier that along with the type identifies the logical type that this set represents
	 * @return True if we believe that this data set is defined by the provided set
	 */
	private boolean checkVariableLengthSet(Map<String, Integer> cardinalityUpper, final Set<String> variableSet, final PatternInfo.Type type, final String qualifier) {
		final long realSamples = sampleCount - (nullCount + blankCount);
		final Map<String, Integer> newOutliers = new HashMap<String, Integer>();
		long validCount = 0;
		long misses = 0;				// count of number of groups that are misses
		long missCount = 0;				// count of number of misses

		// Sweep the balance and check they are part of the set
		for (final Map.Entry<String, Integer> entry : cardinalityUpper.entrySet()) {
			if (variableSet.contains(entry.getKey()))
				validCount += entry.getValue();
			else {
				misses++;
				missCount += entry.getValue();
				// Break out early if we know we are going to fail
				// To declare success we need fewer than 40% failures by count and also a limited number of misses by group
				if ((double) missCount / realSamples > .4 || misses > (long)Math.sqrt(variableSet.size()))
					return false;
				newOutliers.put(entry.getKey(), entry.getValue());
			}
		}

		// To declare success we need fewer than 40% failures by count and also a limited number of misses by group
		if ((double) missCount / realSamples > .4 || misses > (long)Math.sqrt(variableSet.size()))
			return false;

		outliers.putAll(newOutliers);
		cardinalityUpper.keySet().removeAll(newOutliers.keySet());
		matchCount = validCount;
		matchPatternInfo = typeInfo.get(type.toString() + "." + qualifier);
		matchPattern = matchPatternInfo.regexp;
		cardinality = cardinalityUpper;
		return true;
	}

	/**
	 * Determine the result of the training complete to date. Typically invoked
	 * after all training is complete, but may be invoked at any stage.
	 *
	 * @return A TextAnalysisResult with the analysis of any training completed.
	 */
	public TextAnalysisResult getResult() {
		String minValue = null;
		String maxValue = null;
		String sum = null;

		// If we have not already determined the type, now we need to
		if (matchType == null) {
			determineType();
		}

		// Compute our confidence
		final long realSamples = sampleCount - (nullCount + blankCount);
		double confidence = 0;

		// Check to see if we are all blanks or all nulls
		if (blankCount == sampleCount || nullCount == sampleCount || blankCount + nullCount == sampleCount) {
			if (nullCount == sampleCount)
				matchPatternInfo = typeInfo.get(PatternInfo.Type.STRING.toString() + "." + "NULL");
			else if (blankCount == sampleCount)
				matchPatternInfo = typeInfo.get(PatternInfo.Type.STRING.toString() + "." + "BLANK");
			else
				matchPatternInfo = typeInfo.get(PatternInfo.Type.STRING.toString() + "." + "BLANKORNULL");
			matchPattern = matchPatternInfo.regexp;
			matchCount = sampleCount;
			confidence = sampleCount >= 10 ? 1.0 : 0.0;
		}
		else {
			confidence = (double) matchCount / realSamples;
		}

		// Do a sanity check - we need a minimum number to declare it a ZIP
		if ("ZIP".equals(matchPatternInfo.typeQualifier) && ((realSamples >= reflectionSamples && confidence < 0.9) || cardinality.size() < 5)) {
			backoutZip(realSamples);
			confidence = (double) matchCount / realSamples;
		}

		if (PATTERN_LONG.equals(matchPattern)) {
			if (matchPatternInfo.typeQualifier == null && negativeLongs != 0) {
				matchPattern = PATTERN_SIGNED_LONG;
				matchPatternInfo = patternInfo.get(matchPattern);
			}

			if (minLong > 19000101 && maxLong < 20400101 &&
					((realSamples >= reflectionSamples && cardinality.size() > 10) || name.toLowerCase(Locale.ROOT).contains("date"))) {
				matchPatternInfo = new PatternInfo("\\d{8}", PatternInfo.Type.LOCALDATE, "yyyyMMdd", 8, 8, null, "yyyyMMdd");
				DateTimeFormatter dtf = DateTimeFormatter.ofPattern(matchPatternInfo.format);
				minLocalDate = LocalDate.parse(String.valueOf(minLong), dtf);
				maxLocalDate = LocalDate.parse(String.valueOf(maxLong), dtf);
			} else if (minLong > 1800 && maxLong < 2030 &&
					((realSamples >= reflectionSamples && cardinality.size() > 10) || name.toLowerCase(Locale.ROOT).contains("year") || name.toLowerCase(Locale.ROOT).contains("date"))) {
				matchPatternInfo = new PatternInfo("\\d{4}", PatternInfo.Type.LOCALDATE, "yyyy", 4, 4, null, "yyyy");
				minLocalDate = LocalDate.of((int)minLong, 1, 1);
				maxLocalDate = LocalDate.of((int)maxLong, 1, 1);
			} else if (cardinality.size() == 2 && minLong == 0 && maxLong == 1) {
				// boolean by any other name
				matchPattern = "[0|1]";
				minBoolean = "0";
				maxBoolean = "1";
				matchPatternInfo = patternInfo.get(matchPattern);
			} else {
				matchPattern = matchPatternInfo.typeQualifier != null ? "-?\\d{" : "\\d{";
				matchPattern += minTrimmedLength;
				if (minTrimmedLength != maxTrimmedLength)
					matchPattern += "," + maxTrimmedLength;
				matchPattern += "}";
				matchPatternInfo = new PatternInfo(matchPattern, matchType, matchPatternInfo.typeQualifier, -1, -1, null, null);

				if (realSamples >= reflectionSamples && confidence < 0.96) {
					// We thought it was an integer field, but on reflection it does not feel like it
					conditionalBackoutToPattern(realSamples, matchPatternInfo);
					confidence = 1.0;
				}
			}
		} else if (PATTERN_DOUBLE.equals(matchPattern)) {
			if (matchPatternInfo.typeQualifier == null && negativeDoubles != 0) {
				matchPattern = PATTERN_SIGNED_DOUBLE;
				matchPatternInfo = patternInfo.get(matchPattern);
			}
		} else if (PatternInfo.Type.STRING.equals(matchPatternInfo.type)) {
			final int length = determineLength(matchPattern);
			// We thought it was a fixed length string, but on reflection it does not feel like it
			if (length != -1 && realSamples >= reflectionSamples && (double) matchCount / realSamples < 0.95) {
				backoutToPattern(realSamples, PATTERN_ANY_VARIABLE);
				confidence = (double) matchCount / realSamples;
			}

			// Need to evaluate if we got the type wrong
			if (matchType == PatternInfo.Type.STRING && matchPatternInfo.typeQualifier == null && matchPatternInfo.isAlphabetic()) {
				conditionalBackoutToPattern(realSamples, matchPatternInfo);
				confidence = (double) matchCount / realSamples;
			}

			// Build Cardinality map ignoring case (and white space)
			int minKeyLength = Integer.MAX_VALUE;
			int maxKeyLength = 0;
			Map<String, Integer> cardinalityUpper = new HashMap<String, Integer>();
			for (final Map.Entry<String, Integer> entry : cardinality.entrySet()) {
				String key = entry.getKey().toUpperCase(Locale.ROOT).trim();
				int keyLength = key.length();
				if (keyLength < minKeyLength)
					minKeyLength = keyLength;
				if (keyLength > maxKeyLength)
					maxKeyLength = keyLength;
				final Integer seen = cardinalityUpper.get(key);
				if (seen == null) {
					cardinalityUpper.put(key, entry.getValue());
				} else
					cardinalityUpper.put(key, seen + entry.getValue());
			}
			// Sort the results so that we consider the most frequent first (we will hopefully fail faster)
			cardinalityUpper = Utils.sortByValue(cardinalityUpper);

			boolean typeIdentified = false;

			if (minKeyLength == maxKeyLength) {
				if (realSamples >= reflectionSamples && cardinalityUpper.size() > 1 && minKeyLength == 3
						&& cardinalityUpper.size() <= monthAbbr.size() + 2) {
					typeIdentified = checkUniformLengthSet(monthAbbr, PatternInfo.Type.STRING, "MONTHABBR");
				}

				if (!typeIdentified && realSamples >= reflectionSamples && PATTERN_ALPHA_2.equals(matchPattern)
						&& cardinalityUpper.size() < usStates.size() + caProvinces.size() + 5
						&& (name.toLowerCase(Locale.ROOT).contains("state") || name.toLowerCase(Locale.ROOT).contains("province")
								|| cardinalityUpper.size() > 5)) {
					int usStateCount = 0;
					int caProvinceCount = 0;
					int misses = 0;
					final Map<String, Integer> newOutliers = new HashMap<String, Integer>();

					for (final Map.Entry<String, Integer> entry : cardinalityUpper.entrySet()) {
						if (usStates.contains(entry.getKey()))
							usStateCount += entry.getValue();
						else if (caProvinces.contains(entry.getKey()))
							caProvinceCount += entry.getValue();
						else {
							misses++;
							newOutliers.put(entry.getKey(), entry.getValue());
						}
					}

					if (misses < 3) {
						String accessor = null;
						if (usStateCount != 0 && caProvinceCount != 0) {
							accessor = "NA_STATE";
							matchCount = usStateCount + caProvinceCount;
						} else if (usStateCount != 0) {
							accessor = "US_STATE";
							matchCount = usStateCount;
						} else if (caProvinceCount != 0) {
							accessor = "CA_PROVINCE";
							matchCount = caProvinceCount;
						}
						confidence = (double) matchCount / realSamples;
						outliers.putAll(newOutliers);
						cardinality.keySet().removeAll(newOutliers.keySet());
						matchPatternInfo = typeInfo.get(PatternInfo.Type.STRING.toString() + "." + accessor);
						matchPattern = matchPatternInfo.regexp;
						typeIdentified = true;
					}
				}
			}
			else {
				// Hunt for a variable length string that we can say something more interesting about

				if (!typeIdentified && realSamples >= reflectionSamples && cardinalityUpper.size() > 1
						&& cardinalityUpper.size() <= gender.size() + 1) {
					typeIdentified = checkVariableLengthSet(cardinalityUpper, gender, PatternInfo.Type.STRING, "GENDER");
				}

				if (!typeIdentified && realSamples >= reflectionSamples && cardinalityUpper.size() > 1
						&& cardinalityUpper.size() <= countries.size() + 1) {
					typeIdentified = checkVariableLengthSet(cardinalityUpper, countries, PatternInfo.Type.STRING, "COUNTRY");
				}
			}

			// Qualify Alpha or Alnum with a min and max length
			if (!typeIdentified && (PATTERN_ALPHA_VARIABLE.equals(matchPattern) || PATTERN_ALPHANUMERIC_VARIABLE.equals(matchPattern))) {
				matchPattern = matchPattern.substring(0, matchPattern.length() - 1) + "{" + minTrimmedLength;
				if (minTrimmedLength != maxTrimmedLength)
					matchPattern += "," + maxTrimmedLength;
				matchPattern += "}";
				matchPatternInfo = new PatternInfo(matchPattern, PatternInfo.Type.STRING, matchPatternInfo.typeQualifier, minTrimmedLength, maxTrimmedLength, null,
						null);
			}

			// Qualify random string with a min and max length
			if (!typeIdentified && PATTERN_ANY_VARIABLE.equals(matchPattern)) {
				matchPattern = matchPattern.substring(0, matchPattern.length() - 1) +
						Utils.lengthQualifier(minRawLength, maxRawLength);
				matchPatternInfo = new PatternInfo(matchPattern, PatternInfo.Type.STRING, matchPatternInfo.typeQualifier, minRawLength, maxRawLength, null,
						null);
			}
		}

		// We know the type - so calculate a minimum and maximum value
		switch (matchPatternInfo.type) {
		case BOOLEAN:
			minValue = String.valueOf(minBoolean);
			maxValue = String.valueOf(maxBoolean);
			break;

		case LONG:
			minValue = String.valueOf(minLong);
			maxValue = String.valueOf(maxLong);
			sum = sumBI.toString();
			break;

		case DOUBLE:
			minValue = String.valueOf(minDouble);
			maxValue = String.valueOf(maxDouble);
			sum = sumBD.toString();
			break;

		case STRING:
			if ("NULL".equals(matchPatternInfo.typeQualifier)) {
				minRawLength = maxRawLength = 0;
			} else if ("BLANK".equals(matchPatternInfo.typeQualifier)) {
				// If all the fields are blank - then we have not saved any of the raw input, so we
				// need to synthesize the min and max value, as well as the minRawlength if not set.
				if (minRawLength == Integer.MAX_VALUE)
					minRawLength = 0;
				final StringBuilder s = new StringBuilder(maxRawLength);
				for (int i = 0; i < maxRawLength; i++) {
					if (i == minRawLength)
						minValue = new String(s.toString());
					s.append(' ');
				}
				maxValue = s.toString();
				if (minRawLength == maxRawLength)
					minValue = maxValue;
			}
			else {
				minValue = minString;
				maxValue = maxString;
			}
			break;

		case LOCALDATE:
			if (collectStatistics) {
				minValue = minLocalDate.format(DateTimeFormatter.ofPattern(matchPatternInfo.format));
				maxValue = maxLocalDate.format(DateTimeFormatter.ofPattern(matchPatternInfo.format));
			}
			break;

		case LOCALTIME:
			if (collectStatistics) {
				minValue = minLocalTime.format(DateTimeFormatter.ofPattern(matchPatternInfo.format));
				maxValue = maxLocalTime.format(DateTimeFormatter.ofPattern(matchPatternInfo.format));
			}
			break;

		case LOCALDATETIME:
			if (collectStatistics) {
				minValue = minLocalDateTime == null ? null : minLocalDateTime.format(DateTimeFormatter.ofPattern(matchPatternInfo.format));
				maxValue = maxLocalDateTime == null ? null : maxLocalDateTime.format(DateTimeFormatter.ofPattern(matchPatternInfo.format));
			}
			break;

		case ZONEDDATETIME:
			if (collectStatistics) {
				minValue = minZonedDateTime.format(DateTimeFormatter.ofPattern(matchPatternInfo.format));
				maxValue = maxZonedDateTime.format(DateTimeFormatter.ofPattern(matchPatternInfo.format));
			}
			break;

		case OFFSETDATETIME:
			if (collectStatistics) {
				minValue = minOffsetDateTime.format(DateTimeFormatter.ofPattern(matchPatternInfo.format));
				maxValue = maxOffsetDateTime.format(DateTimeFormatter.ofPattern(matchPatternInfo.format));
			}
			break;
		}

		// Attempt to identify keys?
		boolean key = false;
		if (sampleCount > MIN_SAMPLES_FOR_KEY && maxCardinality >= MIN_SAMPLES_FOR_KEY / 2
				&& cardinality.size() >= maxCardinality && blankCount == 0 && nullCount == 0
				&& matchPatternInfo.typeQualifier == null
				&& ((PatternInfo.Type.STRING.equals(matchType) && minRawLength == maxRawLength && minRawLength < 32)
						|| PatternInfo.Type.LONG.equals(matchType))) {
			key = true;
			// Might be a key but only iff every element in the cardinality
			// set only has a count of 1
			for (final Map.Entry<String, Integer> entry : cardinality.entrySet()) {
				if (entry.getValue() != 1) {
					key = false;
					break;
				}
			}
		}
 		return new TextAnalysisResult(name, matchCount, matchPatternInfo, leadingWhiteSpace, trailingWhiteSpace, sampleCount,
				nullCount, blankCount, totalLeadingZeros, confidence, minValue, maxValue, minRawLength, maxRawLength, sum,
				cardinality, outliers, key);
	}

	/**
	 * Access the training set - this will typically be the first {@link #SAMPLE_DEFAULT} records.
	 *
	 * @return A List of the raw input strings.
	 */
	public List<String>getTrainingSet() {
		return raw;
	}
}
