#!/usr/bin/env bash
set -ev
pushd "$(dirname "${BASH_SOURCE[0]}")/../../"

source "$(dirname "${BASH_SOURCE[0]}")/build_functions.sh"

DEPENDENCY_VERSION="$(evaluatePomProperty "dependency.alfresco-community-repo.version")"

# Either both the parent and the upstream dependency are the same, or else fail the build
if [ "${DEPENDENCY_VERSION}" != "$(evaluatePomProperty "project.parent.version")" ]; then
  printf "Upstream dependency version (%s) is different then the project parent version!\n" "${DEPENDENCY_VERSION}"
  exit 1
fi

# Prevent merging of any SNAPSHOT dependencies into the master or the release/* branches
if [[ isPullRequest && "${DEPENDENCY_VERSION}" =~ ^.+-SNAPSHOT$ && "${SOURCE_BRANCH}" =~ ^master$|^release/.+$ ]] ; then
  printf "PRs with SNAPSHOT dependencies are not allowed into master or release branches\n"
  exit 1
fi

UPSTREAM_REPO="github.com/Alfresco/alfresco-community-repo.git"
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


