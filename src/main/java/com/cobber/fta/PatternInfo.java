package com.cobber.fta;

/*
 * Copyright 2017 Tim Segall
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
 *
 * The PatternInfo class maintains a set of information about a simple pattern.  This is used
 * to derive a Type from a pattern.  For example,
 * 		new PatternInfo("(?i)true|false", null, "", "Boolean", null)
 * indicates that a case insensitive match for true or false indicates a boolean type ("Boolean").
 */
public class PatternInfo {
	String pattern;
	String generalPattern;
	String format;
	String type;
	String typeQualifier;
	
	/**
	 * Construct a new information block for the supplied pattern.
	 * @param pattern The pattern of interest.
	 * @param generalPattern The general case of this pattern (optional).
	 * @param format The Java format specified for a date pattern (optional). 
	 * @param type The type of the pattern.
	 * @param typeQualifier The type qualifier of the pattern (optional).
	 */
	public PatternInfo(String pattern, String generalPattern, String format, String type, String typeQualifier) {
		this.pattern = pattern;
		this.generalPattern = generalPattern;
		this.format = format;
		this.type = type;
		this.typeQualifier = typeQualifier;
	}
	
	
	/**
	 * Is this pattern Numeric?
	 * @return A boolean indicating if the Type for this pattern is numeric.
	 */
	public boolean isNumeric() {
		return "Long".equals(this.type) || "Double".equals(this.type);
	}
}
