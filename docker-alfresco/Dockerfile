# Fetch image based on Tomcat 8.5.43 and Java 11
# More infos about this image: https://github.com/Alfresco/alfresco-docker-base-tomcat
FROM alfresco/alfresco-community-repo-base:latest

# Set default docker_context. Will / Can be overriden with maven.
ARG resource_path=target

# Set default user information
ARG GROUPNAME=Alfresco
ARG GROUPID=1000
ARG IMAGEUSERNAME=alfresco
ARG USERID=33000

# Set default environment args
ARG TOMCAT_DIR=/usr/local/tomcat

USER root

# Copy the amps from build context to the appropriate location for your application server
COPY ${resource_path}/amps ${TOMCAT_DIR}/amps

# Install amps on alfresco.war
RUN java -jar ${TOMCAT_DIR}/alfresco-mmt/alfresco-mmt*.jar install \
              ${TOMCAT_DIR}/amps \
              ${TOMCAT_DIR}/webapps/alfresco -directory -nobackup

# The standard configuration is to have all Tomcat files owned by root with group GROUPNAME and whilst owner has read/write privileges,
# group only has restricted permissions and world has no permissions.
RUN chgrp -R ${GROUPNAME} ${TOMCAT_DIR} && \
    chmod g+rx ${TOMCAT_DIR}/conf && \
    chmod -R g+r ${TOMCAT_DIR}/conf && \
    find ${TOMCAT_DIR}/webapps -type d -exec chmod 0750 {} \; && \
    find ${TOMCAT_DIR}/webapps -type f -exec chmod 0640 {} \; && \
    chmod -R g+r ${TOMCAT_DIR}/webapps && \
    chmod g+r ${TOMCAT_DIR}/conf/Catalina && \
    chmod g+rwx ${TOMCAT_DIR}/alf_data && \
    chmod g+rwx ${TOMCAT_DIR}/logs && \
    chmod o-w ${TOMCAT_DIR}/logs && \
    chmod g+rwx ${TOMCAT_DIR}/temp && \
    chmod g+rwx ${TOMCAT_DIR}/work && \
    chmod o-w ${TOMCAT_DIR}/work

EXPOSE 10001

# For remote debug
EXPOSE 8000

USER ${IMAGEUSERNAME}