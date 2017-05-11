[![Build Status](https://travis-ci.org/flygare/orcd-generator.svg?branch=master)](https://travis-ci.org/flygare/orcd-generator)
[![codecov](https://codecov.io/gh/flygare/orcd-generator/branch/master/graph/badge.svg)](https://codecov.io/gh/flygare/orcd-generator)

# Generator

## Description
This service is used to generate fake CDRs(Call Data Records) to Cassandra using Spark. The generator also sends performance metrics to Graphite through Kamon(statsD).
The service can be modified to meet specific requirements such as country specific trends and change what products should be generated.

## Usage
Run CDRGenerator:
```
$ sbt run
```

Run just tests:
```
$ sbt test
```

Run the tests with enabled coverage:
```
$ sbt clean coverage test
```

To generate the coverage reports run
```
$ sbt coverageReport
```

Run a performance test:
```
$ ./cdr_perf_test.sh
```
Use -s 60 for 60 seconds intervals, -g to report to graphite and -l 30 to loop every 30s

## Changing configuration
You can find the application config in
```
$ src/main/resources/application.conf
```
You can change settings such as the Cassandra IP, amount of CDRs generated and how far back in time the generator should generate data for. 

## Changing trends

You can find the trend settings under
```
$ src/main/resources/trends
```
Then there are different folders where each folder represents a service.

### Changing trend settings

A trend is represented by a json file. There are three main parts to the json file, the name for the trend, the service type used (mms, sms, data, voice) and a list of points. Each point is representing a value of how many cdrs should be generated at a certain point in time.

### Changing Country settings

The trend file has a list of countries represented by an array where each item contains an iso and a modifier value for that country.

The trend file also has a default modifier, this represents the value that all the countries that are not specified in the countires list will get. So if the trend should be country specific, set this to 0.

