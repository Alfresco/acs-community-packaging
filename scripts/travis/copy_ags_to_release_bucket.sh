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

SOURCE="s3://alfresco-artefacts-staging/community/RM/${RELEASE_VERSION}"
DESTINATION="s3://eu.dl.alfresco.com/release/community/RM/${RELEASE_VERSION}"

printf "\n%s\n%s\n" "${SOURCE}" "${DESTINATION}"

aws s3 cp --acl private \
  "${SOURCE}/alfresco-governance-services-community-${RELEASE_VERSION}.zip" \
  "${DESTINATION}/alfresco-governance-services-community-${RELEASE_VERSION}.zip"


set +vex
echo "=========================== Finishing Copy to Release Bucket Script =========================="

