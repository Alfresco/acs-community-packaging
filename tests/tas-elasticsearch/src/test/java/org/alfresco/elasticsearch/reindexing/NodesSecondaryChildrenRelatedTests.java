package org.alfresco.elasticsearch.reindexing;

import static org.alfresco.utility.report.log.Step.STEP;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;

import org.alfresco.elasticsearch.SearchQueryService;
import org.alfresco.rest.core.RestWrapper;
import org.alfresco.rest.repo.resource.Folders;
import org.alfresco.tas.AlfrescoStackInitializer;
import org.alfresco.utility.data.DataContent;
import org.alfresco.utility.data.DataSite;
import org.alfresco.utility.data.DataUser;
import org.alfresco.utility.model.SiteModel;
import org.alfresco.utility.model.UserModel;
import org.alfresco.utility.network.ServerHealth;

@ContextConfiguration(locations = "classpath:alfresco-elasticsearch-context.xml",
        initializers = AlfrescoStackInitializer.class)
public abstract class NodesSecondaryChildrenRelatedTests extends AbstractTestNGSpringContextTests
{
    @SuppressWarnings("PMD.OneDeclarationPerLine")
    protected static final String A = "A", B = "B", C = "C", K = "K", L = "L", M = "M", P = "P", Q = "Q", R = "R", S = "S", X = "X", Y = "Y", Z = "Z";

    @Autowired
    private ServerHealth serverHealth;
    @Autowired
    private DataUser dataUser;
    @Autowired
    private DataSite dataSite;
    @Autowired
    protected DataContent dataContent;
    @Autowired
    protected RestWrapper restClient;
    @Autowired
    protected SearchQueryService searchQueryService;

    protected UserModel testUser;
    protected SiteModel testSite;
    protected Folders folders;

    @BeforeClass(alwaysRun = true)
    public void dataPreparation()
    {
        STEP("Verify environment health.");
        serverHealth.isServerReachable();
        serverHealth.assertServerIsOnline();

        STEP("Create a test user and private site.");
        testUser = dataUser.createRandomTestUser();
        testSite = dataSite.usingUser(testUser).createPrivateRandomSite();
        folders = new Folders(dataContent, restClient, testUser, testSite);
    }

    @AfterClass(alwaysRun = true)
    public void dataCleanUp()
    {
        STEP("Clean up data after tests.");
        dataSite.usingUser(testUser).deleteSite(testSite);
        dataUser.usingAdmin().deleteUser(testUser);
    }
}
