#!/bin/bash
##########################################################################################
## Do something after maven build.
##
## Include the following content:
## - Change directory
## - Delete some temporary files or directories
## - Create a output directory
## - Move files
##########################################################################################
mvn clean package -DskipTests=true
rc=$?
if [[ $rc -ne 0 ]] ; then
    echo "build failed"; exit $rc
fi

mkdir output