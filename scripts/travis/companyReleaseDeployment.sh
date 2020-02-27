#!/usr/bin/env bash
set -e

if [ ${COMM_RELEASE_VERSION} = "" ];
then
  exit -1
fi

build_number=$1
branch_name=$2
build_stage=release
SOURCE=s3://alfresco-artefacts-staging/alfresco-content-services-community/$build_stage/$branch_name/$build_number
DESTINATION=s3://eu.dl.alfresco.com/release/community/$COMM_RELEASE_VERSION-build-$build_number

aws s3 cp --acl private $SOURCE/alfresco.war $DESTINATION/alfresco.war
aws s3 cp --acl private $SOURCE/alfresco-content-services-community-distribution-$release_version.zip $DESTINATION/alfresco-content-services-community-distribution-$release_version.zip