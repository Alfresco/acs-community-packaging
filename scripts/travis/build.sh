#!/usr/bin/env bash
set -ev
pushd "$(dirname "${BASH_SOURCE[0]}")/../../"

# get the source branch name
[ "${TRAVIS_PULL_REQUEST}" = "false" ] && BRANCH="${TRAVIS_BRANCH}" || BRANCH="${TRAVIS_PULL_REQUEST_BRANCH}"

# if BRANCH is not 'master' or 'release/' AND if it exists in the upstream project
UPSTREAM_REPO="github.com/Alfresco/alfresco-community-repo.git"
if ! [[ "${BRANCH}" =~ ^master$\|^release/.+$ ]] && \
  git ls-remote --exit-code --heads "https://${GIT_USERNAME}:${GIT_PASSWORD}@${UPSTREAM_REPO}" "${BRANCH}" ; then
  # clone and build the upstream repository
  pushd ..

  rm -rf alfresco-community-repo
  git clone -b "${BRANCH}" --depth=1 "https://${GIT_USERNAME}:${GIT_PASSWORD}@${UPSTREAM_REPO}"
  cd alfresco-community-repo
  mvn -B -V -q clean install -DskipTests -Dmaven.javadoc.skip=true -PcommunityDocker
  mvn -B -V install -f packaging/tests/pom.xml -DskipTests
  UPSTREAM_VERSION=$(mvn -B -q help:evaluate -Dexpression=project.version -DforceStdout)

  popd
fi

# update the parent dependency if needed
[ -n "${UPSTREAM_VERSION}" ] && mvn -B versions:update-parent "-DparentVersion=(0,${UPSTREAM_VERSION}]" versions:commit

# Build the current project also
mvn -B -V -q install -DskipTests -Dmaven.javadoc.skip=true "-Dversion.edition=${VERSION_EDITION}" -PcommunityDocker \
  $(test -n "${UPSTREAM_VERSION}" && echo "-Ddependency.alfresco-community-repo.version=${UPSTREAM_VERSION}")


