# QvantelCDRGenerator

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

