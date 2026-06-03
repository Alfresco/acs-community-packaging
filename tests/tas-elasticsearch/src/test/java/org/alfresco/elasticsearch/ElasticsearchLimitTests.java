package org.alfresco.elasticsearch;

import static org.alfresco.elasticsearch.SearchQueryService.req;
import static org.alfresco.tas.TestDataUtility.getAlphabeticUUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import org.alfresco.rest.search.RestRequestLimitsModel;
import org.alfresco.rest.search.SearchRequest;
import org.alfresco.tas.AlfrescoStackInitializer;
import org.alfresco.utility.constants.UserRole;
import org.alfresco.utility.data.DataContent;
import org.alfresco.utility.data.DataSite;
import org.alfresco.utility.data.DataUser;
import org.alfresco.utility.model.FileModel;
import org.alfresco.utility.model.FileType;
import org.alfresco.utility.model.SiteModel;
import org.alfresco.utility.model.TestGroup;
import org.alfresco.utility.model.UserModel;
import org.alfresco.utility.network.ServerHealth;
import org.alfresco.utility.testrail.ExecutionType;
import org.alfresco.utility.testrail.annotation.TestRail;

@ContextConfiguration(locations = "classpath:alfresco-elasticsearch-context.xml", initializers = AlfrescoStackInitializer.class)
/**
 * In this test we are verifying the Track Total Hits feature
 */
public class ElasticsearchLimitTests extends AbstractTestNGSpringContextTests
{
    private static final String PREFIX = getAlphabeticUUID() + "_";
    private static final int TOTAL_DOCUMENT_COUNT = 20;

    @Autowired
    private DataUser dataUser;

    @Autowired
    private DataContent dataContent;

    @Autowired
    private DataSite dataSite;

    @Autowired
    private ServerHealth serverHealth;

    @Autowired
    protected SearchQueryService searchQueryService;

    private UserModel userSite1;
    private SiteModel siteModel1;

    @BeforeClass(alwaysRun = true)
    public void dataPreparation()
    {
        serverHealth.assertServerIsOnline();

        userSite1 = dataUser.createRandomTestUser();

        siteModel1 = dataSite.usingUser(userSite1).createPrivateRandomSite();

        dataUser.addUserToSite(userSite1, siteModel1, UserRole.SiteContributor);

        for (int i = 0; i < TOTAL_DOCUMENT_COUNT; i++)
        {
            createContent(PREFIX + i + ".txt", "Document " + i, siteModel1, userSite1);
        }
    }

    @TestRail(section = TestGroup.SEARCH, executionType = ExecutionType.REGRESSION, description = "Total hit tracking unset should use default behaviour")
    @Test(groups = TestGroup.SEARCH)
    public void searchUsingTotalHitsLimitDefaultValue()
    {
        SearchRequest request = req("cm:name:" + PREFIX + "*.txt");

        // Without setting limits, we should get all up to 10k
        searchQueryService.expectTotalHitsFromQuery(request, userSite1, TOTAL_DOCUMENT_COUNT);
    }

    @TestRail(section = TestGroup.SEARCH, executionType = ExecutionType.REGRESSION, description = "Total hit tracking with 0 should use default behaviour")
    @Test(groups = TestGroup.SEARCH)
    public void searchUsingTotalHitsLimitZeroShouldDefault()
    {
        SearchRequest request = req("cm:name:" + PREFIX + "*.txt");

        // Setting trackTotalHitsLimit to 0 should behave as not setting it, counting all up to 10k
        request.setLimits(new RestRequestLimitsModel(null, null, 0));
        searchQueryService.expectTotalHitsFromQuery(request, userSite1, TOTAL_DOCUMENT_COUNT);
    }

    @TestRail(section = TestGroup.SEARCH, executionType = ExecutionType.REGRESSION, description = "Total hit tracking set should use the defined limit")
    @Test(groups = TestGroup.SEARCH)
    public void searchUsingTotalHitsLimit()
    {
        SearchRequest request = req("cm:name:" + PREFIX + "*.txt");

        // Setting trackTotalHitsLimit to 10 should only count up to 10
        request.setLimits(new RestRequestLimitsModel(null, null, 10));
        searchQueryService.expectTotalHitsFromQuery(request, userSite1, 10);
    }

    @TestRail(section = TestGroup.SEARCH, executionType = ExecutionType.REGRESSION, description = "Total hit tracking with -1 should count all docs")
    @Test(groups = TestGroup.SEARCH)
    public void searchUsingTotalHitsUnlimited()
    {
        SearchRequest request = req("cm:name:" + PREFIX + "*.txt");

        // Setting trackTotalHitsLimit to -1 should only count up to max int
        request.setLimits(new RestRequestLimitsModel(null, null, -1));
        searchQueryService.expectTotalHitsFromQuery(request, userSite1, TOTAL_DOCUMENT_COUNT);
    }

    private FileModel createContent(String filename, String content, SiteModel site, UserModel user)
    {
        FileModel fileModel = new FileModel(filename, FileType.TEXT_PLAIN, content);
        return dataContent.usingUser(user).usingSite(site).createContent(fileModel);
    }
}
