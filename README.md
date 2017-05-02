[![Build Status](https://travis-ci.com/flygare/QvantelCDRGenerator.svg?token=B6YLB31LLNNKsSzKXpCe&branch=master)](https://travis-ci.com/flygare/QvantelCDRGenerator)

# QvantelCDRGenerator

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

## Changing trends

You can find the trend settings under
```
$ src/main/resources/trends
```
Then there are different folders where each folder represents a service.

### Changing trend settings

A trend is represented by a json file. There are 3 main parts to the json file, the name for the trend, the service type used (mms, sms, data, voice) and a list of points. Each point is representing a value of how many cdrs should be generated at a certain point in time.

### Changing Country settingst
The trend file has a list of countries represented by an array, each item with the iso and modifier value for that country.

The trend file also has a default modifier, this represents the value that all the countries that are not in the countires list will get. So if the trend should be country specific, set this to 0.
