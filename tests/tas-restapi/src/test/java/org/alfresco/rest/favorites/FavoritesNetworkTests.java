package org.alfresco.rest.favorites;

import org.alfresco.rest.NetworkDataPrep;
import org.alfresco.rest.model.RestErrorModel;
import org.alfresco.utility.model.SiteModel;
import org.alfresco.utility.model.TestGroup;
import org.alfresco.utility.report.Bug;
import org.alfresco.utility.testrail.ExecutionType;
import org.alfresco.utility.testrail.annotation.TestRail;
import org.springframework.http.HttpStatus;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

public class FavoritesNetworkTests extends NetworkDataPrep
{
    private SiteModel publicSite, siteModel;

    @BeforeClass(alwaysRun = true)
    public void dataPreparation() throws Exception
    {
        init();
        siteModel = dataSite.usingUser(adminTenantUser).createPublicRandomSite();
    }

    @Test(groups = { TestGroup.REST_API, TestGroup.PEOPLE, TestGroup.NETWORKS, TestGroup.REGRESSION})
    @TestRail(section = { TestGroup.REST_API, TestGroup.PEOPLE}, executionType = ExecutionType.REGRESSION,
            description = "Verify tenant user is able to add to favorites his site and status code is (201)")
    public void tenantAddFavoriteSiteValidNetwork() throws  Exception
    {
        publicSite = dataSite.usingUser(tenantUser).createPublicRandomSite();

        restClient.authenticateUser(tenantUser).withCoreAPI();

        restClient.withCoreAPI()
                .usingAuthUser().addSiteToFavorites(publicSite);
        restClient.assertStatusCodeIs(HttpStatus.CREATED);

        restClient.withCoreAPI().usingAuthUser().getFavorites()
                .assertThat()
                .entriesListContains("targetGuid", publicSite.getGuidWithoutVersion());
    }

    @Bug(id="MNT-16904")
    @Test(groups = { TestGroup.REST_API, TestGroup.PEOPLE, TestGroup.NETWORKS, TestGroup.REGRESSION})
    @TestRail(section = { TestGroup.REST_API, TestGroup.PEOPLE, TestGroup.NETWORKS }, executionType = ExecutionType.REGRESSION,
            description = "Verify tenant user is not able to add favorites using an invalid network ID - status code (401)")
    public void tenantIsNotAbleToAddFavoriteSiteWithInvalidNetworkID() throws  Exception
    {
        publicSite = dataSite.usingUser(tenantUser).createPublicRandomSite();

        restClient.authenticateUser(tenantUser).withCoreAPI();
        String domain = tenantUser.getDomain();
        try
        {
        tenantUser.setDomain("invalidNetwork");

        restClient.withCoreAPI().usingAuthUser().addSiteToFavorites(publicSite);
        restClient.assertStatusCodeIs(HttpStatus.UNAUTHORIZED)
                .assertLastError()
                .containsSummary(RestErrorModel.AUTHENTICATION_FAILED)
                .containsErrorKey(RestErrorModel.ENTITY_NOT_FOUND_ERRORKEY)
                .descriptionURLIs(RestErrorModel.RESTAPIEXPLORER)
                .stackTraceIs(RestErrorModel.STACKTRACE);
        }
        finally
        {
            tenantUser.setDomain(domain);
        }
    }

    @Bug(id="MNT-16904")
    @TestRail(section = { TestGroup.REST_API, TestGroup.PEOPLE}, executionType = ExecutionType.REGRESSION,
            description = "Verify tenant user is not able to remove favorites using an invalid network ID")
    @Test(groups = { TestGroup.REST_API, TestGroup.PEOPLE,TestGroup.NETWORKS, TestGroup.REGRESSION })
    public void tenantIsNotAbleToRemoveFavoriteSiteWithInvalidNetworkID() throws Exception
    {
        SiteModel siteModel = dataSite.usingUser(tenantUser).createPublicRandomSite();

        restClient.authenticateUser(tenantUser).withCoreAPI().usingAuthUser().addSiteToFavorites(siteModel);
        String domain = tenantUser.getDomain();
        try
        {
        tenantUser.setDomain("invalidNetwork");

        restClient.withCoreAPI().usingAuthUser().removeFavoriteSite(siteModel);
        restClient.assertStatusCodeIs(HttpStatus.UNAUTHORIZED)
                .assertLastError()
                .containsSummary(RestErrorModel.AUTHENTICATION_FAILED)
                .containsErrorKey(RestErrorModel.ENTITY_NOT_FOUND_ERRORKEY)
                .descriptionURLIs(RestErrorModel.RESTAPIEXPLORER)
                .stackTraceIs(RestErrorModel.STACKTRACE);
        }
        finally
        {
            tenantUser.setDomain(domain);
        }
    }

    @Bug(id="MNT-16904")
    @TestRail(section = { TestGroup.REST_API, TestGroup.FAVORITES }, executionType = ExecutionType.REGRESSION,
            description = "Verify tenant user is not able to delete favorites using an invalid network ID")
    @Test(groups = { TestGroup.REST_API, TestGroup.FAVORITES,TestGroup.NETWORKS, TestGroup.REGRESSION })
    public void tenantIsNotAbleToDeleteFavoriteSiteWithInvalidNetworkID() throws Exception
    {
        restClient.authenticateUser(tenantUser).withCoreAPI().usingAuthUser().addSiteToFavorites(siteModel);
        String domain = tenantUser.getDomain();
        try
        {
        tenantUser.setDomain("invalidNetwork");

        restClient.withCoreAPI()
                .usingAuthUser().deleteSiteFromFavorites(siteModel)
                .assertStatusCodeIs(HttpStatus.UNAUTHORIZED)
                .assertLastError()
                .containsSummary(RestErrorModel.AUTHENTICATION_FAILED)
                .containsErrorKey(RestErrorModel.ENTITY_NOT_FOUND_ERRORKEY)
                .descriptionURLIs(RestErrorModel.RESTAPIEXPLORER)
                .stackTraceIs(RestErrorModel.STACKTRACE);
        }
        finally
        {
            tenantUser.setDomain(domain);
        }
    }

    @Test(groups = { TestGroup.REST_API, TestGroup.PEOPLE, TestGroup.NETWORKS, TestGroup.REGRESSION})
    @TestRail(section = { TestGroup.REST_API, TestGroup.PEOPLE}, executionType = ExecutionType.REGRESSION,
            description = "Verify tenant user is able to add to favorites a site created by admin tenant - same network and status code is (201)")
    public void tenantUserIsAbleToAddFavoriteSiteAddedByAdminSameNetwork() throws Exception
    {
        publicSite = dataSite.usingUser(adminTenantUser).createPublicRandomSite();

        restClient.authenticateUser(tenantUser).withCoreAPI();
        tenantUser.setDomain(adminTenantUser.getDomain());

        restClient.withCoreAPI().usingAuthUser().addFavoriteSite(publicSite);
        restClient.assertStatusCodeIs(HttpStatus.CREATED);

        restClient.withCoreAPI().usingAuthUser().getFavorites()
                .assertThat()
                .entriesListContains("targetGuid", publicSite.getGuidWithoutVersion());
    }

    @Bug(id = "MNT-16904")
    @TestRail(section = { TestGroup.REST_API, TestGroup.FAVORITES }, executionType = ExecutionType.REGRESSION,
            description = "Verify the get favorites request when network id is invalid for tenant user")
    @Test(groups = { TestGroup.REST_API, TestGroup.FAVORITES, TestGroup.REGRESSION, TestGroup.NETWORKS})
    public void getFavoriteSitesWhenNetworkIdIsInvalid() throws Exception
    {
        SiteModel firstSiteModel = dataSite.usingUser(adminUserModel).createPublicRandomSite();

        restClient.authenticateUser(tenantUser).withCoreAPI().usingAuthUser().addSiteToFavorites(firstSiteModel);

        String domain = tenantUser.getDomain();
        try
        {
            tenantUser.setDomain("invalidNetwork");
            restClient.authenticateUser(tenantUser).withCoreAPI().usingAuthUser().getFavorites();
            restClient.assertStatusCodeIs(HttpStatus.UNAUTHORIZED)
                    .assertLastError().containsSummary(RestErrorModel.AUTHENTICATION_FAILED);
        }
        finally
        {
            tenantUser.setDomain(domain);
        }
    }

    @Bug(id = "MNT-16904")
    @TestRail(section = { TestGroup.REST_API, TestGroup.FAVORITES }, executionType = ExecutionType.REGRESSION,
            description = "Verify the post favorites request when network id is invalid for tenant user")
    @Test(groups = { TestGroup.REST_API, TestGroup.FAVORITES, TestGroup.REGRESSION, TestGroup.NETWORKS})
    public void addFavoriteSitesWhenNetworkIdIsInvalid() throws Exception
    {
        String domain = tenantUser.getDomain();
        try
        {
            tenantUser.setDomain("invalidNetwork");
            restClient.authenticateUser(tenantUser).withCoreAPI().usingAuthUser().addSiteToFavorites(siteModel);
            restClient.assertStatusCodeIs(HttpStatus.UNAUTHORIZED)
                    .assertLastError().containsSummary(RestErrorModel.AUTHENTICATION_FAILED);
        }
        finally{
            tenantUser.setDomain(domain);
        }
    }

    @Bug(id = "MNT-16904")
    @TestRail(section = { TestGroup.REST_API,
            TestGroup.FAVORITES }, executionType = ExecutionType.REGRESSION, description = "Verify the get favorite request for invalid network id")
    @Test(groups = { TestGroup.REST_API, TestGroup.FAVORITES, TestGroup.REGRESSION, TestGroup.NETWORKS })
    public void getFavoriteSiteWithInvalidNetworkId()  throws Exception
    {
        restClient.authenticateUser(tenantUser).withCoreAPI().usingAuthUser().addSiteToFavorites(siteModel);

        String domain = tenantUser.getDomain();
        try
        {
            tenantUser.setDomain("invalidNetwork");
            restClient.authenticateUser(tenantUser).withCoreAPI().usingAuthUser().getFavorite(siteModel.getGuid());
            restClient.assertStatusCodeIs(HttpStatus.UNAUTHORIZED)
                    .assertLastError()
                    .containsSummary(RestErrorModel.AUTHENTICATION_FAILED);
        }
        finally
        {
            tenantUser.setDomain(domain);
        }
    }

    @TestRail(section = { TestGroup.REST_API,
            TestGroup.FAVORITES }, executionType = ExecutionType.REGRESSION, description = "Verify the get favorite request with tenant user")
    @Test(groups = { TestGroup.REST_API, TestGroup.FAVORITES, TestGroup.REGRESSION, TestGroup.NETWORKS })
    public void getFavoriteSiteWithTenantUser()  throws Exception
    {
        restClient.authenticateUser(tenantUser).withCoreAPI().usingAuthUser().addSiteToFavorites(siteModel);

        restClient.withCoreAPI().usingAuthUser().getFavorite(siteModel.getGuid());
        restClient.assertStatusCodeIs(HttpStatus.OK);
    }

    @TestRail(section = { TestGroup.REST_API, TestGroup.PEOPLE}, executionType = ExecutionType.REGRESSION,
            description = "Verify tenant user deletes favorites site and status code is (204)")
    @Test(groups = { TestGroup.REST_API, TestGroup.PEOPLE,TestGroup.NETWORKS, TestGroup.REGRESSION })
    public void tenantRemoveFavoriteSiteValidNetwork() throws Exception
    {
        SiteModel siteModel = dataSite.usingUser(tenantUser).createPublicRandomSite();
        restClient.authenticateUser(tenantUser).withCoreAPI().usingAuthUser().addSiteToFavorites(siteModel);

        restClient.withCoreAPI()
                .usingAuthUser().removeFavoriteSite(siteModel);
        restClient.assertStatusCodeIs(HttpStatus.NO_CONTENT);

        restClient.withCoreAPI().usingAuthUser().getFavorites()
                .assertThat()
                .entriesListDoesNotContain("targetGuid", siteModel.getGuidWithoutVersion());
    }

    @TestRail(section = { TestGroup.REST_API, TestGroup.FAVORITES}, executionType = ExecutionType.REGRESSION,
            description = "Verify tenant user deletes favorites site and status code is (204)")
    @Test(groups = { TestGroup.REST_API, TestGroup.FAVORITES,TestGroup.NETWORKS, TestGroup.REGRESSION })
    public void tenantDeleteFavoriteSiteValidNetwork() throws Exception
    {

        restClient.authenticateUser(tenantUser).withCoreAPI().usingAuthUser().addSiteToFavorites(siteModel);

        restClient.withCoreAPI()
                .usingAuthUser().deleteSiteFromFavorites(siteModel)
                .assertStatusCodeIs(HttpStatus.NO_CONTENT);

        restClient.withCoreAPI().usingAuthUser().getFavorites()
                .assertThat()
                .entriesListDoesNotContain("targetGuid", siteModel.getGuidWithoutVersion());
        siteModel = dataSite.usingUser(tenantUser).createPublicRandomSite();
    }

    @TestRail(section = { TestGroup.REST_API, TestGroup.PEOPLE}, executionType = ExecutionType.REGRESSION,
            description = "Verify tenant user remove favorites site created by admin tenant - same network and status code is (204)")
    @Test(groups = { TestGroup.REST_API, TestGroup.PEOPLE,TestGroup.NETWORKS, TestGroup.REGRESSION })
    public void tenantUserIsAbleToRemoveFavoriteSiteAddedByAdminSameNetwork() throws Exception
    {
        SiteModel siteModel = dataSite.usingUser(adminTenantUser).createPublicRandomSite();

        restClient.authenticateUser(tenantUser).withCoreAPI().usingAuthUser().addSiteToFavorites(siteModel);
        tenantUser.setDomain(adminTenantUser.getDomain());

        restClient.withCoreAPI().usingAuthUser().removeFavoriteSite(siteModel);
        restClient.assertStatusCodeIs(HttpStatus.NO_CONTENT);

        restClient.withCoreAPI().usingAuthUser().getFavorites()
                .assertThat()
                .entriesListDoesNotContain("targetGuid", siteModel.getGuidWithoutVersion());
    }

    @TestRail(section = { TestGroup.REST_API, TestGroup.FAVORITES}, executionType = ExecutionType.REGRESSION,
            description = "Verify tenant user deletes favorites site created by admin tenant - same network and status code is (204)")
    @Test(groups = { TestGroup.REST_API, TestGroup.FAVORITES,TestGroup.NETWORKS, TestGroup.REGRESSION })
    public void tenantUserIsAbleToDeleteFavoriteSiteAddedByAdminSameNetwork() throws Exception
    {

        restClient.authenticateUser(tenantUser).withCoreAPI().usingAuthUser().addSiteToFavorites(siteModel);
        tenantUser.setDomain(adminTenantUser.getDomain());

        restClient.withCoreAPI()
                .usingAuthUser().deleteSiteFromFavorites(siteModel)
                .assertStatusCodeIs(HttpStatus.NO_CONTENT);

        restClient.withCoreAPI().usingAuthUser().getFavorites()
                .assertThat()
                .entriesListDoesNotContain("targetGuid", siteModel.getGuidWithoutVersion());
        siteModel = dataSite.usingUser(tenantUser).createPublicRandomSite();
    }
}
