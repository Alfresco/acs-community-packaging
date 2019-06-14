package org.alfresco.rest.networks;

import java.util.ArrayList;

import org.alfresco.rest.NetworkDataPrep;
import org.alfresco.rest.model.RestErrorModel;
import org.alfresco.rest.model.RestNetworkQuotaModel;
import org.alfresco.utility.model.TestGroup;
import org.alfresco.utility.model.UserModel;
import org.alfresco.utility.report.Bug;
import org.alfresco.utility.testrail.ExecutionType;
import org.alfresco.utility.testrail.annotation.TestRail;
import org.springframework.http.HttpStatus;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * Tests for /people/{personId}/networks
 */
public class RestGetNetworkForPersonTests extends NetworkDataPrep
{

    @BeforeClass(alwaysRun = true)
    public void dataPreparation() throws Exception
    {
        init();
    }

    @Bug(id = "MNT-16904")
    @TestRail(section = { TestGroup.REST_API,
            TestGroup.NETWORKS }, executionType = ExecutionType.REGRESSION, description = "Verify non existing user gets another existing network with Rest API and checks the forbidden status")
    @Test(groups = { TestGroup.REST_API, TestGroup.NETWORKS, TestGroup.REGRESSION })
    public void nonExistingTenantUserIsNotAuthorizedToRequest() throws Exception
    {
        UserModel tenantUser = new UserModel("nonexisting", "password");
        tenantUser.setDomain(adminTenantUser.getDomain());
        restClient.authenticateUser(tenantUser).withCoreAPI().usingAuthUser().getNetwork(adminTenantUser);
        restClient.assertStatusCodeIs(HttpStatus.UNAUTHORIZED);
    }

    @TestRail(section = { TestGroup.REST_API,
            TestGroup.NETWORKS }, executionType = ExecutionType.SANITY, description = "Verify tenant admin user gets specific network with Rest API and response is not empty")
    @Test(groups = { TestGroup.REST_API, TestGroup.NETWORKS, TestGroup.SANITY })
    public void adminTenantChecksIfNetworkIsPresent() throws Exception
    {
        restClient.authenticateUser(adminTenantUser).withCoreAPI().usingUser(adminTenantUser).getNetwork();
        restClient.assertStatusCodeIs(HttpStatus.OK);
    }

    @TestRail(section = { TestGroup.REST_API,
            TestGroup.NETWORKS }, executionType = ExecutionType.SANITY, description = "Verify tenant user gets specific network with Rest API and response is not empty")
    @Test(groups = { TestGroup.REST_API, TestGroup.NETWORKS, TestGroup.SANITY })
    public void tenantUserChecksIfNetworkIsPresent() throws Exception
    {
        restClient.authenticateUser(tenantUser).withCoreAPI().usingUser(tenantUser).getNetwork();
        restClient.assertStatusCodeIs(HttpStatus.OK);
    }

    @Bug(id = "needs to be checked")
    @TestRail(section = { TestGroup.REST_API,
            TestGroup.NETWORKS }, executionType = ExecutionType.REGRESSION, description = "Verify tenant user check network of admin user with Rest API")
    @Test(groups = { TestGroup.REST_API, TestGroup.NETWORKS, TestGroup.REGRESSION })
    public void tenantUserIsNotAuthorizedToCheckNetworkOfAdminUser() throws Exception
    {
        restClient.authenticateUser(tenantUser).withCoreAPI().usingAuthUser().getNetwork(adminTenantUser);
        restClient.assertStatusCodeIs(HttpStatus.OK);
    }

    @Bug(id = "needs to be checked")
    @TestRail(section = { TestGroup.REST_API,
            TestGroup.NETWORKS }, executionType = ExecutionType.REGRESSION, description = "Verify getNetwork request status code is 200 if a user tries to get network information of another user")
    @Test(groups = { TestGroup.REST_API, TestGroup.NETWORKS, TestGroup.REGRESSION })
    public void verifyGetNetworkByAUserForAnotherUser() throws Exception
    {
        UserModel randomTestUser = dataUser.usingUser(adminTenantUser).createUserWithTenant("randomTestUser");
        restNetworkModel = restClient.authenticateUser(tenantUser).withCoreAPI().usingAuthUser().getNetwork(randomTestUser);
        restClient.assertStatusCodeIs(HttpStatus.OK);
        restNetworkModel.assertThat().field("id").is(tenantUser.getDomain());
    }

    @TestRail(section = { TestGroup.REST_API,
            TestGroup.NETWORKS }, executionType = ExecutionType.REGRESSION, description = "Verify that getNetwork status code is 404 for a personId that does not exist")
    @Test(groups = { TestGroup.REST_API, TestGroup.NETWORKS, TestGroup.REGRESSION })
    public void verifyThatGetNetworkStatusIs404ForAPersonIdThatDoesNotExist() throws Exception
    {
        UserModel invalidUser = dataUser.usingUser(adminTenantUser).createUserWithTenant("validUsername");
        invalidUser.setUsername("invalidUsername");

        restClient.authenticateUser(adminTenantUser).withCoreAPI().usingUser(invalidUser).getNetwork();
        restClient.assertStatusCodeIs(HttpStatus.NOT_FOUND).assertLastError().containsErrorKey(RestErrorModel.ENTITY_NOT_FOUND_ERRORKEY)
                .containsSummary(String.format(RestErrorModel.ENTITY_NOT_FOUND, invalidUser.getUsername())).descriptionURLIs(RestErrorModel.RESTAPIEXPLORER)
                .stackTraceIs(RestErrorModel.STACKTRACE);
    }

    @TestRail(section = { TestGroup.REST_API,
            TestGroup.NETWORKS }, executionType = ExecutionType.REGRESSION, description = "Verify that getNetwork status code is 404 for a networkId that does not exist")
    @Test(groups = { TestGroup.REST_API, TestGroup.NETWORKS, TestGroup.REGRESSION })
    public void verifyThatGetNetworkStatusIs404ForANetworkIdThatDoesNotExist() throws Exception
    {
        UserModel invalidUser = dataUser.usingUser(adminTenantUser).createUserWithTenant("invalidNetworkId");
        invalidUser.setDomain("invalidNetworkId");

        restClient.authenticateUser(adminTenantUser).withCoreAPI().usingUser(invalidUser).getNetwork();
        restClient.assertStatusCodeIs(HttpStatus.NOT_FOUND).assertLastError().containsErrorKey(RestErrorModel.ENTITY_NOT_FOUND_ERRORKEY)
                .containsSummary(String.format(RestErrorModel.ENTITY_NOT_FOUND, invalidUser.getUsername())).descriptionURLIs(RestErrorModel.RESTAPIEXPLORER)
                .stackTraceIs(RestErrorModel.STACKTRACE);
    }

    @TestRail(section = { TestGroup.REST_API,
            TestGroup.NETWORKS }, executionType = ExecutionType.REGRESSION, description = "Verify getNetwork request that is made using -me- instead of personId")
    @Test(groups = { TestGroup.REST_API, TestGroup.NETWORKS, TestGroup.REGRESSION })
    public void verifyGetNetworkRequestUsingMeInsteadOfPersonId() throws Exception
    {
        restNetworkModel = restClient.authenticateUser(adminTenantUser).withCoreAPI().usingMe().getNetwork();
        restClient.assertStatusCodeIs(HttpStatus.OK);
        restNetworkModel.assertThat().field("quotas").is(new ArrayList<RestNetworkQuotaModel>()).assertThat().field("isEnabled").is("true").assertThat()
                .field("homeNetwork").is("true").assertThat().field("id").is(adminTenantUser.getDomain().toLowerCase());
    }

    @TestRail(section = { TestGroup.REST_API,
            TestGroup.NETWORKS }, executionType = ExecutionType.REGRESSION, description = "Verify that properties parameter is applied to getNetwork request")
    @Test(groups = { TestGroup.REST_API, TestGroup.NETWORKS, TestGroup.REGRESSION })
    public void verifyPropertiesParameterIsAppliedToGetNetworkRequest() throws Exception
    {
        restNetworkModel = restClient.authenticateUser(adminTenantUser).withParams("properties=id,isEnabled,homeNetwork,paidNetwork").withCoreAPI().usingMe()
                .getNetwork();
        restClient.assertStatusCodeIs(HttpStatus.OK);
        restNetworkModel.assertThat().field("id").is(adminTenantUser.getDomain().toLowerCase()).assertThat().field("isEnabled").is("true").assertThat()
                .field("homeNetwork").is("true").assertThat().field("paidNetwork").is("false").assertThat().field("quotas").isNull();
        restNetworkModel.assertThat().fieldsCount().is(4);
    }

    @TestRail(section = { TestGroup.REST_API,
            TestGroup.NETWORKS }, executionType = ExecutionType.REGRESSION, description = "Verify getNetwork request status code is 404 for a network to which user does not belong")
    @Test(groups = { TestGroup.REST_API, TestGroup.NETWORKS, TestGroup.REGRESSION })
    public void verifyGetNetworkRequestStatusCodeIs404ForANetworkToWhichTheUserDoesNotBelong() throws Exception
    {
        restClient.authenticateUser(adminTenantUser).withCoreAPI().usingAuthUser().getNetwork(secondAdminTenantUser);
        restClient.assertStatusCodeIs(HttpStatus.NOT_FOUND);
    }

    @TestRail(section = { TestGroup.REST_API,
            TestGroup.NETWORKS }, executionType = ExecutionType.REGRESSION, description = "Verify admin tenant user is not authorized to check network of another user with Rest API and checks the forbidden status")
    @Test(groups = { TestGroup.REST_API, TestGroup.NETWORKS, TestGroup.REGRESSION })
    public void adminTenantUserIsNotAuthorizedToCheckNetworkOfAnotherUser() throws Exception
    {
        UserModel secondTenantUser = dataUser.usingUser(adminTenantUser).createUserWithTenant("anotherTenant");
        restClient.authenticateUser(secondAdminTenantUser).withCoreAPI().usingAuthUser().getNetwork(secondTenantUser);
        restClient.assertStatusCodeIs(HttpStatus.NOT_FOUND);
    }
}
