package org.alfresco.rest.networks;

import org.alfresco.rest.NetworkDataPrep;
import org.alfresco.rest.model.RestErrorModel;
import org.alfresco.rest.model.RestNetworkModelsCollection;
import org.alfresco.utility.model.TestGroup;
import org.alfresco.utility.model.UserModel;
import org.alfresco.utility.report.Bug;
import org.alfresco.utility.testrail.ExecutionType;
import org.alfresco.utility.testrail.annotation.TestRail;
import org.springframework.http.HttpStatus;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

public class RestGetNetworksForPersonTests extends NetworkDataPrep
{
    @BeforeClass(alwaysRun = true)
    public void dataPreparation() throws Exception
    {
        init();
    }

    @Bug(id = "MNT-16904")
    @TestRail(section = { TestGroup.REST_API,TestGroup.NETWORKS }, executionType = ExecutionType.SANITY,
            description = "Verify non existing user gets another exisiting network with Rest API and checks the unauthorized status")
    @Test(groups = { TestGroup.REST_API, TestGroup.SANITY, TestGroup.NETWORKS })
    public void nonExistingTenantUserIsNotAuthorizedToRequest() throws Exception
    {
        UserModel tenantUser = new UserModel("nonexisting", "password");
        tenantUser.setDomain(adminTenantUser.getDomain());
        restClient.authenticateUser(tenantUser);
        restClient.withCoreAPI().usingAuthUser().getNetworks(adminTenantUser);
        restClient.assertStatusCodeIs(HttpStatus.UNAUTHORIZED).assertLastError()
                .containsSummary(RestErrorModel.AUTHENTICATION_FAILED);
    }

    @TestRail(section = { TestGroup.REST_API,TestGroup.NETWORKS }, executionType = ExecutionType.SANITY,
            description = "Verify tenant admin user gets specific network with Rest API and response is not empty")
    @Test(groups = {TestGroup.REST_API, TestGroup.SANITY, TestGroup.NETWORKS })
    public void adminTenantChecksIfNetworkIsPresent() throws Exception
    {
        RestNetworkModelsCollection networks = restClient.authenticateUser(adminTenantUser).withCoreAPI().usingAuthUser().getNetworks();
        restClient.assertStatusCodeIs(HttpStatus.OK);
        networks.getOneRandomEntry().onModel().assertNetworkIsEnabled()
                .and().field("id").is(adminTenantUser.getDomain().toLowerCase())
                .and().field("quotas").is("[]")
                .and().field("homeNetwork").is("false");
    }

    @TestRail(section = { TestGroup.REST_API,TestGroup.NETWORKS }, executionType = ExecutionType.SANITY,
            description = "Verify tenant user gets specific network with Rest API and response is not empty")
    @Test(groups = {TestGroup.REST_API, TestGroup.SANITY, TestGroup.NETWORKS })
    public void tenantUserChecksIfNetworkIsPresent() throws Exception
    {
        restClient.authenticateUser(tenantUser).withCoreAPI().usingAuthUser().getNetworks();
        restClient.assertStatusCodeIs(HttpStatus.OK);
    }

    @TestRail(section = { TestGroup.REST_API,TestGroup.NETWORKS }, executionType = ExecutionType.REGRESSION,
            description = "Verify tenant user is not authorized to check network of admin user with Rest API and checks the forbidden status")
    @Test(groups = {TestGroup.REST_API, TestGroup.REGRESSION, TestGroup.NETWORKS })
    public void tenantUserIsNotAuthorizedToCheckNetworkOfAdminUser() throws Exception
    { 
        restClient.authenticateUser(tenantUser);
        restClient.withCoreAPI().usingAuthUser().getNetworks(adminTenantUser);
        restClient.assertStatusCodeIs(HttpStatus.NOT_FOUND);
    }

    @TestRail(section = { TestGroup.REST_API, TestGroup.NETWORKS }, executionType = ExecutionType.REGRESSION,
            description = "Verify admin tenant user is not authorized to check network of another user with Rest API and checks the forbidden status")
    @Test(groups = {TestGroup.REST_API, TestGroup.REGRESSION, TestGroup.NETWORKS })
    public void adminTenantUserIsNotAuthorizedToCheckNetworkOfAnotherUser() throws Exception
    {
        restClient.authenticateUser(secondAdminTenantUser);
        restClient.withCoreAPI().usingAuthUser().getNetworks(secondTenantUser);
        restClient.assertStatusCodeIs(HttpStatus.NOT_FOUND);
    }

    @TestRail(section = { TestGroup.REST_API,TestGroup.NETWORKS }, executionType = ExecutionType.REGRESSION,
            description = "Get tenant user networks using invalid value for skipCount")
    @Test(groups = {TestGroup.REST_API, TestGroup.REGRESSION, TestGroup.NETWORKS })
    public void invalidValueForSkipCountTest() throws Exception
    {
        restClient.authenticateUser(adminTenantUser).withParams("skipCount=abc").withCoreAPI().usingAuthUser().getNetworks();
        restClient.assertStatusCodeIs(HttpStatus.BAD_REQUEST).assertLastError().containsSummary(String.format(RestErrorModel.INVALID_SKIPCOUNT, "abc"));
    }

    @TestRail(section = { TestGroup.REST_API,TestGroup.NETWORKS }, executionType = ExecutionType.REGRESSION,
            description = "Get tenant user networks using invalid value for maxItems")
    @Test(groups = {TestGroup.REST_API, TestGroup.REGRESSION, TestGroup.NETWORKS })
    public void invalidValueForMaxItemsTest() throws Exception
    {
        restClient.authenticateUser(adminTenantUser).withParams("maxItems=abc").withCoreAPI().usingAuthUser().getNetworks();
        restClient.assertStatusCodeIs(HttpStatus.BAD_REQUEST).assertLastError().containsSummary(String.format(RestErrorModel.INVALID_MAXITEMS, "abc"));
    }

    @TestRail(section = { TestGroup.REST_API,TestGroup.NETWORKS }, executionType = ExecutionType.REGRESSION,
            description = "Get tenant user networks using personId that does not exist")
    @Test(groups = {TestGroup.REST_API, TestGroup.REGRESSION, TestGroup.NETWORKS })
    public void inexistentTenantTest() throws Exception
    {
        restClient.authenticateUser(adminTenantUser).withCoreAPI().usingUser(new UserModel("invalidTenantUser", "password")).getNetworks();
        restClient.assertStatusCodeIs(HttpStatus.NOT_FOUND).assertLastError().containsErrorKey(RestErrorModel.ENTITY_NOT_FOUND_ERRORKEY)
                .containsSummary(String.format(RestErrorModel.ENTITY_NOT_FOUND, "invalidTenantUser"))
                .descriptionURLIs(RestErrorModel.RESTAPIEXPLORER)
                .stackTraceIs(RestErrorModel.STACKTRACE);
    }

    @TestRail(section = { TestGroup.REST_API,TestGroup.NETWORKS }, executionType = ExecutionType.REGRESSION,
            description = "Get tenant user networks using -me- instead of personId")
    @Test(groups = {TestGroup.REST_API, TestGroup.REGRESSION, TestGroup.NETWORKS })
    public void specifyMeInsteadOfPersonIdTest() throws Exception
    {
        RestNetworkModelsCollection networks = restClient.authenticateUser(adminTenantUser).withCoreAPI().usingMe().getNetworks();
        restClient.assertStatusCodeIs(HttpStatus.OK);
        networks.getOneRandomEntry().onModel().assertNetworkIsEnabled().and().field("id").is(adminTenantUser.getDomain().toLowerCase());
    }

    @TestRail(section = { TestGroup.REST_API,TestGroup.NETWORKS }, executionType = ExecutionType.REGRESSION,
            description = "Check default error schema for get networks for member")
    @Test(groups = { TestGroup.REST_API, TestGroup.REGRESSION, TestGroup.NETWORKS })
    public void getNetworksAndCheckPropertiesParameter() throws Exception
    {
        RestNetworkModelsCollection networks = restClient.authenticateUser(adminTenantUser).withParams("properties=isEnabled,id").withCoreAPI().usingAuthUser().getNetworks();
        restClient.assertStatusCodeIs(HttpStatus.OK);
        networks.getOneRandomEntry().onModel().assertThat().field("homeNetwork").is("false")
                .and().field("quotas").isNull()
                .and().field("isEnabled").is("true")
                .and().field("id").is(adminTenantUser.getDomain().toLowerCase());
    }

    @TestRail(section = { TestGroup.REST_API,TestGroup.NETWORKS }, executionType = ExecutionType.REGRESSION,
            description = "Check that skipCount parameter is applied")
    @Test(groups = { TestGroup.REST_API, TestGroup.REGRESSION, TestGroup.NETWORKS })
    public void checkThatSkipCountParameterIsApplied() throws Exception
    {
        RestNetworkModelsCollection networks = restClient.authenticateUser(adminTenantUser).withParams("skipCount=1").withCoreAPI().usingAuthUser().getNetworks();
        restClient.assertStatusCodeIs(HttpStatus.OK);
        networks.assertThat().entriesListIsEmpty();
    }

    @TestRail(section = { TestGroup.REST_API,TestGroup.NETWORKS }, executionType = ExecutionType.REGRESSION,
            description = "Check that high skipCount parameter is applied")
    @Test(groups = { TestGroup.REST_API, TestGroup.REGRESSION, TestGroup.NETWORKS })
    public void checkThatHighSkipCountParameterIsApplied() throws Exception
    {
        RestNetworkModelsCollection networks = restClient.authenticateUser(adminTenantUser).withParams("skipCount=100").withCoreAPI().usingAuthUser().getNetworks();
        restClient.assertStatusCodeIs(HttpStatus.OK);
        networks.assertThat().entriesListIsEmpty();
        networks.assertThat().paginationField("skipCount").is("100");
    }

    @TestRail(section = { TestGroup.REST_API,TestGroup.NETWORKS }, executionType = ExecutionType.REGRESSION,
            description = "Check that maxItems parameter is applied")
    @Test(groups = { TestGroup.REST_API, TestGroup.REGRESSION, TestGroup.NETWORKS })
    public void checkThatMaxItemsParameterIsApplied() throws Exception
    {
        RestNetworkModelsCollection networks = restClient.authenticateUser(adminTenantUser).withParams("maxItems=1").withCoreAPI().usingAuthUser().getNetworks();
        restClient.assertStatusCodeIs(HttpStatus.OK);
        networks.getOneRandomEntry().onModel().assertThat().field("homeNetwork").is("false")
                .and().field("quotas").is("[]")
                .and().field("isEnabled").is("true")
                .and().field("id").is(adminTenantUser.getDomain().toLowerCase());
        networks.assertThat().paginationField("maxItems").is("1");
    }
}