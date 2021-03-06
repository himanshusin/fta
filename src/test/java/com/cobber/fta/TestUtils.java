package com.cobber.fta;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Locale;

public class TestUtils {
	final static String validZips = "01770|01772|01773|02027|02030|02170|02379|02657|02861|03216|03561|03848|04066|04281|04481|04671|04921|05072|05463|05761|" +
			"06045|06233|06431|06704|06910|07101|07510|07764|08006|08205|08534|08829|10044|10260|10549|10965|11239|11501|11743|11976|" +
			"12138|12260|12503|12746|12878|13040|13166|13418|13641|13801|14068|14276|14548|14731|14865|15077|15261|15430|15613|15741|" +
			"15951|16210|16410|16662|17053|17247|17516|17765|17951|18109|18428|18702|18957|19095|19339|19489|19808|20043|20170|20370|" +
			"20540|20687|20827|21047|21236|21779|22030|22209|22526|22741|23016|23162|23310|23503|23868|24038|24210|24430|24594|24856|" +
			"25030|25186|25389|25638|25841|26059|26524|26525|26763|27199|27395|27587|27832|27954|28119|28280|28397|28543|28668|28774|" +
			"29111|29329|29475|29622|29744|30016|30119|30235|30343|30503|30643|31002|31141|31518|31724|31901|32134|32297|32454|32617|" +
			"32780|32934|33093|33265|33448|33603|33763|33907|34138|34470|34731|35053|35221|35491|35752|36022|36460|36616|36860|37087|";

	final static String validUSStates = "AL|AK|AZ|KY|KS|LA|ME|MD|MI|MA|MN|MS|MO|NE|MT|SD|TN|TX|UT|VT|WI|" +
			"VA|WA|WV|HI|ID|IL|IN|IA|KS|KY|LA|ME|MD|MA|MI|MN|MS|MO|MT|NE|NV|" +
			"NH|NJ|NM|NY|NC|ND|OH|OK|OR|PA|RI|SC|SD|TN|TX|UT|VT|VA|WA|WV|WI|" +
			"WY|AL|AK|AZ|AR|CA|CO|CT|DC|DE|FL|GA|HI|ID|IL|IN|IA|KS|KY|LA|ME|" +
			"MD|MA|MI|MN|MS|MO|MT|NE|NV|NH|NJ|NM|NY|NC|ND|OH|OK|OR|RI|SC|SD|" +
			"TX|UT|VT|WV|WI|WY|NV|NH|NJ|OR|PA|RI|SC|AR|CA|CO|CT|ID|HI|IL|IN|";

	final static String validCAProvinces = "AB|BC|MB|NB|NL|NS|NT|NU|ON|PE|QC|SK|YT|" +
			"AB|BC|MB|NB|NL|NS|NT|NU|ON|PE|QC|SK|YT|" +
			"AB|BC|MB|NB|NL|NS|NT|NU|ON|PE|QC|SK|YT|" +
			"AB|BC|MB|NB|NL|NS|NT|NU|ON|PE|QC|SK|YT|" +
			"AB|BC|MB|NB|NL|NS|NT|NU|ON|PE|QC|SK|YT|";

	// Set of valid months + 4 x "UNK"
	final static String months = "Jan|Mar|Jun|Jul|Feb|Dec|Apr|Nov|Apr|Oct|May|Aug|Aug|Jan|Jun|Sep|Nov|Jan|" +
			"Dec|Oct|Apr|May|Jun|Jan|Feb|Mar|Oct|Nov|Dec|Jul|Aug|Sep|Jan|Oct|Oct|Oct|" +
			"Jan|Mar|Jun|Jul|Feb|Dec|Apr|Nov|Apr|Oct|May|Aug|Aug|Jan|Jun|Sep|Nov|Jan|" +
			"Dec|Oct|Apr|May|Jun|Jan|Feb|Mar|Oct|Nov|Dec|Jul|Aug|Sep|Jan|Oct|Oct|Oct|" +
			"Jan|Mar|Jun|Jul|Feb|Dec|UNK|Nov|Apr|Oct|May|Aug|Aug|Jan|Jun|Sep|Nov|Jan|" +
			"Dec|Oct|Apr|May|Jun|Jan|Feb|Mar|Oct|Nov|Dec|Jul|Aug|UNK|Sep|Jan|Oct|Oct|Oct|" +
			"Jan|UNK|Jun|Jul|Feb|Dec|Apr|Nov|Apr|Oct|May|Aug|Aug|Jan|Jun|Sep|Nov|Jan|" +
			"Dec|Oct|Apr|May|May|Jan|Feb|Mar|Oct|Nov|Dec|Jul|Aug|Sep|Jan|Oct|Oct|Oct|" +
			"Jan|Mar|Jun|Jul|Feb|Dec|Apr|Nov|Apr|Oct|May|Aug|Aug|Jan|Jun|Sep|Nov|Jan|" +
			"Dec|Oct|Apr|May|Jun|Jan|Feb|Mar|Oct|Nov|Dec|Jul|Aug|UNK|Sep|Jan|Oct|Oct|Oct|";

	// Set of valid months + 4 x "UNK"
	final static String monthsFrench =
			"janv.|févr.|mars|avr.|mai|juin|juil.|août|sept.|oct.|nov.|déc.|" +
					"janv.|févr.|mars|avr.|mai|juin|juil.|août|sept.|oct.|nov.|déc.|" +
					"janv.|févr.|mars|avr.|mai|juin|juil.|août|sept.|oct.|nov.|déc.|" +
					"janv.|févr.|mars|avr.|mai|juin|juil.|août|sept.|oct.|nov.|déc.|" +
					"janv.|févr.|mars|UNK|mai|juin|juil.|août|sept.|oct.|nov.|déc.|" +
					"janv.|févr.|mars|avr.|mai|juin|juil.|août|sept.|oct.|nov.|déc.|" +
					"janv.|févr.|mars|avr.|mai|UNK|juil.|août|sept.|oct.|nov.|déc.|" +
					"janv.|févr.|mars|UNK|mai|juin|juil.|août|sept.|UNK|nov.|déc.|";

	final static String valid3166_2 =  "AL|AW|BZ|BW|BV|BR|IO|BN|BG|" +
			"BF|BI|CV|KH|CF|CK|DM|FK|GE|" +
			"GG|IS|JP|LA|LT|LU|MO|MK|MG|" +
			"MW|MY|MV|ML|MT|MU|MZ|NG|PG|" +
			"RO|WS|SK|SR|TG|TC|TV|UG|UA|" +
			"AE|GB|UM|US|UY|UZ|VG|";

	final static String valid3166_3 = "ALA|ARM|BEL|BIH|BWA|BVT|BRA|IOT|BRN|BGR|BFA|" +
			"BDI|CPV|CYM|COG|DJI|ETH|GMB|GTM|HUN|JAM|KGZ|" +
			"LIE|LTU|LUX|MAC|MKD|MDG|MWI|MYS|MDV|MLI|MRT|" +
			"MAR|NER|PAN|REU|VCT|SXM|SDN|TLS|TKM|TCA|TUV|" +
			"UGA|UKR|ARE|GBR|UMI|USA|URY|VNM|";

	final static String validUSStreets[] = new String[] {
			"9885 Princeton Court",
			"11 San Pablo Rd.",
			"365 3rd St.",
			"426 Brewery Street",
			"676 Thatcher St.",
			"848 Hawthorne St.",
			"788 West Coffee St.",
			"240 Arnold Avenue",
			"25 S. Hawthorne St.",
			"9314 Rose Street",
			"32 West Bellevue St.",
			"8168 Thomas Road",
			"353 Homewood Ave.",
			"14 North Cambridge Street",
			"30 Leeton Ridge Drive",
			"8412 North Mulberry Dr.",
			"7691 Beacon Street",
			"187 Lake View Drive",
			"318 Summerhouse Road",
			"609 Taylor Ave.",
			"47 Broad St.",
			"525 Valley View St.",
			"8 Greenview Ave.",
			"86 North Helen St.",
			"8763 Virginia Street",
			"10 Front Avenue",
			"141 Blue Spring Street",
			"99 W. Airport Ave.",
			"32 NW. Rocky River Ave.",
			"324 North Lancaster Dr."
	};



	static String
	getNegativePrefix(Locale locale) {
		String negPrefix = "-";
		NumberFormat nf = NumberFormat.getIntegerInstance(locale);
		if (nf instanceof DecimalFormat) {
			negPrefix = ((DecimalFormat) nf).getNegativePrefix();
			if (!negPrefix.isEmpty())
				if ("-".equals(negPrefix))
					negPrefix = "[+-]?";
				else
					negPrefix = RegExpGenerator.slosh(negPrefix) + "?";
		}
		return negPrefix;
	}

	static String
	getNegativeSuffix(Locale locale) {
		String negSuffix = "";
		NumberFormat nf = NumberFormat.getIntegerInstance(locale);
		if (nf instanceof DecimalFormat) {
			negSuffix = ((DecimalFormat) nf).getNegativeSuffix();
			if (!negSuffix.isEmpty())
				if ("-".equals(negSuffix))
					negSuffix = "[+-]?";
				else
					negSuffix = RegExpGenerator.slosh(negSuffix) + "?";
		}
		return negSuffix;
	}

	static boolean isValidLocale(String value) {
		Locale[] locales = Locale.getAvailableLocales();
		for (Locale locale : locales) {
			if (value.equals(locale.toString())) {
				return true;
		    }
		}
		return false;
	}

}
