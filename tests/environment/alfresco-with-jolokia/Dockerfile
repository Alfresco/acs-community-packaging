FROM alfresco/alfresco-content-repository-community:latest

ARG TOMCAT_DIR=/usr/local/tomcat
ARG GROUPNAME=Alfresco
ARG GROUPID=1000
ARG USERNAME=alfresco
ARG USERID=33000

USER root

RUN mkdir ${TOMCAT_DIR}/webapps/jolokia && \
    curl -o /tmp/jolokia.war http://search.maven.org/remotecontent?filepath=org/jolokia/jolokia-war-unsecured/1.6.2/jolokia-war-unsecured-1.6.2.war && \
    cd ${TOMCAT_DIR}/webapps/jolokia && \
    jar xvf /tmp/jolokia.war && \
    cd ${TOMCAT_DIR} && \
    rm /tmp/jolokia.war && \
# Grant all security permissions to jolokia in order to work properly.
    sed -i -e "\$a\grant\ codeBase\ \"file:\$\{catalina.base\}\/webapps\/jolokia\/-\" \{\n\    permission\ java.security.AllPermission\;\n\};" ${TOMCAT_DIR}/conf/catalina.policy && \
    # Restore permissions
    chgrp -R ${GROUPNAME} ${TOMCAT_DIR}/webapps/jolokia && \
    find ${TOMCAT_DIR}/webapps/jolokia -type d -exec chmod 0750 {} \; && \
    find ${TOMCAT_DIR}/webapps/jolokia -type f -exec chmod 0640 {} \; && \
    chmod -R g+r ${TOMCAT_DIR}/webapps/jolokia && \
    chgrp -R ${GROUPNAME} ${TOMCAT_DIR}/webapps/jolokia

RUN echo -e '\n\
# every second \n\
activities.feed.generator.cronExpression=* * * ? * * \n\
activities.post.lookup.cronExpression=* * * ? * * \n\
' >> ${TOMCAT_DIR}/shared/classes/alfresco-global.properties

USER ${USERID}