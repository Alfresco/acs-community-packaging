package org.alfresco.elasticsearch.parallel;

import static java.util.Arrays.stream;
import static java.util.stream.Collectors.joining;

import static org.alfresco.elasticsearch.SearchQueryService.req;

import java.util.Arrays;
import jakarta.json.Json;
import jakarta.json.JsonObject;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import org.alfresco.elasticsearch.SearchQueryService;
import org.alfresco.elasticsearch.utility.ElasticsearchRESTHelper;
import org.alfresco.rest.core.RestWrapper;
import org.alfresco.rest.model.RestCategoryModel;
import org.alfresco.rest.model.RestNodeBodyMoveCopyModel;
import org.alfresco.rest.model.RestNodeModel;
import org.alfresco.rest.search.SearchRequest;
import org.alfresco.tas.AlfrescoStackInitializer;
import org.alfresco.utility.data.DataUser;
import org.alfresco.utility.model.ContentModel;
import org.alfresco.utility.model.FileModel;
import org.alfresco.utility.model.FolderModel;
import org.alfresco.utility.model.SiteModel;
import org.alfresco.utility.model.UserModel;
import org.alfresco.utility.network.ServerHealth;
import org.alfresco.utility.report.log.Step;

/**
 * Tests to check that paths are updated correctly.
 * <p>
 * Path updates require waiting for an index refresh to happen and so have been designed to be run in parallel.
 */
@ContextConfiguration(locations = "classpath:alfresco-elasticsearch-context.xml",
        initializers = AlfrescoStackInitializer.class)
public class PathUpdateTests extends AbstractTestNGSpringContextTests
{
    @Autowired
    private ServerHealth serverHealth;
    @Autowired
    private DataUser dataUser;
    @Autowired
    protected RestWrapper restClient;
    @Autowired
    private ElasticsearchRESTHelper helper;
    @Autowired
    protected SearchQueryService searchQueryService;

    private UserModel testUser;
    private SiteModel testSite;

    /**
     * Create a user and a private site containing some nested folders with a document in.
     */
    @BeforeClass(alwaysRun = true)
    public void dataPreparation()
    {
        serverHealth.isServerReachable();
        serverHealth.assertServerIsOnline();

        Step.STEP("Create a test user and private site.");
        testUser = dataUser.createRandomTestUser();
        testSite = helper.createPrivateSite(testUser);
    }

    @Test
    public void testChangeFileNameUpdatesPath()
    {
        Step.STEP("Create a file in the site.");
        FileModel testFile = helper.createFileInSite(testUser, testSite);

        Step.STEP("Update the filename.");
        RestNodeModel updatedFile = renameNode(testFile);

        Step.STEP("Check the path is updated");
        SearchRequest query = req("PATH:\"" + pathInSite(testSite, updatedFile.getName()) + "\"");
        searchQueryService.expectResultsFromQuery(query, testUser, updatedFile.getName());
    }

    @Test
    public void testChangeFileParentUpdatesPath()
    {
        Step.STEP("Create a file next to a folder.");
        FileModel testFile = helper.createFileInSite(testUser, testSite);
        FolderModel testFolder = helper.createFolderInSite(testUser, testSite);

        Step.STEP("Move the file into the folder and check the path is updated.");
        moveNode(testFile, testFolder);

        Step.STEP("Check the path is updated");
        SearchRequest query = req("PATH:\"" + pathInSite(testSite, testFolder.getName(), testFile.getName()) + "\"");
        searchQueryService.expectResultsFromQuery(query, testUser, testFile.getName());
    }

    @Test
    public void testChangeFolderNameUpdatesPath()
    {
        Step.STEP("Create a file in a folder.");
        FolderModel testFolder = helper.createFolderInSite(testUser, testSite);
        FileModel testFile = helper.createFileInFolder(testUser, testFolder);

        Step.STEP("Update the folder's name.");
        RestNodeModel updatedFolder = renameNode(testFolder);

        Step.STEP("Check the path is updated");
        SearchRequest query = req("PATH:\"" + pathInSite(testSite, updatedFolder.getName(), testFile.getName()) + "\"");
        searchQueryService.expectResultsFromQuery(query, testUser, testFile.getName());
    }

    @Test
    public void testChangeFolderParentUpdatesPath()
    {
        Step.STEP("Create two folders with a file in the first.");
        FolderModel firstFolder = helper.createFolderInSite(testUser, testSite);
        FolderModel secondFolder = helper.createFolderInSite(testUser, testSite);
        FileModel testFile = helper.createFileInFolder(testUser, firstFolder);

        Step.STEP("Move the first folder into the second.");
        moveNode(firstFolder, secondFolder);

        Step.STEP("Check the path is updated");
        SearchRequest query = req("PATH:\"" + pathInSite(testSite, secondFolder.getName(), firstFolder.getName(), testFile.getName()) + "\"");
        searchQueryService.expectResultsFromQuery(query, testUser, testFile.getName());
    }

    @Test
    public void testChangeCategoriesUpdatesPath()
    {
        Step.STEP("Create two categories and a file in the first.");
        RestCategoryModel firstCategory = helper.createCategory();
        RestCategoryModel secondCategory = helper.createCategory();
        FileModel testFile = helper.createFileInSite(testUser, testSite);
        helper.linkToCategory(testUser, testFile, firstCategory);

        Step.STEP("Remove the file from the first category and add it to the second.");
        helper.unlinkFromCategory(testUser, testFile, firstCategory);
        helper.linkToCategory(testUser, testFile, secondCategory);

        Step.STEP("Check there is no path for the first category.");
        SearchRequest query = req("PATH:\"" + categoryPath(firstCategory.getName(), testFile.getName()) + "\"");
        searchQueryService.expectNoResultsFromQuery(query, testUser);

        Step.STEP("Check there is a path for the second category.");
        query = req("PATH:\"" + categoryPath(secondCategory.getName(), testFile.getName()) + "\"");
        searchQueryService.expectResultsFromQuery(query, testUser, testFile.getName());
    }

    @Test
    public void testChangeCategoryNameUpdatesPath()
    {
        Step.STEP("Create a category and a file in it.");
        RestCategoryModel category = helper.createCategory();
        FileModel testFile = helper.createFileInSite(testUser, testSite);
        helper.linkToCategory(testUser, testFile, category);

        Step.STEP("Update the name of the category.");
        String newCategoryName = category.getName() + "_updated";
        restClient.authenticateUser(dataUser.getAdminUser()).withCoreAPI()
                .usingCategory(category)
                .updateCategory(RestCategoryModel.builder().name(newCategoryName).create());

        Step.STEP("Check there is a path with the updated category name.");
        SearchRequest query = req("PATH:\"" + categoryPath(newCategoryName, testFile.getName()) + "\"");
        searchQueryService.expectResultsFromQuery(query, testUser, testFile.getName());
    }

    @Test
    public void testChangeCategoryPathUpdatesPath()
    {
        Step.STEP("Create two nested categories and assign a file to the child.");
        RestCategoryModel parentCategory = helper.createCategory();
        RestCategoryModel childCategory = helper.createCategory(parentCategory);
        FileModel testFile = helper.createFileInSite(testUser, testSite);
        helper.linkToCategory(testUser, testFile, parentCategory, childCategory);

        Step.STEP("Update the parent category name and check the file's paths are updated.");
        String newCategoryName = parentCategory.getName() + "_updated";
        restClient.authenticateUser(dataUser.getAdminUser()).withCoreAPI()
                .usingCategory(parentCategory)
                .updateCategory(RestCategoryModel.builder().name(newCategoryName).create());

        Step.STEP("Check there is a path with the updated category name.");
        SearchRequest query = req("PATH:\"" + categoryPath(newCategoryName, childCategory.getName(), testFile.getName()) + "\"");
        searchQueryService.expectResultsFromQuery(query, testUser, testFile.getName());
    }

    /**
     * Rename the specified node to have "_updated" on the end.
     *
     * @param node
     *            The node to update.
     * @return The updated node.
     */
    private RestNodeModel renameNode(ContentModel node)
    {
        String newName = node.getName() + "_updated";
        JsonObject renameJson = Json.createObjectBuilder().add("properties",
                Json.createObjectBuilder().add("cm:name", newName)).build();
        return restClient.authenticateUser(testUser).withCoreAPI().usingNode(node).updateNode(renameJson.toString());
    }

    /**
     * Move the specified node to a folder.
     *
     * @param node
     *            The node to move.
     * @param targetFolder
     *            The folder to move the node to.
     * @return The updated node.
     */
    private RestNodeModel moveNode(ContentModel node, FolderModel targetFolder)
    {
        RestNodeBodyMoveCopyModel moveBody = new RestNodeBodyMoveCopyModel();
        moveBody.setTargetParentId(targetFolder.getNodeRef());
        return restClient.authenticateUser(testUser).withCoreAPI().usingNode(node).move(moveBody);
    }

    /**
     * Create a path to a file or folder in a site.
     *
     * @param site
     *            The site object.
     * @param documentLibraryNames
     *            The list of names of nodes from the document library to the target file or folder.
     * @return An absolute path suitable for use in a path query.
     */
    private String pathInSite(SiteModel site, String... documentLibraryNames)
    {
        return "/app:company_home/st:sites/cm:" + site.getId() + "/cm:documentLibrary/cm:" + stream(documentLibraryNames).collect(joining("/cm:"));
    }

    /**
     * Create a path to a file or folder via the category hierarchy.
     *
     * @param nodeNames
     *            The ordered list of node names from the root category to the node that was categorised.
     * @return An absolute path through the categories to the specified node.
     */
    private String categoryPath(String... nodeNames)
    {
        return "/cm:categoryRoot/cm:generalclassifiable/cm:" + Arrays.stream(nodeNames).collect(joining("/cm:"));
    }
}
