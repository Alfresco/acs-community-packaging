#!/usr/bin/env bash

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="${SCRIPT_DIR}/../../.."
ENV_FILE=".linkPoms.env"
ENV_PATH="${ROOT_DIR}/${ENV_FILE}"

source "$(dirname "${BASH_SOURCE[0]}")/dev_functions.sh"

usage() {
    echo "Updates the downstream projects with the versions of the upstream projects. Reversed by unlinkPoms.sh" 1>&2;
    echo 1>&2;
    echo "Usage: $0 [-b <branch>] [-mpxuh]" 1>&2;
    echo "  -m: Checkout master of each project" 1>&2;
    echo "  -b: Checkout the <branch> of each project or master if <branch> is blank" 1>&2;
    echo "  -p: Pull the latest version of each project" 1>&2;
    echo "  -x: Skip the extract of values from each project" 1>&2;
    echo "  -u: Skip the update of values in each project" 1>&2;
    echo "  -h: Display this help" 1>&2;
    exit 1;
}

function checkout() {
    local PROJECT="${1}"
    local BRANCH="${2}"

    if [ -d "${ROOT_DIR}/${PROJECT}" ]
    then
      pushd "${ROOT_DIR}/${PROJECT}" &>/dev/null
      git checkout "${BRANCH}" &>/tmp/$$.log
      if [ $? -ne 0 ]
      then
        echo
        echo "\"git checkout ${BRANCH}\" failed on ${PROJECT}"
        cat "/tmp/$$.log"
        exit 1
      fi
      echo "${PROJECT} is now on ${BRANCH}"
      popd &>/dev/null
    fi
}

function pull_latest() {
    local PROJECT="${1}"

    if [ -d "${ROOT_DIR}/${PROJECT}" ]
    then
      pushd "${ROOT_DIR}/${PROJECT}" &>/dev/null
      git pull &>/tmp/$$.log
      if [ $? -ne 0 ]
      then
        echo
        echo "\"git pull\" failed on ${PROJECT}"
        cat "/tmp/$$.log"
        exit 1
      fi
      echo "${PROJECT} is now using latest"
      popd &>/dev/null
    fi
}

function readTopLevelTag() {
  local TAG_NAME="${1}"
  local POM_FILE="${2}"
  # Might be possible to generalise this function to accept an XPath so it could be used in place of sed commands

  # Read the file with an IFS (Input Field Separator) of the start of XML tag character <
  local IFS=\>
  local DEPTH=-99
  while read -d \< ENTITY CONTENT
  do
    if [[ $ENTITY == project\ * ]] # outer <project> tag
    then
      DEPTH=0
    elif [[ $ENTITY == /* ]] # end tag
    then
      ((DEPTH=DEPTH-1))
    else                     # start tag
      ((DEPTH=DEPTH+1))
    fi

    if [[ $ENTITY = "${TAG_NAME}" ]] && [[ $DEPTH == 1 ]] ; then
        echo $CONTENT
        exit
    fi
  done < $POM_FILE
  exit 1
}

function exportPomVersion() {
  local PROJECT="${1}"
  local ENV_NAME="${2}"

  if [ -d "${ROOT_DIR}/${PROJECT}" ]
    then
    pushd "${ROOT_DIR}/${PROJECT}" &>/dev/null
    # Same as slower/simpler: "mvn help:evaluate -Dexpression=project.version"
    VERSION=$(readTopLevelTag version pom.xml)
    if [ $? -ne 0 ]
    then
      echo
      echo "\"readTopLevelTagContent version pom.xml\" failed on ${PROJECT}"
      exit 1
    fi
    echo "export ${ENV_NAME}=${VERSION}" >> "${ENV_PATH}"
    popd &>/dev/null
  fi
}

function exportPomParent() {
  local PROJECT="${1}"
  local ENV_NAME="${2}"

  if [ -d "${ROOT_DIR}/${PROJECT}" ]
    then
    pushd "${ROOT_DIR}/${PROJECT}" &>/dev/null
    # Same as slower/simpler: "mvn help:evaluate -Dexpression=project.parent.version"
    PROPERTY_VALUE=$(sed -n '/<parent>/,/<\/parent>/p' pom.xml | sed -n "s/.*<version>\(.*\)<\/version>/\1/p" | sed 's/\r//g')
    if [ $? -ne 0 ]
    then
      echo
      echo "\"sed -n '/<parent>/,/<\/parent>/p' pom.xml | sed -n \\\"s/.*<version>\(.*\)<\/version>/\1/p\\\" | sed 's/\r//g'\" failed on ${PROJECT}"
      exit 1
    fi
    echo "export ${ENV_NAME}=${PROPERTY_VALUE}" >> "${ENV_PATH}"
    popd &>/dev/null
  fi
}

# Original version was simpler/slower: exportPomPropertyOrig <project> <env_name> project.parent.version
function exportPomProperty() {
    local PROJECT="${1}"
    local ENV_NAME="${2}"
    local PROPERTY_NAME="${3}"

  if [ -d "${ROOT_DIR}/${PROJECT}" ]
    then
    pushd "${ROOT_DIR}/${PROJECT}" &>/dev/null
    # Same as slower/simpler: "mvn help:evaluate -Dexpression=${PROPERTY_NAME}"
    PROPERTY_VALUE=$(sed -n '/<properties>/,/<\/properties>/p' pom.xml | sed -n "s/.*<${PROPERTY_NAME}>\(.*\)<\/${PROPERTY_NAME}>/\1/p" | sed 's/\r//g')
    if [ $? -ne 0 ]
    then
      echo
      echo "\"sed -n '/<properties>/,/<\/properties>/p' pom.xml | sed -n \\\"s/.*<${PROPERTY_NAME}>\(.*\)<\/${PROPERTY_NAME}>/\1/p\\\" | sed 's/\r//g'\" failed on ${PROJECT}"
      exit 1
    fi
    echo "export ${ENV_NAME}=${PROPERTY_VALUE}" >> "${ENV_PATH}"
    popd &>/dev/null
  fi
}

while getopts "b:mpxuh" arg; do
    case $arg in
        b)
            B_FLAG_SET="true"
            ;;
        m)
            M_FLAG_B_FLAG_SET="true"
            ;;
        p)
            # git pull after git checkout
            ;;
        x)
            SKIP_EXPORT="true"
            ;;
        u)
            SKIP_UPDATE="true"
            ;;
        h | *)
            usage
            exit 0
            ;;
    esac
done
if [ -n "${B_FLAG_SET}" -a -n "${M_FLAG_B_FLAG_SET}" ]
then
  echo "-m and -b may not both be set"
  exit 1
fi
OPTIND=1
while getopts "b:mpxuh" arg; do
    case $arg in
        b)
            BRANCH="${OPTARG:-master}"
            checkout alfresco-community-repo   "${BRANCH}"
            checkout alfresco-community-share  "${BRANCH}"
            checkout acs-community-packaging   "${BRANCH}"
            ;;
        m)
            BRANCH="master"
            checkout alfresco-community-repo   "${BRANCH}"
            checkout alfresco-community-share  "${BRANCH}"
            checkout acs-community-packaging   "${BRANCH}"
            ;;
    esac
done
OPTIND=1
while getopts "b:mpxuh" arg; do
    case $arg in
        p)
            pull_latest alfresco-community-repo
            pull_latest alfresco-community-share
            pull_latest acs-community-packaging
            ;;
    esac
done

if [ -z ${SKIP_EXPORT+x} ]
then
  rm -f "${ENV_FILE}"

  exportPomVersion  alfresco-community-repo   COM_R_VERSION

  exportPomVersion  alfresco-enterprise-repo  ENT_R_VERSION
  exportPomParent   alfresco-enterprise-repo  ENT_R_PARENT
  exportPomProperty alfresco-enterprise-repo  ENT_R_DEP_COM_R dependency.alfresco-community-repo.version

  exportPomVersion  alfresco-community-share  COM_S_VERSION
  exportPomProperty alfresco-community-share  COM_S_DEP_COM_R dependency.alfresco-community-repo.version
  exportPomProperty alfresco-community-share  COM_S_DEP_ENT_R dependency.alfresco-enterprise-repo.version

  exportPomVersion  acs-community-packaging   COM_P_VERSION
  exportPomParent   acs-community-packaging   COM_P_PARENT
  exportPomProperty acs-community-packaging   COM_P_DEP_COM_R dependency.alfresco-community-repo.version
  exportPomProperty acs-community-packaging   COM_P_DEP_COM_S dependency.alfresco-community-share.version

  cat "${ENV_FILE}"
fi

if [ -z ${SKIP_UPDATE+x} ]
then
  if [ ! -f "${ENV_FILE}" ]
  then
    echo ""${ENV_FILE}" does not exist."
    exit 1
  fi

  source "${ENV_FILE}"

  updatePomProperty alfresco-community-share  "$COM_R_VERSION" dependency.alfresco-community-repo.version

  updatePomParent   acs-community-packaging   "$COM_R_VERSION"
  updatePomProperty acs-community-packaging   "$COM_R_VERSION" dependency.alfresco-community-repo.version
  updatePomProperty acs-community-packaging   "$COM_S_VERSION" dependency.alfresco-community-share.version
fi
