set -x
export TRANSFORMERS_TAG=$(mvn -f acs-community-packaging/pom.xml help:evaluate -Dexpression=dependency.alfresco-transform-core.version -q -DforceStdout)

# .env files are picked up from project directory correctly on docker-compose 1.23.0+
docker-compose -f acs-community-packaging/dev/docker-compose.yml up