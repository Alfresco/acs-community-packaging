#!/usr/bin/env bash
set -ev

DEPLOYMENT_DIR=deploy_dir

mkdir $DEPLOYMENT_DIR -p
cp war/target/alfresco.war ${DEPLOYMENT_DIR}
cp distribution/target/alfresco-content-services-community-distribution-$release_version.zip ${DEPLOYMENT_DIR}