package org.alfresco.elasticsearch.reindexing;

import org.alfresco.dataprep.AlfrescoHttpClientFactory;
import org.alfresco.elasticsearch.SearchQueryService;
import org.alfresco.rest.core.RestWrapper;
import org.alfresco.rest.model.RestTagModel;
import org.alfresco.rest.search.SearchRequest;
import org.alfresco.tas.AlfrescoStackInitializer;
import org.alfresco.utility.data.DataContent;
import org.alfresco.utility.data.DataUser;
import org.alfresco.utility.model.*;
import org.alfresco.utility.network.ServerHealth;
import org.alfresco.utility.report.log.Step;
import org.alfresco.utility.testrail.ExecutionType;
import org.alfresco.utility.testrail.annotation.TestRail;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.*;
import java.util.stream.Collectors;

import static org.alfresco.elasticsearch.SearchQueryService.req;
import static org.springframework.http.HttpStatus.OK;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

@ContextConfiguration(locations = "classpath:alfresco-elasticsearch-context.xml",
        initializers = AlfrescoStackInitializer.class)
public class ElasticsearchTagIndexingTests extends AbstractTestNGSpringContextTests
{
    private static final Iterable<String> LANGUAGES_TO_CHECK = List.of("afts", "lucene");

    @Autowired
    ServerHealth serverHealth;

    @Autowired
    DataUser dataUser;

    @Autowired
    DataContent dataContent;

    @Autowired
    private AlfrescoHttpClientFactory alfrescoHttpClientFactory;

    @Autowired
    SearchQueryService searchQueryService;

    @Autowired
    protected RestWrapper restClient;

    private UserModel testUser;
    private FolderModel testFolder;

    @BeforeClass(alwaysRun = true)
    public void dataPreparation()
    {
        serverHealth.assertServerIsOnline();

        testUser = dataUser.createRandomTestUser();
        testFolder = dataContent
                .usingAdmin()
                .usingResource(contentRoot())
                .createFolder(new FolderModel(unique("FOLDER")));
    }

    @TestRail(section = {TestGroup.SEARCH, TestGroup.TAGS}, executionType = ExecutionType.REGRESSION,
            description = "Verify the TAG queries work correctly")
    @Test(groups = {TestGroup.SEARCH, TestGroup.TAGS, TestGroup.REGRESSION})
    public void testTAGUseCases()
    {
        final String tag1 = unique("TAG1");

        // No result for not existing tag
        assertTagQueryResult(tag1, List.of());

        // Tag first file - tag is created - expect single result
        final FileModel file1 = givenFile("test1");
        tagContent(file1, tag1);
        assertTagQueryResult(tag1, List.of(file1));

        // Tag another file - tag is reused - expect two items in the result
        final FileModel file2 = givenFile("test2");
        tagContent(file2, tag1);
        assertTagQueryResult(tag1, List.of(file1, file2));

        final String tag2 = unique("TAG2");

        // Just a sanity check - No result for the second tag
        assertTagQueryResult(tag2, List.of());

        // Tag second file with a new tag
        final String tag2Id = tagContent(file2, tag2);
        assertTagQueryResult(tag1, List.of(file1, file2));
        assertTagQueryResult(tag2, List.of(file2));

        // Tag new file with the second tag
        final FileModel file3 = givenFile("test3");
        tagContent(file3, tag2);
        assertTagQueryResult(tag1, List.of(file1, file2));
        assertTagQueryResult(tag2, List.of(file2, file3));

        // test disjunction and conjunction
        assertTagQueryResult(tag1, "OR", tag2, List.of(file1, file2, file3));
        assertTagQueryResult(tag1, "AND", tag2, List.of(file2));
        final String unknownTag = unique("unknown");
        assertTagQueryResult(tag1, "OR", unknownTag, List.of(file1, file2));
        assertTagQueryResult(tag1, "AND", unknownTag, List.of());

        // Delete file
        deleteFile(file1);
        assertTagQueryResult(tag1, List.of(file2));
        assertTagQueryResult(tag2, List.of(file2, file3));

        // Rename tag
        final String newTag2 = unique("NEW-TAG2");
        assertTagQueryResult(newTag2, List.of());
        renameTag(tag2Id, newTag2);
        assertTagQueryResult(tag1, List.of(file2));
        assertTagQueryResult(newTag2, List.of(file2, file3));
        assertTagQueryResult(tag2, List.of());

        // Delete tag
        deleteTag(file3, newTag2);
        assertTagQueryResult(tag1, List.of(file2));
        assertTagQueryResult(tag2, List.of());
        assertTagQueryResult(newTag2, List.of(file2));
    }

    private void assertTagQueryResult(String tag1Name, String operator, String tag2Name, Collection<ContentModel> contentModels)
    {
        final List<String> contentNames = contentModels
                .stream()
                .map(ContentModel::getName)
                .collect(Collectors.toList());

        for (final String tag1 : tagVariantsToCheck(tag1Name))
        {
            for (final String tag2 : tagVariantsToCheck(tag2Name))
            {
                for (final String language : LANGUAGES_TO_CHECK)
                {
                    Step.STEP("Searching for TAG `" + tag1 + "` " + operator + " `" + tag2 + "` using `" + language + "` language.");
                    final SearchRequest query = req(language, "TAG:" + tag1 + " " + operator + " TAG:" + tag2);
                    if (contentNames.isEmpty())
                    {
                        searchQueryService.expectNoResultsFromQuery(query, testUser);
                    }
                    else
                    {
                        Collections.shuffle(contentNames);
                        searchQueryService.expectResultsFromQuery(query, testUser, contentNames.toArray(String[]::new));
                    }
                }
            }
        }
    }

    private void assertTagQueryResult(String tagName, Collection<ContentModel> contentModels)
    {
        final List<String> contentNames = contentModels
                .stream()
                .map(ContentModel::getName)
                .collect(Collectors.toList());

        for (final String tag : tagVariantsToCheck(tagName))
        {
            for (final String language : LANGUAGES_TO_CHECK)
            {
                Step.STEP("Searching for TAG `" + tag + "` using `" + language + "` language.");
                final SearchRequest query = req(language, "TAG:" + tag);
                if (contentNames.isEmpty())
                {
                    searchQueryService.expectNoResultsFromQuery(query, testUser);
                }
                else
                {
                    Collections.shuffle(contentNames);
                    searchQueryService.expectResultsFromQuery(query, testUser, contentNames.toArray(String[]::new));
                }
            }
        }
    }

    private Iterable<String> tagVariantsToCheck(String tagName)
    {
        return List.of(
                tagName.toLowerCase(Locale.ROOT),
                tagName.toUpperCase(Locale.ROOT),
                "\"" + tagName.toLowerCase(Locale.ROOT) + "\"",
                "\"" + tagName.toUpperCase(Locale.ROOT) + "\"");
    }

    private FileModel givenFile(final String fileName)
    {
        final FileModel file = new FileModel(unique(fileName), FileType.TEXT_PLAIN, "Content for " + fileName);
        return dataContent
                .usingAdmin()
                .usingResource(testFolder)
                .createContent(file);
    }

    private String tagContent(ContentModel contentModel, String tag)
    {
        var tagModel = new TagModel(tag);
        dataContent
                .usingAdmin()
                .usingResource(contentModel)
                .addTagToContent(tagModel);

        final String tagNodeId = dataContent.getContentActions().getTagNodeRef(
                dataContent.getCurrentUser().getUsername(), dataContent.getCurrentUser().getPassword(),
                contentModel.getCmisLocation(), tag);
        assertNotNull(tagNodeId, "Tag node ref must exist.");
        return tagNodeId;
    }

    private void deleteFile(ContentModel contentModel)
    {
        dataContent
                .usingAdmin()
                .usingResource(contentModel)
                .deleteContent();
    }

    private void deleteTag(ContentModel contentModel, String tagName)
    {
        boolean deleted = dataContent
                .usingAdmin()
                .getContentActions()
                .removeTag(
                        dataContent.getCurrentUser().getUsername(), dataContent.getCurrentUser().getPassword(),
                        contentModel.getCmisLocation(), tagName);
        assertTrue(deleted, "Tag should be deleted.");
    }

    private void renameTag(String tagId, String newName)
    {
        RestTagModel tag = RestTagModel.builder().id(tagId).create();
        RestTagModel update = restClient.authenticateUser(dataContent.getAdminUser()).withCoreAPI().usingTag(tag).update(newName);
        restClient.assertStatusCodeIs(OK);
    }

    private static ContentModel contentRoot()
    {
        final ContentModel root = new ContentModel("-root-");
        root.setNodeRef(root.getName());
        return root;
    }

    private static String unique(String prefix)
    {
        return prefix + "-" + UUID.randomUUID();
    }
}
