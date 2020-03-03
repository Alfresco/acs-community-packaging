#!/usr/bin/env bash
set -ev

if [ -z ${RELEASE_VERSION} ];
then
  exit -1
fi

DEPLOYMENT_DIR=deploy_dir

mkdir $DEPLOYMENT_DIR -p
cp war/target/alfresco.war ${DEPLOYMENT_DIR}
cp distribution/target/alfresco-content-services-community-distribution-$RELEASE_VERSION.zip ${DEPLOYMENT_DIR}