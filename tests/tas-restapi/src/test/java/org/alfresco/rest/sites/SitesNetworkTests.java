package org.alfresco.rest.sites;

import org.alfresco.dataprep.SiteService;
import org.alfresco.rest.NetworkDataPrep;
import org.alfresco.rest.model.RestErrorModel;
import org.alfresco.rest.model.RestSiteMembershipRequestModel;
import org.alfresco.utility.model.SiteModel;
import org.alfresco.utility.model.TestGroup;
import org.alfresco.utility.report.Bug;
import org.alfresco.utility.testrail.ExecutionType;
import org.alfresco.utility.testrail.annotation.TestRail;
import org.springframework.http.HttpStatus;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

public class SitesNetworkTests extends NetworkDataPrep
{
    protected SiteModel siteModel;

    @BeforeClass(alwaysRun = true)
    public void dataPreparation() throws Exception
    {
        init();
        siteModel = dataSite.usingUser(adminUserModel).createPublicRandomSite();
    }

    @Bug(id="REPO-4301")
    @Test(groups = { TestGroup.REST_API, TestGroup.PEOPLE, TestGroup.REGRESSION, TestGroup.NETWORKS })
    @TestRail(section = { TestGroup.REST_API, TestGroup.PEOPLE }, executionType = ExecutionType.REGRESSION,
            description = "Verify create site membership request returns status code 404 when personId is not member of the domain.")
    public void addSiteMembershipRequestWhenPersonIdIsNotInTheDomain() throws Exception
    {
        restClient.authenticateUser(adminTenantUser).withCoreAPI().usingAuthUser().addSiteMembershipRequest(siteModel);
        restClient.assertStatusCodeIs(HttpStatus.NOT_FOUND)
                .assertLastError().containsSummary(String.format(RestErrorModel.RELATIONSHIP_NOT_FOUND, adminTenantUser.getUsername().toLowerCase(), siteModel.getId()));
    }

    @Test(groups = { TestGroup.REST_API, TestGroup.PEOPLE, TestGroup.REGRESSION, TestGroup.NETWORKS })
    @TestRail(section = { TestGroup.REST_API, TestGroup.PEOPLE }, executionType = ExecutionType.REGRESSION,
            description = "Verify create site membership request returns status code 200 with tenant.")
    public void addSiteMembershipRequestWithTenant() throws Exception
    {
        SiteModel tenantPublicSite = dataSite.usingUser(adminTenantUser).createPublicRandomSite();
        RestSiteMembershipRequestModel siteMembershipRequest = restClient.authenticateUser(tenantUser).withCoreAPI().usingAuthUser().addSiteMembershipRequest(tenantPublicSite);
        restClient.assertStatusCodeIs(HttpStatus.CREATED);
        siteMembershipRequest.assertThat().field("id").is(tenantPublicSite.getId())
                .assertThat().field("site").isNotEmpty();
    }

    @Bug(id="REPO-4301")
    @TestRail(section = { TestGroup.REST_API, TestGroup.PEOPLE, TestGroup.NETWORKS }, executionType = ExecutionType.REGRESSION,
            description = "Add process item using by admin in other network.")
    @Test(groups = { TestGroup.REST_API, TestGroup.PEOPLE, TestGroup.REGRESSION, TestGroup.NETWORKS })
    public void getSiteMembershipRequestByAdminInOtherNetwork() throws Exception
    {
        SiteModel moderatedSite = dataSite.usingUser(secondAdminTenantUser).createModeratedRandomSite();

        restClient.authenticateUser(adminTenantUser).withCoreAPI().usingAuthUser().addSiteMembershipRequest(moderatedSite);
        restClient.authenticateUser(secondAdminTenantUser).withCoreAPI().usingAuthUser().getSiteMembershipRequest(moderatedSite);

        restClient.assertStatusCodeIs(HttpStatus.NOT_FOUND)
                .assertLastError()
                .containsErrorKey(RestErrorModel.RELATIONSHIP_NOT_FOUND_ERRORKEY)
                .containsSummary(String.format(RestErrorModel.RELATIONSHIP_NOT_FOUND, secondAdminTenantUser.getUsername().toLowerCase(), moderatedSite.getId()))
                .descriptionURLIs(RestErrorModel.RESTAPIEXPLORER)
                .stackTraceIs(RestErrorModel.STACKTRACE);
    }

    @TestRail(section = { TestGroup.REST_API, TestGroup.PEOPLE, TestGroup.NETWORKS }, executionType = ExecutionType.REGRESSION,
            description = "Add process item using by the admin in same network.")
    @Test(groups = { TestGroup.REST_API, TestGroup.PEOPLE, TestGroup.REGRESSION, TestGroup.NETWORKS })
    public void getSiteMembershipRequestByAdminSameNetwork() throws Exception
    {
        SiteModel moderatedSite = dataSite.usingUser(adminTenantUser).createModeratedRandomSite();
        restClient.authenticateUser(tenantUser).withCoreAPI().usingAuthUser().addSiteMembershipRequest(moderatedSite);

        RestSiteMembershipRequestModel returnedModel = restClient.authenticateUser(tenantUser).withCoreAPI().usingAuthUser().getSiteMembershipRequest(moderatedSite);

        restClient.assertStatusCodeIs(HttpStatus.OK);
        returnedModel.assertThat().field("id").is(moderatedSite.getId())
                .and().field("message").is("Please accept me")
                .and().field("site.title").is(moderatedSite.getTitle())
                .and().field("site.visibility").is(SiteService.Visibility.MODERATED.toString())
                .and().field("site.guid").isNotEmpty()
                .and().field("site.description").is(moderatedSite.getDescription())
                .and().field("site.preset").is("site-dashboard");
    }
}
