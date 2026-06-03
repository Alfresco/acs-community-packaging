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

        STEP("Create a test user and few files and folders containing searched term in name, title, description, content and tag");
        fileWithTermInName = createFile(SEARCH_TERM + ".txt", "some text");
        fileWithPhraseInContent = createFile(getRandomFile(FileType.TEXT_PLAIN), "Dummy " + SEARCH_TERM + " irrelevant text");
        fileWithTermInTitle = createRandomFileWithTitle(SEARCH_TERM);
        fileWithTermInDescription = createRandomFileWithDescription(SEARCH_TERM);
        fileWithTermInTag = createRandomFileWithTag(SEARCH_TERM);
        fileWithDifferentTermInName = createFile("dummy.txt", "content without phrase");
        folderWithTermInName = createFolder(SEARCH_TERM);

        testUser = dataUser.createRandomTestUser();
    }

    @AfterClass
    public void dataCleanup()
    {
        STEP("Clean up created files, folders and user");
        dataContent.usingAdmin().usingResource(fileWithTermInName).deleteContent();
        dataContent.usingAdmin().usingResource(fileWithPhraseInContent).deleteContent();
        dataContent.usingAdmin().usingResource(fileWithTermInTitle).deleteContent();
        dataContent.usingAdmin().usingResource(fileWithTermInDescription).deleteContent();
        dataContent.usingAdmin().usingResource(fileWithTermInTag).deleteContent();
        dataContent.usingAdmin().usingResource(fileWithDifferentTermInName).deleteContent();
        dataContent.usingAdmin().usingResource(folderWithTermInName).deleteContent();
        dataUser.deleteUser(testUser);
    }

    @Test(groups = {TestGroup.SEARCH})
    public void testAftsQuery_simpleTemplate()
    {
        STEP("Search for files by name using simple template with one property");
        Map<String, String> templates = Map.of("_NODE", "%cm:name");
        String query = "TYPE:'cm:content' AND _NODE:" + SEARCH_TERM;
        SearchRequest request = req("afts", query, templates);

        searchQueryService.expectNodeRefsFromQuery(request, testUser, fileWithTermInName.getNodeRef());
    }

    @Test(groups = {TestGroup.SEARCH})
    public void testAftsQuery_simpleTemplateWithPhrase()
    {
        STEP("Search for files containing specific phrase using simple template with one property");
        Map<String, String> templates = Map.of("_NODE", "%TEXT");
        String query = "TYPE:'cm:content' AND _NODE:\"" + SEARCH_TERM + " irrelevant\"";
        SearchRequest request = req("afts", query, templates);

        searchQueryService.expectNodeRefsFromQuery(request, testUser, fileWithPhraseInContent.getNodeRef());
    }

    @Test(groups = {TestGroup.SEARCH})
    public void testAftsQuery_templateWithTwoParameters()
    {
        STEP("Search for files using template containing multiple properties");
        Map<String, String> templates = Map.of("_NODE", "%(cm:name cm:title)");
        String query = "TYPE:'cm:content' AND _NODE:" + SEARCH_TERM;
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
        String query = "TYPE:'cm:content' AND _NODEX:" + SEARCH_TERM;
        SearchRequest request = req("afts", query, templates);
        searchQueryService.expectNodeRefsFromQuery(request, testUser,
                fileWithTermInName.getNodeRef(), fileWithTermInDescription.getNodeRef(), fileWithTermInTitle.getNodeRef());

        STEP("Search for files using three-level nested template containing multiple properties");
        String queryIncludingTag = "TYPE:'cm:content' AND _NODET:" + SEARCH_TERM;
        SearchRequest requestIncludingTag = req("afts", queryIncludingTag, templates);
        searchQueryService.expectNodeRefsFromQuery(requestIncludingTag, testUser,
                fileWithTermInName.getNodeRef(), fileWithTermInDescription.getNodeRef(), fileWithTermInTitle.getNodeRef(), fileWithTermInTag.getNodeRef());
    }

    @Test(groups = {TestGroup.SEARCH})
    public void testAftsQuery_templateNameAsQueryDefaultFieldName()
    {
        STEP("Search for files using template containing multiple properties, as a default search field");
        Map<String, String> templates = Map.of("_NODE", "%(cm:name cm:title)");
        String query = "TYPE:'cm:content' AND " + SEARCH_TERM;
        SearchRequest request = req("afts", query, templates);
        request.setDefaults(RestRequestDefaultsModel.builder().defaultFieldName("_NODE").create());

        searchQueryService.expectNodeRefsFromQuery(request, testUser, fileWithTermInName.getNodeRef(), fileWithTermInTitle.getNodeRef());
    }

    @Test(groups = {TestGroup.SEARCH})
    public void testAftsQuery_templateWithFixedValue()
    {
        STEP("Search for files and folders using template containing multiple properties, including a fixed one");
        Map<String, String> templates = Map.of("_NODE", "%cm:name AND TYPE:'cm:folder'");
        String query = "_NODE:" + SEARCH_TERM;
        SearchRequest request = req("afts", query, templates);

        searchQueryService.expectNodeRefsFromQuery(request, testUser, folderWithTermInName.getNodeRef());
    }

    @Test(groups = {TestGroup.SEARCH})
    public void testAftsQuery_boostedTemplate()
    {
        STEP("Search for files using templates and boosts, where second term has higher priority ");
        Map<String, String> templates = Map.of("_NODE", "%cm:name");
        String query = "TYPE:'cm:content' AND _NODE:" + SEARCH_TERM + "^0.5 OR _NODE:dummy^2";
        SearchRequest request = req("afts", query, templates);
        searchQueryService.expectResultsInOrder(request, testUser, fileWithDifferentTermInName.getName(), fileWithTermInName.getName());

        STEP("Search for files using templates and boosts, where first term has higher priority ");
        String queryInvertedBoost = "TYPE:'cm:content' AND _NODE:" + SEARCH_TERM + "^2 OR _NODE:dummy^0.5";
        SearchRequest requestInvertedBoost = req("afts", queryInvertedBoost, templates);
        searchQueryService.expectResultsInOrder(requestInvertedBoost, testUser, fileWithTermInName.getName(), fileWithDifferentTermInName.getName());
    }

    @Test(groups = {TestGroup.SEARCH})
    public void testAftsQuery_expandedTemplate()
    {
        Map<String, String> templates = Map.of("_NODE", "%cm:name");
        String query = "TYPE:'cm:content' AND ~_NODE:" + SEARCH_TERM;
        SearchRequest request = req("afts", query, templates);

        searchQueryService.expectNodeRefsFromQuery(request, testUser, fileWithTermInName.getNodeRef());
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
        ContentModel contentRoot = new ContentModel("-root-");
        contentRoot.setNodeRef(contentRoot.getName());
        FileModel fileModel = new FileModel(filename, FileType.TEXT_PLAIN, content);
        fileModel.setTitle(title);
        fileModel.setDescription(description);

        FileModel file = dataContent
                .usingAdmin()
                .usingResource(contentRoot)
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
        ContentModel contentRoot = new ContentModel("-root-");
        contentRoot.setNodeRef(contentRoot.getName());
        FolderModel folderModel = new FolderModel(folderName, title, description);

        FolderModel folder = dataContent
                .usingAdmin()
                .usingResource(contentRoot)
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
}
