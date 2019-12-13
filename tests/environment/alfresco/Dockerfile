FROM alfresco/alfresco-content-repository-community:latest

RUN echo -e '\n\
# every second \n\
activities.feed.generator.cronExpression=* * * ? * * \n\
activities.post.lookup.cronExpression=* * * ? * * \n\
' >> ${TOMCAT_DIR}/shared/classes/alfresco-global.properties