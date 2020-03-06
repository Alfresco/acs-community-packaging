#!/usr/bin/env bash
set -e

if [ -v ${RELEASE_VERSION} ]||[ -z ${RELEASE_VERSION} ]; then
    # if we don't have a user added release version, get the verison from the pom
    # TODO: Set up continuous release. As of REPO-4735 the following is not required if release stage is manual
    # pom_version=$(mvn help:evaluate -Dexpression=project.version -q -DforceStdout)
    # if echo $pom_version | grep -q  ".*-SNAPSHOT"; then
    #     RELEASE_VERSION=${pom_version%-*}  # remove everything after the last '-'
    # else
    #     RELEASE_VERSION=$pom_version
    # fi
    echo "Please provide a RELEASE_VERSION in the format <acs-version>-<additional-info> (6.3.0-EA or 6.3.0-SNAPSHOT)"
    exit -1
fi
# get the image name from the pom file
alfresco_docker_image=$(mvn help:evaluate -f ./docker-alfresco/pom.xml -Dexpression=image.name -q -DforceStdout)
docker_image_full_name="$alfresco_docker_image:$RELEASE_VERSION"

function docker_image_exists() {
  local image_full_name="$1"; shift
    local wait_time="${1:-5}"
    local search_term='Pulling|is up to date|not found'
    echo "Looking to see if $image_full_name already exists..."
    local result="$((timeout --preserve-status "$wait_time" docker 2>&1 pull "$image_full_name" &) | grep -v 'Pulling repository' | egrep -o "$search_term")"
    test "$result" || { echo "Timed out too soon. Try using a wait_time greater than $wait_time..."; return 1 ;}
    if echo $result | grep -vq 'not found'; then
        true
    else 
        false
    fi
}

if docker_image_exists $docker_image_full_name; then
    echo "Tag $RELEASE_VERSION already pushed, release process will interrupt."
    exit -1 
else
    echo "The $RELEASE_VERSION tag was not found"
fi