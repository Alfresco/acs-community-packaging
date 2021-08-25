### Apply AGS community share AMP to Share image
FROM alfresco/alfresco-share-base:${share.image.tag}

LABEL quay.expires-after=${docker.quay-expires.value}

### Copy the AMP from build context to amps_share
COPY target/alfresco-governance-services-community-share-*.amp /usr/local/tomcat/amps_share/
### Install AMP on share
RUN java -jar /usr/local/tomcat/alfresco-mmt/alfresco-mmt*.jar install \
              /usr/local/tomcat/amps_share/alfresco-governance-services-community-share-*.amp /usr/local/tomcat/webapps/share -nobackup

ENTRYPOINT ["/usr/local/tomcat/shared/classes/alfresco/substituter.sh", "catalina.sh run"]

EXPOSE 8000
