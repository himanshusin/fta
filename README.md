# Fast Text Analyzer #

Analyze Text data to determine type information and other key metrics associated with a text stream.
A key objective of the analysis is that it should be sufficiently fast to be in-line (e.g. as the
data is input from some source it should be possible to stream the data through this class without
undue performance degradation).

Typical usage is:
```java
  		TextAnalyzer analysis = new TextAnalyzer("Age");
 
  		analysis.train("12");
  		analysis.train("62");
  		analysis.train("21");
  		analysis.train("37");
  		...
 
  		TextAnalysisResult result = analysis.getResult();
```

## Building ##

`$ gradle wrapper --gradle-version 4.0`

`$ ./gradlew installDist`

## Running Tests ##
`$ ./gradlew test jacocoTestReport`

## Generate JavaDoc ##
`$ ./gradlew javadoc`

## Setup eclipse project  ##
`$ ./gradlew eclipse`

