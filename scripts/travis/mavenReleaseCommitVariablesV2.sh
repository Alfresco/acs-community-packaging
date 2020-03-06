#!/usr/bin/env bash
set -ev

commitMessage=$1
echo "the commit message is $commitMessage"
releaseVersion=$(echo $commitMessage | grep -Po '\[release[^\]]*=\K[^\]]*(?=\])')
developmentVersion=$(echo $commitMessage | grep -Po '\[devRelease[^\]]*=\K[^\]]*(?=\])')

scm_path=$(mvn help:evaluate -Dexpression=project.scm.url -q -DforceStdout)
# Use full history for release
git checkout -B "${TRAVIS_BRANCH}"
# Add email to link commits to user
git config user.email "${GIT_EMAIL}"
echo "releaseVersion is assigned $releaseVersion"
echo "developmentVersion is assigned $developmentVersion"
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
    echo "Reaching this point means we have successfully extract the release and development versions from the commit message"
    echo "releaseVersion=$releaseVersion"
    echo "developmentVersion=$developmentVersion"
fi