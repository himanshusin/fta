package com.cobber.fta.examples;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.cobber.fta.LogicalTypeFinite;

public class PluginColor extends LogicalTypeFinite {
	private static Set<String> members = new HashSet<String>();
	private static String colors[] = new String[] {
			"RED",  "GREEN", "BLUE", "PINK", "BLACK", "WHITE", "ORANGE", "PURPLE",
			"GREY", "GREEN", "YELLOW", "MAUVE", "CREAM", "BROWN", "SILVER", "GOLD",
			"PEACH", "OLIVE", "LEMON", "LILAC", "BEIGE", "AMBER", "BURGUNDY"
	};

	static {
		members.addAll(Arrays.asList(colors));
	}

	@Override
	public Set<String> getMembers() {
		return members;
	}

	@Override
	public String getQualifier() {
		return "COLOR_EN";
	}

	@Override
	public String getRegExp() {
		return ".+";
	}

	@Override
	public String isValidSet(String dataStreamName, long matchCount, long realSamples, Map<String, Integer> cardinality,
			Map<String, Integer> outliers) {
		if (outliers.size() > 3)
			return ".+";

		if ((double)matchCount / realSamples >= getThreshold()/100.0)
			return null;

		return ".+";
	}
}
