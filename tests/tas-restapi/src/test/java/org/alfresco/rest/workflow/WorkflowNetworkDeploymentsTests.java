package org.alfresco.rest.workflow;

import java.util.List;

import org.alfresco.rest.NetworkDataPrep;
import org.alfresco.rest.exception.JsonToModelConversionException;
import org.alfresco.rest.model.RestDeploymentModel;
import org.alfresco.rest.model.RestDeploymentModelsCollection;
import org.alfresco.rest.model.RestErrorModel;
import org.alfresco.utility.model.TestGroup;
import org.alfresco.utility.report.Bug;
import org.alfresco.utility.testrail.ExecutionType;
import org.alfresco.utility.testrail.annotation.TestRail;
import org.springframework.http.HttpStatus;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

public class WorkflowNetworkDeploymentsTests extends NetworkDataPrep
{
    private RestDeploymentModel expectedDeployment, actualDeployment;
    private RestDeploymentModelsCollection deployments;

    @BeforeClass(alwaysRun = true)
    public void dataPreparation() throws Exception
    {
        init();
    }

    @TestRail(section = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.DEPLOYMENTS },
            executionType = ExecutionType.REGRESSION, 
            description = "Verify if network admin user gets a network deployment using REST API and status code is OK (200)")
    @Test(groups = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.DEPLOYMENTS, TestGroup.REGRESSION, TestGroup.NETWORKS})
    public void adminGetsNetworkDeploymentWithSuccess() throws Exception
    {

        expectedDeployment = restClient.authenticateUser(adminTenantUser).withWorkflowAPI().getDeployments().getOneRandomEntry().onModel();
        actualDeployment = restClient.withWorkflowAPI().usingDeployment(expectedDeployment).getDeployment();
        restClient.assertStatusCodeIs(HttpStatus.OK);
        actualDeployment.assertThat().field("deployedAt").isNotEmpty()
                .and().field("name").is(expectedDeployment.getName())
                .and().field("id").is(expectedDeployment.getId());
    }

    @TestRail(section = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.DEPLOYMENTS },
            executionType = ExecutionType.REGRESSION, 
            description = "Verify non admin user is forbidden to get a network deployment using REST API (403)")
    @Test(groups = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.DEPLOYMENTS, TestGroup.REGRESSION, TestGroup.NETWORKS })
    public void nonAdminUserIsForbiddenToGetNetworkDeployment() throws Exception
    {
        expectedDeployment = restClient.authenticateUser(adminTenantUser).withWorkflowAPI().getDeployments().getOneRandomEntry().onModel();

        restClient.authenticateUser(tenantUser).withWorkflowAPI().usingDeployment(expectedDeployment).getDeployment();
        restClient.assertStatusCodeIs(HttpStatus.FORBIDDEN)
                .assertLastError().containsSummary(RestErrorModel.PERMISSION_WAS_DENIED);
    }

    @Bug(id = "MNT-16996")
    @TestRail(section = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.DEPLOYMENTS },
            executionType = ExecutionType.REGRESSION, 
            description = "Verify get deployments returns an empty list after deleting all network deployments.")
    @Test(groups = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.DEPLOYMENTS, TestGroup.REGRESSION, TestGroup.NETWORKS})
    public void getNetworkDeploymentsAfterDeletingAllNetworkDeployments() throws Exception
    {

        List<RestDeploymentModel> networkDeployments = restClient.authenticateUser(adminTenantUser).withWorkflowAPI().getDeployments().getEntries();
        for (RestDeploymentModel networkDeployment: networkDeployments)
        {
            restClient.withWorkflowAPI().usingDeployment(networkDeployment.onModel()).deleteDeployment();
            restClient.assertStatusCodeIs(HttpStatus.NO_CONTENT);
        }
        deployments = restClient.authenticateUser(adminTenantUser).withWorkflowAPI().getDeployments();
        restClient.assertStatusCodeIs(HttpStatus.OK);
        deployments.assertThat().entriesListIsEmpty();
    }

    @TestRail(section = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.DEPLOYMENTS },
            executionType = ExecutionType.REGRESSION, 
            description = "Verify non admin user is not able to get network deployments using REST API and status code is Forbidden")
    @Test(groups = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.DEPLOYMENTS, TestGroup.REGRESSION, TestGroup.NETWORKS})
    public void nonAdminUserCanNotGetNetworkDeployments() throws Exception
    {
        restClient.authenticateUser(tenantUser).withWorkflowAPI().getDeployments();
        restClient.assertStatusCodeIs(HttpStatus.FORBIDDEN)
                  .assertLastError().containsSummary(RestErrorModel.PERMISSION_WAS_DENIED);
    }

    @TestRail(section = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.DEPLOYMENTS },
            executionType = ExecutionType.REGRESSION,
            description = "Verify that network admin user is not able to get a deployment from other network using REST API and status code is OK (200)")
    @Test(groups = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.DEPLOYMENTS, TestGroup.REGRESSION, TestGroup.NETWORKS})
    public void adminDoesNotGetDeploymentFromOtherNetwork() throws Exception
    {
        expectedDeployment = restClient.authenticateUser(adminTenantUser).withWorkflowAPI().getDeployments().getOneRandomEntry().onModel();
        actualDeployment = restClient.authenticateUser(secondAdminTenantUser).withWorkflowAPI().usingDeployment(expectedDeployment).getDeployment();
        restClient.assertStatusCodeIs(HttpStatus.NOT_FOUND)
                .assertLastError()
                    .containsSummary(String.format(RestErrorModel.ENTITY_NOT_FOUND, expectedDeployment.getId()))
                    .containsErrorKey(RestErrorModel.ENTITY_NOT_FOUND_ERRORKEY)
                .descriptionURLIs(RestErrorModel.RESTAPIEXPLORER)
                .stackTraceIs(RestErrorModel.STACKTRACE);
    }

    @TestRail(section = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.DEPLOYMENTS },
            executionType = ExecutionType.REGRESSION,
            description = "Verify that network admin user is not able to get a deployment from other network using REST API and status code is OK (200)")
    @Test(groups = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.DEPLOYMENTS, TestGroup.REGRESSION, TestGroup.NETWORKS})
    public void adminDoesNotGetDeploymentsFromOtherNetwork() throws Exception
    {
        RestDeploymentModel deployment = restClient.authenticateUser(adminTenantUser).withWorkflowAPI().getDeployments().getOneRandomEntry().onModel();
        deployments = restClient.authenticateUser(secondAdminTenantUser).withWorkflowAPI().getDeployments();
        restClient.assertStatusCodeIs(HttpStatus.OK);
        deployments.assertThat().entriesListDoesNotContain("id", deployment.getId());
    }

    @TestRail(section = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.DEPLOYMENTS }, executionType = ExecutionType.SANITY, 
        description = "Verify Tenant Admin user gets network deployments using REST API and status code is OK (200)")
    @Test(groups = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.DEPLOYMENTS, TestGroup.SANITY, TestGroup.NETWORKS})
    public void getNetworkDeploymentsWithAdmin() throws JsonToModelConversionException, Exception
    {
        deployments = restClient.authenticateUser(adminTenantUser).withWorkflowAPI().getDeployments();
        restClient.assertStatusCodeIs(HttpStatus.OK);
        deployments.assertThat().entriesListIsNotEmpty();
    }
}
