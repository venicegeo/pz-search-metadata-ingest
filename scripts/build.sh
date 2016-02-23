#!/bin/bash
pushd `dirname $0` > /dev/null
base=$(pwd -P)
popd > /dev/null


# Copy the cloud foundry properties file 
#hmmm service controller exemplar; useful as guide in the future?
#cp $base/../mainServiceController/conf/application-cf.properties $base/../mainServiceController/src/main/resources/application.properties

# Build Spring-boot JAR
mvn clean package

# Path to output JAR
jarfile=$base/../metadata-ingest-service/target/ingest-service-0.0.1-SNAPSHOT.jar

# Gather some data about the repo
source $base/vars.sh

# Do we have this artifact in s3? If not, upload it.
aws s3 ls $S3URL || aws s3 cp $jarfile $S3URL

# success if we have an artifact stored in s3.
aws s3 ls $S3URL
