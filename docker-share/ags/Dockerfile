### Apply AGS community share AMP to Share image
ARG BASE_IMAGE
FROM alfresco/alfresco-base-tomcat:tomcat10-jre21-rockylinux9@sha256:4d3aaaec64518b0126bb7889a2574f1ed6e65231dca98a66d67a266fd1c3af92

LABEL quay.expires-after=${docker.quay-expires.value}

### Copy the AMP from build context to amps_share
COPY target/alfresco-governance-services-community-share-*.amp /usr/local/tomcat/amps_share/
### Install AMP on share
RUN java -jar /usr/local/tomcat/alfresco-mmt/alfresco-mmt*.jar install \
              /usr/local/tomcat/amps_share/alfresco-governance-services-community-share-*.amp /usr/local/tomcat/webapps/share -nobackup

ENTRYPOINT ["/usr/local/tomcat/shared/classes/alfresco/substituter.sh", "catalina.sh run"]

EXPOSE 8000
