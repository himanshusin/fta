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

`$ gradle wrapper --gradle-version 5.0`

`$ ./gradlew installDist`

## Running Tests - including coverage ##
`$ ./gradlew test jacocoTestReport`

## Generate JavaDoc ##
`$ ./gradlew javadoc`

## Everything ...
`$ ./gradlew clean installDist test jacocoTestReport javadoc`

## Setup eclipse project ##
`$ ./gradlew eclipse`

## Releasing a new version ##
`$ ./gradlew uploadArchives`
Then go to http://central.sonatype.org/pages/releasing-the-deployment.html and follow the instructions!!
1. login to OSSRH available at https://oss.sonatype.org/
2. Find and select the latest version in the Staging Repository
3. Close the staging repository (wait until complete)
4. Release the staging repository

## Java code ##

To include the Java code in your application, or download the latest jars from the [Maven
repository](http://repo1.maven.org/maven2/com/cobber/fta/fta/).

## Javadoc ##

Javadoc is automatically updated to reflect the latest release at http://javadoc.io/doc/com.cobber.fta/fta/.
