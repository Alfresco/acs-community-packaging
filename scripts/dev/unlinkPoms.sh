#!/usr/bin/env bash
set -o errexit

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="${SCRIPT_DIR}/../../.."
ENV_FILE=".linkPoms.env"

source "$(dirname "${BASH_SOURCE[0]}")/dev_functions.sh"

usage() {
    echo "Reverts changes made by linkPoms.sh using values stored in ${ENV_FILE}" 1>&2;
    echo 1>&2;
    echo "Usage: $0 [-h]" 1>&2;
    echo "  -h: Display this help" 1>&2;
    exit 1;
}

while getopts "lh" arg; do
    case $arg in
        l)
            LOGGING_OUT=`tty`
            ;;
        h | *)
            usage
            exit 0
            ;;
    esac
done

source ${ENV_FILE}

updatePomProperty alfresco-community-share  "$COM_S_DEP_COM_R" dependency.alfresco-community-repo.version

updatePomParent   acs-community-packaging   "$COM_P_PARENT"
updatePomProperty acs-community-packaging   "$COM_P_DEP_COM_R" dependency.alfresco-community-repo.version
updatePomProperty acs-community-packaging   "$COM_P_DEP_COM_S" dependency.alfresco-community-share.version

