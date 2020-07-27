#!/usr/bin/env bash
set -ev
pushd "$(dirname "${BASH_SOURCE[0]}")/../../"

# get the source branch name
[ "${TRAVIS_PULL_REQUEST}" = "false" ] && BRANCH="${TRAVIS_BRANCH}" || BRANCH="${TRAVIS_PULL_REQUEST_BRANCH}"

# if BRANCH is not 'master' or 'release/' AND if it exists in the upstream project
if ! [[ "${BRANCH}" =~ ^master$\|^release/.+$ ]] && \
  git ls-remote --exit-code --heads \
  https://${GIT_USERNAME}:${GIT_PASSWORD}@github.com/Alfresco/alfresco-community-repo.git \
  "${BRANCH}" ; then

  # clone and build the upstream repository
  pushd ..

  rm -rf alfresco-community-repo
  git clone -b ${BRANCH} https://${GIT_USERNAME}:${GIT_PASSWORD}@github.com/Alfresco/alfresco-community-repo.git
  cd alfresco-community-repo
  # todo set a custom branch version before the SNAPSHOT build?
  mvn -B -V clean install -DskipTests -PcommunityDocker

  popd
  # todo update the parent dependency dependency so that it uses the locally-build SNAPSHOT version?
fi

mvn -B -V -q install -DskipTests -Dmaven.javadoc.skip=true "-Dversion.edition=${VERSION_EDITION}" -PcommunityDocker

