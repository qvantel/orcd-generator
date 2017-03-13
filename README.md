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
Use -v flag for verbose and -s 60 for 60 seconds sleep and -g for graphite reporting 

