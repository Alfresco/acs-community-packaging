package org.alfresco.rest.workflow;

import org.alfresco.dataprep.CMISUtil;
import org.alfresco.dataprep.CMISUtil.DocumentType;
import org.alfresco.dataprep.CMISUtil.Priority;
import org.alfresco.rest.NetworkDataPrep;
import org.alfresco.rest.exception.JsonToModelConversionException;
import org.alfresco.rest.model.RestErrorModel;
import org.alfresco.rest.model.RestItemModel;
import org.alfresco.rest.model.RestItemModelsCollection;
import org.alfresco.rest.model.RestProcessModel;
import org.alfresco.rest.model.RestProcessModelsCollection;
import org.alfresco.rest.model.RestProcessVariableCollection;
import org.alfresco.rest.model.RestProcessVariableModel;
import org.alfresco.utility.model.FileModel;
import org.alfresco.utility.model.SiteModel;
import org.alfresco.utility.model.TestGroup;
import org.alfresco.utility.report.Bug;
import org.alfresco.utility.testrail.ExecutionType;
import org.alfresco.utility.testrail.annotation.TestRail;
import org.springframework.http.HttpStatus;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

public class WorkflowNetworkProcessesTests extends NetworkDataPrep
{
    private RestProcessModel processModel;
    private RestProcessModelsCollection processes;
    private RestItemModel processItem;
    private RestItemModelsCollection processItems;
    private RestProcessVariableModel variableModel, processVariable;
    private RestProcessVariableCollection variables;
    protected SiteModel siteModel;
    protected FileModel document, document2;

    @BeforeClass(alwaysRun = true)
    public void dataPreparation() throws Exception
    {
        init();
        siteModel = dataSite.usingUser(adminUserModel).createPublicRandomSite();
        document = dataContent.usingSite(siteModel).createContent(DocumentType.TEXT_PLAIN);
        document2 = dataContent.usingSite(siteModel).createContent(DocumentType.TEXT_PLAIN);
    }

    @TestRail(section = { TestGroup.REST_API,TestGroup.WORKFLOW,
            TestGroup.PROCESSES }, executionType = ExecutionType.SANITY, 
            description = "Verify network user is able to start new process using REST API and status code is OK (200)")
    @Test(groups = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.PROCESSES, TestGroup.SANITY, TestGroup.NETWORKS })
    public void networkUserStartsNewProcess() throws JsonToModelConversionException, Exception
    {
        processModel = restClient.authenticateUser(tenantUser).withWorkflowAPI().addProcess("activitiAdhoc", secondTenantUser, false, Priority.Normal);
        restClient.assertStatusCodeIs(HttpStatus.CREATED);
        processModel.assertThat().field("id").is(processModel.getId())
                    .and().field("startUserId").is(processModel.getStartUserId());

        processes = restClient.withWorkflowAPI().getProcesses();
        restClient.assertStatusCodeIs(HttpStatus.OK);
        processes.assertThat().entriesListContains("id", processModel.getId());
    }

    @TestRail(section = { TestGroup.REST_API, TestGroup.WORKFLOW,TestGroup.PROCESSES}, executionType = ExecutionType.REGRESSION, 
            description = "Verify that non network user cannot get process from a network")
    @Test(groups = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.PROCESSES, TestGroup.REGRESSION, TestGroup.NETWORKS })
    @Bug(id = "REPO-2092")
    public void nonNetworkUserCannotAccessNetworkProcess() throws Exception
    {
        RestProcessModel networkProcess1 = restClient.authenticateUser(adminTenantUser).withWorkflowAPI().addProcess("activitiReview", tenantUser, false, CMISUtil.Priority.High);
        
        restClient.authenticateUser(adminUserModel).withWorkflowAPI().usingProcess(networkProcess1).getProcess();
        restClient.assertStatusCodeIs(HttpStatus.UNAUTHORIZED)
                  .assertLastError().containsErrorKey(RestErrorModel.API_DEFAULT_ERRORKEY)
                  .containsSummary(RestErrorModel.AUTHENTICATION_FAILED);
    }

    @TestRail(section = { TestGroup.REST_API, TestGroup.WORKFLOW,TestGroup.PROCESSES }, executionType = ExecutionType.REGRESSION, 
            description = "Verify that tenant user can get process from the same network")
    @Test(groups = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.PROCESSES, TestGroup.REGRESSION, TestGroup.NETWORKS })
    public void tenantUserCanGetProcessFromTheSameNetwork() throws Exception
    {
        RestProcessModel networkProcess1 = restClient.authenticateUser(adminTenantUser).withWorkflowAPI().addProcess("activitiReview", tenantUser, false, CMISUtil.Priority.High);       

        restClient.authenticateUser(tenantUser).withWorkflowAPI().usingProcess(networkProcess1).getProcess();
        restClient.assertStatusCodeIs(HttpStatus.OK);
        networkProcess1.assertThat().field("id").is(networkProcess1.getId())
            .and().field("startUserId").is(networkProcess1.getStartUserId());;
    }

    @TestRail(section = { TestGroup.REST_API, TestGroup.WORKFLOW,TestGroup.PROCESSES}, executionType = ExecutionType.REGRESSION, 
            description = "Verify that tenant user cannot get process from another network")
    @Test(groups = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.PROCESSES, TestGroup.REGRESSION, TestGroup.NETWORKS })
    @Bug(id = "REPO-2092")
    public void tenantUserCannotGetProcessFromAnotherNetwork() throws Exception
    {
        RestProcessModel networkProcess1 = restClient.authenticateUser(adminTenantUser).withWorkflowAPI().addProcess("activitiReview", tenantUser, false, CMISUtil.Priority.High);       

        restClient.authenticateUser(differentNetworkTenantUser).withWorkflowAPI().usingProcess(networkProcess1).getProcess();
        restClient.assertStatusCodeIs(HttpStatus.UNAUTHORIZED)
                  .assertLastError().containsErrorKey(RestErrorModel.API_DEFAULT_ERRORKEY)
                  .containsSummary(RestErrorModel.AUTHENTICATION_FAILED);
    }

    @TestRail(section = { TestGroup.REST_API, TestGroup.WORKFLOW,TestGroup.PROCESSES }, executionType = ExecutionType.REGRESSION, 
            description = "Verify that admin user can get process started by a network user")
    @Test(groups = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.PROCESSES, TestGroup.REGRESSION, TestGroup.NETWORKS })
    public void adminUserCanGetProcessFromANetwork() throws Exception
    {
        RestProcessModel networkProcess1 = restClient.authenticateUser(tenantUser).withWorkflowAPI().addProcess("activitiReview", adminTenantUser, false, CMISUtil.Priority.High);       

        restClient.authenticateUser(dataUser.getAdminUser()).withWorkflowAPI().usingProcess(networkProcess1).getProcess();
        restClient.assertStatusCodeIs(HttpStatus.OK);
        networkProcess1.assertThat().field("processDefinitionId").contains(String.format("@%s%s", tenantUser.getDomain().toLowerCase(), "@activitiReview:1"))
            .and().field("startUserId").is(tenantUser.getEmailAddress().substring(0, 2)+tenantUser.getEmailAddress().substring(2).toLowerCase())
            .and().field("startActivityId").is("start")
            .and().field("startedAt").isNotEmpty()
            .and().field("id").is(networkProcess1.getId())
            .and().field("completed").is(false)
            .and().field("processDefinitionKey").is("activitiReview");
    }

    @TestRail(section = { TestGroup.REST_API, TestGroup.WORKFLOW,TestGroup.PROCESSES }, executionType = ExecutionType.REGRESSION,
            description = "Add multiple process items using by admin in other network.")
    @Test(groups = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.PROCESSES, TestGroup.REGRESSION, TestGroup.NETWORKS })
    public void addMultipleProcessItemsByAdminInOtherNetwork() throws Exception
    {
        processModel = restClient.authenticateUser(adminTenantUser).withWorkflowAPI().addProcess("activitiAdhoc", tenantUser, false, Priority.Normal);
        restClient.authenticateUser(secondAdminTenantUser);
        
        processItems = restClient.withWorkflowAPI().usingProcess(processModel).addProcessItems(document, document2);
        restClient.assertStatusCodeIs(HttpStatus.FORBIDDEN)
                  .assertLastError()
                  .containsSummary(RestErrorModel.PROCESS_RUNNING_IN_ANOTHER_TENANT);
    }

    @TestRail(section = { TestGroup.REST_API,TestGroup.WORKFLOW,TestGroup.PROCESSES }, executionType = ExecutionType.REGRESSION, 
            description = "Add multiple process items using by the admin in same network.")
    @Test(groups = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.PROCESSES, TestGroup.REGRESSION, TestGroup.NETWORKS })
    public void addMultipleProcessItemsByAdminSameNetwork() throws Exception
    {
        processModel = restClient.authenticateUser(tenantUser).withWorkflowAPI().addProcess("activitiAdhoc", secondTenantUser, false, Priority.Normal);

        siteModel = dataSite.usingUser(adminTenantUser).createPublicRandomSite();
        document = dataContent.usingUser(adminTenantUser).usingSite(siteModel).createContent(DocumentType.TEXT_PLAIN);
        document2 = dataContent.usingUser(adminTenantUser).usingSite(siteModel).createContent(DocumentType.TEXT_PLAIN);
        processItems = restClient.authenticateUser(adminTenantUser).withWorkflowAPI().usingProcess(processModel).addProcessItems(document, document2);

        restClient.assertStatusCodeIs(HttpStatus.CREATED);
        processItems.getEntries().get(0).onModel().assertThat()
                    .field("createdAt").isNotEmpty().and()
                    .field("size").is("19").and()    
                    .field("createdBy").is(adminTenantUser.getUsername().toLowerCase()).and()
                    .field("modifiedAt").isNotEmpty().and()
                    .field("name").is(document.getName()).and()
                    .field("modifiedBy").is(adminTenantUser.getUsername().toLowerCase()).and()
                    .field("id").isNotEmpty().and()
                    .field("mimeType").is(document.getFileType().mimeType);
        processItems.getEntries().get(1).onModel().assertThat()
                    .field("createdAt").isNotEmpty().and()
                    .field("size").is("19").and()    
                    .field("createdBy").is(adminTenantUser.getUsername().toLowerCase()).and()
                    .field("modifiedAt").isNotEmpty().and()
                    .field("name").is(document2.getName()).and()
                    .field("modifiedBy").is(adminTenantUser.getUsername().toLowerCase()).and()
                    .field("id").isNotEmpty().and()
                    .field("mimeType").is(document2.getFileType().mimeType);
    }

    @TestRail(section = { TestGroup.REST_API, TestGroup.WORKFLOW,TestGroup.PROCESSES }, executionType = ExecutionType.REGRESSION,
            description = "Add process item using by admin in other network.")
    @Test(groups = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.PROCESSES, TestGroup.REGRESSION, TestGroup.NETWORKS })
    public void addProcessItemByAdminInOtherNetwork() throws Exception
    {
        processModel = restClient.authenticateUser(adminTenantUser).withWorkflowAPI().addProcess("activitiAdhoc", tenantUser, false, Priority.Normal);
        restClient.authenticateUser(secondAdminTenantUser);
        processItem = restClient.withWorkflowAPI().usingProcess(processModel).addProcessItem(document);
        restClient.assertStatusCodeIs(HttpStatus.FORBIDDEN).assertLastError().containsSummary(RestErrorModel.PROCESS_RUNNING_IN_ANOTHER_TENANT);
    }

    @TestRail(section = { TestGroup.REST_API, TestGroup.WORKFLOW,TestGroup.PROCESSES }, executionType = ExecutionType.REGRESSION, 
            description = "Add process item using by the admin in same network.")
    @Test(groups = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.PROCESSES, TestGroup.REGRESSION, TestGroup.NETWORKS })
    public void addProcessItemByAdminSameNetwork() throws Exception
    {
        processModel = restClient.authenticateUser(tenantUser).withWorkflowAPI().addProcess("activitiAdhoc", secondTenantUser, false, Priority.Normal);

        siteModel = dataSite.usingUser(adminTenantUser).createPublicRandomSite();
        document = dataContent.usingUser(adminTenantUser).usingSite(siteModel).createContent(DocumentType.TEXT_PLAIN);
        processItem = restClient.authenticateUser(adminTenantUser).withWorkflowAPI().usingProcess(processModel).addProcessItem(document);

        restClient.assertStatusCodeIs(HttpStatus.CREATED);
        processItem.assertThat().field("createdAt").isNotEmpty().and().field("size").is("19").and().field("createdBy").is(adminTenantUser.getUsername().toLowerCase()).and()
                .field("modifiedAt").isNotEmpty().and().field("name").is(document.getName()).and().field("modifiedBy").is(adminTenantUser.getUsername().toLowerCase()).and()
                .field("id").isNotEmpty().and().field("mimeType").is(document.getFileType().mimeType);
    }

    @TestRail(section = { TestGroup.REST_API, TestGroup.WORKFLOW,TestGroup.PROCESSES }, executionType = ExecutionType.REGRESSION,
              description = "Delete process item using by admin in other network.")
    @Test(groups = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.PROCESSES, TestGroup.REGRESSION, TestGroup.NETWORKS })
    public void deleteProcessItemByAdminInOtherNetwork() throws Exception
    {
        processModel = restClient.authenticateUser(tenantUser).withWorkflowAPI().addProcess("activitiAdhoc", secondTenantUser, false, CMISUtil.Priority.Normal);
        processItem = restClient.withWorkflowAPI().usingProcess(processModel).addProcessItem(document);
        restClient.assertStatusCodeIs(HttpStatus.CREATED);

        restClient.authenticateUser(secondAdminTenantUser).withWorkflowAPI().usingProcess(processModel)
                  .deleteProcessItem(processItem);
        restClient.assertStatusCodeIs(HttpStatus.FORBIDDEN)
                  .assertLastError()
                  .containsSummary(RestErrorModel.PROCESS_RUNNING_IN_ANOTHER_TENANT)
                  .descriptionURLIs(RestErrorModel.RESTAPIEXPLORER)
                  .containsErrorKey(RestErrorModel.PROCESS_RUNNING_IN_ANOTHER_TENANT)
                  .stackTraceIs(RestErrorModel.STACKTRACE);

        //restore document
        document = dataContent.usingSite(siteModel).createContent(DocumentType.TEXT_PLAIN);
    }

    @TestRail(section = { TestGroup.REST_API, TestGroup.WORKFLOW,TestGroup.PROCESSES }, executionType = ExecutionType.REGRESSION, 
              description = "Delete process item using by the admin in same network.")
    @Test(groups = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.PROCESSES, TestGroup.REGRESSION, TestGroup.NETWORKS })
    public void deleteProcessItemByAdminSameNetwork() throws Exception
    {
        processModel = restClient.authenticateUser(tenantUser).withWorkflowAPI().addProcess("activitiAdhoc", secondTenantUser, false, CMISUtil.Priority.Normal);

        processItem = restClient.withWorkflowAPI().usingProcess(processModel).addProcessItem(document);
        restClient.assertStatusCodeIs(HttpStatus.CREATED);

        restClient.authenticateUser(adminTenantUser).withWorkflowAPI().usingProcess(processModel)
                  .deleteProcessItem(processItem);
        restClient.assertStatusCodeIs(HttpStatus.NO_CONTENT);
        restClient.withWorkflowAPI().usingProcess(processModel).getProcessVariables()
                  .assertThat().entriesListDoesNotContain("name", processItem.getName());

        //restore document
        document = dataContent.usingSite(siteModel).createContent(DocumentType.TEXT_PLAIN);
    }

    @TestRail(section = { TestGroup.REST_API, TestGroup.PROCESSES }, executionType = ExecutionType.REGRESSION,
            description = "Get process items using admin from different network")
    @Test(groups = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.PROCESSES, TestGroup.REGRESSION, TestGroup.NETWORKS })
    public void getProcessItemsReturnsOnlyItemsInsideNetwork() throws Exception
    {
        processModel = restClient.authenticateUser(tenantUser).withWorkflowAPI().addProcess("activitiAdhoc", secondTenantUser, false, CMISUtil.Priority.Normal);

        siteModel = dataSite.usingUser(adminTenantUser).createPublicRandomSite();
        document = dataContent.usingUser(adminTenantUser).usingSite(siteModel).createContent(CMISUtil.DocumentType.TEXT_PLAIN);
        restClient.withWorkflowAPI().usingProcess(processModel).addProcessItem(document);
        restClient.assertStatusCodeIs(HttpStatus.CREATED);

        RestProcessModel processModel2 = restClient.authenticateUser(secondAdminTenantUser).withWorkflowAPI().addProcess("activitiAdhoc", secondAdminTenantUser, false, CMISUtil.Priority.Normal);

        SiteModel siteModel2 = dataSite.usingUser(secondAdminTenantUser).createPublicRandomSite();
        document2 = dataContent.usingUser(secondAdminTenantUser).usingSite(siteModel2).createContent(CMISUtil.DocumentType.TEXT_PLAIN);
        restClient.authenticateUser(secondAdminTenantUser).withWorkflowAPI().usingProcess(processModel2).addProcessItem(document2);
        restClient.assertStatusCodeIs(HttpStatus.CREATED);

        processItems = restClient.withWorkflowAPI().usingProcess(processModel2).getProcessItems();
        restClient.assertStatusCodeIs(HttpStatus.OK);
        processItems.assertThat().entriesListDoesNotContain("name", document.getName());
    }

    @TestRail(section = { TestGroup.REST_API, TestGroup.PROCESSES }, executionType = ExecutionType.REGRESSION,
            description = "Get process items using admin from different network")
    @Test(groups = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.PROCESSES, TestGroup.REGRESSION, TestGroup.NETWORKS })
    public void getProcessItemsUsingAdminUserFromDifferentNetwork() throws Exception
    {
        processModel = restClient.authenticateUser(tenantUser).withWorkflowAPI().addProcess("activitiAdhoc", secondTenantUser, false, CMISUtil.Priority.Normal);

        siteModel = dataSite.usingUser(adminTenantUser).createPublicRandomSite();
        document = dataContent.usingUser(adminTenantUser).usingSite(siteModel).createContent(CMISUtil.DocumentType.TEXT_PLAIN);
        restClient.withWorkflowAPI().usingProcess(processModel).addProcessItem(document);
        restClient.assertStatusCodeIs(HttpStatus.CREATED);

        restClient.authenticateUser(secondAdminTenantUser).withWorkflowAPI().usingProcess(processModel).getProcessItems();
        restClient.assertStatusCodeIs(HttpStatus.FORBIDDEN)
                .assertLastError().containsSummary(RestErrorModel.PROCESS_RUNNING_IN_ANOTHER_TENANT);
    }

    @TestRail(section = { TestGroup.REST_API, TestGroup.WORKFLOW,TestGroup.PROCESSES }, executionType = ExecutionType.REGRESSION, 
            description = "Add process variable using by admin in other network.")
    @Test(groups = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.PROCESSES, TestGroup.REGRESSION, TestGroup.NETWORKS })
    public void addProcessVariableByAdminInOtherNetwork() throws Exception
    {
        processModel = restClient.authenticateUser(adminTenantUser).withWorkflowAPI().addProcess("activitiAdhoc", secondTenantUser, false, Priority.Normal);

        restClient.authenticateUser(secondAdminTenantUser);
        variableModel = RestProcessVariableModel.getRandomProcessVariableModel("d:text");
        processVariable = restClient.withWorkflowAPI().usingProcess(processModel).updateProcessVariable(variableModel);
        restClient.assertStatusCodeIs(HttpStatus.FORBIDDEN)
                  .assertLastError()
                  .containsSummary(RestErrorModel.PROCESS_RUNNING_IN_ANOTHER_TENANT);
    }

    @TestRail(section = {TestGroup.REST_API, TestGroup.WORKFLOW,TestGroup.PROCESSES }, executionType = ExecutionType.SANITY,
            description = "Add process variable using admin user from same network")
    @Test(groups = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.PROCESSES, TestGroup.SANITY, TestGroup.NETWORKS })
    public void addProcessVariableByAdmin() throws Exception
    {
        restClient.authenticateUser(tenantUser).withWorkflowAPI().addProcess("activitiAdhoc", secondTenantUser, false, Priority.Normal);
        variableModel = RestProcessVariableModel.getRandomProcessVariableModel("d:text");
        processModel = restClient.authenticateUser(adminTenantUser).withWorkflowAPI().getProcesses().getOneRandomEntry().onModel();
        
        processVariable = restClient.withWorkflowAPI().usingProcess(processModel).updateProcessVariable(variableModel);
        restClient.assertStatusCodeIs(HttpStatus.OK);
        processVariable.assertThat().field("name").is(variableModel.getName())
                        .and().field("type").is(variableModel.getType())
                        .and().field("value").is(variableModel.getValue());
    }

    @TestRail(section = { TestGroup.REST_API, TestGroup.WORKFLOW,TestGroup.PROCESSES, TestGroup.NETWORKS }, executionType = ExecutionType.REGRESSION, 
             description = "Verify that admin from different network is not able to delete network process variables")
    @Test(groups = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.PROCESSES, TestGroup.REGRESSION, TestGroup.NETWORKS })
    public void deleteProcessVariablesWithAdminFromDifferentNetwork() throws Exception
    {
        RestProcessModel processModel = restClient.authenticateUser(adminTenantUser).withWorkflowAPI()
                                                  .addProcess("activitiAdhoc", tenantUser, false, Priority.Normal);
        variableModel = RestProcessVariableModel.getRandomProcessVariableModel("d:text");
        RestProcessModel networkProcess1 = restClient.authenticateUser(tenantUser).withWorkflowAPI()
                                                     .getProcesses().getProcessModelByProcessDefId(processModel.getId());
        restClient.withWorkflowAPI().usingProcess(networkProcess1).addProcessVariable(variableModel);
        restClient.assertStatusCodeIs(HttpStatus.CREATED);

        restClient.authenticateUser(adminUserModel).withWorkflowAPI().usingProcess(networkProcess1)
                  .deleteProcessVariable(variableModel);
        restClient.assertStatusCodeIs(HttpStatus.FORBIDDEN);
        restClient.assertLastError().containsSummary(RestErrorModel.PROCESS_RUNNING_IN_ANOTHER_TENANT)
                  .descriptionURLIs(RestErrorModel.RESTAPIEXPLORER)
                  .containsErrorKey(RestErrorModel.PROCESS_RUNNING_IN_ANOTHER_TENANT)
                  .stackTraceIs(RestErrorModel.STACKTRACE);

        restClient.authenticateUser(tenantUser).withWorkflowAPI().usingProcess(networkProcess1).getProcessVariables().assertThat()
                  .entriesListContains("name", variableModel.getName());
    }

    @TestRail(section = { TestGroup.REST_API, TestGroup.WORKFLOW,TestGroup.PROCESSES, TestGroup.NETWORKS }, executionType = ExecutionType.REGRESSION,
              description = "Verify that admin from the same network is able to delete network process variables")
    @Test(groups = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.PROCESSES, TestGroup.REGRESSION, TestGroup.NETWORKS })
    public void deleteProcessVariablesWithAdminFromSameNetwork() throws Exception
    {
        RestProcessModel processModel = restClient.authenticateUser(adminTenantUser).withWorkflowAPI()
                .addProcess("activitiAdhoc", tenantUser, false, Priority.Normal);
        variableModel = RestProcessVariableModel.getRandomProcessVariableModel("d:text");
        RestProcessModel networkProcess1 = restClient.authenticateUser(tenantUser).withWorkflowAPI()
                                                     .getProcesses().getProcessModelByProcessDefId(processModel.getId());
        restClient.withWorkflowAPI().usingProcess(networkProcess1).addProcessVariable(variableModel);
        restClient.assertStatusCodeIs(HttpStatus.CREATED);

        restClient.authenticateUser(adminTenantUser).withWorkflowAPI().usingProcess(networkProcess1)
                  .deleteProcessVariable(variableModel);
        restClient.assertStatusCodeIs(HttpStatus.NO_CONTENT);
        restClient.withWorkflowAPI().usingProcess(networkProcess1).getProcessVariables()
                  .assertThat()
                  .entriesListDoesNotContain("name", variableModel.getName());
    }

    @TestRail(section = {TestGroup.REST_API, TestGroup.WORKFLOW,TestGroup.PROCESSES }, executionType = ExecutionType.REGRESSION,
            description = "Verify that admin from different network is not able to retrieve network process variables")
    @Test(groups = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.PROCESSES, TestGroup.REGRESSION, TestGroup.NETWORKS })
    public void getProcessVariablesWithAdminFromDifferentNetwork() throws Exception
    {
        RestProcessModel networkProcess1 = restClient.authenticateUser(adminTenantUser).withWorkflowAPI()
                .addProcess("activitiReview", tenantUser, false, CMISUtil.Priority.High);

        variables = restClient.authenticateUser(adminUserModel).withWorkflowAPI().usingProcess(networkProcess1).getProcessVariables();
        restClient.assertStatusCodeIs(HttpStatus.FORBIDDEN).assertLastError().containsSummary(RestErrorModel.PROCESS_RUNNING_IN_ANOTHER_TENANT);
    }

    @TestRail(section = {TestGroup.REST_API,TestGroup.WORKFLOW, TestGroup.PROCESSES }, executionType = ExecutionType.REGRESSION,
            description = "Verify that admin from the same network is able to retrieve network process variables")
    @Test(groups = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.PROCESSES, TestGroup.REGRESSION, TestGroup.NETWORKS })
    public void getProcessVariablesWithAdminFromSameNetwork() throws Exception
    {
        RestProcessModel networkProcess1 = restClient.authenticateUser(tenantUser).withWorkflowAPI()
                .addProcess("activitiReview", tenantUser, false, CMISUtil.Priority.High);

        variables = restClient.authenticateUser(adminTenantUser).withWorkflowAPI().usingProcess(networkProcess1).getProcessVariables();
        restClient.assertStatusCodeIs(HttpStatus.OK);
        variables.assertThat().entriesListIsNotEmpty();
    }

    @TestRail(section = {TestGroup.REST_API, TestGroup.WORKFLOW,TestGroup.PROCESSES }, executionType = ExecutionType.REGRESSION,
            description = "Verify admin gets all processes from same network")
    @Test(groups = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.PROCESSES, TestGroup.REGRESSION, TestGroup.NETWORKS })
    public void getProcessFromSameNetworkUsingAdmin() throws Exception
    {
        RestProcessModel process = restClient.authenticateUser(tenantUser).withWorkflowAPI().addProcess("activitiAdhoc", secondTenantUser, false, Priority.Normal);

        RestProcessModelsCollection tenantProcesses = restClient.authenticateUser(adminTenantUser).withWorkflowAPI().getProcesses();
        restClient.assertStatusCodeIs(HttpStatus.OK);
        
        tenantProcesses.assertThat().entriesListContains("id", process.getId());
    }

    @TestRail(section = {TestGroup.REST_API, TestGroup.PROCESSES }, executionType = ExecutionType.SANITY,
            description = "Get process items using admin from same network")
    @Test(groups = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.PROCESSES, TestGroup.SANITY, TestGroup.NETWORKS })
    public void getProcessItemsUsingAdminUserFromSameNetwork() throws Exception
    {
        processModel = restClient.authenticateUser(tenantUser).withWorkflowAPI().addProcess("activitiAdhoc", secondTenantUser, false, Priority.Normal);

        siteModel = dataSite.usingUser(adminTenantUser).createPublicRandomSite();
        document = dataContent.usingUser(adminTenantUser).usingSite(siteModel).createContent(DocumentType.TEXT_PLAIN);
        restClient.withWorkflowAPI().usingProcess(processModel).addProcessItem(document);

        restClient.assertStatusCodeIs(HttpStatus.CREATED);

        processItems = restClient.withWorkflowAPI().usingProcess(processModel).getProcessItems();
        restClient.assertStatusCodeIs(HttpStatus.OK);
        processItems.assertThat().entriesListContains("name", document.getName());
    }

    @TestRail(section = {TestGroup.REST_API, TestGroup.WORKFLOW,TestGroup.PROCESSES }, executionType = ExecutionType.SANITY,
            description = "Add multiple process variables using admin user from same network")
    @Test(groups = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.PROCESSES, TestGroup.SANITY, TestGroup.NETWORKS })
    public void addMultipleProcessVariablesByAdminSameNetwork() throws Exception
    {
        restClient.authenticateUser(tenantUser).withWorkflowAPI().addProcess("activitiAdhoc", secondTenantUser, false, Priority.Normal);
        variableModel = RestProcessVariableModel.getRandomProcessVariableModel("d:text");
        processVariable = RestProcessVariableModel.getRandomProcessVariableModel("d:text");
        processModel = restClient.authenticateUser(adminTenantUser).withWorkflowAPI().getProcesses().getOneRandomEntry().onModel();
       
        variables = restClient.withWorkflowAPI().usingProcess(processModel).addProcessVariables(variableModel, processVariable);
        restClient.assertStatusCodeIs(HttpStatus.CREATED);
        
        variables.assertThat().entriesListContains("name", variableModel.getName())
                                 .assertThat().entriesListContains("name", processVariable.getName());

        variables.getEntries().get(0).onModel().assertThat()
                                 .field("name").is(variableModel.getName()).and()
                                 .field("value").is(variableModel.getValue()).and()
                                 .field("type").is(variableModel.getType());
        variables.getEntries().get(1).onModel().assertThat()
                                 .field("name").is(processVariable.getName()).and()
                                 .field("value").is(processVariable.getValue()).and()
                                 .field("type").is(processVariable.getType());
    }

    @TestRail(section = {TestGroup.REST_API, TestGroup.WORKFLOW,TestGroup.PROCESSES }, executionType = ExecutionType.SANITY,
            description = "Add process variables using admin user from same network")
    @Test(groups = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.PROCESSES, TestGroup.SANITY, TestGroup.NETWORKS })
    public void addProcessVariableByAdminSameNetwork() throws Exception
    {
        restClient.authenticateUser(tenantUser).withWorkflowAPI().addProcess("activitiAdhoc", secondTenantUser, false, Priority.Normal);
        variableModel = RestProcessVariableModel.getRandomProcessVariableModel("d:text");
        processModel = restClient.authenticateUser(adminTenantUser).withWorkflowAPI().getProcesses().getOneRandomEntry().onModel();
       
        processVariable = restClient.withWorkflowAPI().usingProcess(processModel).addProcessVariable(variableModel);
        restClient.assertStatusCodeIs(HttpStatus.CREATED);
        processVariable.assertThat().field("name").is(variableModel.getName())
                        .and().field("type").is(variableModel.getType())
                        .and().field("value").is(variableModel.getValue());
    }

    @TestRail(section = {TestGroup.REST_API, TestGroup.WORKFLOW,TestGroup.PROCESSES }, executionType = ExecutionType.REGRESSION,
            description = "Verify any user cannot get processes from same network")
    @Test(groups = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.PROCESSES, TestGroup.REGRESSION, TestGroup.NETWORKS })
    public void getProcessFromSameNetworkUsingAnyUser() throws Exception
    {
        restClient.authenticateUser(tenantUser).withWorkflowAPI().addProcess("activitiAdhoc", secondTenantUser, false, Priority.Normal);

        restClient.authenticateUser(tenantUser).withWorkflowAPI().getProcesses();
        restClient.assertStatusCodeIs(HttpStatus.OK); 
    }

    @TestRail(section = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.PROCESSES }, executionType = ExecutionType.REGRESSION,
            description = "Verify getProcessTasks using admin from different network with REST API and status code is FORBIDDEN (403)")
    @Test(groups = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.PROCESSES, TestGroup.REGRESSION, TestGroup.NETWORKS })
    public void getProcessTasksWithAdminFromDifferentNetwork() throws Exception
    {
        siteModel = dataSite.usingUser(adminTenantUser).createPublicRandomSite();
        dataContent.usingUser(adminTenantUser).usingSite(siteModel).createContent(CMISUtil.DocumentType.TEXT_PLAIN);
        RestProcessModel processModel = restClient.authenticateUser(adminTenantUser).withWorkflowAPI().addProcess("activitiAdhoc", secondTenantUser, false, CMISUtil.Priority.Normal);

        restClient.authenticateUser(secondAdminTenantUser).withWorkflowAPI()
                .usingProcess(processModel).getProcessTasks().assertThat().entriesListIsEmpty();
        restClient.assertStatusCodeIs(HttpStatus.FORBIDDEN)
                .assertLastError().containsSummary(RestErrorModel.PROCESS_RUNNING_IN_ANOTHER_TENANT);
    }

    @TestRail(section = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.PROCESSES }, executionType = ExecutionType.REGRESSION,
            description = "Verify getProcessTasks using admin from same network with REST API and status code is OK")
    @Test(groups = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.PROCESSES, TestGroup.REGRESSION, TestGroup.NETWORKS })
    public void getProcessTasksWithAdminFromSameNetwork() throws Exception
    {
        siteModel = dataSite.usingUser(adminTenantUser).createPublicRandomSite();
        dataContent.usingUser(adminTenantUser).usingSite(siteModel).createContent(CMISUtil.DocumentType.TEXT_PLAIN);

        RestProcessModel processModel = restClient.authenticateUser(secondTenantUser).withWorkflowAPI().addProcess("activitiAdhoc", secondTenantUser, false, CMISUtil.Priority.Normal);

        restClient.authenticateUser(adminTenantUser).withWorkflowAPI()
                .usingProcess(processModel).getProcessTasks().assertThat().entriesListIsNotEmpty();
        restClient.assertStatusCodeIs(HttpStatus.OK);
    }

    @TestRail(section = { TestGroup.REST_API,TestGroup.WORKFLOW,TestGroup.PROCESSES}, executionType = ExecutionType.REGRESSION, 
            description = "Add multiple process variables using by Admin in other network.")
    @Test(groups = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.PROCESSES, TestGroup.REGRESSION, TestGroup.NETWORKS })
    public void addMultipleProcessVariablesByAdminInOtherNetwork() throws Exception
    {

        siteModel = dataSite.usingUser(adminTenantUser).createPublicRandomSite();
        dataWorkflow.usingUser(tenantUser).usingSite(siteModel).usingResource(document)
                    .createNewTaskAndAssignTo(secondTenantUser);

        variableModel = RestProcessVariableModel.getRandomProcessVariableModel("d:text");
        processVariable = RestProcessVariableModel.getRandomProcessVariableModel("d:text");
        processModel = restClient.authenticateUser(tenantUser).withWorkflowAPI().addProcess("activitiAdhoc", secondTenantUser, false, Priority.Normal);
        restClient.authenticateUser(secondAdminTenantUser).withWorkflowAPI().usingProcess(processModel)
                  .addProcessVariables(variableModel, processVariable);

        restClient.assertStatusCodeIs(HttpStatus.FORBIDDEN)
                  .assertLastError()
                  .containsSummary(RestErrorModel.PROCESS_RUNNING_IN_ANOTHER_TENANT)
                  .descriptionURLIs(RestErrorModel.RESTAPIEXPLORER)
                  .containsErrorKey(RestErrorModel.PROCESS_RUNNING_IN_ANOTHER_TENANT)
                  .stackTraceIs(RestErrorModel.STACKTRACE);
    }

    @TestRail(section = { TestGroup.REST_API,TestGroup.WORKFLOW, TestGroup.PROCESSES}, executionType = ExecutionType.REGRESSION, 
            description = "Add process variables using by Admin in other network.")
    @Test(groups = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.PROCESSES, TestGroup.REGRESSION, TestGroup.NETWORKS })
    public void addProcessVariablesByAdminInOtherNetwork() throws Exception
    { 
        siteModel = dataSite.usingUser(adminTenantUser).createPublicRandomSite();
        dataWorkflow.usingUser(tenantUser).usingSite(siteModel).usingResource(document)
                    .createNewTaskAndAssignTo(secondTenantUser);

        variableModel = RestProcessVariableModel.getRandomProcessVariableModel("d:text");
        processModel = restClient.authenticateUser(tenantUser).withWorkflowAPI().addProcess("activitiAdhoc", secondTenantUser, false, Priority.Normal);
        processVariable = restClient.authenticateUser(secondAdminTenantUser).withWorkflowAPI().usingProcess(processModel)
                .addProcessVariable(variableModel);

        restClient.assertStatusCodeIs(HttpStatus.FORBIDDEN)
                  .assertLastError()
                  .containsSummary(RestErrorModel.PROCESS_RUNNING_IN_ANOTHER_TENANT)
                  .descriptionURLIs(RestErrorModel.RESTAPIEXPLORER)
                  .containsErrorKey(RestErrorModel.PROCESS_RUNNING_IN_ANOTHER_TENANT)
                  .stackTraceIs(RestErrorModel.STACKTRACE);
    }

}
