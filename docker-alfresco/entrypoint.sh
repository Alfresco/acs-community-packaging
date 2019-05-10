#!/bin/sh

set -e

setInPropertiesFile() {
   local fileName="$1"
   local key="$2"
   local value="${3:-''}"

   # escape typical special characters in key / value (. and / for dot-separated keys or path values)
   regexSafeKey=`echo "$key" | sed -r 's/\\//\\\\\//g' | sed -r 's/\\./\\\\\./g'`
   replacementSafeKey=`echo "$key" | sed -r 's/\\//\\\\\//g' | sed -r 's/&/\\\\&/g'`
   replacementSafeValue=`echo "$value" | sed -r 's/\\//\\\\\//g' | sed -r 's/&/\\\\&/g'`

   if grep --quiet -E "^#?${regexSafeKey}=" ${fileName}; then
      sed -i -r "s/^#?${regexSafeKey}=.*/${replacementSafeKey}=${replacementSafeValue}/" ${fileName}
   else
      echo "${key}=${value}" >> ${fileName}
   fi
}

TOMCAT_DIR=/usr/local/tomcat

# ensure alfresco-global.properties / custom-log4j.properties exist and end with proper line ending
echo "" >> ${TOMCAT_DIR}/shared/classes/alfresco-global.properties
echo "" >> ${TOMCAT_DIR}/shared/classes/alfresco/extension/custom-log4j.properties

# otherwise for will also cut on whitespace
IFS=$'\n'
for i in `env`
do
   key=`echo "$i" | cut -d '=' -f 1 | cut -d '_' -f 2-`
   value=`echo "$i" | cut -d '=' -f 2-`
      
   if [[ $i == GLOBAL_* ]]
   then
      echo "Processing ENV global properties key ${key} with value ${value}"

      # support secrets mounted via files
      if [[ $key == *_FILE ]]
      then
         value="$(< "${value}")"
         key=`echo "$key" | sed -r 's/_FILE$//'`
      fi
      
      setInPropertiesFile ${TOMCAT_DIR}/shared/classes/alfresco-global.properties ${key} ${value}

   elif [[ $i == LOG4J-LOGGER_* ]]
   then
      echo "Processing ENV Log4J logger key ${key} with value ${value}"

      setInPropertiesFile ${TOMCAT_DIR}/shared/classes/alfresco/extension/custom-log4j.properties "log4j.logger.${key}" ${value}

   elif [[ $i == LOG4J-ADDITIVITY_* ]]
   then
      echo "Processing ENV Log4J additivity key ${key} with value ${value}"

      setInPropertiesFile ${TOMCAT_DIR}/shared/classes/alfresco/extension/custom-log4j.properties "log4j.additivity.${key}" ${value}
   fi
done

bash -c "$@"
