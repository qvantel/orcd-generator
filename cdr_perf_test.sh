#!/bin/bash

graphite_host=127.0.0.1
graphite_port=2003

interval=10
loop=0
report_to_graphite=0
cassandra_name="cassandra_qvantel"

USAGE="$0 [-s 0-9|-l 0-9|-g]"

while [[ $# -gt 0 ]]; do
    key="$1"
    case $key in
        -s)
            if [ -z "$2" ]; then
                echo "Argument -s needs an value"
		exit 1
            fi
            interval="$2"
            shift # past argument
        ;;
        -l)
            if [ -z "$2" ]; then
                echo "Argument -l needs an value"
		exit 1
            fi
            loop="$2"
            shift # past argument
        ;;
        -g)
            report_to_graphite=1
        ;;
        *) # unknown option
            echo "Unknown argument $1"
            echo $USAGE
            exit 1
        ;;
    esac
    shift
done

# Check how many cdr entries were created 10s ago (to make sure that all data is commited)
start_time_relative=$(( -10 - $interval ))
end_time_relative=$(( 0 - $interval ))

CQLSH="docker exec -i "$cassandra_name" cqlsh"
CQLSH="cqlsh --cqlversion=3.4.2"

function count_cdr() {
    QUERY_LAST="SELECT created_at FROM qvantel.cdr WHERE clustering_key=0 ORDER BY created_at DESC LIMIT 1;"
    last_ts=$($CQLSH -e "$QUERY_LAST" | head -n 4 | tail -n 1 | tr -d ' ')

    echo 'Sleeping '$interval's'
    sleep $interval
    
    QUERY_DIFF="SELECT count(id) FROM qvantel.cdr WHERE created_at > $last_ts ALLOW FILTERING;"
    count=$($CQLSH -e "$QUERY_DIFF" | head -n 4 | tail -n 1 | tr -d ' ')

    # If fetch fails (isn't a number), set number to 0
    [ -n "${count##*[!0-9]*}"  ] || count=0

    throughput=$(( $count / $interval ))

    # Print results
    echo "$throughput cdr/s (fetched $count events in $interval seconds)"

    # Report to graphite
    if [ "$report_to_graphite" -ne 0 ]; then
        timestamp=$(date +%s)

        echo "qvantel.cdrgenerator.throughput $throughput $timestamp" | timeout 1 nc $graphite_host $graphite_port &> /dev/null
        echo "qvantel.cdrgenerator.entries $count_total $timestamp" | timeout 1 nc $graphite_host $graphite_port &> /dev/null
    fi
}


if [ "$loop" -gt 0 ]; then
    interval=$loop
    echo "Loop interval: $loop"
    while [ true ] ; do
        target_time=$(date -d "$loop seconds" +%s)
        count_cdr
        current_time=$(date +%s)
        sleep_time=$(( $target_time - $current_time ))
        echo $sleep_time | grep -q '-' && sleep_time=0 # If sleep time is negative, set it to 0
        sleep $sleep_time
    done
else
    echo "Interval: $interval"
    count_cdr
fi
