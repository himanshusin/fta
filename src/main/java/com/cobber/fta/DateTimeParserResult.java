package com.cobber.fta;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class DateTimeParserResult {
	String format = null;
	int timeElements = -1;
	int hourLength = -1;
	int dateElements = -1;
	Boolean timeFirst = null;
	Character dateTimeSeparator = null;
	int yearOffset = -1;
	int yearLength = -1;
	int monthOffset = -1;
	int monthLength = -1;
	int dayOffset = -1;
	int dayLength = -1;
	int[] dateFieldLengths = new int[] {-1, -1, -1};
	String timeZone = "";
	Character dateSeparator = null;
	String formatString = null;
	Boolean dayFirst = null;

	static Map<String, DateTimeParserResult> options = new ConcurrentHashMap<String, DateTimeParserResult>();

	DateTimeParserResult(String formatString, int timeElements, int hourLength, int dateElements,
			int[] dateFieldLengths, Boolean timeFirst, Character dateTimeSeparator, int yearOffset, int monthOffset,
			int dayOffset, Character dateSeparator, String timeZone) {
		this.formatString = formatString;
		this.timeElements = timeElements;
		this.hourLength = hourLength;
		this.dateElements = dateElements;
		this.dateFieldLengths = dateFieldLengths;
		this.timeFirst = timeFirst;
		this.dateTimeSeparator = dateTimeSeparator;
		this.dayOffset = dayOffset;
		if (dayOffset != -1)
			this.dayLength = dateFieldLengths[dayOffset];
		this.monthOffset = monthOffset;
		if (monthOffset != -1)
			this.monthLength = dateFieldLengths[monthOffset];
		this.yearOffset = yearOffset;
		if (yearOffset != -1)
			this.yearLength = dateFieldLengths[yearOffset];
		this.dateSeparator = dateSeparator;
		this.timeZone = timeZone;
	}

	private enum NextToken {
		CONSTANT_CHAR, DAYS_1_OR_2, DAYS_2, DIGITS_1_OR_2, MONTHS_1_OR_2, MONTHS_2, DIGITS_2, DIGITS_4, MONTH_ABBR, TIMEZONE, TIMEZONE_OFFSET, UNDEFINED
	}

	/**
	 * Determine whether a string input matches this DateTimeParserResult.
	 * @param input The string to validate (stripped of whitespace.
	 * @return A boolean indicating if the input is valid.
	 */
	public boolean isValid8(String input) {
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern(getFormatString());

		try {
			if ("Time".equals(getType()))
				LocalTime.parse(input, formatter);
			else if ("Date".equals(getType()))
				LocalDate.parse(input, formatter);
			else if ("DateTime".equals(getType()))
				LocalDateTime.parse(input, formatter);
			else if ("ZonedDateTime".equals(getType()))
				ZonedDateTime.parse(input, formatter);
			else
				OffsetDateTime.parse(input, formatter);
			return true;
		}
		catch (DateTimeParseException exc) {
			return false;
		}
	}

	/**
	 * Determine whether a string input matches this DateTimeParserResult.
	 * @param input The string to validate (stripped of whitespace.
	 * @return A boolean indicating if the input is valid.
	 */
	public boolean isValid(String input) {
		try {
			parse(input);
			return true;
		}
		catch (DateTimeParseException exc) {
			return false;
		}
	}


	/**
	 * Given an input string in SimpleDateTimeFormat convert to a DateTimeParserResult
	 * @param formatString A DateTimeString using DateTimeFormatter patterns
	 * @return The corresponding DateTimeParserResult
	 */
	public static DateTimeParserResult asResult(String formatString) {
		DateTimeParserResult ret = options.get(formatString);
		if (ret != null)
			return ret;

		int dayOffset = -1;
		int dayLength = -1;
		int monthOffset = -1;
		int monthLength = -1;
		int yearOffset = -1;
		int yearLength = -1;
		int hourLength = -1;
		int dateElements = 0;
		int timeElements = 0;
		int[] dateFieldLengths = new int[] {-1, -1, -1};
		String timeZone = "";
		Boolean timeFirst = null;
		Character dateSeparator = null;
		Character dateTimeSeparator = ' ';

		int formatLength = formatString.length();

		for (int i = 0; i < formatLength; i++) {
			char ch = formatString.charAt(i);
			switch (ch) {
			case '?':
				++dateElements;
				if (i + 1 < formatLength && formatString.charAt(i + 1) == '?') {
					i++;
					dateFieldLengths[dateElements - 1] = 2;
				}
				else {
					dateFieldLengths[dateElements - 1] = 1;
				}
				if (dateElements == 1)
					dateSeparator = formatString.charAt(i + 1);
				break;

			case 'M':
				monthOffset = dateElements++;
				if (i + 1 < formatLength && formatString.charAt(i + 1) == 'M') {
					i++;
					if (i + 1 < formatLength && formatString.charAt(i + 1) == 'M') {
						i++;
						monthLength = 3;
					}
					else
						monthLength = 2;
				} else
					monthLength = 1;
				dateFieldLengths[dateElements - 1] = monthLength;

				if (dateElements == 1)
					dateSeparator = formatString.charAt(i + 1);
				break;

			case 'd':
				dayOffset = dateElements++;
				if (i + 1 < formatLength && formatString.charAt(i + 1) == 'd') {
					i++;
					dayLength = 2;
				}
				else
					dayLength = 1;
				dateFieldLengths[dateElements - 1] = dayLength;
				if (dateElements == 1)
					dateSeparator = formatString.charAt(i + 1);
				break;

			case 'H':
				timeFirst = dateElements == 0;
				timeElements++;
				if (i + 1 < formatLength && formatString.charAt(i + 1) == ch) {
					i++;
					hourLength = 2;
				}
				else
					hourLength = 1;
				break;

			case 'm':
			case 's':
				timeElements++;
				if (i + 1 >= formatLength || formatString.charAt(i + 1) != ch)
					return null;
				i++;
				break;

			case 'y':
				yearOffset = dateElements++;
				i++;
				if (i + 1 < formatLength && formatString.charAt(i + 1) == 'y') {
					yearLength = 4;
					i += 2;
				} else
					yearLength = 2;
				dateFieldLengths[dateElements - 1] = yearLength;
				if (dateElements == 1)
					dateSeparator = formatString.charAt(i + 1);
				break;

			case 'x':
				timeZone = "x";
				while (i + 1 < formatLength && formatString.charAt(i + 1) == 'x') {
					timeZone += "x";
					i++;
				}
				break;

			case 'z':
				timeZone = " z";
				break;

			case 'T':
				dateTimeSeparator = 'T';
				break;

			default:
			}
		}

		if (dateElements == 0)
			dateElements = -1;
		if (timeElements == 0)
			timeElements = -1;

		// Add to cache
		ret  = new DateTimeParserResult(formatString, timeElements, hourLength, dateElements, dateFieldLengths, timeFirst,
				dateTimeSeparator, yearOffset, monthOffset, dayOffset, dateSeparator, timeZone);
		options.put(formatString, ret);

		return ret;
	}

	/**
	 * Determine whether a string input matches this DateTimeParserResult.
	 * @param input The string to validate (stripped of whitespace).
	 *  if non-zero then this is the offset where the parse failed
	 */
	public void parse(String input) throws DateTimeParseException {
		NextToken nextToken = NextToken.UNDEFINED;
		char nextChar = 'þ';
		int nextCount = 1;
		int upto = 0;
		int inputLength = input.length();

		if (formatString == null)
			formatString = getFormatString();

		int formatLength = formatString.length();

		for (int i = 0; i < formatLength; i++) {
			char ch = formatString.charAt(i);
			switch (ch) {
			case 'M':
				if (i + 1 < formatLength && formatString.charAt(i + 1) == 'M') {
					i++;
					if (i + 1 < formatLength && formatString.charAt(i + 1) == 'M') {
						i++;
						nextToken = NextToken.MONTH_ABBR;
					}
					else
						nextToken = NextToken.MONTHS_2;
				} else
					nextToken = NextToken.MONTHS_1_OR_2;
				break;

			case 'd':
				if (i + 1 < formatLength && formatString.charAt(i + 1) == 'd') {
					i++;
					nextToken = NextToken.DAYS_2;
				}
				else
					nextToken = NextToken.DAYS_1_OR_2;

				break;

			case 'H':
			case '?':
				if (i + 1 < formatLength && formatString.charAt(i + 1) == ch) {
					i++;
					nextToken = NextToken.DIGITS_2;
				}
				else
					nextToken = NextToken.DIGITS_1_OR_2;
				break;

			case 'm':
			case 's':
				nextToken = NextToken.DIGITS_2;
				i++;
				break;

			case 'y':
				i++;
				if (i + 1 < formatLength && formatString.charAt(i + 1) == 'y') {
					nextToken = NextToken.DIGITS_4;
					i += 2;
				} else
					nextToken = NextToken.DIGITS_2;
				break;

			case 'x':
				nextToken = NextToken.TIMEZONE_OFFSET;
				nextCount = 1;
				while (i + 1 < formatLength && formatString.charAt(i + 1) == 'x') {
					nextCount++;
					i++;
				}
				break;

			case 'z':
				nextToken = NextToken.TIMEZONE;
				break;

			case '\'':
				i++;
				nextToken = NextToken.CONSTANT_CHAR;
				nextChar = formatString.charAt(i);
				if (i + 1 >= formatLength || formatString.charAt(i + 1) != '\'') {
					throw new DateTimeParseException("Unterminated quote in format String", input, upto);
				}
				i++;
				break;

			default:
				nextToken = NextToken.CONSTANT_CHAR;
				nextChar = ch;
			}

			char inputChar;
			int value = 0;

			switch (nextToken) {
			case MONTH_ABBR:
				if (upto + 3 > inputLength)
					throw new DateTimeParseException("Month Abbreviation not complete", input, upto);
				String monthAbbreviation = input.substring(upto, upto + 3);
				if (DateTimeParser.monthAbbreviationOffset(monthAbbreviation) == -1)
					throw new DateTimeParseException("Month Abbreviation incorrect", input, upto);
				upto += 3;
				break;

			case DIGITS_1_OR_2:
				if (upto == inputLength)
					throw new DateTimeParseException("Expecting digit, end of input", input, upto);
				if (!Character.isDigit(input.charAt(upto)))
					throw new DateTimeParseException("Expecting digit", input, upto);
				upto++;
				if (upto != inputLength && Character.isDigit(input.charAt(upto)))
					upto++;
				break;

			case DAYS_2:
			case MONTHS_2:
			case DAYS_1_OR_2:
			case MONTHS_1_OR_2:
				if (upto == inputLength)
					throw new DateTimeParseException("Expecting digit, end of input", input, upto);
				inputChar = input.charAt(upto);
				if (!Character.isDigit(input.charAt(upto)))
					throw new DateTimeParseException("Expecting digit", input, upto);
				value = inputChar - '0';
				upto++;
				if (nextToken == NextToken.DAYS_2 && upto < inputLength && dateSeparator == input.charAt(upto))
					throw new DateTimeParseException("Insufficient digits in input (d)", input, upto);
				if (nextToken == NextToken.MONTHS_2 && upto < inputLength && dateSeparator == input.charAt(upto))
					throw new DateTimeParseException("Insufficient digits in input (M)", input, upto);

				if ((nextToken == NextToken.DAYS_2 || nextToken == NextToken.MONTHS_2) && (upto == inputLength || !Character.isDigit(input.charAt(upto))))
					throw new DateTimeParseException("Expecting digit", input, upto);
				if (upto < inputLength && Character.isDigit(input.charAt(upto))) {
					value = 10 * value + (input.charAt(upto) - '0');
					int limit = (nextToken == NextToken.DAYS_1_OR_2  || nextToken == NextToken.DAYS_2) ? 31 : 12;
					if (value > limit)
						throw new DateTimeParseException("Value too large for day/month", input, upto);
					upto++;
				}
				if (value == 0)
					throw new DateTimeParseException("0 value illegal for day/month", input, upto);
				break;

			case DIGITS_2:
				if (upto == inputLength)
					throw new DateTimeParseException("Expecting digit, end of input", input, upto);
				if (!Character.isDigit(input.charAt(upto)))
					throw new DateTimeParseException("Expecting digit", input, upto);
				upto++;
				if (upto == inputLength)
					throw new DateTimeParseException("Expecting digit, end of input", input, upto);
				if (!Character.isDigit(input.charAt(upto)))
					throw new DateTimeParseException("Expecting digit", input, upto);
				upto++;
				break;

			case DIGITS_4:
				if (upto + 4 > inputLength)
					throw new DateTimeParseException("Expecting digit, end of input", input, upto);
				for (int j = 0; j < 4; j++) {
					if (!Character.isDigit(input.charAt(upto)))
						throw new DateTimeParseException("Expecting digit", input, upto);
					upto++;
				}
				break;

			case CONSTANT_CHAR:
				if (upto == inputLength)
					throw new DateTimeParseException("Expecting constant char, end of input", input, upto);
				if (input.charAt(upto) != nextChar)
					throw new DateTimeParseException("Expecting constant char", input, upto);
				upto++;
				break;

			case TIMEZONE:
				String currentTimeZone = input.substring(upto, inputLength);
				if (!DateTimeParser.timeZones.contains(currentTimeZone))
					throw new DateTimeParseException("Expecting time zone - bad time zone: " + currentTimeZone, input, upto);
				upto = inputLength;
				break;

			case TIMEZONE_OFFSET:
				final int[] timeZoneLength = { -1, 2, 4, 5, 6, 8 };
				final String[] timeZonePattern = { null, "þþ", "þþþþ", "þþ:þþ", "þþþþþþ", "þþ:þþ:þþ" };
				final int[] minuteOffset = { -1, -1, 2, 3, 2, 3 };
				final int[] secondOffset = { -1, -1, -1, -1, -1, 4, 7 };

				if (nextCount < 1 || nextCount > 5)
					throw new DateTimeParseException("Invalid time zone offset", input, upto);
				int len = timeZoneLength[nextCount];
				String pattern = timeZonePattern[nextCount];

				if (upto + len >= inputLength)
					throw new DateTimeParseException("Expecting time zone offset, end of input", input, upto);
				char direction = input.charAt(upto);
				String offset = input.substring(upto + 1).replaceAll("[0-9]", "þ");
				if ((direction != '-' && direction != '+') || !pattern.equals(offset))
					throw new DateTimeParseException("Expecting time zone offset, bad time zone offset", input, upto);

				// Validate hour offset
				int hour = DateTimeParser.getValue(input, upto + 1, 2);
				if (hour > 18)
					throw new DateTimeParseException("Expecting time zone offset, invalid hour offset", input, upto + 1);

				// Validate minute offset (if necessary)
				if (minuteOffset[nextCount] != -1) {
					int minute = DateTimeParser.getValue(input, upto + 1 + minuteOffset[nextCount], 2);
					if (minute > 59)
						throw new DateTimeParseException("Expecting time zone offset, invalid minute offset", input, upto + 1 + minuteOffset[nextCount]);
				}

				// Validate second offset (if necessary)
				if (secondOffset[nextCount] != -1) {
					int minute = DateTimeParser.getValue(input, upto + 1 + secondOffset[nextCount], 2);
					if (minute > 59)
						throw new DateTimeParseException("Expecting time zone offset, invalid minute offset", input, upto + 1 + secondOffset[nextCount]);
				}
				upto += len + 1;
				break;
			}
		}

		if (upto != inputLength)
			throw new DateTimeParseException("Expecting end of input, extraneous input found", input, upto);
	}

	/**
	 * Return the detected type of this input.
	 * @return The detected type of this input, will be either "Date", "Time",
	 *  "DateTime", "ZonedDateTime" or "OffsetDateTime".
	 */
	public String getType() {
		if (timeElements == -1)
			return "Date";
		if (dateElements == -1)
			return "Time";
		if (timeZone == null || timeZone.length() == 0)
			return "DateTime";
		return timeZone.indexOf('z') == -1 ? "OffsetDateTime" : "ZonedDateTime";
	}

	public void forceResolve(Boolean first) {
		this.dayFirst = first;
		this.formatString = null;
	}

	private String asDate(char[] fieldChars) {
		StringBuilder ret = new StringBuilder();
		for (int f = 0; f < fieldChars.length; f++) {
			for (int i = 0; i < dateFieldLengths[f]; i++) {
				ret.append(fieldChars[f]);
			}
			if (f + 1 < fieldChars.length)
				ret.append(dateSeparator);
		}

		return ret.toString();
	}

	/**
	 * Return a DateTimeFormatter representation of the DateTimeParserResult.
	 * @return a String in DateTimeFormatter
	 */
	public String getFormatString() {
		String hours = hourLength == 1 ? "H" : "HH";
		String timeAnswer = timeElements == 0 ? "" : hours + (timeElements == 2 ? ":mm" : ":mm:ss");
		String dateAnswer = "";
		if (dateElements != 0) {
			if (yearOffset == -1) {
				if (dayOffset != -1)
					dateAnswer = asDate(new char[] {'M', 'd', 'y'});
				else
					if (dayFirst != null)
						if (dayFirst)
							dateAnswer = asDate(new char[] {'d', 'M', 'y'});
						else
							dateAnswer = asDate(new char[] {'M', 'd', 'y'});
					else
						dateAnswer = asDate(new char[] {'?', '?', '?'});

			}
			else if (yearOffset == 0) {
				if (dayOffset != -1) {
					if (dayOffset == 1)
						dateAnswer = asDate(new char[] {'y', 'd', 'M'});
					else
						dateAnswer = asDate(new char[] {'y', 'M', 'd'});
				} else
					dateAnswer += asDate(new char[] {'y', '?', '?'});
			}
			else if (yearOffset == 2) {
				if (dayOffset != -1) {
					if (dayOffset == 0)
						dateAnswer = asDate(new char[] {'d', 'M', 'y'});
					else
						dateAnswer = asDate(new char[] {'M', 'd', 'y'});
				} else {
					if (dayFirst != null)
						if (dayFirst)
							dateAnswer = asDate(new char[] {'d', 'M', 'y'});
						else
							dateAnswer = asDate(new char[] {'M', 'd', 'y'});
					else
						dateAnswer = asDate(new char[] {'?', '?', 'y'});
				}
			}
		}

		if (timeElements == -1)
			return dateAnswer + timeZone;
		if (dateElements == -1)
			return timeAnswer;

		String separator = dateTimeSeparator == ' ' ? " " : "'T'";
		return (timeFirst != null && timeFirst) ? timeAnswer + separator + dateAnswer + timeZone
				: dateAnswer + separator + timeAnswer + timeZone;
	}
}