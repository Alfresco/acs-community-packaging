#!/usr/bin/env bash
#set -e

alfresco_docker_image="alfresco/alfresco-content-repository-community"
if [ -v ${release_version} ]||[ -z ${release_version} ]; then
    # if we don't have a user added release version, get the verison from the pom
    pom_version=$(mvn help:evaluate -Dexpression=project.version -q -DforceStdout)
    if echo $pom_version | grep -q  ".*-SNAPSHOT"; then
        release_version=${pom_version%-*}  # remove everything after the last '-'
    else
        release_version=$pom_version
    fi
fi
docker_image_full_name="$alfresco_docker_image:$release_version"

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
    echo "Tag $release_version already pushed, release process will interrupt."
    exit -1 
else
    echo "The $release_version tag was not found"
fi

if [ -z ${release_version} ] || [ -z ${development_version} ]; then echo skip 
else echo "did not skip" 
fi