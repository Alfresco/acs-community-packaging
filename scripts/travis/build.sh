#!/usr/bin/env bash
echo "=========================== Starting Build Script ==========================="
PS4="\[\e[35m\]+ \[\e[m\]"
set -vex
pushd "$(dirname "${BASH_SOURCE[0]}")/../../"

source "$(dirname "${BASH_SOURCE[0]}")/build_functions.sh"

DEPENDENCY_VERSION="$(retrievePomProperty "dependency.alfresco-community-repo.version")"

# Either both the parent and the upstream dependency are the same, or else fail the build
if [ "${DEPENDENCY_VERSION}" != "$(retrievePomParentVersion)" ]; then
  printf "Upstream dependency version (%s) is different then the project parent version!\n" "${DEPENDENCY_VERSION}"
  exit 1
fi

# Prevent merging of any SNAPSHOT dependencies into the master or the release/* branches
if [[ $(isPullRequestBuild) && "${DEPENDENCY_VERSION}" =~ ^.+-SNAPSHOT$ && "${TRAVIS_BRANCH}" =~ ^master$|^release/.+$ ]] ; then
  printf "PRs with SNAPSHOT dependencies are not allowed into master or release branches\n"
  exit 1
fi

# Prevent release jobs from starting when there are SNAPSHOT upstream dependencies
if [[ "${DEPENDENCY_VERSION}" =~ ^.+-SNAPSHOT$ ]] && [ "${TRAVIS_BUILD_STAGE_NAME,,}" = "release" ] ; then
  printf "Cannot release project with SNAPSHOT dependencies!\n"
  exit 1
fi

UPSTREAM_REPO="github.com/Alfresco/alfresco-community-repo.git"

# For release jobs, check if the upstream dependency is the latest tag on the upstream repository (on the same branch)
if isBranchBuild && [ "${TRAVIS_BUILD_STAGE_NAME,,}" = "release" ] && [ "${DEPENDENCY_VERSION}" != "$(retieveLatestTag "${UPSTREAM_REPO}" "${TRAVIS_BRANCH}")" ] ; then
  printf "Upstream dependency is not up to date with %s / %s\n" "${UPSTREAM_REPO}" "${TRAVIS_BRANCH}"
  exit 1
fi

# Search, checkout and build the same branch on the upstream project in case of SNAPSHOT dependencies
# Otherwise just checkout the upstream dependency sources
if [[ "${DEPENDENCY_VERSION}" =~ ^.+-SNAPSHOT$ ]] ; then
  pullAndBuildSameBranchOnUpstream "${UPSTREAM_REPO}" "-PcommunityDocker"
else
  pullUpstreamTag "${UPSTREAM_REPO}" "${DEPENDENCY_VERSION}"
fi

# Build the current project
mvn -B -V -q install -DskipTests -Dmaven.javadoc.skip=true -PcommunityDocker \
  $([[ "${DEPENDENCY_VERSION}" =~ ^.+-SNAPSHOT$ ]] && echo "-Dupstream.image.tag=latest")


popd
set +vex
echo "=========================== Finishing Build Script =========================="

