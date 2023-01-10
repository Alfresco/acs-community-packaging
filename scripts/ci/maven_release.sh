#!/usr/bin/env bash
echo "=========================== Starting Release Script ==========================="
PS4="\[\e[35m\]+ \[\e[m\]"
set -vex
pushd "$(dirname "${BASH_SOURCE[0]}")/../../"


if [ -z "${RELEASE_VERSION}" ] || [ -z "${DEVELOPMENT_VERSION}" ]; then
  echo "Please provide a Release and Development version in the format <acs-version>-<additional-info> (e.g. 7.2.0-A2)"
  exit 1
fi

# Use full history for release
git checkout -B "${BRANCH_NAME}"
# Define git identity for commits
git config user.email "${GIT_EMAIL}"
git config user.name "${GIT_USERNAME}"

mvn -B \
  -ntp \
  -Prelease,all-tas-tests -Pags \
  -DreleaseVersion="${RELEASE_VERSION}" \
  -DdevelopmentVersion="${DEVELOPMENT_VERSION}" \
  "-Darguments=-Prelease,all-tas-tests -Pags -DskipTests -Dbuild-number=${BUILD_NUMBER}" \
  release:clean release:prepare release:perform \
  -DscmCommentPrefix="[maven-release-plugin][skip ci] " \
  -Dusername="${GIT_USERNAME}" \
  -Dpassword="${GIT_PASSWORD}"


popd
set +vex
echo "=========================== Finishing Release Script =========================="

