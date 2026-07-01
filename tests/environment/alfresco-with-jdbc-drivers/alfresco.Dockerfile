#BUILDING CONTAINER FOR TAS TESTING WITH DIFFERENT JDBC CONNECTORS
ARG BASE_IMAGE
FROM $BASE_IMAGE

COPY tests/environment/alfresco-with-jdbc-drivers/*.jar /usr/local/tomcat/lib/

