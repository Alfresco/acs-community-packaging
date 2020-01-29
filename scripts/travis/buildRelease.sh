#!/usr/bin/env bash
set -e

releaseVersion=$1
developmentVersion=$2
scm_path=$(mvn help:evaluate -Dexpression=project.scm.url -q -DforceStdout)

if [ -z ${releaseVersion} ] || [ -z ${developmentVersion} ]; 
    then echo skip  
else   
    echo "Do not skip"
    # mvn --batch-mode \
    # -DreleaseVersion=${releaseVersion} \
    # -DdevelopmentVersion=${developmentVersion} \
    # -Dbuild-number=${TRAVIS_BUILD_NUMBER} \
    # -Dbuild-key=${bamboo.buildKey} \
    # -Dbuild-name="${TRAVIS_BUILD_STAGE_NAME}" \
    # -Dscm-path=${scm_path} \
    # "-Dversion.edition=${bamboo.version.edition}" \
    # -DskipTests \
    # "-Darguments=-DskipTests '-Dversion.edition=${bamboo.version.edition}' -Dbuild-number=${TRAVIS_BUILD_NUMBER} -Dbuild-key=${bamboo.buildKey} '-Dbuild-name=${TRAVIS_BUILD_STAGE_NAME}' -Dscm-path=${scm_path} " \   
    # -Prelease \
    # release:prepare release:perform  
fi