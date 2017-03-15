#!/bin/bash

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

function count_cdr() {
    # Set start and end time
    start_date=$(date --iso-8601=s --date "$start_time_relative seconds")
    end_date=$(date --iso-8601=s --date "$end_time_relative seconds")
    echo "Counting between $start_date and $end_date"

    QUERY_CALL="SELECT count(id) FROM qvantel.call WHERE created_at > '$start_date' AND created_at < '$end_date' ALLOW FILTERING;"
    QUERY_PRODUCT="SELECT count(id) FROM qvantel.product WHERE created_at > '$start_date' AND created_at < '$end_date' ALLOW FILTERING;"
    QUERY_CALL_TOTAL="SELECT count(id) FROM qvantel.call;"
    QUERY_PRODUCT_TOTAL="SELECT count(id) FROM qvantel.product;"

    echo "Counting calls..."
    call_count=$($CQLSH -e "$QUERY_CALL" | head -n 4 | tail -n 1 | tr -d ' ')
    call_count_total=$($CQLSH -e "$QUERY_CALL_TOTAL" | head -n 4 | tail -n 1 | tr -d ' ')

    echo "Counting products..."
    product_count=$($CQLSH -e "$QUERY_PRODUCT" | head -n 4 | tail -n 1 | tr -d ' ')
    product_count_total=$($CQLSH -e "$QUERY_PRODUCT_TOTAL" | head -n 4 | tail -n 1 | tr -d ' ')

    # If fetch fails (isn't a number), set number to 0
    [ -n "${call_count##*[!0-9]*}" ]          || call_count=0
    [ -n "${call_count_total##*[!0-9]*}" ]    || call_count_total=0
    [ -n "${product_count##*[!0-9]*}" ]       || product_count=0
    [ -n "${product_count_total##*[!0-9]*}" ] || product_count_total=0

    call_throughput=$(( $call_count / $interval ))
    product_throughput=$(( $product_count / $interval ))

    # Print results
    echo "Call: $call_throughput cdr/s ($call_count in interval, $call_count_total total)"
    echo "Product: $product_throughput cdr/s ($product_count in interval, $product_count_total total)"
    total_throughput=$(( $call_throughput + $product_throughput ))
    total_count=$(( $call_count_total + $product_count_total ))
    echo "Total: $total_throughput cdr/s ($total_count cdr records in the database)"

    # Report to graphite
    if [ "$report_to_graphite" -ne 0 ]; then
        timestamp=$(date +%s)

        echo "qvantel.cdrgenerator.call.throughput $call_throughput $timestamp" | timeout 1 nc 0.0.0.0 2003 &> /dev/null
        echo "qvantel.cdrgenerator.call.entries $call_count_total $timestamp" | timeout 1 nc 0.0.0.0 2003 &> /dev/null

        echo "qvantel.cdrgenerator.product.throughput $product_throughput $timestamp" | timeout 1 nc 0.0.0.0 2003 &> /dev/null
        echo "qvantel.cdrgenerator.product.entries $product_count_total $timestamp" | timeout 1 nc 0.0.0.0 2003 &> /dev/null

        echo "qvantel.cdrgenerator.throughput $total_throughput $timestamp" | timeout 1 nc 0.0.0.0 2003 &> /dev/null
        echo "qvantel.cdrgenerator.entries $total_count $timestamp" | timeout 1 nc 0.0.0.0 2003 &> /dev/null
    fi
}


echo "Interval: $interval"
if [ "$loop" -gt 0 ]; then
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
    count_cdr
fi
