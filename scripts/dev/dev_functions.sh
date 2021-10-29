#!/usr/bin/env bash

function updatePomParent() {
    local PROJECT="$1"
    local PARENT_POM_VERSION="$2"

    if [ -d "${ROOT_DIR}/${PROJECT}" -a -n "${PARENT_POM_VERSION}" ]
    then
      # Cannot use "mvn versions:update-parent" as the version must exist before it can be set. The ed command is also faster.
      pushd "${ROOT_DIR}/${PROJECT}" &>/dev/null
      ed -s pom.xml &>/tmp/$$.log << EOF
/<parent>
/<version>.*<\/version>/s//<version>${PARENT_POM_VERSION}<\/version>/
wq
EOF
      if [ $? -ne 0 ]
      then
        echo
        echo "\"update of <pom><parent><version> version failed on ${PROJECT}"
        cat "/tmp/$$.log"
        exit 1
      fi
      printf "%-25s %46s=%s\n" "${PROJECT}" "<pom><parent><version>" "${PARENT_POM_VERSION}"
      popd &>/dev/null
  fi
}

function updatePomProperty() {
    local PROJECT="$1"
    local PROPERTY_VALUE="$2"
    local PROPERTY_NAME="$3"

    if [ -d "${ROOT_DIR}/${PROJECT}" -a -n "${PROPERTY_VALUE}" ]
    then
      # Can use "mvn versions:set-property", but ed is so much faster.
      # mvn -B versions:set-property  versions:commit  -Dproperty="${PROPERTY_NAME}" "-DnewVersion=${PROPERTY_VALUE}"
      pushd "${ROOT_DIR}/${PROJECT}" &>/dev/null
      ed -s pom.xml &>/tmp/$$.log << EOF
/\(<${PROPERTY_NAME}>\).*\(<\/${PROPERTY_NAME}>\)/s//\1${PROPERTY_VALUE}\2/
wq
EOF
      if [ $? -ne 0 ]
      then
        echo
        echo "\"update of <${PROPERTY_NAME}> failed on ${PROJECT}"
        cat "/tmp/$$.log"
        exit 1
      fi
      printf "%-25s %46s=%s\n" "${PROJECT}" "<${PROPERTY_NAME}>" "${PROPERTY_VALUE}"
      popd &>/dev/null
    fi
}
