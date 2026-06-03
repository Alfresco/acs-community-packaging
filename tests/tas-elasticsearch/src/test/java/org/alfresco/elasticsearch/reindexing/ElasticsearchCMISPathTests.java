package org.alfresco.elasticsearch.reindexing;

import static org.alfresco.elasticsearch.SearchQueryService.req;
import static org.alfresco.tas.TestDataUtility.getAlphabeticUUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import org.alfresco.elasticsearch.SearchQueryService;
import org.alfresco.rest.search.SearchRequest;
import org.alfresco.tas.AlfrescoStackInitializer;
import org.alfresco.utility.data.DataContent;
import org.alfresco.utility.data.DataSite;
import org.alfresco.utility.data.DataUser;
import org.alfresco.utility.model.FileModel;
import org.alfresco.utility.model.FileType;
import org.alfresco.utility.model.FolderModel;
import org.alfresco.utility.model.SiteModel;
import org.alfresco.utility.model.TestGroup;
import org.alfresco.utility.model.UserModel;
import org.alfresco.utility.network.ServerHealth;
import org.alfresco.utility.report.log.Step;
import org.alfresco.utility.testrail.ExecutionType;
import org.alfresco.utility.testrail.annotation.TestRail;

@ContextConfiguration(locations = "classpath:alfresco-elasticsearch-context.xml",
        initializers = AlfrescoStackInitializer.class)
/**
 * Tests for CMIS queries that require path indexing against Elasticsearch.
 */
public class ElasticsearchCMISPathTests extends AbstractTestNGSpringContextTests
{
    private static final String PREFIX = getAlphabeticUUID();
    private static final String FOLDER_0_NAME = PREFIX + "_folder0";
    private static final String FOLDER_00_NAME = PREFIX + "_folder00";
    private static final String FOLDER_000_NAME = PREFIX + "_folder000";
    private static final String FOLDER_1_NAME = PREFIX + "_folder1";
    private static final String DOC_0000_NAME = PREFIX + "_doc0000.txt";
    private static final String DOC_00_NAME = PREFIX + "_doc00.txt";
    private static final String DOC_01_NAME = PREFIX + "_doc01.txt";
    private static final String DOC_10_NAME = PREFIX + "_doc10.txt";

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

    private UserModel user;
    private SiteModel siteModel;
    private FolderModel folder0;
    private FolderModel folder00;
    private FolderModel folder000;
    private FolderModel folder1;
    private FileModel document0000;
    private FileModel document00;
    private FileModel document01;
    private FileModel document10;

    /**
     * Data will be prepared using the schema below:
     * 
     * <pre>
     * Site
     * + Document Library
     *   +-folder0
     *   | +-folder00
     *   | | +-folder000
     *   | |   +-document0000
     *   | +-document00
     *   | +-document01
     *   +-folder1
     *   +-document10
     * </pre>
     */
    @BeforeClass(alwaysRun = true)
    public void dataPreparation()
    {
        serverHealth.assertServerIsOnline();

        user = dataUser.createRandomTestUser();

        siteModel = dataSite.usingUser(user).createPrivateRandomSite();

        folder0 = dataContent.usingUser(user).usingSite(siteModel).createFolderCmisApi(FOLDER_0_NAME);
        folder1 = dataContent.usingUser(user).usingSite(siteModel).createFolderCmisApi(FOLDER_1_NAME);

        folder00 = dataContent.usingUser(user).usingResource(folder0).createFolderCmisApi(FOLDER_00_NAME);
        folder000 = dataContent.usingUser(user).usingResource(folder00).createFolderCmisApi(FOLDER_000_NAME);

        document0000 = createContent(DOC_0000_NAME, "This is document 0000", folder000, user);
        document00 = createContent(DOC_00_NAME, "This is document 00", folder0, user);
        document01 = createContent(DOC_01_NAME, "This is document 01", folder0, user);
        document10 = createContent(DOC_10_NAME, "This is document 10", folder1, user);
    }

    @TestRail(description = "Check that we can select subfolders using IN_TREE with Elasticsearch.", section = TestGroup.SEARCH, executionType = ExecutionType.REGRESSION)
    @Test(groups = TestGroup.SEARCH)
    public void inTreeQuery_selectSubfolders()
    {
        SearchRequest query = req("cmis", "SELECT * FROM cmis:folder WHERE IN_TREE('" + folder0.getNodeRef() + "')");
        searchQueryService.expectResultsFromQuery(query, user, FOLDER_00_NAME, FOLDER_000_NAME);
    }

    @TestRail(description = "Check that we can select documents using IN_TREE with Elasticsearch.", section = TestGroup.SEARCH, executionType = ExecutionType.REGRESSION)
    @Test(groups = TestGroup.SEARCH)
    public void inTreeQuery_selectDocuments()
    {
        SearchRequest query = req("cmis", "SELECT * FROM cmis:document WHERE IN_TREE('" + folder0.getNodeRef() + "')");
        searchQueryService.expectResultsFromQuery(query, user, DOC_0000_NAME, DOC_00_NAME, DOC_01_NAME);
    }

    private FileModel createContent(String filename, String content, FolderModel folderModel, UserModel user)
    {
        FileModel fileModel = new FileModel(filename, FileType.TEXT_PLAIN, content);
        return dataContent.usingUser(user).usingResource(folderModel).createContent(fileModel);
    }
}
