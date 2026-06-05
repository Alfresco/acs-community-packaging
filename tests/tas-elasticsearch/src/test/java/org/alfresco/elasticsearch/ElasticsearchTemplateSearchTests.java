package org.alfresco.elasticsearch;

import static org.alfresco.elasticsearch.SearchQueryService.req;
import static org.alfresco.utility.data.RandomData.getRandomFile;
import static org.alfresco.utility.data.RandomData.getRandomName;
import static org.alfresco.utility.report.log.Step.STEP;

import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import org.alfresco.rest.model.RestTagModel;
import org.alfresco.rest.search.RestRequestDefaultsModel;
import org.alfresco.rest.search.SearchRequest;
import org.alfresco.tas.AlfrescoStackInitializer;
import org.alfresco.utility.data.DataContent;
import org.alfresco.utility.data.DataUser;
import org.alfresco.utility.model.ContentModel;
import org.alfresco.utility.model.FileModel;
import org.alfresco.utility.model.FileType;
import org.alfresco.utility.model.FolderModel;
import org.alfresco.utility.model.TestGroup;
import org.alfresco.utility.model.UserModel;
import org.alfresco.utility.network.ServerHealth;

@ContextConfiguration(locations = "classpath:alfresco-elasticsearch-context.xml",
        initializers = AlfrescoStackInitializer.class)
public class ElasticsearchTemplateSearchTests extends AbstractTestNGSpringContextTests
{
    private static final String SEARCH_TERM = "sample";

    @Autowired
    private ServerHealth serverHealth;

    @Autowired
    private DataUser dataUser;

    @Autowired
    private DataContent dataContent;

    @Autowired
    private SearchQueryService searchQueryService;

    private UserModel testUser;
    private FolderModel testParentFolder;
    /** Pre-built ANCESTOR clause for the test parent folder. */
    private String ancestorClause;
    private ContentModel fileWithTermInName;
    private ContentModel fileWithPhraseInContent;
    private ContentModel fileWithTermInTitle;
    private ContentModel fileWithTermInDescription;
    private ContentModel fileWithTermInTag;
    private ContentModel fileWithDifferentTermInName;
    private ContentModel folderWithTermInName;

    @BeforeClass(alwaysRun = true)
    public void dataPreparation() throws Exception
    {
        serverHealth.assertServerIsOnline();

        STEP("Create a dedicated parent folder so all queries can be scoped via ANCESTOR");
        testParentFolder = createTestParentFolder();
        ancestorClause = "ANCESTOR:\"" + testParentFolder.getNodeRef() + "\"";

        STEP("Create a test user and few files and folders containing searched term in name, title, description, content and tag");
        fileWithTermInName = createFile(SEARCH_TERM + ".txt", "some text");
        fileWithPhraseInContent = createFile(getRandomFile(FileType.TEXT_PLAIN), "Dummy " + SEARCH_TERM + " irrelevant text");
        fileWithTermInTitle = createRandomFileWithTitle(SEARCH_TERM);
        fileWithTermInDescription = createRandomFileWithDescription(SEARCH_TERM);
        fileWithTermInTag = createRandomFileWithTag(SEARCH_TERM);
        fileWithDifferentTermInName = createFile("dummy.txt", "content without phrase");
        folderWithTermInName = createFolder(SEARCH_TERM);

        testUser = dataUser.createRandomTestUser();

        STEP("Wait for the batch indexer to index all 6 content files under the test parent folder");
        waitForAllContentToBeIndexed();
    }

    @AfterClass
    public void dataCleanup()
    {
        STEP("Clean up the test parent folder (cascades to all children) and the test user");
        if (testParentFolder != null)
        {
            dataContent.usingAdmin().usingResource(testParentFolder).deleteContent();
        }
        if (testUser != null)
        {
            dataUser.deleteUser(testUser);
        }
    }

    @Test(groups = {TestGroup.SEARCH})
    public void testAftsQuery_simpleTemplate()
    {
        STEP("Search for files by name using simple template with one property");
        Map<String, String> templates = Map.of("_NODE", "%cm:name");
        String query = ancestorClause + " AND TYPE:'cm:content' AND _NODE:" + SEARCH_TERM;
        SearchRequest request = req("afts", query, templates);

        searchQueryService.expectNodeRefsFromQuery(request, testUser, fileWithTermInName.getNodeRef());
    }

    @Test(groups = {TestGroup.SEARCH})
    public void testAftsQuery_simpleTemplateWithPhrase()
    {
        STEP("Search for files containing specific phrase using simple template with one property");
        Map<String, String> templates = Map.of("_NODE", "%TEXT");
        String query = ancestorClause + " AND TYPE:'cm:content' AND _NODE:\"" + SEARCH_TERM + " irrelevant\"";
        SearchRequest request = req("afts", query, templates);

        searchQueryService.expectNodeRefsFromQuery(request, testUser, fileWithPhraseInContent.getNodeRef());
    }

    @Test(groups = {TestGroup.SEARCH})
    public void testAftsQuery_templateWithTwoParameters()
    {
        STEP("Search for files using template containing multiple properties");
        Map<String, String> templates = Map.of("_NODE", "%(cm:name cm:title)");
        String query = ancestorClause + " AND TYPE:'cm:content' AND _NODE:" + SEARCH_TERM;
        SearchRequest request = req("afts", query, templates);

        searchQueryService.expectNodeRefsFromQuery(request, testUser, fileWithTermInName.getNodeRef(), fileWithTermInTitle.getNodeRef());
    }

    @Test(groups = {TestGroup.SEARCH})
    public void testAftsQuery_nestedTemplate()
    {
        Map<String, String> templates = Map.of(
                "_NODE", "%(cm:name cm:title)",
                "_NODET", "%(_NODEX TAG)",
                "_NODEX", "%(_NODE cm:description)"

        );
        STEP("Search for files using more complex two-level nested template containing multiple properties");
        String query = ancestorClause + " AND TYPE:'cm:content' AND _NODEX:" + SEARCH_TERM;
        SearchRequest request = req("afts", query, templates);
        searchQueryService.expectNodeRefsFromQuery(request, testUser,
                fileWithTermInName.getNodeRef(), fileWithTermInDescription.getNodeRef(), fileWithTermInTitle.getNodeRef());

        STEP("Search for files using three-level nested template containing multiple properties");
        String queryIncludingTag = ancestorClause + " AND TYPE:'cm:content' AND _NODET:" + SEARCH_TERM;
        SearchRequest requestIncludingTag = req("afts", queryIncludingTag, templates);
        searchQueryService.expectNodeRefsFromQuery(requestIncludingTag, testUser,
                fileWithTermInName.getNodeRef(), fileWithTermInDescription.getNodeRef(), fileWithTermInTitle.getNodeRef(), fileWithTermInTag.getNodeRef());
    }

    @Test(groups = {TestGroup.SEARCH})
    public void testAftsQuery_templateNameAsQueryDefaultFieldName()
    {
        STEP("Search for files using template containing multiple properties, as a default search field");
        Map<String, String> templates = Map.of("_NODE", "%(cm:name cm:title)");
        // ANCESTOR must be its own clause; only the bare term uses the default field name.
        String query = ancestorClause + " AND TYPE:'cm:content' AND " + SEARCH_TERM;
        SearchRequest request = req("afts", query, templates);
        request.setDefaults(RestRequestDefaultsModel.builder().defaultFieldName("_NODE").create());

        searchQueryService.expectNodeRefsFromQuery(request, testUser, fileWithTermInName.getNodeRef(), fileWithTermInTitle.getNodeRef());
    }

    @Test(groups = {TestGroup.SEARCH})
    public void testAftsQuery_templateWithFixedValue()
    {
        STEP("Search for files and folders using template containing multiple properties, including a fixed one");
        Map<String, String> templates = Map.of("_NODE", "%cm:name AND TYPE:'cm:folder'");
        String query = ancestorClause + " AND _NODE:" + SEARCH_TERM;
        SearchRequest request = req("afts", query, templates);

        searchQueryService.expectNodeRefsFromQuery(request, testUser, folderWithTermInName.getNodeRef());
    }

    @Test(groups = {TestGroup.SEARCH})
    public void testAftsQuery_boostedTemplate()
    {
        STEP("Search for files using templates and boosts, where second term has higher priority ");
        Map<String, String> templates = Map.of("_NODE", "%cm:name");
        String query = ancestorClause + " AND TYPE:'cm:content' AND (_NODE:" + SEARCH_TERM + "^0.5 OR _NODE:dummy^2)";
        SearchRequest request = req("afts", query, templates);
        searchQueryService.expectResultsInOrder(request, testUser, fileWithDifferentTermInName.getName(), fileWithTermInName.getName());

        STEP("Search for files using templates and boosts, where first term has higher priority ");
        String queryInvertedBoost = ancestorClause + " AND TYPE:'cm:content' AND (_NODE:" + SEARCH_TERM + "^2 OR _NODE:dummy^0.5)";
        SearchRequest requestInvertedBoost = req("afts", queryInvertedBoost, templates);
        searchQueryService.expectResultsInOrder(requestInvertedBoost, testUser, fileWithTermInName.getName(), fileWithDifferentTermInName.getName());
    }

    @Test(groups = {TestGroup.SEARCH})
    public void testAftsQuery_expandedTemplate()
    {
        Map<String, String> templates = Map.of("_NODE", "%cm:name");
        String query = ancestorClause + " AND TYPE:'cm:content' AND ~_NODE:" + SEARCH_TERM;
        SearchRequest request = req("afts", query, templates);

        searchQueryService.expectNodeRefsFromQuery(request, testUser, fileWithTermInName.getNodeRef());
    }

    private FolderModel createTestParentFolder()
    {
        ContentModel contentRoot = new ContentModel("-root-");
        contentRoot.setNodeRef(contentRoot.getName());
        FolderModel parent = new FolderModel(getRandomName("templateSearchTests-"));
        return dataContent
                .usingAdmin()
                .usingResource(contentRoot)
                .createFolder(parent);
    }

    private ContentModel createRandomFileWithTitle(String title)
    {
        return createRandomFile(title, null, null);
    }

    private ContentModel createRandomFileWithDescription(String description)
    {
        return createRandomFile(null, description, null);
    }

    private ContentModel createRandomFileWithTag(String tag)
    {
        return createRandomFile(null, null, tag);
    }

    private ContentModel createRandomFile(String title, String description, String tag)
    {
        return createFile(getRandomFile(FileType.TEXT_PLAIN), getRandomName("dummy text "), title, description, tag);
    }

    private ContentModel createFile(String filename, String content)
    {
        return createFile(filename, content, null, null, null);
    }

    private ContentModel createFile(String filename, String content, String title, String description, String tag)
    {
        FileModel fileModel = new FileModel(filename, FileType.TEXT_PLAIN, content);
        fileModel.setTitle(title);
        fileModel.setDescription(description);

        // Create inside the dedicated parent folder so queries scoped with ANCESTOR will pick this up
        // (and so bootstrap / cross-class content does NOT match those queries).
        FileModel file = dataContent
                .usingAdmin()
                .usingResource(testParentFolder)
                .createContent(fileModel);

        if (StringUtils.isNotBlank(tag))
        {
            dataContent
                    .usingAdmin()
                    .usingResource(file)
                    .addTagToContent(RestTagModel.builder().tag(tag).create());
        }

        return file;
    }

    private ContentModel createFolder(String folderName)
    {
        return createFolder(folderName, null, null, null);
    }

    private ContentModel createFolder(String folderName, String title, String description, String tag)
    {
        FolderModel folderModel = new FolderModel(folderName, title, description);

        FolderModel folder = dataContent
                .usingAdmin()
                .usingResource(testParentFolder)
                .createFolder(folderModel);

        if (StringUtils.isNotBlank(tag))
        {
            dataContent
                    .usingAdmin()
                    .usingResource(folder)
                    .addTagToContent(RestTagModel.builder().tag(tag).create());
        }

        return folder;
    }

    /**
     * Block until the batch indexer has indexed every content node we just created
     */
    private void waitForAllContentToBeIndexed()
    {
        SearchRequest probe = req("afts", ancestorClause + " AND TYPE:'cm:content'", Map.of());
        // 6 files (the folder is excluded by TYPE:'cm:content').
        String[] expectedRefs = new String[] {
                fileWithTermInName.getNodeRef(),
                fileWithPhraseInContent.getNodeRef(),
                fileWithTermInTitle.getNodeRef(),
                fileWithTermInDescription.getNodeRef(),
                fileWithTermInTag.getNodeRef(),
                fileWithDifferentTermInName.getNodeRef()
        };
        AssertionError last = null;
        for (int attempt = 1; attempt <= 6; attempt++)
        {
            try
            {
                searchQueryService.expectNodeRefsFromQuery(probe, dataUser.getAdminUser(), expectedRefs);
                return;
            }
            catch (AssertionError e)
            {
                last = e;
                STEP("Indexing barrier attempt " + attempt + " still incomplete; retrying. " + e.getMessage());
            }
        }
        throw new AssertionError(
                "Batch indexer did not index all " + expectedRefs.length
                        + " content nodes under test folder " + testParentFolder.getNodeRef()
                        + " within the barrier timeout. Last error: " + (last == null ? "none" : last.getMessage()),
                last);
    }
}
