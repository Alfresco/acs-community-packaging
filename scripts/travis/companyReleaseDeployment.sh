#!/usr/bin/env bash
set -ev
. ./scripts/travis/common_functions.sh

releaseVersion=$(extractVariable "release" "$TRAVIS_COMMIT_MESSAGE")
commReleaseVersion==$(extractVariable "commRelease" "$TRAVIS_COMMIT_MESSAGE")
if [ -z ${commReleaseVersion} ] || [ -z ${releaseVersion} ];
then
  echo "Please provide a commReleaseVersion and releaseVersion in the format <acs-version>-<additional-info> (eg. 6.3.0-EA or 6.3.0-SNAPSHOT)"
  exit -1
fi

echo "Successfully provided releaseVersion=$releaseVersion and commReleaseVersion=$commReleaseVersion"
# build_number=$1
# branch_name=$2
# build_stage=release
# SOURCE=s3://alfresco-artefacts-staging/alfresco-content-services-community/$build_stage/$branch_name/$build_number
# DESTINATION=s3://eu.dl.alfresco.com/release/community/$commReleaseVersion-build-$build_number

# aws s3 cp --acl private $SOURCE/alfresco.war $DESTINATION/alfresco.war
# aws s3 cp --acl private $SOURCE/alfresco-content-services-community-distribution-$releaseVersion.zip $DESTINATION/alfresco-content-services-community-distribution-$releaseVersion.zip