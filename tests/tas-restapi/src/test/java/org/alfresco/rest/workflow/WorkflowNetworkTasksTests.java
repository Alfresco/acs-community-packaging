package org.alfresco.rest.workflow;

import org.alfresco.dataprep.CMISUtil;
import org.alfresco.dataprep.CMISUtil.DocumentType;
import org.alfresco.dataprep.CMISUtil.Priority;
import org.alfresco.rest.NetworkDataPrep;
import org.alfresco.rest.model.RestErrorModel;
import org.alfresco.rest.model.RestFormModelsCollection;
import org.alfresco.rest.model.RestItemModel;
import org.alfresco.rest.model.RestItemModelsCollection;
import org.alfresco.rest.model.RestProcessDefinitionModel;
import org.alfresco.rest.model.RestProcessModel;
import org.alfresco.rest.model.RestTaskModel;
import org.alfresco.rest.model.RestTaskModelsCollection;
import org.alfresco.rest.model.RestVariableModel;
import org.alfresco.rest.model.RestVariableModelsCollection;
import org.alfresco.utility.model.FileModel;
import org.alfresco.utility.model.SiteModel;
import org.alfresco.utility.model.TaskModel;
import org.alfresco.utility.model.TestGroup;
import org.alfresco.utility.report.Bug;
import org.alfresco.utility.testrail.ExecutionType;
import org.alfresco.utility.testrail.annotation.TestRail;
import org.springframework.http.HttpStatus;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

public class WorkflowNetworkTasksTests extends NetworkDataPrep
{
    private TaskModel taskModel;
    private RestFormModelsCollection returnedCollection;
    private RestTaskModel restTaskModel;
    private RestTaskModelsCollection taskModels;
    private RestVariableModel variableModel, variableModel1;
    private RestVariableModelsCollection restVariableCollection;
    private RestProcessModel restProcessModel;
    private RestItemModelsCollection itemModels;
    private RestItemModel restTaskItem;
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

    @Test(groups = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.TASKS, TestGroup.REGRESSION, TestGroup.NETWORKS })
    @TestRail(section = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.TASKS },
            executionType = ExecutionType.REGRESSION, description = "Verify network admin user gets all task form models inside his network with Rest API and response is successful (200)")
    public void networkAdminGetsTaskFormModels() throws Exception
    {
        RestProcessModel networkProcess = restClient.authenticateUser(adminTenantUser).withWorkflowAPI()
                .addProcess("activitiReview", tenantUser, false, CMISUtil.Priority.High);
        RestTaskModel networkTask = restClient.authenticateUser(adminTenantUser).withWorkflowAPI()
                .usingProcess(networkProcess).getProcessTasks().getOneRandomEntry().onModel();

        restClient.authenticateUser(adminUserModel).withWorkflowAPI().usingTask(networkTask).getTaskFormModel();
        restClient.assertStatusCodeIs(HttpStatus.FORBIDDEN).assertLastError().containsSummary(RestErrorModel.PERMISSION_WAS_DENIED);

        returnedCollection = restClient.authenticateUser(adminTenantUser).withWorkflowAPI().usingTask(networkTask).getTaskFormModel();
        restClient.assertStatusCodeIs(HttpStatus.OK);
        returnedCollection.assertThat().entriesListIsNotEmpty();
    }

    @TestRail(section = { TestGroup.REST_API,  TestGroup.WORKFLOW,TestGroup.TASKS }, executionType = ExecutionType.REGRESSION,
            description = "Add multiple task item using by admin in other network.")
    @Test(groups = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.TASKS, TestGroup.REGRESSION, TestGroup.NETWORKS })
    public void addMultipleTaskItemByAdminInOtherNetwork() throws Exception
    {

        SiteModel siteModel1 = dataSite.usingUser(adminTenantUser).createPublicRandomSite();
        document = dataContent.usingUser(adminTenantUser).usingSite(siteModel1).createContent(DocumentType.XML);
        document2 = dataContent.usingSite(siteModel1).createContent(DocumentType.XML);

        RestProcessModel addedProcess = restClient.authenticateUser(tenantUser).withWorkflowAPI().addProcess("activitiAdhoc", secondTenantUser, false, Priority.Normal);
        RestTaskModel addedTask = restClient.withWorkflowAPI().getTasks().getTaskModelByProcess(addedProcess);
        restClient.authenticateUser(secondAdminTenantUser).withWorkflowAPI().usingTask(addedTask).addTaskItems(document, document2);

        restClient.assertStatusCodeIs(HttpStatus.FORBIDDEN).assertLastError()
                  .containsSummary(RestErrorModel.PERMISSION_WAS_DENIED)
                  .descriptionURLIs(RestErrorModel.RESTAPIEXPLORER)
                  .containsErrorKey(RestErrorModel.PERMISSION_DENIED_ERRORKEY)
                  .stackTraceIs(RestErrorModel.STACKTRACE);;
    }

    @TestRail(section = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.TASKS }, executionType = ExecutionType.REGRESSION,
            description = "Add task item using by admin in other network.")
    @Test(groups = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.TASKS, TestGroup.REGRESSION, TestGroup.NETWORKS })
    public void addTaskItemByAdminInOtherNetwork() throws Exception
    {
        document = dataContent.usingUser(adminTenantUser).usingSite(siteModel).createContent(DocumentType.XML);
        RestProcessModel addedProcess = restClient.authenticateUser(tenantUser).withWorkflowAPI().addProcess("activitiAdhoc", secondTenantUser, false, Priority.Normal);
        RestTaskModel addedTask = restClient.withWorkflowAPI().getTasks().getTaskModelByProcess(addedProcess);
        restClient.authenticateUser(secondAdminTenantUser).withWorkflowAPI().usingTask(addedTask).addTaskItem(document);
                         
        restClient.assertStatusCodeIs(HttpStatus.FORBIDDEN).assertLastError()
                  .containsSummary(RestErrorModel.PERMISSION_WAS_DENIED)
                  .descriptionURLIs(RestErrorModel.RESTAPIEXPLORER)
                  .containsErrorKey(RestErrorModel.PERMISSION_DENIED_ERRORKEY)
                  .stackTraceIs(RestErrorModel.STACKTRACE);        
    }

    @Bug(id = "REPO-1980")
    @Test(groups = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.TASKS, TestGroup.REGRESSION, TestGroup.NETWORKS })
    @TestRail(section = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.TASKS }, executionType = ExecutionType.REGRESSION,
            description = "Verify tenant admin can update only task from its network and response is 200")
    public void adminTenantCanUpdateOnlyTaskInItsNetwork() throws Exception
    {
        FileModel fileModel1 = dataContent.usingUser(tenantUser).usingSite(siteModel).createContent(DocumentType.TEXT_PLAIN);
        RestProcessDefinitionModel def1 = restClient.authenticateUser(tenantUser).withWorkflowAPI().getAllProcessDefinitions().getEntries().get(0).onModel();
        TaskModel taskModel1 = dataWorkflow.usingUser(tenantUser).usingSite(siteModel).usingResource(fileModel1)
                .createTaskWithProcessDefAndAssignTo(def1.getId(), secondTenantUser);

        SiteModel siteModel2 = dataSite.usingUser(differentNetworkTenantUser).createPublicRandomSite();
        FileModel fileModel2 = dataContent.usingUser(differentNetworkTenantUser).usingSite(siteModel2).createContent(DocumentType.TEXT_PLAIN);
        RestProcessDefinitionModel def2 = restClient.authenticateUser(differentNetworkTenantUser).withWorkflowAPI().getAllProcessDefinitions().getEntries().get(0).onModel();
        TaskModel taskModel2 = dataWorkflow.usingUser(differentNetworkTenantUser).usingSite(siteModel2).usingResource(fileModel2)
                .createTaskWithProcessDefAndAssignTo(def2.getId(), secondAdminTenantUser);

        restTaskModel = restClient.authenticateUser(secondAdminTenantUser).withParams("select=state").withWorkflowAPI().usingTask(taskModel2).updateTask("completed");
        restClient.assertStatusCodeIs(HttpStatus.OK);
        restTaskModel.assertThat().field("id").is(taskModel2.getId())
                .and().field("state").is("completed");

        restTaskModel = restClient.authenticateUser(secondAdminTenantUser).withParams("select=state").withWorkflowAPI().usingTask(taskModel1).updateTask("completed");
        restClient.assertStatusCodeIs(HttpStatus.FORBIDDEN).assertLastError()
                .containsErrorKey(RestErrorModel.PERMISSION_DENIED_ERRORKEY)
                .containsSummary(RestErrorModel.PERMISSION_WAS_DENIED)
                .descriptionURLIs(RestErrorModel.RESTAPIEXPLORER)
                .stackTraceIs(RestErrorModel.STACKTRACE);
    }

    @Test(groups = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.TASKS, TestGroup.REGRESSION, TestGroup.NETWORKS })
    @TestRail(section = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.TASKS }, executionType = ExecutionType.REGRESSION,
            description = "Verify that admin from the same network is able to add multiple task variables")
    public void addMultipleTaskVariablesByTenantAdmin() throws Exception
    {
        RestProcessModel networkProcess1 = restClient.authenticateUser(tenantUser).withWorkflowAPI()
                .addProcess("activitiReview", tenantUser, false, CMISUtil.Priority.High);
        RestTaskModel task = restClient.authenticateUser(adminTenantUser)
                            .withWorkflowAPI().usingProcess(networkProcess1).getProcessTasks().getOneRandomEntry();
        restClient.authenticateUser(adminTenantUser);
        
        variableModel = RestVariableModel.getRandomTaskVariableModel("local", "d:text");
        variableModel1 = RestVariableModel.getRandomTaskVariableModel("global", "d:text");
                
        restVariableCollection = restClient.withWorkflowAPI().usingTask(task.onModel())
                                           .addTaskVariables(variableModel, variableModel1);
        restClient.assertStatusCodeIs(HttpStatus.CREATED);
        restVariableCollection.getEntries().get(0).onModel().assertThat()
                              .field("scope").is(variableModel.getScope())
                              .and().field("name").is(variableModel.getName())
                              .and().field("value").is(variableModel.getValue())
                              .and().field("type").is(variableModel.getType());

        restVariableCollection.getEntries().get(1).onModel().assertThat()
                              .field("scope").is(variableModel1.getScope())
                              .and().field("name").is(variableModel1.getName())
                              .and().field("value").is(variableModel1.getValue())
                              .and().field("type").is(variableModel1.getType());
    }

    @Test(groups = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.TASKS, TestGroup.REGRESSION, TestGroup.NETWORKS })
    @TestRail(section = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.TASKS }, executionType = ExecutionType.REGRESSION,
            description = "Verify that admin from the same network is able to add and remove task variables")
    public void addAndRemoveTaskVariablesByTenantAdmin() throws Exception
    {
        RestProcessModel networkProcess1 = restClient.authenticateUser(adminTenantUser).withWorkflowAPI()
                .addProcess("activitiReview", adminTenantUser, false, CMISUtil.Priority.High);
        RestTaskModel task = restClient.authenticateUser(adminTenantUser)
                            .withWorkflowAPI().usingProcess(networkProcess1).getProcessTasks().getOneRandomEntry();
        restClient.authenticateUser(adminTenantUser);
        RestVariableModel variableModel = RestVariableModel.getRandomTaskVariableModel("local", "d:text");
        variableModel = restClient.withWorkflowAPI()
                                    .usingTask(task.onModel())
                                        .addTaskVariable(variableModel);
        restClient.assertStatusCodeIs(HttpStatus.CREATED);
        variableModel.assertThat()
                        .field("scope").is(variableModel.getScope())
                        .and().field("name").is(variableModel.getName())
                        .and().field("value").is(variableModel.getValue())
                        .and().field("type").is(variableModel.getType());

        restClient.withWorkflowAPI().usingTask(task.onModel()).deleteTaskVariable(variableModel);
        restClient.assertStatusCodeIs(HttpStatus.NO_CONTENT);
        restClient.withWorkflowAPI().usingTask(task.onModel()).getTaskVariables()
            .assertThat().entriesListDoesNotContain("name", variableModel.getName());
    }

    @Test(groups = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.TASKS, TestGroup.REGRESSION, TestGroup.NETWORKS })
    @TestRail(section = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.TASKS }, executionType = ExecutionType.REGRESSION,
            description = "Verify that admin from the other network is not able to remove task variables")
    public void addTaskVariablesByTenantAdminOtherNetwork() throws Exception
    {
        RestProcessModel networkProcess1 = restClient.authenticateUser(adminTenantUser).withWorkflowAPI()
                .addProcess("activitiReview", adminTenantUser, false, CMISUtil.Priority.High);
        RestTaskModel task = restClient.authenticateUser(adminTenantUser)
                            .withWorkflowAPI().usingProcess(networkProcess1).getProcessTasks().getOneRandomEntry();
        restClient.authenticateUser(adminTenantUser);
        RestVariableModel variableModel = RestVariableModel.getRandomTaskVariableModel("local", "d:text");
        restClient.withWorkflowAPI()
            .usingTask(task.onModel())
                .addTaskVariable(variableModel);
        restClient.assertStatusCodeIs(HttpStatus.CREATED);
        restClient.authenticateUser(secondAdminTenantUser).withWorkflowAPI().usingTask(task.onModel()).deleteTaskVariable(variableModel);
        restClient.assertStatusCodeIs(HttpStatus.FORBIDDEN)
            .assertLastError().containsErrorKey(RestErrorModel.PERMISSION_DENIED_ERRORKEY)
            .containsSummary(RestErrorModel.PERMISSION_WAS_DENIED)
            .descriptionURLIs(RestErrorModel.RESTAPIEXPLORER)
            .stackTraceIs(RestErrorModel.STACKTRACE);
    }

    @Test(groups = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.TASKS, TestGroup.REGRESSION, TestGroup.NETWORKS })
    @TestRail(section = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.TASKS }, executionType = ExecutionType.REGRESSION,
            description = "Verify that admin from another network is not able to add task variables")
    public void addTaskVariablesByTenantFromAnotherNetwork() throws Exception
    {
        RestProcessModel networkProcess1 = restClient.authenticateUser(tenantUser).withWorkflowAPI()
                .addProcess("activitiReview", tenantUser, false, CMISUtil.Priority.High);
        RestTaskModel task = restClient.authenticateUser(adminTenantUser)
                            .withWorkflowAPI().usingProcess(networkProcess1).getProcessTasks().getOneRandomEntry();
        RestVariableModel variableModel = RestVariableModel.getRandomTaskVariableModel("local", "d:text");
        variableModel = restClient.authenticateUser(secondTenantUser).withWorkflowAPI()
                    .usingTask(task.onModel())
                        .addTaskVariable(variableModel);
        restClient.assertStatusCodeIs(HttpStatus.FORBIDDEN)
            .assertLastError().containsErrorKey(RestErrorModel.PERMISSION_DENIED_ERRORKEY)
                              .containsSummary(RestErrorModel.PERMISSION_WAS_DENIED)
                              .descriptionURLIs(RestErrorModel.RESTAPIEXPLORER)
                              .stackTraceIs(RestErrorModel.STACKTRACE);
    }

    @Test(groups = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.TASKS, TestGroup.REGRESSION, TestGroup.NETWORKS })
    @TestRail(section = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.TASKS }, executionType = ExecutionType.REGRESSION,
            description = "Verify that admin from another network is not able to add multiple task variables")
    public void addMultipleTaskVariablesByTenantFromAnotherNetwork() throws Exception
    {
        RestProcessModel networkProcess1 = restClient.authenticateUser(tenantUser).withWorkflowAPI()
                .addProcess("activitiReview", tenantUser, false, CMISUtil.Priority.High);
        RestTaskModel task = restClient.authenticateUser(adminTenantUser)
                            .withWorkflowAPI().usingProcess(networkProcess1).getProcessTasks().getOneRandomEntry();
        restClient.authenticateUser(adminTenantUser);

        variableModel = RestVariableModel.getRandomTaskVariableModel("local", "d:text");
        variableModel1 = RestVariableModel.getRandomTaskVariableModel("global", "d:text");
        restVariableCollection = restClient.authenticateUser(secondAdminTenantUser).withWorkflowAPI()
                                           .usingTask(task.onModel())
                                           .addTaskVariables(variableModel,variableModel1);
        restClient.assertStatusCodeIs(HttpStatus.FORBIDDEN)
            .assertLastError().containsErrorKey(RestErrorModel.PERMISSION_DENIED_ERRORKEY)
                              .containsSummary(RestErrorModel.PERMISSION_WAS_DENIED)
                              .descriptionURLIs(RestErrorModel.RESTAPIEXPLORER)
                              .stackTraceIs(RestErrorModel.STACKTRACE);
    }

    @Test(groups = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.TASKS, TestGroup.REGRESSION, TestGroup.NETWORKS })
    @TestRail(section = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.TASKS }, executionType = ExecutionType.REGRESSION,
            description = "Verify that admin from another network is not able to update task variables")
    public void updateTaskVariablesByTenantFromAnotherNetwork() throws Exception
    {
        RestProcessModel networkProcess1 = restClient.authenticateUser(tenantUser).withWorkflowAPI()
                .addProcess("activitiReview", tenantUser, false, CMISUtil.Priority.High);
        RestTaskModel task = restClient.authenticateUser(adminTenantUser)
                            .withWorkflowAPI().usingProcess(networkProcess1).getProcessTasks().getOneRandomEntry();
        restClient.authenticateUser(adminTenantUser);
        RestVariableModel variableModel = RestVariableModel.getRandomTaskVariableModel("local", "d:text");
        variableModel = restClient.authenticateUser(adminTenantUser).withWorkflowAPI().usingTask(task.onModel())
                                 .addTaskVariable(variableModel);
        restClient.assertStatusCodeIs(HttpStatus.CREATED);
        
        restClient.authenticateUser(secondAdminTenantUser).withWorkflowAPI().usingTask(task.onModel())
                 .updateTaskVariable(variableModel);
                        
        restClient.assertStatusCodeIs(HttpStatus.FORBIDDEN)
            .assertLastError().containsErrorKey(RestErrorModel.PERMISSION_DENIED_ERRORKEY)
                              .containsSummary(RestErrorModel.PERMISSION_WAS_DENIED)
                              .descriptionURLIs(RestErrorModel.RESTAPIEXPLORER)
                              .stackTraceIs(RestErrorModel.STACKTRACE);
    }

    @Test(groups = {  TestGroup.REST_API, TestGroup.NETWORKS, TestGroup.WORKFLOW, TestGroup.TASKS, TestGroup.REGRESSION })
    @TestRail(section = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.TASKS },
            executionType = ExecutionType.REGRESSION, description = "Check that for admin network gets tasks only from its network.")
    public void adminTenantGetsTasksOnlyFromItsNetwork() throws Exception
    {
        RestProcessModel processOnTenant1 = restClient.authenticateUser(tenantUser).withWorkflowAPI().addProcess("activitiAdhoc", secondTenantUser, false, Priority.Low);
        restClient.assertStatusCodeIs(HttpStatus.CREATED);

        RestProcessModel processOnTenant2 = restClient.authenticateUser(differentNetworkTenantUser).withWorkflowAPI().addProcess("activitiAdhoc", secondAdminTenantUser, false, Priority.Normal);
        restClient.assertStatusCodeIs(HttpStatus.CREATED);

        RestTaskModelsCollection tenantTasks1 = restClient.authenticateUser(adminTenantUser).withWorkflowAPI().getTasks();
        restClient.assertStatusCodeIs(HttpStatus.OK);
        tenantTasks1.assertThat().entriesListIsNotEmpty()
                .and().entriesListIsNotEmpty()
                .and().entriesListContains("assignee", String.format("sTenant@%s", secondTenantUser.getDomain().toLowerCase()))
                .and().entriesListContains("processId", processOnTenant1.getId())
                .and().entriesListDoesNotContain("assignee", String.format("admin@%s", secondAdminTenantUser.getDomain().toLowerCase()))
                .and().entriesListDoesNotContain("processId", processOnTenant2.getId());

        RestTaskModelsCollection tenantTasks2 = restClient.authenticateUser(secondAdminTenantUser).withWorkflowAPI().getTasks();
        restClient.assertStatusCodeIs(HttpStatus.OK);
        tenantTasks2.assertThat().entriesListIsNotEmpty()
                .and().entriesListIsNotEmpty()
                .and().entriesListContains("assignee", String.format("admin@%s", secondAdminTenantUser.getDomain().toLowerCase()))
                .and().entriesListContains("processId", processOnTenant2.getId())
                .and().entriesListDoesNotContain("assignee", String.format("sTenant@%s", secondTenantUser.getDomain().toLowerCase()))
                .and().entriesListDoesNotContain("processId", processOnTenant1.getId());
    }

    @TestRail(section = { TestGroup.REST_API, TestGroup.WORKFLOW,
            TestGroup.TASKS }, executionType = ExecutionType.REGRESSION, description = "Check that for network enabled only that network tasks are returned.")
    @Test(groups = {  TestGroup.REST_API, TestGroup.NETWORKS,  TestGroup.REGRESSION })
    public void networkEnabledTasksReturned() throws Exception
    {
        restClient.authenticateUser(tenantUser).withWorkflowAPI().addProcess("activitiAdhoc", secondTenantUser, false, Priority.Normal);

        taskModels = restClient.authenticateUser(secondTenantUser).withWorkflowAPI().getTasks();
        restClient.assertStatusCodeIs(HttpStatus.OK);
        taskModels.assertThat().entriesListIsNotEmpty().and().entriesListContains("assignee",
                String.format("sTenant@%s", secondTenantUser.getDomain().toLowerCase()));
    }

    @TestRail(section = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.TASKS }, executionType = ExecutionType.REGRESSION,
            description = "Verify gets task items call with admin from different network")
    @Test(groups = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.TASKS, TestGroup.REGRESSION, TestGroup.NETWORKS  })
    public void getTaskItemsByAdminFromAnotherNetwork() throws Exception
    {

        siteModel = dataSite.usingUser(adminTenantUser).createPublicRandomSite();
        document = dataContent.usingUser(adminTenantUser).usingSite(siteModel).createContent(DocumentType.XML);
        restProcessModel = restClient.authenticateUser(tenantUser).withWorkflowAPI().addProcess("activitiAdhoc", secondTenantUser, false, Priority.Normal);
        restTaskModel = restClient.withWorkflowAPI().getTasks().getTaskModelByProcess(restProcessModel);
        restClient.withWorkflowAPI().usingTask(restTaskModel).addTaskItem(document);

        restClient.authenticateUser(secondAdminTenantUser).withWorkflowAPI().usingTask(restTaskModel).getTaskItems();
        restClient.assertStatusCodeIs(HttpStatus.FORBIDDEN)
                .assertLastError().containsSummary(RestErrorModel.PERMISSION_WAS_DENIED);
    }

    @TestRail(section = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.TASKS }, executionType = ExecutionType.REGRESSION,
            description = "Verify gets task items call returns only task items inside network")
    @Test(groups = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.TASKS, TestGroup.REGRESSION, TestGroup.NETWORKS  })
    public void getTaskItemsReturnsOnlyItemsFromThatNetwork() throws Exception
    {
        SiteModel siteModel1 = dataSite.usingUser(adminTenantUser).createPublicRandomSite();
        FileModel fileModel1 = dataContent.usingUser(adminTenantUser).usingSite(siteModel1).createContent(DocumentType.XML);

        RestProcessModel addedProcess1 = restClient.authenticateUser(adminTenantUser).withWorkflowAPI().addProcess("activitiAdhoc", tenantUser, false, Priority.Normal);
        RestTaskModel addedTask1 = restClient.withWorkflowAPI().getTasks().getTaskModelByProcess(addedProcess1);
        RestItemModel taskItem1 = restClient.withWorkflowAPI().usingTask(addedTask1).addTaskItem(fileModel1);

        SiteModel siteModel2 = dataSite.usingUser(secondAdminTenantUser).createPublicRandomSite();
        FileModel fileModel2 = dataContent.usingUser(secondAdminTenantUser).usingSite(siteModel2).createContent(DocumentType.XML);

        RestProcessModel addedProcess2 = restClient.authenticateUser(secondAdminTenantUser).withWorkflowAPI().addProcess("activitiAdhoc", differentNetworkTenantUser, false, Priority.Normal);
        RestTaskModel addedTask2 = restClient.withWorkflowAPI().getTasks().getTaskModelByProcess(addedProcess2);
        RestItemModel taskItem2 = restClient.withWorkflowAPI().usingTask(addedTask2).addTaskItem(fileModel2);

        itemModels = restClient.withWorkflowAPI().usingTask(addedTask2).getTaskItems();
        restClient.assertStatusCodeIs(HttpStatus.OK);
        itemModels.assertThat()
                .entriesListIsNotEmpty().and()
                .entriesListContains("id", taskItem2.getId()).and()
                .entriesListContains("name", fileModel2.getName()).and()
                .entriesListDoesNotContain("id", taskItem1.getId()).and()
                .entriesListDoesNotContain("name", fileModel1.getName());
    }

    @Test(groups = {  TestGroup.REST_API, TestGroup.NETWORKS, TestGroup.WORKFLOW, TestGroup.TASKS, TestGroup.REGRESSION })
    @TestRail(section = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.TASKS }, 
        executionType = ExecutionType.REGRESSION, description = "Check that for admin network gets task only from its network.")
    public void adminTenantGetsTaskOnlyFromItsNetwork() throws Exception
    {
        RestProcessModel processOnTenant1 = restClient.authenticateUser(tenantUser).withWorkflowAPI().addProcess("activitiAdhoc", secondTenantUser, false, Priority.Low);
        restClient.assertStatusCodeIs(HttpStatus.CREATED);

        RestProcessModel processOnTenant2 = restClient.authenticateUser(differentNetworkTenantUser).withWorkflowAPI().addProcess("activitiAdhoc", secondAdminTenantUser, false, Priority.Normal);
        restClient.assertStatusCodeIs(HttpStatus.CREATED);

        RestTaskModelsCollection tenantTasks2 = restClient.authenticateUser(differentNetworkTenantUser).withWorkflowAPI().getTasks();
        restClient.assertStatusCodeIs(HttpStatus.OK);
        tenantTasks2.assertThat().entriesListIsNotEmpty()
            .and().entriesListCountIs(1)
            .and().entriesListContains("assignee", String.format("admin@%s", secondAdminTenantUser.getDomain().toLowerCase()))
            .and().entriesListContains("processId", processOnTenant2.getId())
            .and().entriesListDoesNotContain("assignee", String.format("admin@%s", secondTenantUser.getDomain().toLowerCase()))
            .and().entriesListDoesNotContain("processId", processOnTenant1.getId());
    }

    @TestRail(section = { TestGroup.REST_API, TestGroup.WORKFLOW,
            TestGroup.TASKS }, executionType = ExecutionType.REGRESSION, description = "Verify that in a network only tasks inside network are returned.")
    @Test(groups = {TestGroup.NETWORKS,TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.TASKS, TestGroup.REGRESSION  })
    public void tasksInsideNetworkReturned() throws Exception
    {

        RestProcessModel addedProcess = restClient.authenticateUser(tenantUser).withWorkflowAPI().addProcess("activitiAdhoc", secondTenantUser, false,
                Priority.Normal);
        restClient.assertStatusCodeIs(HttpStatus.CREATED);
        taskModel = restClient.withWorkflowAPI().getTasks().getTaskModelByProcess(addedProcess);
        restTaskModel = restClient.authenticateUser(secondTenantUser).withWorkflowAPI().usingTask(taskModel).getTask();
        restClient.assertStatusCodeIs(HttpStatus.OK);
        restTaskModel.assertThat().field("assignee").is(String.format("sTenant@%s", secondTenantUser.getDomain().toLowerCase())).and()
                .field("processId").is(addedProcess.getId());
    }

    @TestRail(section = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.TASKS }, executionType = ExecutionType.SANITY,
            description = "Verify that user that started the process gets task items")
    @Test(groups = { TestGroup.NETWORKS,TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.TASKS, TestGroup.SANITY})
    public void getTaskItemsByAdminInSameNetwork() throws Exception
    {

        siteModel = dataSite.usingUser(adminTenantUser).createPublicRandomSite();
        document = dataContent.usingUser(adminTenantUser).usingSite(siteModel).createContent(DocumentType.XML);
        RestProcessModel addedProcess = restClient.authenticateUser(tenantUser).withWorkflowAPI().addProcess("activitiAdhoc", secondTenantUser, false, Priority.Normal);
        RestTaskModel addedTask = restClient.authenticateUser(adminTenantUser).withWorkflowAPI().getTasks().getTaskModelByProcess(addedProcess);  
        restTaskItem = restClient.withWorkflowAPI().usingTask(addedTask).addTaskItem(document);
        
        itemModels = restClient.withWorkflowAPI().usingTask(addedTask).getTaskItems();
        restClient.assertStatusCodeIs(HttpStatus.OK);
        itemModels.assertThat()
            .entriesListIsNotEmpty().and()
            .entriesListContains("id", restTaskItem.getId()).and()
            .entriesListContains("name", document.getName());
    }

    @Test(groups = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.TASKS, TestGroup.REGRESSION, TestGroup.NETWORKS })
    @TestRail(section = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.TASKS }, executionType = ExecutionType.REGRESSION,
            description = "Verify that admin from the same network is able to retrieve network task variables")
    public void getTaskVariablesByTenantAdmin() throws Exception
    {
        RestProcessModel networkProcess1 = restClient.authenticateUser(tenantUser).withWorkflowAPI()
                .addProcess("activitiReview", tenantUser, false, CMISUtil.Priority.High);
        RestTaskModel task = restClient.authenticateUser(adminTenantUser)
                        .withWorkflowAPI().usingProcess(networkProcess1).getProcessTasks().getOneRandomEntry();
        
        restVariableCollection = restClient.withWorkflowAPI().usingTask(task.onModel()).getTaskVariables();
        restClient.assertStatusCodeIs(HttpStatus.OK);
        restVariableCollection.assertThat().entriesListIsNotEmpty();
    }

    @Test(groups = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.TASKS, TestGroup.REGRESSION, TestGroup.NETWORKS })
    @TestRail(section = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.TASKS }, executionType = ExecutionType.REGRESSION,
            description = "Verify that admin from another network is not able to retrieve network task variables")
    public void getTaskVariablesByTenantFromAnotherNetwork() throws Exception
    {
        RestProcessModel networkProcess1 = restClient.authenticateUser(tenantUser).withWorkflowAPI()
                .addProcess("activitiReview", tenantUser, false, CMISUtil.Priority.High);
        RestTaskModel task = restClient.authenticateUser(adminTenantUser)
                        .withWorkflowAPI().usingProcess(networkProcess1).getProcessTasks().getOneRandomEntry();
        
        restVariableCollection = restClient.authenticateUser(secondAdminTenantUser).withWorkflowAPI().usingTask(task.onModel()).getTaskVariables();
        restClient.assertStatusCodeIs(HttpStatus.FORBIDDEN)
            .assertLastError().containsSummary(RestErrorModel.PERMISSION_WAS_DENIED)
            .containsErrorKey(RestErrorModel.PERMISSION_DENIED_ERRORKEY)
            .stackTraceIs(RestErrorModel.STACKTRACE)
            .descriptionURLIs(RestErrorModel.RESTAPIEXPLORER);
    }

    @Test(groups = { TestGroup.NETWORKS,TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.TASKS, TestGroup.REGRESSION })
    @TestRail(section = {TestGroup.REST_API, TestGroup.PROCESSES }, executionType = ExecutionType.REGRESSION,
            description = "Add task item using admin user from same network")
    public void addMultipleTaskItemByAdminSameNetwork() throws Exception
    {

        siteModel = dataSite.usingUser(adminTenantUser).createPublicRandomSite();
        document = dataContent.usingUser(adminTenantUser).usingSite(siteModel).createContent(DocumentType.XML);
        document2 = dataContent.usingUser(adminTenantUser).usingUser(adminTenantUser).usingSite(siteModel).createContent(DocumentType.XML);
        RestProcessModel addedProcess = restClient.authenticateUser(tenantUser).withWorkflowAPI().addProcess("activitiAdhoc", secondTenantUser, false, Priority.Normal);
        RestTaskModel addedTask = restClient.authenticateUser(adminTenantUser).withWorkflowAPI().getTasks().getTaskModelByProcess(addedProcess);
                            
        itemModels = restClient.authenticateUser(adminTenantUser).withWorkflowAPI().usingTask(addedTask)
                              .addTaskItems(document2,document);
        restClient.assertStatusCodeIs(HttpStatus.CREATED);
        
        itemModels.getEntries().get(0).onModel()
                 .assertThat().field("createdAt").is(itemModels.getEntries().get(0).onModel().getCreatedAt())
                 .assertThat().field("size").is(itemModels.getEntries().get(0).onModel().getSize())
                 .assertThat().field("createdBy").is(itemModels.getEntries().get(0).onModel().getCreatedBy())
                 .assertThat().field("modifiedAt").is(itemModels.getEntries().get(0).onModel().getModifiedAt())
                 .assertThat().field("name").is(itemModels.getEntries().get(0).onModel().getName())
                 .assertThat().field("modifiedBy").is(itemModels.getEntries().get(0).onModel().getModifiedBy())
                 .assertThat().field("id").is(itemModels.getEntries().get(0).onModel().getId())
                 .assertThat().field("mimeType").is(itemModels.getEntries().get(0).onModel().getMimeType());

        itemModels.getEntries().get(1).onModel()
                 .assertThat().field("createdAt").is(itemModels.getEntries().get(1).onModel().getCreatedAt())
                 .assertThat().field("size").is(itemModels.getEntries().get(1).onModel().getSize())
                 .assertThat().field("createdBy").is(itemModels.getEntries().get(1).onModel().getCreatedBy())
                 .assertThat().field("modifiedAt").is(itemModels.getEntries().get(1).onModel().getModifiedAt())
                 .assertThat().field("name").is(itemModels.getEntries().get(1).onModel().getName())
                 .assertThat().field("modifiedBy").is(itemModels.getEntries().get(1).onModel().getModifiedBy())
                 .assertThat().field("id").is(itemModels.getEntries().get(1).onModel().getId());
    }

    @Test(groups = { TestGroup.NETWORKS,TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.TASKS, TestGroup.REGRESSION })
    @TestRail(section = {TestGroup.REST_API, TestGroup.PROCESSES }, executionType = ExecutionType.REGRESSION,
            description = "Add task item using admin user from same network")
    public void addTaskItemByAdminSameNetwork() throws Exception
    {
        siteModel = dataSite.usingUser(adminTenantUser).createPublicRandomSite();
        document = dataContent.usingUser(adminTenantUser).usingSite(siteModel).createContent(DocumentType.XML);
        RestProcessModel addedProcess = restClient.authenticateUser(tenantUser).withWorkflowAPI().addProcess("activitiAdhoc", secondTenantUser, false, Priority.Normal);
        RestTaskModel addedTask = restClient.authenticateUser(adminTenantUser).withWorkflowAPI().getTasks().getTaskModelByProcess(addedProcess);  
                        
        restTaskItem = restClient.withWorkflowAPI().usingTask(addedTask).addTaskItem(document);
        restClient.assertStatusCodeIs(HttpStatus.CREATED);
        restTaskItem.assertThat().field("createdAt").is(restTaskItem.getCreatedAt())
                .and().field("size").is(restTaskItem.getSize())
                .and().field("createdBy").is(restTaskItem.getCreatedBy())
                .and().field("modifiedAt").is(restTaskItem.getModifiedAt())
                .and().field("name").is(restTaskItem.getName())
                .and().field("modifiedBy").is(restTaskItem.getModifiedBy())
                .and().field("id").is(restTaskItem.getId())
                .and().field("mimeType").is(restTaskItem.getMimeType());
    }

    @Test(groups = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.TASKS, TestGroup.REGRESSION, TestGroup.NETWORKS})
    @TestRail(section = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.TASKS }, executionType = ExecutionType.REGRESSION,
            description = "Update existing task variable by admin in the same network")    
    public void updateTaskVariableByAdminInSameNetwork() throws Exception
    {
        RestProcessModel networkProcess1 = restClient.authenticateUser(tenantUser).withWorkflowAPI()
                .addProcess("activitiReview", tenantUser, false, CMISUtil.Priority.High);
        RestTaskModel task = restClient.authenticateUser(adminTenantUser)
                            .withWorkflowAPI().usingProcess(networkProcess1).getProcessTasks().getOneRandomEntry();

        variableModel = RestVariableModel.getRandomTaskVariableModel("local", "d:text");
        variableModel1 = restClient.withWorkflowAPI().usingTask(task.onModel())
                                 .updateTaskVariable(variableModel);
        restClient.assertStatusCodeIs(HttpStatus.OK);
        variableModel1.assertThat().field("scope").is(variableModel1.getScope())
            .and().field("name").is(variableModel1.getName())
            .and().field("type").is(variableModel1.getType())
            .and().field("value").is(variableModel1.getValue());

        variableModel.setValue("updatedValue");
        variableModel1 = restClient.withWorkflowAPI().usingTask(task.onModel()).updateTaskVariable(variableModel).and().field("value").is("updatedValue");
        restClient.assertStatusCodeIs(HttpStatus.OK);
    }

    @Test(groups = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.TASKS, TestGroup.REGRESSION, TestGroup.NETWORKS })
    @TestRail(section = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.TASKS }, executionType = ExecutionType.REGRESSION,
            description = "Verify tenant user that created the task can update task only in its network and response is 200, for other networks is 403")
    public void ownerTenantCanUpdatedTaskOnlyInItsNetwork() throws Exception
    {


        SiteModel siteModel1 = dataSite.usingUser(tenantUser).createPublicRandomSite();
        FileModel fileModel1 = dataContent.usingUser(tenantUser).usingSite(siteModel1).createContent(DocumentType.TEXT_PLAIN);
        RestProcessDefinitionModel def1 = restClient.authenticateUser(tenantUser).withWorkflowAPI().getAllProcessDefinitions().getEntries().get(0).onModel();
        TaskModel taskModel1 = dataWorkflow.usingUser(tenantUser).usingSite(siteModel1).usingResource(fileModel1)
                .createTaskWithProcessDefAndAssignTo(def1.getId(), secondTenantUser);

        SiteModel siteModel2 = dataSite.usingUser(differentNetworkTenantUser).createPublicRandomSite();
        FileModel fileModel2 = dataContent.usingUser(differentNetworkTenantUser).usingSite(siteModel2).createContent(DocumentType.TEXT_PLAIN);
        RestProcessDefinitionModel def2 = restClient.authenticateUser(differentNetworkTenantUser).withWorkflowAPI().getAllProcessDefinitions().getEntries().get(0).onModel();
        TaskModel taskModel2 = dataWorkflow.usingUser(differentNetworkTenantUser).usingSite(siteModel2).usingResource(fileModel2)
                .createTaskWithProcessDefAndAssignTo(def2.getId(), secondAdminTenantUser);

        restTaskModel = restClient.authenticateUser(differentNetworkTenantUser).withParams("select=state").withWorkflowAPI().usingTask(taskModel2).updateTask("completed");
        restClient.assertStatusCodeIs(HttpStatus.OK);
        restTaskModel.assertThat().field("id").is(taskModel2.getId())
                .and().field("state").is("completed");

        restTaskModel = restClient.authenticateUser(differentNetworkTenantUser).withParams("select=state").withWorkflowAPI().usingTask(taskModel1).updateTask("completed");
        restClient.assertStatusCodeIs(HttpStatus.FORBIDDEN).assertLastError()
                .containsErrorKey(RestErrorModel.PERMISSION_DENIED_ERRORKEY)
                .containsSummary(RestErrorModel.PERMISSION_WAS_DENIED)
                .descriptionURLIs(RestErrorModel.RESTAPIEXPLORER)
                .stackTraceIs(RestErrorModel.STACKTRACE);

    }
}
