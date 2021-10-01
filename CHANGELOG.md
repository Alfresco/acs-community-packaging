<h1>        7.2.0 Community
</h1>
<h2>
  New Features
</h2>
<h1>        7.1.0 Community
</h1>
<h2>
  New Features
</h2>
<li>Removal of 3rd party libraries

With the offloading of both transforms and metadata extraction to T-Engines a number of 3rd party libraries
are no longer needed within the content repository. They do still exist within the T-Engines performing the
same tasks. Any AMPs that where making use of these will need to provide these libraries themselves. This will
reduce the footprint of the repository and allow more frequent releases of the T-Engines to take advantage of
new functionality or security patches in these libraries.
<ul>
<li>PdfBox org.apache.pdfbox:pdfbox:2.0.21 removed - transforms are now performed in T-Engines</li>
<li>PdfBox org.apache.pdfbox:fontbox:2.0.21 removed - transforms are now performed in T-Engines</li>
<li>PdfBox org.apache.pdfbox:pdfbox-tools:2.0.21 removed - transforms are now performed in T-Engines</li>
</ul>
<br>

<h1>        7.0.0 Community
</h1>
<h2>
  New Features
</h2>

<ul>
<li>Metadata Extract

The out of the box extraction of metadata is now generally performed asynchronously via a T-Engine connected to the
repository either as part of the Alfresco Transform Service or as a Local transformer. This provides better security,
scalability and reliability. The framework used for metadata extraction within the content repository remains,
allowing custom extractors / embedders of metadata to still function, as long as they don't extend the extractors
that have been removed. Ideally such custom code should be gradually moved into a T-Engine. For more information see
[Metadata Extractors](https://github.com/Alfresco/acs-packaging/blob/master/docs/metadata-extract-embbed.md). </li>
<li>Removal of Legacy transformers

In ACS 6, the Alfresco Transform Service and Local transformers where introduced to help offload the transformation
of content to a separate process. In ACS 7, the out of the box Legacy transformers and transformation framework have
been removed. This helps provide greater clarity around installation and administration of transformations and
technically a more scalable, reliable and secure environment.</li>

<li>Custom Transforms and Renditions

ACS 7 provides a number of content transforms, but also allows custom transforms to be added.

It is possible to create custom transforms that run in separate processes known as T-Engines. The same engines may
be used in Community and Enterprise Editions. 

For more information, see [Custom Transforms and Renditions](https://github.com/Alfresco/acs-packaging/blob/master/docs/custom-transforms-and-renditions.md)
</li>

<li>Core All-In-One (AIO) Transform Engine

We have previously used T-Engines for Community and Enterprise Editions that run in separate processes. (https://docs.alfresco.com/transform-service/latest/)

The Core All-In-One (AIO) Transform Engine combines the current 5x core T-Engines  (LibreOffice, imagemagick,
Alfresco PDF Renderer, Tika) packaged together into a single Docker image.  Enterprise deployments require
greater scalability and we anticipate in these situations the individual T-Engines will be preferable.  

For Community deployments the AIO T-Engine, running it in a single JVM is recommended.  In addition the
AIO solution has been updated at with the option to build a single AIO T-Engine.
</li>

<li>Events related to node and association actions

With Alfresco Content Services 7.0, the Content Repository publishes events related to an initial set of actions
to nodes and associations. This is the first time that this feature is introduced as part of the ACS Core Services,
and it will be used in many use cases, as an example by the Alfresco SDK 5. For the moment the supported events
are related to node creation/update/deletion, secondary child association creation/deletion, peer association
creation/deletion.
</li>

<li>New REST API Endpoints:

    File  Rendition Management API is now available under /s
    POST '/nodes/{nodeId}/s/{Id}/renditions'
    GET '/nodes/{nodeId}/s/{Id}/renditions'
    GET '/nodes/{nodeId}/s/{Id}/renditions/{renditionId}'
    GET '/nodes/{nodeId}/s/{Id}/renditions/{renditionId}/content'

    Site Membership Management API is now available under /sites
    GET '/sites/{siteId}/group-members'
    POST '/sites/{siteId}/group-members'
    GET '/sites/{siteId}/group-members/{groupId}'
    PUT '/sites/{siteId}/group-members/{groupId}'
    DELETE '/sites/{siteId}/group-members/{groupId}'

    Model API: https://develop.envalfresco.com/api-explorer/?urls.primaryName=Model API
</li>

<li>Recommended Database Patch

ACS 7 contains a recommended database patch, which adds two indexes to the alf_node table and three to alf_transaction.
This patch is optional, but recommended for larger implementations as it can have a big positive performance impact.
These indexes are not automatically applied during upgrade, as the amount of time needed to create them might be
considerable. They should be run manually after the upgrade process completes. 

To apply the patch, an admin should set the following Alfresco global property to “true”. Like other patches it will
only be run once, so there is no need to reset the property afterwards.

    system.new-node-transaction-indexes.ignored=false

Until this step is completed, you will see Schema Validation warnings reported in the alfresco.log on each startup.
The log will also indicate that the patch was not run.

    INFO  [org.alfresco.repo.domain.schema.SchemaBootstrap] [...] Ignoring script patch (post-Hibernate): patch.db-V6.3-add-indexes-node-transaction
    ...
    WARN  [org.alfresco.repo.domain.schema.SchemaBootstrap] [...] Schema validation found ... potential problems, results written to ...
 </li>
    

<h1>        Release Notes - Alfresco - Version Community Edition 201911 GA
</h1>
<h2>        Bug
</h2>
<ul>
<li>[<a href='https://issues.alfresco.com/jira/browse/MNT-20436'>MNT-20436</a>] -         “POST /nodes/{nodeId}/children” RestAPI does not create a node without having a mandatory value object, but it outputs the 201 successful response.
</li>
<li>[<a href='https://issues.alfresco.com/jira/browse/MNT-20714'>MNT-20714</a>] -         [HotFix] /nodes/{nodeId}/content REST API fails for content created by a deleted user
</li>
<li>[<a href='https://issues.alfresco.com/jira/browse/MNT-20770'>MNT-20770</a>] -         Share non responsive during direct download from S3 if content store selector is also configured
</li>
<li>[<a href='https://issues.alfresco.com/jira/browse/MNT-20863'>MNT-20863</a>] -         Changing cm:name with REST API /nodes/{nodeId} does not update Primary Path
</li>
</ul>

<h1>        Release Notes - Alfresco - Version Community Edition 201910 EA
</h1>
<h2>
  New Features
</h2>
<ul>
  <li>
    <h4>Custom Transforms and Renditions</h4>
    <p>Alfresco Content Services (ACS) provides a number of content
     transforms, but also allows custom transforms to be added.
    <p>It is now possible to create custom transforms that run in 
    separate processes known as T-Engines (short for Transformer
    Engines). The same engines may be used in Community and 
    Enterprise Editions. They may be directly connected to the ACS 
    repository as Local Transforms, but in the Enterprise edition there 
    is the option to include them as part of the Transform Service 
    which provides more balanced throughput and better administration 
    capabilities.
    <p>For more information see <a href='https://github.com/Alfresco/acs-packaging/blob/master/docs/custom-transforms-and-renditions.md'>Custom Transforms and Renditions</a>
  </li>
      <li>
      <h4>Open-source Transformation Engines</h4>
      <p>The core T-Engine images can now be used in Community
       Edition.</p>
       <p>T-Engines code has been Open-Sourced and is available on Github:</p>
       <ul><a href='https://github.com/Alfresco/alfresco-transform-core'>alfresco/alfresco-transform-core</a></ul>
       <p>Images are available on Docker Hub:</p>
       <ul><a href='https://hub.docker.com/r/alfresco/alfresco-imagemagick'>alfresco/alfresco-imagemagick</a></ul>
       <ul><a href='https://hub.docker.com/r/alfresco/alfresco-pdf-renderer'>alfresco/alfresco-pdf-renderer</a></ul>
       <ul><a href='https://hub.docker.com/r/alfresco/alfresco-libreoffice'>alfresco/alfresco-libreoffice</a></ul>
       <ul><a href='https://hub.docker.com/r/alfresco/alfresco-tika'>alfresco/alfresco-tika</a></ul>
       <ul><a href='https://hub.docker.com/r/alfresco/alfresco-transform-misc'>alfresco/alfresco-transform-misc</a></ul>
       </p>
    </li>
    <li>
    <h4>Removal of external executables from docker image</h4>
    <p>With the introduction of the new Local Transform Service
    in Alfresco Community Edition, the capability of executing
    remote transformations on T-Engines was enabled. Because of
    this, the external executables (Alfresco-Pdf-renderer, Libreoffice
    and Imagemagick) have been removed from the docker container to
    facilitate the usage of out-of-process transformations.
     </p>
  </li>
</ul>
<h2>        Bug
</h2>
<ul>
<li>[<a href='https://issues.alfresco.com/jira/browse/ALF-22013'>ALF-22013</a>] -         Docker Image for Base Tomcat locale is POSIX
</li>
<li>[<a href='https://issues.alfresco.com/jira/browse/ALF-22060'>ALF-22060</a>] -         Reader on the backing store is obtained twice in CachingContentStore
</li>
<li>[<a href='https://issues.alfresco.com/jira/browse/ALF-22056'>ALF-22056</a>] -         onCopyCompleteBehaviour not called in order of copy-action
</li>
<li>[<a href='https://issues.alfresco.com/jira/browse/ALF-22073'>ALF-22073</a>] -         MailActionExecutor doesn't consider email bodies with a HTML doctype as HTML
</li>
<li>[<a href='https://issues.alfresco.com/jira/browse/ALF-21988'>ALF-21988</a>] -         Tab order for number ranges not ok
</li>
<li>[<a href='https://issues.alfresco.com/jira/browse/ALF-22097'>ALF-22097</a>] -         T Engine - add source nodeId parameter
</li>
</ul>
<h2>        Improvement
</h2>
<ul>
<li>[<a href='https://issues.alfresco.com/jira/browse/REPO-4318'>REPO-4318</a>] -         [COMPLETE] Extraction of transformers and metadata extractors
</li>
</ul>
<h2>

<h1>        Release Notes - Alfresco - Version Community Edition 201901 GA
</h1>
<h2>
  New Features
</h2>
<ul>
  <li>
    <h4>ActiveMQ:</h4>
    Alfresco ActiveMQ Docker images: <a href='https://github.com/Alfresco/alfresco-docker-activemq'>GitHub Repo</a> <a href='https://hub.docker.com/r/alfresco/alfresco-activemq/'>DockerHub Repo</a><p>
  </li>
    <li>
    <h4>Alfresco Benchmark Framework:</h4>
    <p>The benchmark framework project provides a way to run highly scalable, easy-to-run Java-based load and benchmark tests on an Alfresco instance.</p>
    <p>It comprises the following: <a href='https://github.com/Alfresco/alfresco-bm-manager'>Alfresco BM Manager</a> and Alfresco BM Drivers.</p> 
    <p>The currently provided drivers are:</p>
      <ul>
        <li><a href='https://github.com/Alfresco/alfresco-bm-load-data'>Alfresco Benchmark Load Data</a></li>
        <li><a href='https://github.com/Alfresco/alfresco-bm-rest-api'>Alfresco Benchmark Rest Api</a></li>
        <li><a href='https://github.com/Alfresco/alfresco-bm-load-users'>Alfresco Benchmark Load Users</a></li>
      </ul>	 
  </li>
    <li>
    <h4>Java 11 support</h4>
    <p>ACS is now runnable with OpenJDK 11.0.1. It still remains compatible with JDK 1.8.</p>
  </li>
</ul>
<h2>        Bug
</h2>
<ul>
<li>[<a href='https://issues.alfresco.com/jira/browse/ALF-22049'>ALF-22049</a>] -         Alfresco does not start
</li>
<li>[<a href='https://issues.alfresco.com/jira/browse/ALF-22041'>ALF-22041</a>] -         EKS deployment - SOLR_ALFRESCO_HOST set to wrong host name
</li>
<li>[<a href='https://issues.alfresco.com/jira/browse/ALF-22031'>ALF-22031</a>] -         REST API calls silently rollback after the returning a success status
</li>
<li>[<a href='https://issues.alfresco.com/jira/browse/ALF-21963'>ALF-21963</a>] -         Workflow - backslash in nodeRef properties url.
</li>
<li>[<a href='https://issues.alfresco.com/jira/browse/ALF-21803'>ALF-21803</a>] -         Unable to add users to sites whose 'short name' is a substring of 'site'
</li>
<li>[<a href='https://issues.alfresco.com/jira/browse/ALF-21664'>ALF-21664</a>] -         Exception on workflow image by REST API
</li>
<li>[<a href='https://issues.alfresco.com/jira/browse/ALF-20854'>ALF-20854</a>] -         webdav error opening Spanish Accent files
</li>
</ul>
<h2>        Improvement
</h2>
<ul>
<li>[<a href='https://issues.alfresco.com/jira/browse/REPO-3668'>REPO-3668</a>] -         Renditions: Transform event consumer
</li>
<li>[<a href='https://issues.alfresco.com/jira/browse/REPO-7'>REPO-7</a>] -         Embed ActiveMQ in the Platform
</li>
<li>[<a href='https://issues.alfresco.com/jira/browse/REPO-1957'>REPO-1957</a>] -         Transformations Improvement Plan
</li>
</ul>
<h2>
  Deprecations
</h2>
<ul>
  <li>
    TransformService and RenditionService: All Java APIs related to TransformService and RenditionService have been deprecated; the ability to perform arbitrary transformations will be phased out as the new DBP Transform Service takes effect.  Renditions can be triggered using the existing repository REST API but will be processed asynchronously using the new services.<br/>
  </li>
</ul>
<h2>
  Known issues
</h2>
<ul>
  <li>
    Due to the changes to the RenditionService the Media Management AMP is not supported yet.<br/>
  </li>
</ul>
<h2>

<h1>        Release Notes - Alfresco - Version Community Edition 201810 EA
</h1>  

<h2>        Bug
</h2>
<ul>
<li>[<a href='https://issues.alfresco.com/jira/browse/ALF-21783'>ALF-21783</a>] -         ScriptAuthorityService: No way to get more than 100 results with some methods
</li>
<li>[<a href='https://issues.alfresco.com/jira/browse/ALF-21917'>ALF-21917</a>] -         Document list edit metadata incorrect url escaping
</li>
<li>[<a href='https://issues.alfresco.com/jira/browse/ALF-22001'>ALF-22001</a>] -         Faceted search does not work in Japanese
</li>
<li>[<a href='https://issues.alfresco.com/jira/browse/ALF-22030'>ALF-22030</a>] -         ADF UI freezes noticeably on a periodic basis during KeyCloak auth requests
</li>
</ul>                                                                                
<h2>        Improvement
</h2>
<ul>
<li>[<a href='https://issues.alfresco.com/jira/browse/REPO-2491'>REPO-2491</a>] -         Renditions: Rendition Testing
</li>
<li>[<a href='https://issues.alfresco.com/jira/browse/REPO-3651'>REPO-3651</a>] -         AWS Load Tests: Infrastructure and Revamp
</li>
<li>[<a href='https://issues.alfresco.com/jira/browse/REPO-3663'>REPO-3663</a>] -         AWS Load Tests: Initial AWS Cost Estimation with BMF
</li>
<li>[<a href='https://issues.alfresco.com/jira/browse/REPO-3667'>REPO-3667</a>] -         Renditions: Transform event producer
</li>
<li>[<a href='https://issues.alfresco.com/jira/browse/REPO-3677'>REPO-3677</a>] -         AWS Services: Basic Deployment
</li>
<li>[<a href='https://issues.alfresco.com/jira/browse/REPO-3703'>REPO-3703</a>] -         AWS Services: Native Services of ACS
</li>
</ul>
<h2>

<h1>        Release Notes - Alfresco - Version Community Edition 201808 EA
</h1>                                                                                                                                                                                                                                                            
<h2>        Bug
</h2>
<ul>
<li>[<a href='https://issues.alfresco.com/jira/browse/ALF-21992'>ALF-21992</a>] -         BehaviourFilterImpl.isEnabled(NodeRef, QName) is checking wrong QName in case of subClass
</li>
<li>[<a href='https://issues.alfresco.com/jira/browse/ALF-22006'>ALF-22006</a>] -         VersionServicePolicies cannot be disabled on a specific node
</li>
<li>[<a href='https://issues.alfresco.com/jira/browse/ALF-22007'>ALF-22007</a>] -         TransactionListeners are executed in unpredictable order
</li>
</ul>
<h2>        Improvement
</h2>
<ul>
<li>[<a href='https://issues.alfresco.com/jira/browse/ALF-22011'>ALF-22011</a>] -         Upgrade to XMLBeans 3.0.0
</li>
</ul>

<h1>        Release Notes - Alfresco - Version Community Edition 201806 GA
</h1>                                                                                                                                                                                                                                                            
<h2>        Bug
</h2>
<ul>
<li>[<a href='https://issues.alfresco.com/jira/browse/ALF-22000'>ALF-22000</a>] -         Docker: HTML link 693ce565f4c4:8080/share but name 
</li>
<li>[<a href='https://issues.alfresco.com/jira/browse/ALF-22008'>ALF-22008</a>] -         It is not possible to upload a document without versions using CMIS
</li>
</ul>
