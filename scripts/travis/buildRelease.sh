#!/usr/bin/env bash
set -e

releaseVersion=$1
developmentVersion=$2
scm_path=$(mvn help:evaluate -Dexpression=project.scm.url -q -DforceStdout)

git checkout -B "${TRAVIS_BRANCH}"

if [ -z ${releaseVersion} ] || [ -z ${developmentVersion} ]; 
    then echo skip 
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
    mvn --batch-mode \
    -Dusername="${GIT_USERNAME}" \
    -Dpassword="${GIT_PASSWORD}" \
    -DreleaseVersion=${releaseVersion} \
    -DdevelopmentVersion=${developmentVersion} \
    -Dbuild-number=${TRAVIS_BUILD_NUMBER} \
    -Dbuild-name="${TRAVIS_BUILD_STAGE_NAME}" \
    -Dscm-path=${scm_path} \
    -DscmCommentPrefix="[maven-release-plugin][skip ci]" \
    -DskipTests \
    "-Darguments=-DskipTests -Dbuild-number=${TRAVIS_BUILD_NUMBER} '-Dbuild-name=${TRAVIS_BUILD_STAGE_NAME}' -Dscm-path=${scm_path} " \
    -Prelease \
    release:prepare release:perform
fi