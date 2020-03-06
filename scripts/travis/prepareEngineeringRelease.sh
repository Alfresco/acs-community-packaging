#!/usr/bin/env bash
set -e
. ./scripts/travis/common_functions.sh

releaseVersion=$(extractVariable "release" "$TRAVIS_COMMIT_MESSAGE")

if [ -z ${releaseVersion} ];
then
  echo "Please provide a releaseVersion via commit message in the format [release=<acs-version>-<additional-info>] (eg. [release=6.3.0-EA])"
  exit -1
fi

DEPLOYMENT_DIR=deploy_dir

mkdir $DEPLOYMENT_DIR -p
cp war/target/alfresco.war ${DEPLOYMENT_DIR}
cp distribution/target/alfresco-content-services-community-distribution-$releaseVersion.zip ${DEPLOYMENT_DIR}