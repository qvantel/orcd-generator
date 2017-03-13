#!/bin/bash

verbose=0
sleep_time_in_seconds=10
report_to_graphite=0
cassandra_name="cassandra"

while [[ $# -gt 0 ]]; do
    key="$1"
    case $key in
        -v)
            verbose=1
            echo "Running in verbose mode"
        ;;
        -s)
            if [ -z "$2" ]; then
                echo "Argument -s needs an value"
		exit 1
            fi
            sleep_time_in_seconds="$2"
            echo "Sleep time: $sleep_time_in_seconds"
            shift # past argument
        ;;
        -g)
            report_to_graphite=1
	    echo "Reporting to graphite is not yet implemented"
	    exit 1
        ;;
        *) # unknown option
            echo "Unknown argument $1"
            exit 1
        ;;
    esac
    shift
done



retrieval_date=$(date +%s)
test $verbose -eq 1 && echo "Running cassandra fetch"
docker exec -i "$cassandra_name" cqlsh << EOF > output
use qvantel;
select count(*) as call from call;
select count(*) as product from product;
EOF
test $verbose -eq 1 && echo "Done with cassandra fetch"

# open file and read contents
list_of_measures=$(cat output | grep -oP '\s+[0-9]+' | sed 's#\ ##g')

call=$(echo "$list_of_measures" | head -n1)
test $verbose -eq 1 && echo "current no calls: $call"
product=$(echo "$list_of_measures" | tail -n1)
test $verbose -eq 1 && echo "current no products: $product"


if [ -f output ] ; then
    rm output;
fi

test $verbose -eq 1 && echo "Sleeping for $sleep_time_in_seconds"
sleep "$sleep_time_in_seconds"

retrieval_date2=$(date +%s)
test $verbose -eq 1 && echo "Running cassandra fetch"
docker exec -i cassandra cqlsh << EOF > output
use qvantel;
select count(*) as call from call;
select count(*) as product from product;
EOF
test $verbose -eq 1 && echo "Done with cassandra fetch"

# open file and read contents
list_of_measures=$(cat output | grep -oP '\s+[0-9]+' | sed 's#\ ##g')

call2=$(echo "$list_of_measures" | head -n1)
test $verbose -eq 1 && echo "current no calls: $call2"
product2=$(echo "$list_of_measures" | tail -n1)
test $verbose -eq 1 && echo "current no products: $product2"

real_call=$(expr $call2 - $call)
test $verbose -eq 1 && echo "diff calls: $real_call"

real_product=$(expr $product2 - $product)
test $verbose -eq 1 && echo "diff product: $real_product"


real_time=$(expr $retrieval_date2 - $retrieval_date)
call_per_sec=$(awk "BEGIN {print ($real_call/$real_time)}")
product_per_sec=$(awk "BEGIN {print ($real_product/$real_time)}")

test $verbose -eq 1 && echo "call: $call_per_sec"
test $verbose -eq 1 && echo "prod: $product_per_sec"

echo "{\"ts\": \"$retrieval_date2\", \"call_per_second\": $call_per_sec}"
echo "{\"ts\": \"$retrieval_date2\", \"product_per_second\": $product_per_sec}"

if [ -f output ] ; then
    rm output;
fi

