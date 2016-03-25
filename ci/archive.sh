#!/bin/bash -ex

pushd `dirname $0`/.. > /dev/null
root=$(pwd -P)
popd > /dev/null

# gather some data about the repo
source $root/ci/vars.sh

# Build Spring-boot JAR
mvn clean package -U

# Path to output JAR
src=$(find $root/target/. -name $APP*.$EXT -a ! -name '*tests.jar')

# stage the artifact for a mvn deploy
mv $src $root/$APP.$EXT
