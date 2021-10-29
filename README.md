
# Alfresco Content Services Community Packaging
This project is producing packaging for Alfresco Content Services Community.

The SNAPSHOT version of artifacts are **never** published.

### Contributing guide
Please use [this guide](CONTRIBUTING.md) to make a contribution to the project.

# General

This project is the Community equivalent of the [Enterprise Packaging Project](https://github.com/Alfresco/acs-packaging).

This project creates the `alfresco/alfresco-content-repository-community` docker image and the distribution zip
for the Alfresco Content Services Community product.

The `alfresco/alfresco-content-repository-community` image extends the `alfresco-community-repo-base` created by the
`alfresco-community-repo` project to add additional ACS components.

# Build:
For more detailed build instructions, see the [Development Tomcat Environment](https://github.com/Alfresco/acs-community-packaging/tree/master/dev/README.md)
page.

To build the project, including the distribution zip, but not the Docker images, issue the following commands:
```
$ # The comP alias includes the following:
$ cd acs-community-packaging
$ mvn clean install
$ cd ..
```
## Docker Alfresco
Releases are published to https://hub.docker.com/r/alfresco/alfresco-content-repository-community/tags/

To build the Docker images, you will need to build the `alfresco-community-repo`, `alfresco-community-share` and
`acs-community-packaging` projects. The simplest way is to use the `comRD`, `comSD` and `comPD` aliases.
For more information, see [build aliases](dev/aliases). `latest` images are created locally.
```
comRD && comSD && comPD
```

## Docker-compose & Kubernetes
Use the https://github.com/Alfresco/acs-deployment project as the basis for your own docker-compose or helm chart deployments.


## Distribution zip
The distribution zip contains the war files, libraries, certificates and settings files you need, to deploy
Alfresco Content Services on the supported application servers.


# How to

* [Development Tomcat Environment](dev/README.md)
* [aliases](dev/aliases)
* [Create a custom Docker image](https://github.com/Alfresco/acs-packaging/blob/master/docs/create-custom-image.md)
* [Creating customized Docker images using an existing Docker image](https://github.com/Alfresco/acs-packaging/blob/master/docs/create-custom-image-using-existing-docker-image.md)
* [Verifying that AMPs have been applied](https://github.com/Alfresco/acs-packaging/blob/master/docs/verify-the-amp-has-been-applied.md)
* [Create and configure custom Transforms](https://github.com/Alfresco/acs-packaging/blob/master/docs/custom-transforms-and-renditions.md)
