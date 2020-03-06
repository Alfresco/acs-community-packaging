#!/usr/bin/env bash
set -ev
. ./scripts/travis/common_functions.sh

releaseVersion=$(extractVariable "release" "$TRAVIS_COMMIT_MESSAGE")
developmentVersion=$(extractVariable "devRelease" "$TRAVIS_COMMIT_MESSAGE")

scm_path=$(mvn help:evaluate -Dexpression=project.scm.url -q -DforceStdout)
# Use full history for release
git checkout -B "${TRAVIS_BRANCH}"
# Add email to link commits to user
git config user.email "${GIT_EMAIL}"

if [ -z ${releaseVersion} ] || [ -z ${developmentVersion} ]; 
    then echo "Please provide a Release and Development verison in the format <acs-version>-<additional-info> (6.3.0-EA or 6.3.0-SNAPSHOT) by adding a commit message"
         exit -1
    # TODO: Set up continuous release. As of REPO-4735 the following is not required if release stage is manual
    # mvn --batch-mode \
    # -Dusername="${GIT_USERNAME}" \
    # -Dpassword="${GIT_PASSWORD}" \
    # -Dbuild-number=${TRAVIS_BUILD_NUMBER} \
    # -Dbuild-name="${TRAVIS_BUILD_STAGE_NAME}" \
    # -Dscm-path=${scm_path} \
    # -DscmCommentPrefix="[maven-release-plugin][skip ci]" \
    # -DskipTests \
    # "-Darguments=-DskipTests -Dbuild-number=${TRAVIS_BUILD_NUMBER} '-Dbuild-name=${TRAVIS_BUILD_STAGE_NAME}' -Dscm-path=${scm_path} " \
    # -Prelease \
    # release:prepare release:perform
else   
    # mvn --batch-mode \
    # -Dusername="${GIT_USERNAME}" \
    # -Dpassword="${GIT_PASSWORD}" \
    # -DreleaseVersion=${releaseVersion} \
    # -DdevelopmentVersion=${developmentVersion} \
    # -Dbuild-number=${TRAVIS_BUILD_NUMBER} \
    # -Dbuild-name="${TRAVIS_BUILD_STAGE_NAME}" \
    # -Dscm-path=${scm_path} \
    # -DscmCommentPrefix="[maven-release-plugin][skip ci]" \
    # -DskipTests \
    # "-Darguments=-DskipTests -Dbuild-number=${TRAVIS_BUILD_NUMBER} '-Dbuild-name=${TRAVIS_BUILD_STAGE_NAME}' -Dscm-path=${scm_path} " \
    # release:clean release:prepare release:perform \
    # -Prelease
    echo "We're going to skip the release this time."
    echo "Reaching this point means we have successfully extracted the release and development versions from the commit message"
    echo "releaseVersion=$releaseVersion"
    echo "developmentVersion=$developmentVersion"
fi