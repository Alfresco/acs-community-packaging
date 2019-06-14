package org.alfresco.rest.workflow;

import org.alfresco.dataprep.CMISUtil.Priority;
import org.alfresco.rest.NetworkDataPrep;
import org.alfresco.rest.exception.JsonToModelConversionException;
import org.alfresco.rest.model.RestErrorModel;
import org.alfresco.rest.model.RestProcessDefinitionModel;
import org.alfresco.rest.model.RestProcessDefinitionModelsCollection;
import org.alfresco.rest.model.RestProcessModel;
import org.alfresco.utility.model.TestGroup;
import org.alfresco.utility.report.Bug;
import org.alfresco.utility.testrail.ExecutionType;
import org.alfresco.utility.testrail.annotation.TestRail;
import org.springframework.http.HttpStatus;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

public class WorkflowNetworkProcessDefinitionsTests extends NetworkDataPrep
{
    private RestProcessDefinitionModel randomProcessDefinition, returnedProcessDefinition;
    private RestProcessModel addedProcess;

    @BeforeClass(alwaysRun = true)
    public void dataPreparation() throws Exception
    {
        init();
    }

    @TestRail(section = { TestGroup.REST_API, TestGroup.WORKFLOW,  TestGroup.PROCESS_DEFINITION },
            executionType = ExecutionType.REGRESSION,
            description = "Verify network admin is able to get a process definition using REST API and status code is OK (200)")
    @Test(groups = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.PROCESS_DEFINITION, TestGroup.REGRESSION, TestGroup.NETWORKS })
    public void networkAdminGetProcessDefinition() throws Exception
    {
        randomProcessDefinition = restClient.authenticateUser(adminTenantUser).withWorkflowAPI().getAllProcessDefinitions().getOneRandomEntry().onModel();
        returnedProcessDefinition = restClient.withWorkflowAPI().usingProcessDefinitions(randomProcessDefinition).getProcessDefinition();
        restClient.assertStatusCodeIs(HttpStatus.OK);
        returnedProcessDefinition.assertThat().field("name").is(randomProcessDefinition.getName());
    }

    @TestRail(section = { TestGroup.REST_API,  TestGroup.WORKFLOW, TestGroup.PROCESS_DEFINITION },
            executionType = ExecutionType.REGRESSION,
            description = "Verify network user is able to get a process definition using REST API and status code is OK (200)")
    @Test(groups = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.PROCESS_DEFINITION, TestGroup.REGRESSION, TestGroup.NETWORKS })
    public void networkUserGetProcessDefinition() throws Exception
    {
        randomProcessDefinition = restClient.authenticateUser(adminTenantUser).withWorkflowAPI()
                .getAllProcessDefinitions().getOneRandomEntry().onModel();
        returnedProcessDefinition = restClient.authenticateUser(tenantUser).withWorkflowAPI()
                .usingProcessDefinitions(randomProcessDefinition).getProcessDefinition();
        restClient.assertStatusCodeIs(HttpStatus.OK);
        returnedProcessDefinition.assertThat().field("name").is(randomProcessDefinition.getName());
    }

    @TestRail(section = { TestGroup.REST_API,  TestGroup.WORKFLOW, TestGroup.PROCESS_DEFINITION }, executionType = ExecutionType.REGRESSION,
            description = "Verify get process definitions using any network user for network enabled deployments with REST API status code is OK (200)")
    @Test(groups = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.PROCESS_DEFINITION, TestGroup.REGRESSION, TestGroup.NETWORKS})
    public void networkUserGetsProcessDefinitions() throws Exception
    {
        restClient.authenticateUser(tenantUser)
                .withWorkflowAPI()
                .getAllProcessDefinitions()
                .assertThat().entriesListIsNotEmpty();
        restClient.assertStatusCodeIs(HttpStatus.OK);
    }

    @TestRail(section = { TestGroup.REST_API,  TestGroup.WORKFLOW, TestGroup.PROCESS_DEFINITION },
            executionType = ExecutionType.REGRESSION,
            description = "Verify Network user is not able to get a process definition from another network")
    @Test(groups = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.PROCESS_DEFINITION, TestGroup.NETWORKS, TestGroup.REGRESSION })
    public void getProcessDefinitionFromAnotherNetwork() throws Exception
    {
        randomProcessDefinition = restClient.authenticateUser(adminTenantUser).withWorkflowAPI()
                .getAllProcessDefinitions().getOneRandomEntry().onModel();
        restClient.authenticateUser(secondAdminTenantUser).withWorkflowAPI()
                .usingProcessDefinitions(randomProcessDefinition).getProcessDefinition();
        restClient.assertStatusCodeIs(HttpStatus.NOT_FOUND)
                .assertLastError().containsSummary(String.format(RestErrorModel.ENTITY_NOT_FOUND, randomProcessDefinition.getId()))
                .containsErrorKey(RestErrorModel.ENTITY_NOT_FOUND_ERRORKEY)
                .descriptionURLIs(RestErrorModel.RESTAPIEXPLORER)
                .stackTraceIs(RestErrorModel.STACKTRACE);
    }

    @TestRail(section = { TestGroup.REST_API, TestGroup.WORKFLOW,  TestGroup.PROCESS_DEFINITION },
            executionType = ExecutionType.REGRESSION,
            description = "Verify Tenant User doesn't get process definitions for another network deployment using REST API")
    @Test(groups = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.PROCESS_DEFINITION, TestGroup.REGRESSION, TestGroup.NETWORKS })
    public void networkUserIsNotAbleToGetProcessDefinitionsForAnotherNetwork() throws Exception
    {
        RestProcessDefinitionModel randomProcessDefinitionTenant1 = restClient.authenticateUser(adminTenantUser).withWorkflowAPI()
                .getAllProcessDefinitions().getOneRandomEntry().onModel();
        RestProcessDefinitionModelsCollection processDefinitionsTenant2 = restClient.authenticateUser(secondAdminTenantUser).withWorkflowAPI()
                .getAllProcessDefinitions();
        restClient.assertStatusCodeIs(HttpStatus.OK);
        processDefinitionsTenant2.assertThat().entriesListDoesNotContain("id", randomProcessDefinitionTenant1.getId());
    }

    @TestRail(section = { TestGroup.REST_API,  TestGroup.WORKFLOW, TestGroup.PROCESS_DEFINITION },
            executionType = ExecutionType.REGRESSION,
            description = "Verify network admin is able to get a process definition image using REST API and status code is OK (200)")
    @Test(groups = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.PROCESS_DEFINITION, TestGroup.REGRESSION, TestGroup.NETWORKS})
    @Bug(id = "MNT-17243")
    public void networkAdminGetProcessDefinitionImage() throws Exception
    {
        randomProcessDefinition = restClient.authenticateUser(adminTenantUser).withWorkflowAPI().getAllProcessDefinitions().getOneRandomEntry().onModel();
        restClient.withWorkflowAPI().usingProcessDefinitions(randomProcessDefinition).getProcessDefinitionImage()
                .assertResponseContainsImage();
        restClient.assertStatusCodeIs(HttpStatus.OK);
    }

    @TestRail(section = { TestGroup.REST_API,  TestGroup.WORKFLOW,TestGroup.PROCESS_DEFINITION },
            executionType = ExecutionType.REGRESSION,
            description = "Verify network user is able to get a process definition image using REST API and status code is OK (200)")
    @Test(groups = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.PROCESS_DEFINITION, TestGroup.REGRESSION, TestGroup.NETWORKS})
    @Bug(id = "MNT-17243")
    public void networkUserGetProcessDefinitionImage() throws Exception
    {
        randomProcessDefinition = restClient.authenticateUser(adminTenantUser).withWorkflowAPI()
                .getAllProcessDefinitions().getOneRandomEntry().onModel();
        restClient.authenticateUser(tenantUser).withWorkflowAPI()
                .usingProcessDefinitions(randomProcessDefinition).getProcessDefinitionImage()
                .assertResponseContainsImage();
        restClient.assertStatusCodeIs(HttpStatus.OK);
    }

    @TestRail(section = { TestGroup.REST_API,  TestGroup.WORKFLOW, TestGroup.PROCESS_DEFINITION },
            executionType = ExecutionType.SANITY, description = "Verify Tenant Admin user gets process definitions for network deployments using REST API and status code is OK (200)")
    @Test(groups = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.PROCESS_DEFINITION, TestGroup.SANITY, TestGroup.NETWORKS})
    public void networkAdminGetsProcessDefinitions() throws Exception
    {
        restClient.authenticateUser(adminTenantUser);
        restClient.withWorkflowAPI().getAllProcessDefinitions().assertThat().entriesListIsNotEmpty();
        restClient.assertStatusCodeIs(HttpStatus.OK);
    }

    @TestRail(section = { TestGroup.REST_API, TestGroup.WORKFLOW,  TestGroup.PROCESS_DEFINITION },
            executionType = ExecutionType.REGRESSION,
            description = "Verify Tenant User doesn't get process definition image for another network deployment using REST API")
    @Test(groups = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.PROCESS_DEFINITION, TestGroup.REGRESSION, TestGroup.NETWORKS })
    public void networkUserIsNotAbleToGetProcessDefinitionImageForAnotherNetwork() throws Exception
    {
        RestProcessDefinitionModel randomProcessDefinition = restClient.authenticateUser(adminTenantUser).withWorkflowAPI()
                .getAllProcessDefinitions().getOneRandomEntry().onModel();
        restClient.authenticateUser(secondAdminTenantUser).withWorkflowAPI()
                .usingProcessDefinitions(randomProcessDefinition).getProcessDefinitionImage();
        restClient.assertStatusCodeIs(HttpStatus.NOT_FOUND)
                .assertLastError().containsSummary(String.format(RestErrorModel.ENTITY_NOT_FOUND, randomProcessDefinition.getId()));
    }

    @TestRail(section = { TestGroup.REST_API,  TestGroup.WORKFLOW, TestGroup.PROCESS_DEFINITION },
            executionType = ExecutionType.REGRESSION,
            description = "Verify Tenant User gets a model of the start form type definition for network deployments using REST API and status code is OK (200)")
    @Test(groups = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.PROCESS_DEFINITION, TestGroup.REGRESSION, TestGroup.NETWORKS })
    public void networkUserGetsStartFormModel() throws Exception
    {
        randomProcessDefinition = restClient.authenticateUser(adminTenantUser).withWorkflowAPI()
                .getAllProcessDefinitions().getOneRandomEntry().onModel();
        restClient.authenticateUser(tenantUser).withWorkflowAPI()
                .usingProcessDefinitions(randomProcessDefinition).getProcessDefinitionStartFormModel()
                .assertThat().entriesListIsNotEmpty();
        restClient.assertStatusCodeIs(HttpStatus.OK);
    }

    @TestRail(section = { TestGroup.REST_API,  TestGroup.WORKFLOW, TestGroup.PROCESS_DEFINITION },
            executionType = ExecutionType.REGRESSION,
            description = "Verify Tenant User doesn't get a model of the start form type definition for another network deployment using REST API")
    @Test(groups = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.PROCESS_DEFINITION, TestGroup.REGRESSION, TestGroup.NETWORKS })
    public void networkUserIsNotAbleToGetStartFormModelForAnotherNetwork() throws Exception
    {
        RestProcessDefinitionModel randomProcessDefinition = restClient.authenticateUser(adminTenantUser).withWorkflowAPI()
                .getAllProcessDefinitions().getOneRandomEntry().onModel();
        restClient.authenticateUser(secondAdminTenantUser).withWorkflowAPI()
                .usingProcessDefinitions(randomProcessDefinition).getProcessDefinitionStartFormModel();
        restClient.assertStatusCodeIs(HttpStatus.NOT_FOUND)
                .assertLastError().containsSummary(String.format(RestErrorModel.ENTITY_NOT_FOUND, randomProcessDefinition.getId()));
    }

    @TestRail(section = { TestGroup.REST_API,  TestGroup.WORKFLOW, TestGroup.PROCESS_DEFINITION },
            executionType = ExecutionType.SANITY,
            description = "Verify Tenant Admin gets a model of the start form type definition for network deployments using REST API and status code is OK (200)")
    @Test(groups = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.PROCESS_DEFINITION, TestGroup.SANITY, TestGroup.NETWORKS })
    public void networkAdminGetsStartFormModel() throws Exception
    {
        restClient.authenticateUser(adminTenantUser);
        randomProcessDefinition = restClient.withWorkflowAPI().getAllProcessDefinitions().getOneRandomEntry().onModel();
        restClient.withWorkflowAPI().usingProcessDefinitions(randomProcessDefinition).getProcessDefinitionStartFormModel().assertThat().entriesListIsNotEmpty();
        restClient.assertStatusCodeIs(HttpStatus.OK);
    }
    
    @TestRail(section = { TestGroup.REST_API, TestGroup.WORKFLOW,TestGroup.PROCESSES }, executionType = ExecutionType.REGRESSION, 
            description = "Verify network user can start new process with processDefinitionKey from same network using REST API and status code is 201")
    @Test(groups = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.PROCESSES, TestGroup.REGRESSION, TestGroup.NETWORKS})
    public void networkUserCanStartNewProcessWithProcessDefinitionFromSameNetwork() throws JsonToModelConversionException, Exception
    { 
        RestProcessDefinitionModel firstProcessDefTenant1 = restClient.authenticateUser(adminTenantUser).withWorkflowAPI().getAllProcessDefinitions().getEntries().get(0).onModel();
 
        String processDefinitionKey = firstProcessDefTenant1.getKey();
        addedProcess = restClient.authenticateUser(tenantUser).withWorkflowAPI().addProcess(processDefinitionKey, secondTenantUser, false, Priority.Normal);
        restClient.assertStatusCodeIs(HttpStatus.CREATED);
        addedProcess.assertThat().field("processDefinitionId").is(firstProcessDefTenant1.getId())
            .and().field("startUserId").is(tenantUser.getEmailAddress().substring(0,2)+tenantUser.getEmailAddress().substring(2).toLowerCase())
            .and().field("processDefinitionKey").is(processDefinitionKey);
    }

    
    @TestRail(section = { TestGroup.REST_API, TestGroup.WORKFLOW,TestGroup.PROCESSES }, executionType = ExecutionType.REGRESSION, 
            description = "Verify network user cannot start new process with processDefinitionKey from another network using REST API and status code is 400")
    @Test(groups = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.PROCESSES, TestGroup.REGRESSION, TestGroup.NETWORKS})
    public void networkUserCannotStartNewProcessWithProcessDefinitionFromAnotherNetwork() throws JsonToModelConversionException, Exception
    { 
        RestProcessDefinitionModel firstProcessDefTenant1 = restClient.authenticateUser(adminTenantUser).withWorkflowAPI().getAllProcessDefinitions().getEntries().get(0).onModel();

        String processDefinitionKey = firstProcessDefTenant1.getId().substring(0,firstProcessDefTenant1.getId().indexOf(":"));
        addedProcess = restClient.authenticateUser(secondAdminTenantUser).withWorkflowAPI().addProcess(processDefinitionKey, differentNetworkTenantUser, false, Priority.Normal);
        restClient.assertStatusCodeIs(HttpStatus.BAD_REQUEST).assertLastError()
            .containsErrorKey(String.format(RestErrorModel.NO_WORKFLOW_DEFINITION_FOUND, processDefinitionKey))
            .containsSummary(String.format(RestErrorModel.NO_WORKFLOW_DEFINITION_FOUND, processDefinitionKey))
            .stackTraceIs(RestErrorModel.STACKTRACE)
            .descriptionURLIs(RestErrorModel.RESTAPIEXPLORER);
    }
}
