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
    # Set start and end time
    start_date=$(date --iso-8601=s --date "$start_time_relative seconds")
    end_date=$(date --iso-8601=s --date "$end_time_relative seconds")
    echo "Counting between $start_date and $end_date"

    QUERY_TOTAL="SELECT count(id) FROM qvantel.cdr;"

    echo "Counting 1"
    count_pre=$($CQLSH -e "$QUERY_TOTAL" | head -n 4 | tail -n 1 | tr -d ' ')
    echo "Sleeping"
    sleep $interval
    echo "Counting 2"
    count_post=$($CQLSH -e "$QUERY_TOTAL" | head -n 4 | tail -n 1 | tr -d ' ')

    # If fetch fails (isn't a number), set number to 0
    [ -n "${count_pre##*[!0-9]*}"  ] || count_pre=0
    [ -n "${count_post##*[!0-9]*}" ] || count_post=0

    echo $count_pre
    echo $count_post
    count=$(( $count_post - $count_pre ))
    throughput=$(( $count / $interval ))

    # Print results
    echo "$throughput cdr/s ($count_post cdr records in the database)"

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
