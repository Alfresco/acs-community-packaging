#!/usr/bin/env bash
echo "=========================== Starting Copy to Release Bucket Script ==========================="
PS4="\[\e[35m\]+ \[\e[m\]"
set -vex

#
# Copy from S3 Release bucket to S3 eu.dl bucket
#

if [ -z "${RELEASE_VERSION}" ]; then
  echo "Please provide a RELEASE_VERSION in the format <acs-version>-<additional-info> (7.2.0-EA or 7.2.0-SNAPSHOT)"
  exit 1
fi

SOURCE="s3://alfresco-artefacts-staging/alfresco-content-services-community/release/${TRAVIS_BRANCH}/${TRAVIS_BUILD_NUMBER}"
DESTINATION="s3://eu.dl.alfresco.com/release/community/${RELEASE_VERSION}-build-${TRAVIS_BUILD_NUMBER}"

printf "\n%s\n%s\n" "${SOURCE}" "${DESTINATION}"

aws s3 cp --acl private \
  "${SOURCE}/alfresco.war" \
  "${DESTINATION}/alfresco.war"

aws s3 cp --acl private \
  "${SOURCE}/alfresco-content-services-community-distribution-${RELEASE_VERSION}.zip" \
  "${DESTINATION}/alfresco-content-services-community-distribution-${RELEASE_VERSION}.zip"


set +vex
echo "=========================== Finishing Copy to Release Bucket Script =========================="

