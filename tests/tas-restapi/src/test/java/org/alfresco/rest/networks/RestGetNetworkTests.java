package org.alfresco.rest.networks;

import org.alfresco.rest.NetworkDataPrep;
import org.alfresco.rest.model.RestErrorModel;
import org.alfresco.rest.model.RestNetworkModel;
import org.alfresco.utility.model.TestGroup;
import org.alfresco.utility.model.UserModel;
import org.alfresco.utility.report.Bug;
import org.alfresco.utility.testrail.ExecutionType;
import org.alfresco.utility.testrail.annotation.TestRail;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.http.HttpStatus;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * Created by Cristina Axinte on 9/26/2016.
 */
public class RestGetNetworkTests extends NetworkDataPrep
{
    @BeforeClass(alwaysRun = true)
    public void dataPreparation() throws Exception
    {
        init();
    }

    @Bug(id = "MNT-16904")
    @TestRail(section = { TestGroup.REST_API, TestGroup.NETWORKS }, executionType = ExecutionType.SANITY,
            description = "Verify non existing user gets another exisiting network with Rest API and checks the forbidden status")
    @Test(groups = { TestGroup.REST_API, TestGroup.NETWORKS, TestGroup.SANITY})
    public void nonExistingTenantUserIsNotAuthorizedToRequest() throws Exception
    {
        UserModel tenantUser = new UserModel("nonexisting", "password");
        tenantUser.setDomain(adminTenantUser.getDomain());
        restClient.authenticateUser(tenantUser);
        restClient.withCoreAPI().usingNetworks().getNetwork(adminTenantUser);
        restClient.assertStatusCodeIs(HttpStatus.UNAUTHORIZED);
    }

    @TestRail(section = { TestGroup.REST_API, TestGroup.NETWORKS }, executionType = ExecutionType.SANITY,
            description = "Verify tenant admin user gets specific network with Rest API and response is not empty")
    @Test(groups = { TestGroup.REST_API, TestGroup.NETWORKS, TestGroup.SANITY })
    public void adminTenantChecksIfNetworkIsPresent() throws Exception
    {
        restClient.authenticateUser(adminTenantUser);
        restClient.withCoreAPI().usingNetworks().getNetwork().assertNetworkHasName(adminTenantUser).assertNetworkIsEnabled();
        restClient.assertStatusCodeIs(HttpStatus.OK);
    }

    @TestRail(section = { TestGroup.REST_API, TestGroup.NETWORKS }, executionType = ExecutionType.REGRESSION,
            description = "Verify tenant admin user gets another existing network with Rest API and checks the forbidden status")
    @Test(groups = { TestGroup.REST_API, TestGroup.NETWORKS, TestGroup.REGRESSION })
    public void adminTenantChecksIfAnotherExistingNetworkIsForbidden() throws Exception
    {
        restClient.authenticateUser(adminTenantUser);
        restClient.withCoreAPI().usingNetworks().getNetwork(secondAdminTenantUser);
        restClient.assertStatusCodeIs(HttpStatus.FORBIDDEN);
    }

    @TestRail(section = { TestGroup.REST_API, TestGroup.NETWORKS }, executionType = ExecutionType.SANITY,
            description = "Verify any tenant user gets its network with Rest API and response is not empty")
    @Test(groups = { TestGroup.REST_API, TestGroup.NETWORKS, TestGroup.SANITY })
    public void userTenantChecksIfNetworkIsPresent() throws Exception
    {
        restClient.authenticateUser(tenantUser);
        restClient.withCoreAPI().usingNetworks().getNetwork(adminTenantUser).assertNetworkHasName(adminTenantUser).assertNetworkIsEnabled();
        restClient.assertStatusCodeIs(HttpStatus.OK);
    }

    @TestRail(section = { TestGroup.REST_API, TestGroup.NETWORKS }, executionType = ExecutionType.REGRESSION,
            description = "Verify any tenant user gets non existing network with Rest API and checks the not found status")
    @Test(groups = { TestGroup.REST_API, TestGroup.NETWORKS, TestGroup.REGRESSION })
    public void userTenantChecksIfNonExistingNetworkIsNotFound() throws Exception
    {
        restClient.authenticateUser(tenantUser);
        restClient.withCoreAPI().usingNetworks().getNetwork(UserModel.getRandomTenantUser());
        restClient.assertStatusCodeIs(HttpStatus.NOT_FOUND);
    }

    @TestRail(section = { TestGroup.REST_API, TestGroup.NETWORKS }, executionType = ExecutionType.REGRESSION,
            description = "Verify any tenant user gets another existing network with Rest API and checks the forbidden status")
    @Test(groups = { TestGroup.REST_API, TestGroup.NETWORKS, TestGroup.REGRESSION })
    public void userTenantChecksIfAnotherExistingNetworkIsForbidden() throws Exception
    {
        restClient.authenticateUser(tenantUser);
        restClient.withCoreAPI().usingNetworks().getNetwork(secondAdminTenantUser);
        restClient.assertStatusCodeIs(HttpStatus.FORBIDDEN);
    }

    @Bug(id = "ACE-5738")
    @TestRail(section = { TestGroup.REST_API, TestGroup.NETWORKS }, executionType = ExecutionType.REGRESSION,
            description = "Verify tenant admin user gets an invalid network with Rest API and checks the not found status")
    @Test(groups = { TestGroup.REST_API, TestGroup.NETWORKS, TestGroup.REGRESSION })
    public void adminTenantGetsInvalidNetwork() throws Exception
    {
        restClient.authenticateUser(secondAdminTenantUser);
        secondAdminTenantUser.setDomain("tenant.@%");

        restClient.withCoreAPI().usingNetworks().getNetwork(secondAdminTenantUser);
        restClient.assertStatusCodeIs(HttpStatus.NOT_FOUND)
                .assertLastError().containsErrorKey(RestErrorModel.ENTITY_NOT_FOUND_ERRORKEY)
                .containsSummary(String.format(RestErrorModel.UNEXPECTED_TENANT, adminTenantUser.getDomain(), "@"))
                .descriptionURLIs(RestErrorModel.RESTAPIEXPLORER)
                .stackTraceIs(RestErrorModel.STACKTRACE);
    }

    @Bug(id = "ACE-5745")
    @TestRail(section = { TestGroup.REST_API, TestGroup.NETWORKS }, executionType = ExecutionType.REGRESSION,
            description = "Verify admin user gets an existing network successfully with Rest API")
    @Test(groups = { TestGroup.REST_API, TestGroup.NETWORKS, TestGroup.REGRESSION })
    public void adminUserGetsExistingNetwork() throws Exception
    {
        RestNetworkModel restNetworkModel = restClient.authenticateUser(adminUserModel).withCoreAPI().usingNetworks().getNetwork(adminTenantUser);
        restClient.assertStatusCodeIs(HttpStatus.OK);
        restNetworkModel.assertThat().fieldsCount().is("1");
        restNetworkModel.assertThat().field("id").is(adminTenantUser.getDomain())
                .and().field("homeNetwork").isNull()
                .and().field("isEnabled").is("true");
    }

    @TestRail(section = { TestGroup.REST_API, TestGroup.NETWORKS }, executionType = ExecutionType.REGRESSION,
            description = "Verify tenant admin user gets network using properties parameter successfully with Rest API")
    @Test(groups = { TestGroup.REST_API, TestGroup.NETWORKS, TestGroup.REGRESSION })
    public void adminTenantGetsNetworkUsingPropertiesParameter() throws Exception
    {
        JSONObject entryResponse= restClient.authenticateUser(tenantUser).withCoreAPI().usingNetworks().usingParams("properties=id").getNetworkWithParams(adminTenantUser);
        restClient.assertStatusCodeIs(HttpStatus.OK);
        Assert.assertTrue(entryResponse.get("id").equals(adminTenantUser.getDomain().toLowerCase()));
        assertJsonResponseDoesnotContainField(entryResponse, "homeNetwork");
        assertJsonResponseDoesnotContainField(entryResponse, "isEnabled");
    }

    private void assertJsonResponseDoesnotContainField(JSONObject entryResponse, String field)
    {
        try{
            entryResponse.get(field);
        }
        catch(JSONException ex)
        {
            Assert.assertTrue(ex.getMessage().equals(String.format("JSONObject[\"%s\"] not found.", field)));
        }
    }
}
