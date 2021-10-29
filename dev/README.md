# Development Tomcat Environment


It is possible to use Docker containers to test your code, but it is normally more convenient to simply run the
repository webapp (`alfresco.war`) in a tomcat instance.

## Link the projects
Generally you will want to link the different projects together by modifing the top level
pom.xml files of each downstream project so that they references the SNAPSHOT versions of the
upstream projects. To help do this see the `acs-community-packaging` project's `scripts/dev/linkPoms.sh` and
`scripts/dev/unlinkPoms.sh` scripts.

~~~
sh acs-community-packaging/scripts/linkPoms.sh
~~~

## Build -repo projects
Build the `alfresco-community-repo` (if you have not
done so already), so that your changes are in the community alfresco.war file.
~~~
$ # The `comR` alias includes the following commands:
$ cd alfresco-community-repo
$ mvn clean install -Pbuild-docker-images  -DskipTests=true -Dversion.edition=Community
$ cd ..
~~~

## Docker test environment
The repository code will need to talk to other ACS components, such as a databases, message queue and transformers.
The simplest way to create these, is to use the `docker-compose.yml` file in the `dev` directory.
~~~
$ # The `envUp` alias is the same as the following commands:
$ cd acs-community-packaging
$ docker-compose -f dev/docker-compose.yml up -d
Creating dev_transform-core-aio_1 ... done
Creating dev_postgres_1           ... done
Creating dev_solr6_1              ... done
Creating dev_activemq_1           ... done
$ cd ..
~~~

## Alfresco Global Properties and Log4j
Set any alfresco-global.properties or log4j properties you may need in the following files. They will be copied
to the `dev/dev-acs-amps-overlay/target` directory. Other customisations may also be placed in the `extension` directory. 
~~~
dev/dev-tomcat/src/main/tomcat/shared/classes/alfresco/extension/custom-log4j.properties
dev/dev-tomcat/src/main/tomcat/shared/classes/alfresco-global.properties
~~~

## Tomcat
Create the development tomcat environment, apply AMPs on top of the repository code, and
run tomcat. The `run` profile is what starts tomcat. The `withShare` applies
the Share services AMP and adds the `share.war` to tomcat. 
Once started, you will be able to access Share on `http://localhost:8080/share` and various repository
endpoints via `http://localhost:8080/alfresco/`. `entT` is an alias for the
following command and `entTDebug` will allow a debugger to be attached.
~~~
$ # The alias comT is the same as the following commands. comTDebug may also be used.
$ cd acs-community-packaging
$ mvn clean install -Prun -rf dev
$ cd ..
[INFO] ------------------------------------------------------------------------
[INFO] Development Tomcat Environment                                     [pom]
[INFO] Tomcat Configuration                                               [pom]
[INFO] Repo WAR with amps                                                 [war]
[INFO] Share WAR with amps                                                [war]
[INFO] Tomcat                                                             [war]
...
INFO: Starting ProtocolHandler ["http-bio-8080"]
$ cd ..
~~~

If you kill the tomcat instance (^C) and wish to restart it, use the following command
or the `comO` alias, or `comODebug` to attach a debuger.
~~~
$ mvn install -Prun,withShare -rf dev-acs-amps-overlay
~~~


## Clean up
When finished, kill the tomcat instance and stop the Docker instances. You will normally also
remove the Docker containers, as you will need a clean database if you are going to issue
another `mvn clean install` command.
~~~
$ ^C
... Stopped 'sysAdmin' subsystem, ID: [sysAdmin, default]

$ docker-compose -f dev/docker-compose.yml stop
Stopping dev_transform-core-aio_1 ... done
Stopping dev_transform-router_1   ... done
Stopping dev_solr6_1              ... done
Stopping dev_postgres_1           ... done
Stopping dev_activemq_1           ... done
Stopping dev_shared-file-store_1  ... done

$ docker-compose -f dev/docker-compose.yml rm
Going to remove dev_transform-core-aio_1, dev_transform-router_1, dev_solr6_1, dev_postgres_1, dev_activemq_1, dev_shared-file-store_1
Are you sure? [yN] y
Removing dev_transform-core-aio_1 ... done
Removing dev_transform-router_1   ... done
Removing dev_solr6_1              ... done
Removing dev_postgres_1           ... done
Removing dev_activemq_1           ... done
Removing dev_shared-file-store_1  ... done
~~~

If you have not removed the containers, it is possible to restart the tomcat instance with
a `mvn install` (no `clean`), but this may result in failures if there are incompatibilities
between the code, database and content in `dev/dev-acs-amps-overlay/target/dev-instance/runtime/alf_data`.
Any changes made to alfresco-global properties or log4j will not be picked up, unless you
directly edit `dev/dev-acs-amps-overlay/target/dev-instance/tomcat/shared/classes/alfresco/extension/custom-log4j.properties`
and `dev/dev-acs-amps-overlay/target/dev-instance/tomcat/shared/classes/alfresco-global.properties`, but they will be thrown away
on the next `mvn clean`.

## Aliases
You may also find the aliases specified in the following file useful, as they will help you only build selected parts
of the code base and will save you lots of typing.

Aliases ending in `D` provide Maven commands for building local Docker images. The AMPS environment variable will be of
interest, if you wish to build AMPs included in the repo and share projects. 

The `aliases` file includes a more detailed description.
~~~
$ source acs-community-packaging/dev/aliases
~~~
