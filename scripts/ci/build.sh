#!/usr/bin/env bash
echo "=========================== Starting Build Script ==========================="
PS4="\[\e[35m\]+ \[\e[m\]"
set -vex
pushd "$(dirname "${BASH_SOURCE[0]}")/../../"

source "$(dirname "${BASH_SOURCE[0]}")/build_functions.sh"

usage() {
    echo "Builds the upstream projects first, then the current one." 1>&2;
    echo 1>&2;
    echo "Usage: $0 [-m]" 1>&2;
    echo "  -m: Flag to build Docker images with multi-architecture" 1>&2;
    echo "  -h: Display the usage information" 1>&2;
    exit 1;
}

while getopts "mh" option; do
   case $option in
      m)
        DOCKER_BUILD_PROFILE=build-multiarch-docker-images
        ;;
      h)
        usage
        ;;
   esac
done

BUILD_PROFILE=${DOCKER_BUILD_PROFILE:-build-docker-images}

COM_DEPENDENCY_VERSION="$(retrievePomProperty "dependency.alfresco-community-repo.version")"
REPO_IMAGE=$([[ "${COM_DEPENDENCY_VERSION}" =~ ^.+-SNAPSHOT$ ]] && echo "-Drepo.image.tag=latest" || echo)

# Either both the parent and the upstream dependency are the same, or else fail the build
if [ "${COM_DEPENDENCY_VERSION}" != "$(retrievePomParentVersion)" ]; then
  printf "Upstream dependency version (%s) is different then the project parent version!\n" "${COM_DEPENDENCY_VERSION}"
  exit 1
fi

# Prevent merging of any SNAPSHOT dependencies into the master or the release/* branches
if [[ $(isPullRequestBuild) && "${COM_DEPENDENCY_VERSION}" =~ ^.+-SNAPSHOT$ && "${BRANCH_NAME}" =~ ^master$|^release/.+$ ]] ; then
  printf "PRs with SNAPSHOT dependencies are not allowed into master or release branches\n"
  exit 1
fi

# Prevent release jobs from starting when there are SNAPSHOT upstream dependencies
if [[ "${COM_DEPENDENCY_VERSION}" =~ ^.+-SNAPSHOT$ ]] && [ "${JOB_NAME,,}" = "release" ] ; then
  printf "Cannot release project with SNAPSHOT dependencies!\n"
  exit 1
fi

UPSTREAM_REPO="github.com/Alfresco/alfresco-community-repo.git"

# Search, checkout and build the same branch on the upstream project in case of SNAPSHOT dependencies
# Otherwise, checkout the upstream tag and build its Docker image (use just "mvn package", without "mvn install")
if [[ "${COM_DEPENDENCY_VERSION}" =~ ^.+-SNAPSHOT$ ]] ; then
  pullAndBuildSameBranchOnUpstream "${UPSTREAM_REPO}" "-P$BUILD_PROFILE -Pags -Dlicense.failOnNotUptodateHeader=true"
else
  pullUpstreamTagAndBuildDockerImage "${UPSTREAM_REPO}" "${COM_DEPENDENCY_VERSION}" "-P$BUILD_PROFILE -Pags -Dlicense.failOnNotUptodateHeader=true"
fi

SHARE_DEPENDENCY_VERSION="$(retrievePomProperty "dependency.alfresco-community-share.version")"
SHARE_IMAGE=$([[ "${SHARE_DEPENDENCY_VERSION}" =~ ^.+-SNAPSHOT$ ]] && echo "-Dshare.image.tag=latest" || echo)

# Prevent merging of any SNAPSHOT dependencies into the master or the release/* branches
if [[ $(isPullRequestBuild) && "${SHARE_DEPENDENCY_VERSION}" =~ ^.+-SNAPSHOT$ && "${BRANCH_NAME}" =~ ^master$|^release/.+$ ]] ; then
  printf "PRs with SNAPSHOT dependencies are not allowed into master or release branches\n"
  exit 1
fi

# Prevent release jobs from starting when there are SNAPSHOT upstream dependencies
if [[ "${SHARE_DEPENDENCY_VERSION}" =~ ^.+-SNAPSHOT$ ]] && [ "${JOB_NAME,,}" = "release" ] ; then
  printf "Cannot release project with SNAPSHOT dependencies!\n"
  exit 1
fi

SHARE_UPSTREAM_REPO="github.com/Alfresco/alfresco-community-share.git"
# Checkout the upstream share project (tag or branch; + build if the latter)
if [[ "${SHARE_DEPENDENCY_VERSION}" =~ ^.+-SNAPSHOT$ ]] ; then
  pullAndBuildSameBranchOnUpstream "${SHARE_UPSTREAM_REPO}" "-P$BUILD_PROFILE -Pags -Dlicense.failOnNotUptodateHeader=true -Ddocker.quay-expires.value=NEVER ${REPO_IMAGE} -Ddependency.alfresco-community-repo.version=${COM_DEPENDENCY_VERSION}"
else
  pullUpstreamTagAndBuildDockerImage "${SHARE_UPSTREAM_REPO}" "${SHARE_DEPENDENCY_VERSION}" "-P$BUILD_PROFILE -Pags -Dlicense.failOnNotUptodateHeader=true -Ddocker.quay-expires.value=NEVER -Ddependency.alfresco-community-repo.version=${COM_DEPENDENCY_VERSION}"
fi

# Build the current project
mvn -B -ntp -V -q install -DskipTests -Dmaven.javadoc.skip=true -P$BUILD_PROFILE -Pags ${REPO_IMAGE} ${SHARE_IMAGE}


popd
set +vex
echo "=========================== Finishing Build Script =========================="

