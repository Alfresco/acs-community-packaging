#!/usr/bin/env bash
set -e
. ./scripts/travis/common_functions.sh

releaseVersion=$(extractVariable "release" "$TRAVIS_COMMIT_MESSAGE")
comReleaseVersion==$(extractVariable "comRelease" "$TRAVIS_COMMIT_MESSAGE")
if [ -z ${comReleaseVersion} ] || [ -z ${releaseVersion} ];
then
  echo "Please provide a comReleaseVersion and releaseVersion in the format <acs-version>-<additional-info> (eg. 6.3.0-EA or 6.3.0-SNAPSHOT)"
  exit -1
fi

echo "Successfully provided releaseVersion=$releaseVersion and comReleaseVersion=$comReleaseVersion"
# build_number=$1
# branch_name=$2
# build_stage=release
# SOURCE=s3://alfresco-artefacts-staging/alfresco-content-services-community/$build_stage/$branch_name/$build_number
# DESTINATION=s3://eu.dl.alfresco.com/release/community/$comReleaseVersion-build-$build_number

# aws s3 cp --acl private $SOURCE/alfresco.war $DESTINATION/alfresco.war
# aws s3 cp --acl private $SOURCE/alfresco-content-services-community-distribution-$releaseVersion.zip $DESTINATION/alfresco-content-services-community-distribution-$releaseVersion.zip