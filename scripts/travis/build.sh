#!/usr/bin/env bash
set -ev
pushd "$(dirname "${BASH_SOURCE[0]}")/../../"

# reset variables to blank values
unset UPSTREAM_VERSION

# get the source branch name
[ "${TRAVIS_PULL_REQUEST}" = "false" ] && BRANCH="${TRAVIS_BRANCH}" || BRANCH="${TRAVIS_PULL_REQUEST_BRANCH}"

# if BRANCH is 'master' or 'release/'
UPSTREAM_REPO="github.com/Alfresco/alfresco-community-repo.git"
if [[ "${BRANCH}" =~ ^master$\|^release/.+$ ]] ; then
  # clone the upstream repository tag
  pushd ..

  TAG=$(mvn -B -q help:evaluate -Dexpression=project.parent.version -DforceStdout)
  git clone -b "${TAG}" --depth=1 "https://${GIT_USERNAME}:${GIT_PASSWORD}@${UPSTREAM_REPO}"

  popd
# if BRANCH is a feature branch AND if it exists in the upstream project
elif  git ls-remote --exit-code --heads "https://${GIT_USERNAME}:${GIT_PASSWORD}@${UPSTREAM_REPO}" "${BRANCH}" ; then
  # clone and build the upstream repository
  pushd ..

  rm -rf alfresco-community-repo
  git clone -b "${BRANCH}" --depth=1 "https://${GIT_USERNAME}:${GIT_PASSWORD}@${UPSTREAM_REPO}"
  cd alfresco-community-repo
  mvn -B -V -q clean install -DskipTests -Dmaven.javadoc.skip=true -PcommunityDocker
  mvn -B -V install -f packaging/tests/pom.xml -DskipTests
  UPSTREAM_VERSION="$(mvn -B -q help:evaluate -Dexpression=project.version -DforceStdout)"

  popd
fi

# update the parent dependency if needed
if [ -n "${UPSTREAM_VERSION}" ]; then
  mvn -B versions:update-parent "-DparentVersion=(0,${UPSTREAM_VERSION}]" versions:commit
fi

# Build the current project also
mvn -B -V -q install -DskipTests -Dmaven.javadoc.skip=true "-Dversion.edition=${VERSION_EDITION}" -PcommunityDocker \
  $(test -n "${UPSTREAM_VERSION}" && echo "-Ddependency.alfresco-community-repo.version=${UPSTREAM_VERSION}") \
  $(test -n "${UPSTREAM_VERSION}" && echo "-Dupstream.image.tag=latest")


