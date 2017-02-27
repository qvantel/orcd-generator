#!/bin/bash
function eexit {
	echo "$1" && exit 1
}

which wget > /dev/null || eexit "ERROR: wget not installed"

if [ ! -f "mcc-mnc-table.json" ]
then
  wget http://raw.githubusercontent.com/musalbas/mcc-mnc-table/master/mcc-mnc-table.json
fi
