#!/usr/bin/env bash
set -e

alfresco_docker_image="alfresco/alfresco-content-repository-community"
if [ -v ${release_version} ]||[ -z ${release_version}]; then
    release_version=$(mvn help:evaluate -Dexpression=project.version -q -DforceStdout)  
fi
docker_image_full_name="$alfresco_docker_image:$release_version"
echo $docker_image_full_name
function docker_image_exists() {
  local image_full_name="$1"; shift
    local wait_time="${1:-5}"
    local search_term='Pulling|is up to date|not found'
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
else
    echo "The $release_version tag was not found"
fi


