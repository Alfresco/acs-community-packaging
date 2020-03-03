#!/usr/bin/env bash
set -ev

if [ -z ${release_version} ];
then
  exit -1
fi

DEPLOYMENT_DIR=deploy_dir

mkdir $DEPLOYMENT_DIR -p
cp war/target/alfresco.war ${DEPLOYMENT_DIR}
cp distribution/target/alfresco-content-services-community-distribution-$release_version.zip ${DEPLOYMENT_DIR}