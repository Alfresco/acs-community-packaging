#!/usr/bin/env bash
set -e

if [ -z ${RELEASE_VERSION} ];
then
  echo "Please provide a RELEASE_VERSION in the format <acs-version>-<additional-info> (6.3.0-EA or 6.3.0-SNAPSHOT)"
  exit -1
fi

DEPLOYMENT_DIR=deploy_dir

mkdir $DEPLOYMENT_DIR -p
cp war/target/alfresco.war ${DEPLOYMENT_DIR}
cp distribution/target/alfresco-content-services-community-distribution-$RELEASE_VERSION.zip ${DEPLOYMENT_DIR}