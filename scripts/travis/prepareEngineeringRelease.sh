#!/usr/bin/env bash
set -e
. ./scripts/travis/common_functions.sh

releaseVersion=$(extractVariable "release" "$TRAVIS_COMMIT_MESSAGE")

if [ -z ${releaseVersion} ];
then
  echo "Please provide a releaseVersion in the format <acs-version>-<additional-info> (6.3.0-EA or 6.3.0-SNAPSHOT)"
  exit -1
fi
echo "releaseVersion=$releaseVersion"
# DEPLOYMENT_DIR=deploy_dir

# mkdir $DEPLOYMENT_DIR -p
# cp war/target/alfresco.war ${DEPLOYMENT_DIR}
# cp distribution/target/alfresco-content-services-community-distribution-$releaseVersion.zip ${DEPLOYMENT_DIR}