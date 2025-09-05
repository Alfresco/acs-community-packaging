### Apply AGS community share AMP to Share image
ARG BASE_IMAGE
FROM alfresco/alfresco-base-tomcat:tomcat10-jre21-rockylinux9@sha256:ed568167f4c28efc9db4c5bc44a882ee117c475463b526b21ada99e1b6d568dd

LABEL quay.expires-after=${docker.quay-expires.value}

### Copy the AMP from build context to amps_share
COPY target/alfresco-governance-services-community-share-*.amp /usr/local/tomcat/amps_share/
### Install AMP on share
RUN java -jar /usr/local/tomcat/alfresco-mmt/alfresco-mmt*.jar install \
              /usr/local/tomcat/amps_share/alfresco-governance-services-community-share-*.amp /usr/local/tomcat/webapps/share -nobackup

ENTRYPOINT ["/usr/local/tomcat/shared/classes/alfresco/substituter.sh", "catalina.sh run"]

EXPOSE 8000
