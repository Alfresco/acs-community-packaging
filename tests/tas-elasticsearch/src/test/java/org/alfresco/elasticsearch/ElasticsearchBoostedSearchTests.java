package org.alfresco.elasticsearch;

import static org.apache.commons.lang3.StringUtils.EMPTY;

import static org.alfresco.elasticsearch.SearchQueryService.req;
import static org.alfresco.utility.data.RandomData.getRandomFile;
import static org.alfresco.utility.data.RandomData.getRandomName;
import static org.alfresco.utility.report.log.Step.STEP;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import org.alfresco.rest.model.RestTagModel;
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
public class ElasticsearchBoostedSearchTests extends AbstractTestNGSpringContextTests
{
    private static final String SEARCH_TERM = "mountain";
    private static final String DIFFERENT_SEARCH_TERM = "fountain";

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
    private ContentModel fileWithDifferentTermInName;
    private ContentModel fileWithPhraseInContent;
    private ContentModel fileWithTermInTitle;
    private ContentModel folderWithTermInName;
    private ContentModel folderWithTermInTitle;
    private LocalDateTime creationTime;
    private LocalDateTime afterCreationTime;

    @BeforeClass(alwaysRun = true)
    public void dataPreparation()
    {
        serverHealth.assertServerIsOnline();

        STEP("Create a test user and few files and folders containing searched term in name, title and content");
        testUser = dataUser.createRandomTestUser();
        fileWithTermInName = createFile(SEARCH_TERM + ".txt", "dummy content");
        fileWithDifferentTermInName = createFile(DIFFERENT_SEARCH_TERM + ".txt", "dummy other content");
        fileWithPhraseInContent = createFile(getRandomFile(FileType.TEXT_PLAIN), "content with " + SEARCH_TERM + " searched phrase");
        folderWithTermInName = createFolder(SEARCH_TERM);
        creationTime = ZonedDateTime.now(Clock.system(ZoneOffset.UTC)).toLocalDateTime();
        fileWithTermInTitle = createRandomFileWithTitle(SEARCH_TERM);
        folderWithTermInTitle = createRandomFolderWithTitle(SEARCH_TERM);
        afterCreationTime = ZonedDateTime.now(Clock.system(ZoneOffset.UTC)).toLocalDateTime();
    }

    @AfterClass
    public void dataCleanup()
    {
        STEP("Clean up created files, folders and user");
        dataContent.usingAdmin().usingResource(fileWithTermInName).deleteContent();
        dataContent.usingAdmin().usingResource(fileWithDifferentTermInName).deleteContent();
        dataContent.usingAdmin().usingResource(fileWithPhraseInContent).deleteContent();
        dataContent.usingAdmin().usingResource(folderWithTermInName).deleteContent();
        dataContent.usingAdmin().usingResource(fileWithTermInTitle).deleteContent();
        dataContent.usingAdmin().usingResource(folderWithTermInTitle).deleteContent();
        dataUser.deleteUser(testUser);
    }

    @Test(groups = {TestGroup.SEARCH})
    public void testAftsQuery_simpleTermBoost()
    {
        STEP("Search for files and folders by name with higher priority for files");
        String boostedQuery = "TYPE:('cm:content'^2 OR 'cm:folder'^0.5) AND cm:name:" + SEARCH_TERM;
        SearchRequest searchRequest = req("afts", boostedQuery);
        searchQueryService.expectResultsInOrder(searchRequest, testUser, fileWithTermInName.getName(), folderWithTermInName.getName());

        STEP("Search for files and folders by name with higher priority for folders");
        String invertedBoost = "TYPE:('cm:content'^0.5 OR 'cm:folder'^2) AND cm:name:" + SEARCH_TERM;
        searchRequest = req("afts", invertedBoost);
        searchQueryService.expectResultsInOrder(searchRequest, testUser, folderWithTermInName.getName(), fileWithTermInName.getName());
    }

    @Test(groups = {TestGroup.SEARCH})
    public void testAftsQuery_complexTermBoost()
    {
        STEP("Search for files and folders by name or title with higher priority for files by name");
        String boostedQuery1 = "TYPE:('cm:content'^4 OR 'cm:folder'^0.5)^4 AND (cm:name:" + SEARCH_TERM + "^3.5 OR cm:title:" + SEARCH_TERM + "^0.05)";
        SearchRequest searchRequest = req("afts", boostedQuery1);
        searchQueryService.expectResultsInOrder(searchRequest, testUser, fileWithTermInName.getName(), fileWithTermInTitle.getName(), folderWithTermInName.getName(), folderWithTermInTitle.getName());

        STEP("Search for files and folders by name or title with higher priority for folders by name");
        String boostedQuery2 = "TYPE:('cm:content'^0.5 OR 'cm:folder'^4)^4 AND (cm:name:" + SEARCH_TERM + "^3.5 OR cm:title:" + SEARCH_TERM + "^0.05)";
        searchRequest = req("afts", boostedQuery2);
        searchQueryService.expectResultsInOrder(searchRequest, testUser, folderWithTermInName.getName(), folderWithTermInTitle.getName(), fileWithTermInName.getName(), fileWithTermInTitle.getName());

        STEP("Search for files and folders by name or title with higher priority for files by title");
        String boostedQuery3 = "TYPE:('cm:content'^4 OR 'cm:folder'^0.5)^6 AND (cm:name:" + SEARCH_TERM + "^0.05 OR cm:title:" + SEARCH_TERM + "^3.5)";
        searchRequest = req("afts", boostedQuery3);
        searchQueryService.expectResultsInOrder(searchRequest, testUser, fileWithTermInTitle.getName(), fileWithTermInName.getName(), folderWithTermInTitle.getName(), folderWithTermInName.getName());

        STEP("Search for files and folders by name or title with higher priority for folders by title");
        String boostedQuery4 = "TYPE:('cm:content'^0.5 OR 'cm:folder'^4)^6 AND (cm:name:" + SEARCH_TERM + "^0.05 OR cm:title:" + SEARCH_TERM + "^3.5)";
        searchRequest = req("afts", boostedQuery4);
        searchQueryService.expectResultsInOrder(searchRequest, testUser, folderWithTermInTitle.getName(), folderWithTermInName.getName(), fileWithTermInTitle.getName(), fileWithTermInName.getName());
    }

    @Test(groups = {TestGroup.SEARCH})
    public void testAftsQuery_phraseBoost()
    {
        STEP("Search for files by name or TEXT with higher priority for name filter");
        String boostedQuery = "TYPE:'cm:content' AND (cm:name:" + SEARCH_TERM + "^2 OR TEXT:'" + SEARCH_TERM + " searched'^0.1)";
        SearchRequest searchRequest = req("afts", boostedQuery);
        searchQueryService.expectResultsInOrder(searchRequest, testUser, fileWithTermInName.getName(), fileWithPhraseInContent.getName());

        STEP("Search for files by name or TEXT with higher priority for TEXT filter");
        String invertedBoost = "TYPE:'cm:content' AND (cm:name:" + SEARCH_TERM + "^0.1 OR TEXT:'" + SEARCH_TERM + " searched'^2)";
        searchRequest = req("afts", invertedBoost);
        searchQueryService.expectResultsInOrder(searchRequest, testUser, fileWithPhraseInContent.getName(), fileWithTermInName.getName());
    }

    @Test(groups = {TestGroup.SEARCH})
    public void testAftsQuery_exactTermBoost()
    {
        STEP("Search for files by exact name or content with higher priority for exact name filter");
        String boostedQuery = "TYPE:'cm:content' AND (=cm:name:" + SEARCH_TERM + ".txt^2 OR =cm:content:" + SEARCH_TERM + "^0.5)";
        SearchRequest searchRequest = req("afts", boostedQuery);
        searchQueryService.expectResultsInOrder(searchRequest, testUser, fileWithTermInName.getName(), fileWithPhraseInContent.getName());

        STEP("Search for files by exact name or content with higher priority for exact content filter");
        String invertedBoost = "TYPE:'cm:content' AND (=cm:name:" + SEARCH_TERM + ".txt^0.1 OR =cm:content:" + SEARCH_TERM + "^3)";
        searchRequest = req("afts", invertedBoost);
        searchQueryService.expectResultsInOrder(searchRequest, testUser, fileWithPhraseInContent.getName(), fileWithTermInName.getName());
    }

    @Test(groups = {TestGroup.SEARCH})
    public void testAftsQuery_expandedTermBoost()
    {
        STEP("Search for files by expanded name and two different terms with higher priority for first term");
        String boostedQuery = "TYPE:'cm:content' AND (~cm:name:" + SEARCH_TERM + "^3 OR ~cm:name:" + DIFFERENT_SEARCH_TERM + "^0.1)";
        SearchRequest searchRequest = req("afts", boostedQuery);
        searchQueryService.expectResultsInOrder(searchRequest, testUser, fileWithTermInName.getName(), fileWithDifferentTermInName.getName());

        STEP("Search for files by expanded name and two different terms with higher priority for second term");
        String invertedBoost = "TYPE:'cm:content' AND (~cm:name:" + SEARCH_TERM + "^0.1 OR ~cm:name:" + DIFFERENT_SEARCH_TERM + "^3)";
        searchRequest = req("afts", invertedBoost);
        searchQueryService.expectResultsInOrder(searchRequest, testUser, fileWithDifferentTermInName.getName(), fileWithTermInName.getName());
    }

    @Test(groups = {TestGroup.SEARCH})
    public void testAftsQuery_fuzzyMatchingBoost()
    {
        STEP("Fuzzy matching search for files by name or title with higher priority for fuzzy name filter");
        String boostedQuery = "TYPE:'cm:content' AND (cm:name:" + SEARCH_TERM + "~0.7^3 OR cm:title:" + SEARCH_TERM + "^0.01)";
        SearchRequest searchRequest = req("afts", boostedQuery);
        searchQueryService.expectResultsInOrder(searchRequest, testUser, fileWithTermInName.getName(), fileWithDifferentTermInName.getName(), fileWithTermInTitle.getName());

        STEP("Fuzzy matching search for files by name or title with higher priority for fuzzy title filter");
        String invertedBoost = "TYPE:'cm:content' AND (cm:name:" + SEARCH_TERM + "~0.7^0.01 OR cm:title:" + SEARCH_TERM + "^3)";
        searchRequest = req("afts", invertedBoost);
        searchQueryService.expectResultsInOrder(searchRequest, testUser, fileWithTermInTitle.getName(), fileWithTermInName.getName(), fileWithDifferentTermInName.getName());
    }

    /**
     * Verify if boosts works fine with words proximity search. Files containing terms within specific distance from another one should be returned.
     */
    @Test(groups = {TestGroup.SEARCH})
    public void testAftsQuery_proximitySearchBoost()
    {
        STEP("Search for files by name or proximity TEXT with higher priority for name filter");
        String boostedQuery = "TYPE:'cm:content' AND (cm:name:" + SEARCH_TERM + "^5 OR TEXT:(" + SEARCH_TERM + " *(1) phrase)^0.1)";
        SearchRequest searchRequest = req("afts", boostedQuery);
        searchQueryService.expectResultsInOrder(searchRequest, testUser, fileWithTermInName.getName(), fileWithPhraseInContent.getName());

        STEP("Search for files by name or proximity TEXT with higher priority for proximity TEXT filter");
        String invertedBoost = "TYPE:'cm:content' AND (cm:name:" + SEARCH_TERM + "^0.1 OR TEXT:(" + SEARCH_TERM + " *(1) phrase)^5)";
        searchRequest = req("afts", invertedBoost);
        searchQueryService.expectResultsInOrder(searchRequest, testUser, fileWithPhraseInContent.getName(), fileWithTermInName.getName());
    }

    @Test(groups = {TestGroup.SEARCH})
    public void testAftsQuery_dateRangeSearchBoost()
    {
        String timeFrom = creationTime.format(DateTimeFormatter.ISO_DATE_TIME);
        String timeTo = afterCreationTime.format(DateTimeFormatter.ISO_DATE_TIME);

        STEP("Search for files and folders by name or creation time range with higher priority for name filter");
        String boostedQuery = "TYPE:('cm:content'^3 OR 'cm:folder') AND (cm:name:" + SEARCH_TERM + "^4 OR cm:created:['" + timeFrom + "' TO '" + timeTo + "']^0.1)^3";
        SearchRequest searchRequest = req("afts", boostedQuery);
        searchQueryService.expectResultsInOrder(searchRequest, testUser, fileWithTermInName.getName(), folderWithTermInName.getName(), fileWithTermInTitle.getName(), folderWithTermInTitle.getName());

        STEP("Search for files and folders by name or creation time range with higher priority for creation time range filter");
        String invertedBoost = "TYPE:('cm:content' OR 'cm:folder'^3) AND (cm:name:" + SEARCH_TERM + "^0.1 OR cm:created:['" + timeFrom + "' TO '" + timeTo + "']^4)^3";
        searchRequest = req("afts", invertedBoost);
        searchQueryService.expectResultsInOrder(searchRequest, testUser, folderWithTermInTitle.getName(), fileWithTermInTitle.getName(), folderWithTermInName.getName(), fileWithTermInName.getName());
    }

    /**
     * Verify if boosts works fine with words range search. Files containing words from alphabetical range (from 'mountain' to 'phrase', this includes word 'other') should be returned.
     */
    @Test(groups = {TestGroup.SEARCH})
    public void testAftsQuery_wordsRangeSearchBoost()
    {
        STEP("Search for files by name or words in content from given range with higher priority for name filter");
        String boostedQuery = "TYPE:'cm:content' AND (cm:name:" + SEARCH_TERM + "^3 OR cm:content:" + SEARCH_TERM + "..phrase^0.1)";
        SearchRequest searchRequest = req("afts", boostedQuery);
        searchQueryService.expectResultsStartingWithOneOf(searchRequest, testUser, fileWithTermInName.getName());
        searchQueryService.expectResultsFromQuery(searchRequest, testUser, fileWithTermInName.getName(), fileWithPhraseInContent.getName(), fileWithDifferentTermInName.getName());

        STEP("Search for files by name or words in content from given range with higher priority for words in content from given range filter");
        String invertedBoost = "TYPE:'cm:content' AND (cm:name:" + SEARCH_TERM + "^0.1 OR cm:content:" + SEARCH_TERM + "..phrase^3)";
        searchRequest = req("afts", invertedBoost);
        searchQueryService.expectResultsStartingWithOneOf(searchRequest, testUser, fileWithPhraseInContent.getName(), fileWithDifferentTermInName.getName());
        searchQueryService.expectResultsFromQuery(searchRequest, testUser, fileWithTermInName.getName(), fileWithPhraseInContent.getName(), fileWithDifferentTermInName.getName());
    }

    @Test(groups = {TestGroup.SEARCH})
    public void testAftsQuery_wildcardSearchBoost()
    {
        STEP("Search for files by wildcard name or title with higher priority for wildcard name filter");
        String wildcardTerm = SEARCH_TERM.replaceFirst("^.", "?");
        String boostedQuery = "TYPE:'cm:content' AND (cm:name:" + wildcardTerm + "^3 OR cm:title:" + SEARCH_TERM + "^0.1)";
        SearchRequest searchRequest = req("afts", boostedQuery);
        searchQueryService.expectResultsStartingWithOneOf(searchRequest, testUser, fileWithTermInName.getName(), fileWithDifferentTermInName.getName());
        searchQueryService.expectResultsFromQuery(searchRequest, testUser, fileWithTermInName.getName(), fileWithDifferentTermInName.getName(), fileWithTermInTitle.getName());

        STEP("Search for files by wildcard name or title with higher priority for wildcard title filter");
        wildcardTerm = SEARCH_TERM.replaceFirst("^.", "*");
        String invertedBoost = "TYPE:'cm:content' AND (cm:name:" + wildcardTerm + "^0.1 OR cm:title:" + SEARCH_TERM + "^3)";
        searchRequest = req("afts", invertedBoost);
        searchQueryService.expectResultsStartingWithOneOf(searchRequest, testUser, fileWithTermInTitle.getName());
        searchQueryService.expectResultsFromQuery(searchRequest, testUser, fileWithTermInName.getName(), fileWithDifferentTermInName.getName(), fileWithTermInTitle.getName());
    }

    @Test(groups = {TestGroup.SEARCH})
    public void testAftsQuery_invalidNegativeBoost()
    {
        STEP("Try to search for files by name using negative boost and expect 500 error response");
        String boostedQuery = "TYPE:'cm:content'^-2 AND cm:name:" + SEARCH_TERM;
        SearchRequest searchRequest = req("afts", boostedQuery);
        searchQueryService.expectErrorFromQuery(searchRequest, testUser, HttpStatus.INTERNAL_SERVER_ERROR, EMPTY);
    }

    private ContentModel createRandomFileWithTitle(String title)
    {
        return createRandomFile(title, null, null);
    }

    private ContentModel createRandomFile(String title, String description, String tag)
    {
        return createFile(getRandomFile(FileType.TEXT_PLAIN), "dummy content", title, description, tag);
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

    private ContentModel createRandomFolderWithTitle(String title)
    {
        return createFolder(getRandomName("folder"), title, null, null);
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
