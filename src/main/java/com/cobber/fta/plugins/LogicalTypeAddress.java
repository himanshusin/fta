package com.cobber.fta.plugins;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import com.cobber.fta.LogicalTypeInfinite;
import com.cobber.fta.PatternInfo;
import com.cobber.fta.PatternInfo.Type;
import com.cobber.fta.TextAnalyzer;

public class LogicalTypeAddress extends LogicalTypeInfinite {
	private static Set<String> addressMarkers = new HashSet<String>();

	@Override
	public boolean initialize(Locale locale) {
		threshold = 90;

		try (BufferedReader reader = new BufferedReader(new InputStreamReader(TextAnalyzer.class.getResourceAsStream("/reference/address_markers.csv")))){
			String line = null;

			while ((line = reader.readLine()) != null) {
				addressMarkers.add(line);
			}
		} catch (IOException e) {
			throw new IllegalArgumentException("Internal error: Issues with Address database");
		}

		return true;
	}

	@Override
	public String getQualifier() {
		return "ADDRESS_EN";
	}

	@Override
	public String getRegexp() {
		return ".+";
	}

	@Override
	public Type getBaseType() {
		return PatternInfo.Type.STRING;
	}

	@Override
	public boolean isValid(String input) {
		int length = input.length();

		// Attempt to fail fast
		if (length > 60)
			return false;

		// Simple case first - last 'word is something we recognize
		int spaceIndex = input.lastIndexOf(' ');
		if (spaceIndex != -1 && addressMarkers.contains(input.substring(spaceIndex + 1).toUpperCase(Locale.ENGLISH)))
			return true;

		// Accept something of the form, initial digit followed by an address marker word (e.g. Road, Street, etc).
		if (!Character.isDigit(input.charAt(0)))
			return false;

		String[] words = input.split(" ");
		if (words.length < 4)
			return false;

		for (int i = 1; i < words.length  - 1; i++) {
			if (addressMarkers.contains(words[i].toUpperCase(Locale.ENGLISH)))
				return true;
		}

		return false;
	}

	@Override
	public boolean isCandidate(String input, StringBuilder compressed, int[] charCounts, int[] lastIndex) {
		int spaceIndex = lastIndex[' '];
		if (spaceIndex != -1 && addressMarkers.contains(input.substring(spaceIndex + 1, input.length()).toUpperCase(Locale.ENGLISH)))
			return true;

		if (!Character.isDigit(input.charAt(0)) || charCounts[' '] < 3)
			return false;

		String[] words = input.split(" ");
		for (int i = 1; i < words.length  - 1; i++) {
			if (addressMarkers.contains(words[i].toUpperCase(Locale.ENGLISH)))
				return true;
		}

		return false;
	}

	@Override
	public String isValidSet(String dataStreamName, long matchCount, long realSamples, Map<String, Integer> cardinality, Map<String, Integer> outliers) {
		return (double)matchCount/realSamples >= getThreshold()/100.0 ? null : ".+";
	}
}
