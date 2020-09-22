#!/usr/bin/env bash
echo "=========================== Starting Release Script ==========================="
PS4="\[\e[35m\]+ \[\e[m\]"
set -vex
pushd "$(dirname "${BASH_SOURCE[0]}")/../../"


if [ -z "${RELEASE_VERSION}" ] || [ -z "${DEVELOPMENT_VERSION}" ]; then
  echo "Please provide a Release and Development version in the format <acs-version>-<additional-info> (7.0.0-EA or 7.0.0-SNAPSHOT)"
  exit 1
fi

# Use full history for release
git checkout -B "${TRAVIS_BRANCH}"
# Add email to link commits to user
git config user.email "${GIT_EMAIL}"

mvn -B \
  -Prelease,fullBuild,all-tas-tests \
  -DreleaseVersion="${RELEASE_VERSION}" \
  -DdevelopmentVersion="${DEVELOPMENT_VERSION}" \
  "-Darguments=-Prelease,fullBuild,all-tas-tests -DskipTests -Dbuild-number=${TRAVIS_BUILD_NUMBER}" \
  release:clean release:prepare release:perform \
  -DscmCommentPrefix="[maven-release-plugin][skip ci] " \
  -Dusername="${GIT_USERNAME}" \
  -Dpassword="${GIT_PASSWORD}"


popd
set +vex
echo "=========================== Finishing Release Script =========================="

