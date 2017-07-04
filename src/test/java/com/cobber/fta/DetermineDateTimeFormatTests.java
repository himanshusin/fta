package com.cobber.fta;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.TimeZone;

import org.testng.Assert;
import org.testng.annotations.Test;

public class DetermineDateTimeFormatTests {
	@Test
	public void intuitTimeOnly() throws Exception {
		Assert.assertEquals(DateTimeParser.determineFormatString("9:57"), "H:mm");
		Assert.assertEquals(DateTimeParser.determineFormatString("12:57"), "HH:mm");
		Assert.assertEquals(DateTimeParser.determineFormatString("8:57:02"), "H:mm:ss");
		Assert.assertEquals(DateTimeParser.determineFormatString("12:57:02"), "HH:mm:ss");
		Assert.assertNull(DateTimeParser.determineFormatString(":57:02"));
		Assert.assertNull(DateTimeParser.determineFormatString("123:02"));
		Assert.assertNull(DateTimeParser.determineFormatString("12:023"));
		Assert.assertNull(DateTimeParser.determineFormatString("12:023:12"));
		Assert.assertNull(DateTimeParser.determineFormatString("12:0"));
		Assert.assertNull(DateTimeParser.determineFormatString("12:02:1"));
		Assert.assertNull(DateTimeParser.determineFormatString("12:02:12:14"));
		Assert.assertNull(DateTimeParser.determineFormatString("12:02:124"));
		Assert.assertNull(DateTimeParser.determineFormatString("12:02:"));
		Assert.assertNull(DateTimeParser.determineFormatString("12::02"));
	}

	@Test
	public void intuitDateOnlySlash() throws Exception {
		Assert.assertEquals(DateTimeParser.determineFormatString("2/12/98"), "?/??/yy");
		Assert.assertEquals(DateTimeParser.determineFormatString("2/2/02"), "?/?/yy");
		Assert.assertNull(DateTimeParser.determineFormatString("2/31/02"));
		Assert.assertEquals(DateTimeParser.determineFormatString("31/02/02"), "??/??/??");
		Assert.assertEquals(DateTimeParser.determineFormatString("12/12/98"), "??/??/yy");
		Assert.assertEquals(DateTimeParser.determineFormatString("14/12/98"), "dd/MM/yy");
		Assert.assertEquals(DateTimeParser.determineFormatString("12/14/98"), "MM/dd/yy");
		Assert.assertEquals(DateTimeParser.determineFormatString("12/12/2012"), "??/??/yyyy");
		Assert.assertEquals(DateTimeParser.determineFormatString("20/12/2012"), "dd/MM/yyyy");
		Assert.assertEquals(DateTimeParser.determineFormatString("11/15/2012"), "MM/dd/yyyy");
		Assert.assertEquals(DateTimeParser.determineFormatString("2012/12/12"), "yyyy/MM/dd");
		Assert.assertNull(DateTimeParser.determineFormatString("/57/02"));
		Assert.assertNull(DateTimeParser.determineFormatString("123/02"));
		Assert.assertNull(DateTimeParser.determineFormatString("12/023"));
		Assert.assertNull(DateTimeParser.determineFormatString("12/0"));
		Assert.assertNull(DateTimeParser.determineFormatString("12/02/1"));
		Assert.assertNull(DateTimeParser.determineFormatString("12/023/12"));
		Assert.assertNull(DateTimeParser.determineFormatString("12/02/"));
		Assert.assertNull(DateTimeParser.determineFormatString("12/02-99"));
	}

	/*
	@Test
	public void testSpaces() throws Exception {
		Assert.assertEquals(DateTimeParser.parse("2018 12 24"), "yyyy MM dd");
	}
	*/

	@Test
	public void intuitDateOnlyDash() throws Exception {
		Assert.assertEquals(DateTimeParser.determineFormatString("2-12-98"), "?-??-yy");
		Assert.assertEquals(DateTimeParser.determineFormatString("12-12-98"), "??-??-yy");
		Assert.assertEquals(DateTimeParser.determineFormatString("14-12-98"), "dd-MM-yy");
		Assert.assertEquals(DateTimeParser.determineFormatString("12-14-98"), "MM-dd-yy");
		Assert.assertEquals(DateTimeParser.determineFormatString("12-12-2012"), "??-??-yyyy");
		Assert.assertEquals(DateTimeParser.determineFormatString("2012-12-12"), "yyyy-MM-dd");
		Assert.assertNull(DateTimeParser.determineFormatString("20120-12-12"));
	}

	@Test
	public void intuit8601DD_DD() throws Exception {
		Assert.assertEquals(DateTimeParser.determineFormatString("2004-01-01T00:00:00+05:00"), "yyyy-MM-dd'T'HH:mm:ssxxx");

		DateTimeParser det = new DateTimeParser();
		det.train("2004-01-01T00:00:00+05:00");

		DateTimeParserResult result = det.getResult();
		Assert.assertEquals(result.getFormatString(), "yyyy-MM-dd'T'HH:mm:ssxxx");

		Assert.assertTrue(result.isValid("2004-01-01T00:00:00+05:00"));
		Assert.assertTrue(result.isValid("2012-03-04T19:22:10+08:00"));
		Assert.assertFalse(result.isValid("2012-03-04T19:22:10+08:0"));
		Assert.assertFalse(result.isValid("2012-03-04T19:22:10+?08:00"));
		Assert.assertTrue(result.isValid8("2004-01-01T00:00:00+05:00"));
		Assert.assertTrue(result.isValid8("2012-03-04T19:22:10+08:00"));
		Assert.assertFalse(result.isValid8("2012-03-04T19:22:10+08:0"));
		Assert.assertFalse(result.isValid8("2012-03-04T19:22:10+?08:00"));
	}

	@Test
	public void intuit8601DD_DD_DD() throws Exception {
		Assert.assertEquals(DateTimeParser.determineFormatString("2004-01-01T00:00:00+05:00:00"), "yyyy-MM-dd'T'HH:mm:ssxxxxx");

		DateTimeParser det = new DateTimeParser();
		det.train("2004-01-01T00:00:00+05:00:00");

		DateTimeParserResult result = det.getResult();
		Assert.assertEquals(result.getFormatString(), "yyyy-MM-dd'T'HH:mm:ssxxxxx");

		Assert.assertTrue(result.isValid("2004-01-01T00:00:00+05:00:00"));
		Assert.assertTrue(result.isValid("2012-03-04T19:22:10+08:00:00"));
		Assert.assertFalse(result.isValid("2012-03-04T19:22:10+08:00:0"));
		Assert.assertFalse(result.isValid("2012-03-04T19:22:10+O8:00:00"));
		Assert.assertTrue(result.isValid8("2004-01-01T00:00:00+05:00:00"));
		Assert.assertTrue(result.isValid8("2012-03-04T19:22:10+08:00:00"));
		Assert.assertFalse(result.isValid8("2012-03-04T19:22:10+08:00:0"));
		Assert.assertFalse(result.isValid8("2012-03-04T19:22:10+O8:00:00"));
	}

	@Test
	public void testAsResult() throws Exception {
		Assert.assertNull(DateTimeParserResult.asResult("yyyy-MM-ddTHH:m:ssx"));
		Assert.assertNull(DateTimeParserResult.asResult("yyyy-MM-ddTHH:mm:sx"));
		Assert.assertNull(DateTimeParserResult.asResult("yyyy-MM-ddTHH:mm:sx"));
	}

	@Test
	public void testParse() throws Exception {
		DateTimeParserResult result = DateTimeParserResult.asResult("yyyy/MM/dd HH:mm");

		try {
			result.parse("2018/01/31 05:O5");
		}
		catch (DateTimeParseException e) {
			Assert.assertEquals(e.getMessage(), "Expecting digit");
			Assert.assertEquals(e.getErrorIndex(), 14);
		}

		try {
			result.parse("2018/01/31 05:5");
		}
		catch (DateTimeParseException e) {
			Assert.assertEquals(e.getMessage(), "Expecting digit, end of input");
			Assert.assertEquals(e.getErrorIndex(), 15);
		}

		try {
			result.parse("2018/12/24 09:");
		}
		catch (DateTimeParseException e) {
			Assert.assertEquals(e.getMessage(), "Expecting digit, end of input");
			Assert.assertEquals(e.getErrorIndex(), 14);
		}

		try {
			result.parse("2018/1/24 09:00");
		}
		catch (DateTimeParseException e) {
			Assert.assertEquals(e.getMessage(), "Insufficient digits in input (M)");
			Assert.assertEquals(e.getErrorIndex(), 6);
		}

		try {
			result.parse("2018/11/4 09:00");
		}
		catch (DateTimeParseException e) {
			Assert.assertEquals(e.getMessage(), "Expecting digit");
			Assert.assertEquals(e.getErrorIndex(), 9);
		}

		try {
			result.parse("2018/11/O4 09:00");
		}
		catch (DateTimeParseException e) {
			Assert.assertEquals(e.getMessage(), "Expecting digit");
			Assert.assertEquals(e.getErrorIndex(), 8);
		}

		try {
			result.parse("2018/00/24 05:59");
		}
		catch (DateTimeParseException e) {
			Assert.assertEquals(e.getMessage(), "0 value illegal for day/month");
			Assert.assertEquals(e.getErrorIndex(), 7);
		}

		try {
			result.parse("2018/13/24 05:59");
		}
		catch (DateTimeParseException e) {
			Assert.assertEquals(e.getMessage(), "Value too large for day/month");
			Assert.assertEquals(e.getErrorIndex(), 6);
		}

		try {
			result.parse("2018/01/00 05:59");
		}
		catch (DateTimeParseException e) {
			Assert.assertEquals(e.getMessage(), "0 value illegal for day/month");
			Assert.assertEquals(e.getErrorIndex(), 10);
		}

		try {
			result.parse("2018/01/32 05:59");
		}
		catch (DateTimeParseException e) {
			Assert.assertEquals(e.getMessage(), "Value too large for day/month");
			Assert.assertEquals(e.getErrorIndex(), 9);
		}
	}

	@Test
	public void intuitDateTime() throws Exception {
		Assert.assertEquals(DateTimeParser.determineFormatString("  2/12/98 9:57    "), "?/??/yy H:mm");
		Assert.assertNull(DateTimeParser.determineFormatString("0þþþþþ"));
		Assert.assertNull(DateTimeParser.determineFormatString("2/12/98 :57"));
		Assert.assertNull(DateTimeParser.determineFormatString("2/12/98 9:5"));
		Assert.assertNull(DateTimeParser.determineFormatString("2/12/98 9:55:5"));
		Assert.assertEquals(DateTimeParser.determineFormatString("2/13/98 9:57"), "M/dd/yy H:mm");
		Assert.assertEquals(DateTimeParser.determineFormatString("13/12/98 12:57"), "dd/MM/yy HH:mm");
		Assert.assertEquals(DateTimeParser.determineFormatString("12/12/2012 8:57:02"), "??/??/yyyy H:mm:ss");
		Assert.assertEquals(DateTimeParser.determineFormatString("12/12/2012 8:57:02 GMT"),
				"??/??/yyyy H:mm:ss z");
		Assert.assertEquals(DateTimeParser.determineFormatString("13/12/2012 8:57:02"), "dd/MM/yyyy H:mm:ss");
		Assert.assertEquals(DateTimeParser.determineFormatString("2012/12/12 12:57:02"), "yyyy/MM/dd HH:mm:ss");

		DateTimeParser det = new DateTimeParser();
		det.train("12/12/2012 8:57:02 GMT");

		DateTimeParserResult result = det.getResult();
		Assert.assertEquals(result.getFormatString(), "??/??/yyyy H:mm:ss z");

		result.forceResolve(true);
		Assert.assertEquals(result.getFormatString(), "dd/MM/yyyy H:mm:ss z");
		result.forceResolve(false);
		Assert.assertEquals(result.getFormatString(), "MM/dd/yyyy H:mm:ss z");

		Assert.assertTrue(result.isValid("12/12/2012 8:57:02 GMT"));
		Assert.assertFalse(result.isValid("12/12/2012 8:57:02 GM"));
		Assert.assertFalse(result.isValid("12/12/2012 8:57:02 GMZ"));
		Assert.assertFalse(result.isValid("1O/12/2012 8:57:02 GMT"));
		Assert.assertFalse(result.isValid("10/1O/2012 8:57:02 GMT"));
		Assert.assertFalse(result.isValid("1/0/2012 8:57:02 GMT"));
		Assert.assertFalse(result.isValid("1/O/2012 8:57:02 GMT"));
		Assert.assertFalse(result.isValid("2/12/1998 :57"));
		Assert.assertFalse(result.isValid("2/12/1998 9:5"));
		Assert.assertFalse(result.isValid("2/12/1998 9:"));
		Assert.assertFalse(result.isValid("2/12/1998 9:55:5"));

		Assert.assertTrue(result.isValid8("12/12/2012 8:57:02 GMT"));
		Assert.assertFalse(result.isValid8("12/12/2012 8:57:02 GM"));
		Assert.assertFalse(result.isValid8("12/12/2012 8:57:02 GMZ"));
		Assert.assertFalse(result.isValid8("1O/12/2012 8:57:02 GMT"));
		Assert.assertFalse(result.isValid8("10/1O/2012 8:57:02 GMT"));
		Assert.assertFalse(result.isValid8("1/0/2012 8:57:02 GMT"));
		Assert.assertFalse(result.isValid8("1/O/2012 8:57:02 GMT"));
		Assert.assertFalse(result.isValid8("2/12/1998 :57"));
		Assert.assertFalse(result.isValid8("2/12/1998 9:5"));
		Assert.assertFalse(result.isValid8("2/12/1998 9:"));
		Assert.assertFalse(result.isValid8("2/12/1998 9:55:5"));
	}

	@Test
	public void intuitTimeDate() throws Exception {
		Assert.assertEquals(DateTimeParser.determineFormatString("9:57 2/12/98"), "H:mm ?/??/yy");
		Assert.assertEquals(DateTimeParser.determineFormatString("9:57 2/13/98"), "H:mm M/dd/yy");
		Assert.assertEquals(DateTimeParser.determineFormatString("12:57 13/12/98"), "HH:mm dd/MM/yy");
		Assert.assertEquals(DateTimeParser.determineFormatString("8:57:02 12/12/2012"), "H:mm:ss ??/??/yyyy");
		Assert.assertEquals(DateTimeParser.determineFormatString("12:57:02 2012/12/12"), "HH:mm:ss yyyy/MM/dd");
	}

	@Test
	public void parseddMMMyyyy() throws Exception {
		Assert.assertEquals(DateTimeParser.determineFormatString("2-Jan-2017"), "d-MMM-yyyy");
		Assert.assertEquals(DateTimeParser.determineFormatString("12-May-14"), "dd-MMM-yy");
		Assert.assertEquals(DateTimeParser.determineFormatString("21 Jan 2017"), "dd MMM yyyy");
		Assert.assertEquals(DateTimeParser.determineFormatString("8 Dec 1993"), "d MMM yyyy");
		Assert.assertEquals(DateTimeParser.determineFormatString("25-Dec-2017"), "dd-MMM-yyyy");
		Assert.assertNull(DateTimeParser.determineFormatString("21-Jam-2017"));

		DateTimeParser det = new DateTimeParser();
		det.train("2 Jan 2017");

		DateTimeParserResult result = det.getResult();
		Assert.assertEquals(result.getFormatString(), "d MMM yyyy");

		Assert.assertTrue(result.isValid("20 Jun 2017"));
		Assert.assertTrue(result.isValid("1 Jun 2017"));
		Assert.assertFalse(result.isValid("20 0c"));
		Assert.assertFalse(result.isValid(""));
		Assert.assertFalse(result.isValid("1"));
		Assert.assertFalse(result.isValid("20 0ct 2018"));
		Assert.assertFalse(result.isValid("32 Oct 2018"));
		Assert.assertFalse(result.isValid("32 Och 2018"));
		Assert.assertFalse(result.isValid("31 Oct 201"));

		Assert.assertTrue(result.isValid8("20 Jun 2017"));
		Assert.assertTrue(result.isValid8("1 Jun 2017"));
		Assert.assertFalse(result.isValid8("20 0c"));
		Assert.assertFalse(result.isValid8(""));
		Assert.assertFalse(result.isValid8("1"));
		Assert.assertFalse(result.isValid8("20 0ct 2018"));
		Assert.assertFalse(result.isValid8("32 Oct 2018"));
		Assert.assertFalse(result.isValid8("32 Och 2018"));
		Assert.assertFalse(result.isValid8("31 Oct 201"));
	}

	@Test
	public void intuitTimeDateWithTimeZone() throws Exception {
		Assert.assertEquals(DateTimeParser.determineFormatString("01/30/2012 10:59:48 GMT"),
				"MM/dd/yyyy HH:mm:ss z");
	}

	@Test
	public void intuitDateTrainSlash() throws Exception {
		DateTimeParser det = new DateTimeParser();
		det.train("12/12/12");
		det.train("12/12/32");
		det.train("02/22/02");
		for (int i = 0; i < 20; i++)
			det.train("02/02/99");
		det.train("02/O2/99");

		DateTimeParserResult result = det.getResult();
		Assert.assertEquals(result.getFormatString(), "MM/dd/yy");
		Assert.assertEquals(result.getType(), "Date");
	}

	@Test
	public void intuitDateTrainYYYYSlash() throws Exception {
		DateTimeParser det = new DateTimeParser();
		det.train("2012/12/12");
		det.train("2012/11/11");
		det.train("2012/10/32");

		DateTimeParserResult result = det.getResult();
		Assert.assertEquals(result.getFormatString(), "yyyy/MM/dd");

		Assert.assertTrue(result.isValid("2012/12/12"));
		Assert.assertFalse(result.isValid("2012/10/32"));
		Assert.assertFalse(result.isValid("20121/10/32"));
		Assert.assertFalse(result.isValid("201/10/32"));

		Assert.assertTrue(result.isValid8("2012/12/12"));
		Assert.assertFalse(result.isValid8("2012/10/32"));
		Assert.assertFalse(result.isValid8("20121/10/32"));
		Assert.assertFalse(result.isValid8("201/10/32"));
	}

	@Test
	public void yyyyMd() throws Exception {
		DateTimeParser det = new DateTimeParser();
		det.train("8547 8 6");

		DateTimeParserResult result = det.getResult();
		Assert.assertEquals(result.getFormatString(), "yyyy M d");
		Assert.assertEquals(result.getType(), "Date");
	}

	@Test
	public void timeFirst() throws Exception {
		DateTimeParser det = new DateTimeParser();
		det.train("7:05 5/4/38");

		DateTimeParserResult result = det.getResult();
		Assert.assertEquals(result.getFormatString(), "H:mm ?/?/yy");
		Assert.assertEquals(result.getType(), "DateTime");
	}

	@Test
	public void bogusInput() throws Exception {
		String inputs = "21/12/99:|21/12/99:|18:46:|4:38  39|3124/08/|890/65 1/|7/87/33| 89:50|18:52 56:|18/94/06|0463 5 71|50 9:22|" +
				"95/06/88|0-27-98|08/56 22/|31-0-99|0/7:6/11 //61|8:73/4/13 15|14/23/3367| 00/21/79|22-23-00|0/20/2361|0/2/52 9:50 4 |" +
				"1:57:11  1/4/98|2015-8-17T|4/01/41 3:43 T450|37/8/005 5:05|0/6/95|0000 7 1|2000-12-12T12:45-72|2000-12-12T12:45-112|";
		String[] input = inputs.split("\\|");

		for (String testCase : input) {
			DateTimeParser det = new DateTimeParser();
			det.train(testCase);
			DateTimeParserResult result = det.getResult();
			Assert.assertNull(result, testCase);
		}
	}

	@Test
	public void bogusInput2() throws Exception {
//		LocalTime.parse(" 10:45", DateTimeFormatter.ofPattern("HH:mm"));
		String testInput = "2000-12-12 12:45 AGT";
		DateTimeParser det = new DateTimeParser();
		det.train(testInput);
		DateTimeParserResult result = det.getResult();
		String formatString = result.getFormatString();
		String type = result.getType();

		System.err.printf("getFormatString(): '%s', getType(): '%s'\n", formatString, type);

		String trimmed = testInput.trim();

		try {
			result.parse(trimmed);
		}
		catch (DateTimeParseException e) {
			System.err.printf("Message: '%s', at '%s', offset %d\n", e.getMessage(), e.getParsedString(), e.getErrorIndex());
		}
		if (testInput.indexOf('?') != -1)
			return;
		try {
			DateTimeFormatter formatter = DateTimeFormatter.ofPattern(formatString);
			if ("Time".equals(type))
				LocalTime.parse(trimmed, formatter);
			else if ("Date".equals(type))
				LocalDate.parse(trimmed, formatter);
			else if ("DateTime".equals(type))
				LocalDateTime.parse(trimmed, formatter);
			else if ("ZonedDateTime".equals(type))
				ZonedDateTime.parse(trimmed, formatter);
			else
				OffsetDateTime.parse(trimmed, formatter);
		}
		catch (DateTimeParseException e) {
			System.err.printf("Java Message: '%s', at '%s', offset %d\n", e.getMessage(), e.getParsedString(), e.getErrorIndex());
		}
		Assert.assertNull(result);
	}

	@Test
	public void intuitHHMMTrain() throws Exception {
		DateTimeParser det = new DateTimeParser();
		det.train("12:57");
		det.train("13:45");
		det.train("8:03");

		DateTimeParserResult result = det.getResult();
		Assert.assertEquals(result.getFormatString(), "H:mm");
		Assert.assertEquals(result.getType(), "Time");
	}

	private void add(Map<String, Integer> counter, String key) {
		Integer seen = counter.get(key);
		if (seen == null)
			counter.put(key, 1);
		else
			counter.put(key, seen + 1);
	}

	private void dump(Map<String, Integer> counter) {
		Map<String, Integer> byValue = DateTimeParser.sortByValue(counter);
		for (Map.Entry<String, Integer> entry : byValue.entrySet()) {
			System.err.printf("'%s' : %d\n", entry.getKey(), entry.getValue());
		}
	}

	@Test
	public void fuzz() throws Exception {
		Random randomGenerator = new Random();
		Map<String, Integer> formatStrings = new HashMap<String, Integer>();
		Map<String, Integer> types = new HashMap<String, Integer>();
		int good = 0;
		int iterations = 100000000;
		String[] timeZones = TimeZone.getAvailableIDs();

		for (int iters = 0; iters < iterations; iters++) {
			int len = 5 + randomGenerator.nextInt(15);
			StringBuilder s = new StringBuilder(len);
			int digits = 0;
		    for (int i = 0; s.length() <= len; ++i) {
		      int randomInt = randomGenerator.nextInt(100);
		      if (randomInt < 10) {
		    	  if (randomInt % 2 == 1)
		    		  s.append("2000-12-12");
		    	  else
		    		  s.append("12:45");
		    	  continue;
		      }
		      if (randomInt < 50) {
		    	  if (i == 10 && randomInt % 10 == 1) {
		    		  s.append('T');
		    		  continue;
		    	  }
		    	  if (digits == 4) {
		    		  i--;
		    		  continue;
		    	  }
		    	  s.append("0123456789".charAt(randomInt % 10));
		    	  digits++;
		    	  continue;
		      }
		      digits = 0;
		      if (randomInt < 60) {
		    	  s.append(':');
		    	  continue;
		      }
		      if (randomInt < 70) {
		    	  s.append('/');
		    	  continue;
		      }
		      if (randomInt < 80) {
		    	  if (i < 10)
		    		  s.append('-');
		    	  else
		    		  s.append(randomInt % 2  == 1 ? '+' : '-');
		    	  continue;
		      }
		      if (randomInt < 95) {
		    	  s.append(' ');
		    	  continue;
		      }
		      if (randomInt < 97) {
		    	  s.append('T');
		    	  continue;
		      }
		      if (randomInt < 99) {
		    	  int idx = randomGenerator.nextInt(timeZones.length - 1);
		    	  s.append(timeZones[idx]);
		    	  continue;
		      }
		      s.append(",.;':\"[]{}\\|=!@#$%^&*<>".charAt(randomGenerator.nextInt(100) % 23));
		    }
			DateTimeParser det = new DateTimeParser();
			String input = s.toString();
			//System.err.printf("Input ... '%s'\n", input);
			String trimmed = input.trim();
			try {
				det.train(input);

				DateTimeParserResult result = det.getResult();
				if (result != null) {
					good++;
					String formatString = result.getFormatString();
					String type = result.getType();
					add(formatStrings, formatString);
					add(types, type);
					result.parse(trimmed);
					if (formatString.indexOf('?') != -1)
						continue;

					try {
						DateTimeFormatter formatter = DateTimeFormatter.ofPattern(formatString);
						if ("Time".equals(type))
							LocalTime.parse(trimmed, formatter);
						else if ("Date".equals(type))
							LocalDate.parse(trimmed, formatter);
						else if ("DateTime".equals(type))
							LocalDateTime.parse(trimmed, formatter);
						else if ("ZonedDateTime".equals(type))
							ZonedDateTime.parse(trimmed, formatter);
						else
							OffsetDateTime.parse(trimmed, formatter);
					}
					catch (DateTimeParseException exc) {
						System.err.printf("Java: Struggled with input of the form: '%s'\n", input);
					}

				}
			}
			catch (Exception e) {
				System.err.printf("Struggled with input of the form: '%s'\n", input);
			}
		}

		dump(formatStrings);
		dump(types);

		System.err.printf("Good %d out of %d (%%%f)\n", good, iterations, 100*((float)good/iterations));
	}

	@Test
	public void intuitMMDDYYYY_HHMMSSTrain() throws Exception {
		DateTimeParser det = new DateTimeParser();
		det.train("01/26/2012 10:42:23 GMT");
		det.train("01/30/2012 10:59:48 GMT");
		det.train("01/25/2012 16:46:43 GMT");
		det.train("01/25/2012 16:28:42 GMT");
		det.train("01/24/2012 16:53:04 GMT");

		DateTimeParserResult result = det.getResult();
		Assert.assertEquals(result.getFormatString(), "MM/dd/yyyy HH:mm:ss z");
		Assert.assertEquals(result.getType(), "ZonedDateTime");

		Assert.assertTrue(result.isValid("01/26/2012 10:42:23 GMT"));
		Assert.assertTrue(result.isValid8("01/26/2012 10:42:23 GMT"));
	}

	//@Test
	public void testPerf() throws Exception {
		DateTimeParser det = new DateTimeParser();
		det.train("01/26/2012 10:42:23 GMT");
		det.train("01/30/2012 10:59:48 GMT");
		det.train("01/25/2012 16:46:43 GMT");
		det.train("01/25/2012 16:28:42 GMT");
		det.train("01/24/2012 16:53:04 GMT");

		DateTimeParserResult result = det.getResult();
		Assert.assertEquals(result.getFormatString(), "MM/dd/yyyy HH:mm:ss z");
		Assert.assertEquals(result.getType(), "DateTime");

		int iterations = 10000000;
		long start = System.currentTimeMillis();
		for (int i = 0; i < iterations; i++) {
			Assert.assertTrue(result.isValid("01/26/2012 10:42:23 GMT"));
		}
		long doneCustom = System.currentTimeMillis();
		for (int i = 0; i < iterations; i++) {
			Assert.assertTrue(result.isValid8("01/26/2012 10:42:23 GMT"));
		}
		long done = System.currentTimeMillis();
		System.err.printf("Custom = %dms, Java 8 = %dms\n", doneCustom - start, done - doneCustom);
	}

	@Test
	public void intuitInsufficientFactsTrain() throws Exception {
		DateTimeParser det = new DateTimeParser();

		det.train(" 04/03/13");
		det.train(" 05/03/13");
		det.train(" 06/03/13");
		det.train(" 07/03/13");
		det.train(" 08/03/13");
		det.train(" 09/03/13");
		det.train(" 10/03/13");

		DateTimeParserResult result = det.getResult();
		Assert.assertEquals(result.getFormatString(), "??/??/??");

		// Force to be day first
		result.forceResolve(true);
		Assert.assertEquals(result.getFormatString(), "dd/MM/yy");
		Assert.assertTrue(result.isValid("31/12/12"));
		Assert.assertFalse(result.isValid("12/31/12"));
		Assert.assertFalse(result.isValid("2012/12/12"));
		Assert.assertTrue(result.isValid8("31/12/12"));
		Assert.assertFalse(result.isValid8("12/31/12"));
		Assert.assertFalse(result.isValid8("2012/12/12"));

		// Force to be month first
		result.forceResolve(false);
		Assert.assertEquals(result.getFormatString(), "MM/dd/yy");
		Assert.assertFalse(result.isValid("31/12/12"));
		Assert.assertTrue(result.isValid("12/31/12"));
		Assert.assertFalse(result.isValid("2012/12/12"));
		Assert.assertFalse(result.isValid8("31/12/12"));
		Assert.assertTrue(result.isValid8("12/31/12"));
		Assert.assertFalse(result.isValid8("2012/12/12"));
	}

	@Test
	public void intuitDateMMddyy() throws Exception {
		DateTimeParser det = new DateTimeParser();

		det.train(" 04/03/13");
		det.train(" 05/03/13");
		det.train(" 06/03/13");
		det.train(" 07/03/13");
		det.train(" 08/03/13");
		det.train(" 09/30/13");
		det.train(" 10/03/13");
		for (int i = 0; i < 20; i++) {
			det.train("10/10/13");
		}

		DateTimeParserResult result = det.getResult();
		Assert.assertEquals(result.getFormatString(), "MM/dd/yy");

		Assert.assertTrue(result.isValid("12/12/12"));
		Assert.assertFalse(result.isValid("1/1/1"));
		Assert.assertFalse(result.isValid("123/1/1"));
		Assert.assertFalse(result.isValid("1/123/1"));
		Assert.assertFalse(result.isValid("1/1/123"));

		Assert.assertTrue(result.isValid8("12/12/12"));
		Assert.assertFalse(result.isValid8("1/1/1"));
		Assert.assertFalse(result.isValid8("123/1/1"));
		Assert.assertFalse(result.isValid8("1/123/1"));
		Assert.assertFalse(result.isValid8("1/1/123"));
	}

	@Test
	public void intuitDateyyMMdd() throws Exception {
		DateTimeParser det = new DateTimeParser();

		det.train("98/03/13");
		det.train("03/03/13");
		det.train("34/03/13");
		det.train("46/03/13");
		det.train("59/03/13");
		det.train("09/03/31");
		det.train("10/03/13");

		DateTimeParserResult result = det.getResult();
		Assert.assertEquals(result.getFormatString(), "yy/MM/dd");

		Assert.assertTrue(result.isValid("12/12/12"));
		Assert.assertFalse(result.isValid("12/13/12"));
		Assert.assertFalse(result.isValid("1/1/1"));
		Assert.assertFalse(result.isValid("123/1/1"));
		Assert.assertFalse(result.isValid("1/123/1"));
		Assert.assertFalse(result.isValid("1/1/123"));

		Assert.assertTrue(result.isValid8("12/12/12"));
		Assert.assertFalse(result.isValid8("12/13/12"));
		Assert.assertFalse(result.isValid8("1/1/1"));
		Assert.assertFalse(result.isValid8("123/1/1"));
		Assert.assertFalse(result.isValid8("1/123/1"));
		Assert.assertFalse(result.isValid8("1/1/123"));
	}

	@Test
	public void intuitDatedMMMyy() throws Exception {
		DateTimeParser det = new DateTimeParser();
		det.train("1-Jan-14");
		det.train("10-Jan-14");

		DateTimeParserResult result = det.getResult();
		Assert.assertEquals(result.getFormatString(), "d-MMM-yy");

		Assert.assertTrue(result.isValid("1-Jan-14"));
		Assert.assertTrue(result.isValid("10-Jan-14"));

		Assert.assertTrue(result.isValid8("1-Jan-14"));
		Assert.assertTrue(result.isValid8("10-Jan-14"));
	}

	@Test
	public void intuitHHMMSSTrain() throws Exception {
		DateTimeParser det = new DateTimeParser();
		det.train("12:57:03");
		det.train("13:45:00");
		det.train("8:03:59");

		DateTimeParserResult result = det.getResult();
		Assert.assertEquals(result.getFormatString(), "H:mm:ss");

		Assert.assertTrue(result.isValid("12:57:03"));
		Assert.assertTrue(result.isValid("8:03:59"));
		Assert.assertFalse(result.isValid("8:03:599"));
		Assert.assertFalse(result.isValid("118:03:59"));
		Assert.assertFalse(result.isValid("118:3:59"));
		Assert.assertFalse(result.isValid("118:333:59"));

		Assert.assertTrue(result.isValid8("12:57:03"));
		Assert.assertTrue(result.isValid8("8:03:59"));
		Assert.assertFalse(result.isValid8("8:03:599"));
		Assert.assertFalse(result.isValid8("118:03:59"));
		Assert.assertFalse(result.isValid8("118:3:59"));
		Assert.assertFalse(result.isValid8("118:333:59"));
	}
}