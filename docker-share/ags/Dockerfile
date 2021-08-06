### Apply AGS community share AMP to Share image
FROM alfresco/alfresco-share:${image.tag}

ARG TOMCAT_DIR=/usr/local/tomcat

### Copy the AMP from build context to amps_share
COPY target/alfresco-governance-services-community-share-*.amp $TOMCAT_DIR/amps_share/

### Install AMP on share
RUN java -jar $TOMCAT_DIR/alfresco-mmt/alfresco-mmt*.jar install \
              $TOMCAT_DIR/amps_share/alfresco-governance-services-community-share-*.amp $TOMCAT_DIR/webapps/share -nobackup