package org.alfresco.elasticsearch;

import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.HttpStatus.OK;
import static org.testng.Assert.assertTrue;

import static org.alfresco.utility.data.RandomData.getRandomName;
import static org.alfresco.utility.report.log.Step.STEP;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import org.alfresco.dataprep.CMISUtil;
import org.alfresco.rest.core.RestWrapper;
import org.alfresco.rest.model.RestCategoryLinkBodyModel;
import org.alfresco.rest.model.RestCategoryModel;
import org.alfresco.rest.model.RestCategoryModelsCollection;
import org.alfresco.tas.AlfrescoStackInitializer;
import org.alfresco.utility.Utility;
import org.alfresco.utility.data.DataContent;
import org.alfresco.utility.data.DataSite;
import org.alfresco.utility.data.DataUser;
import org.alfresco.utility.model.FileModel;
import org.alfresco.utility.model.FolderModel;
import org.alfresco.utility.model.RepoTestModel;
import org.alfresco.utility.model.SiteModel;
import org.alfresco.utility.model.TestGroup;
import org.alfresco.utility.model.UserModel;
import org.alfresco.utility.network.ServerHealth;

@ContextConfiguration(locations = "classpath:alfresco-elasticsearch-context.xml",
        initializers = AlfrescoStackInitializer.class)
public class ElasticsearchCategoriesCountTests extends AbstractTestNGSpringContextTests
{
    protected static final String INCLUDE_COUNT_PARAM = "count";
    protected static final String ROOT_CATEGORY_ID = "-root-";
    protected static final String CATEGORY_NAME_PREFIX = "CategoryName";
    protected static final String FIELD_NAME = "name";
    protected static final String FIELD_ID = "id";
    protected static final String FIELD_COUNT = "count";

    @Autowired
    private ServerHealth serverHealth;
    @Autowired
    private DataUser dataUser;
    @Autowired
    private DataSite dataSite;
    @Autowired
    private DataContent dataContent;
    @Autowired
    private RestWrapper restClient;

    private UserModel user;
    private SiteModel site;
    private RestCategoryModel categoryLinkedWithFolder;
    private RestCategoryModel categoryLinkedWithFile;
    private RestCategoryModel categoryLinkedWithBoth;
    private RestCategoryModel notLinkedCategory;

    @BeforeClass(alwaysRun = true)
    public void dataPreparation() throws InterruptedException
    {
        serverHealth.assertServerIsOnline();

        STEP("Create user and site");
        user = dataUser.createRandomTestUser();
        site = dataSite.usingUser(user).createPublicRandomSite();

        STEP("Create a folder, file in it and few categories");
        final FolderModel folder = dataContent.usingUser(user).usingSite(site).createFolder();
        final FileModel file = dataContent.usingUser(user).usingResource(folder).createContent(CMISUtil.DocumentType.TEXT_PLAIN);
        categoryLinkedWithFolder = prepareCategoryUnderRoot();
        categoryLinkedWithFile = prepareCategoryUnderRoot();
        categoryLinkedWithBoth = prepareCategoryUnder(prepareCategoryUnderRoot());
        notLinkedCategory = prepareCategoryUnderRoot();

        STEP("Link folder and file to categories");
        linkContentToCategories(folder, categoryLinkedWithFolder, categoryLinkedWithBoth);
        linkContentToCategories(file, categoryLinkedWithFile, categoryLinkedWithBoth);

        STEP("Wait for indexing to complete");
        Utility.sleep(500, 60000, () -> restClient.authenticateUser(user)
                .withCoreAPI()
                .usingCategory(categoryLinkedWithBoth)
                .include(INCLUDE_COUNT_PARAM)
                .getCategory()
                .assertThat()
                .field(FIELD_COUNT)
                .is(2));
    }

    @AfterClass
    public void dataCleanup()
    {
        STEP("Remove site and user");
        dataSite.usingUser(user).deleteSite(site);
        dataUser.deleteUser(user);
    }

    /**
     * Verify count for a category linked with file and folder.
     */
    @Test(groups = {TestGroup.REST_API})
    public void testGetCategoryById_includeCount()
    {
        STEP("Get linked category and verify if count is higher than 0");
        final RestCategoryModel actualCategory = restClient.authenticateUser(user)
                .withCoreAPI()
                .usingCategory(categoryLinkedWithBoth)
                .include(INCLUDE_COUNT_PARAM)
                .getCategory();

        restClient.assertStatusCodeIs(OK);
        actualCategory.assertThat().field(FIELD_ID).is(categoryLinkedWithBoth.getId());
        actualCategory.assertThat().field(FIELD_COUNT).is(2);
    }

    /**
     * Verify count for a category not linked with any content.
     */
    @Test(groups = {TestGroup.REST_API})
    public void testGetCategoryById_includeCountForNonLinkedCategory()
    {
        STEP("Get non-linked category and verify if count is 0");
        final RestCategoryModel actualCategory = restClient.authenticateUser(user)
                .withCoreAPI()
                .usingCategory(notLinkedCategory)
                .include(INCLUDE_COUNT_PARAM)
                .getCategory();

        restClient.assertStatusCodeIs(OK);
        actualCategory.assertThat().field(FIELD_ID).is(notLinkedCategory.getId());
        actualCategory.assertThat().field(FIELD_COUNT).is(0);
    }

    /**
     * Verify count for three categories: linked with file, linked with folder and third not linked to any content.
     */
    @Test(groups = {TestGroup.REST_API})
    public void testGetCategories_includeCount()
    {
        STEP("Get few categories and verify its counts");
        final RestCategoryModel parentCategory = createCategoryModelWithId(ROOT_CATEGORY_ID);
        final RestCategoryModelsCollection actualCategories = restClient.authenticateUser(user)
                .withCoreAPI()
                .usingCategory(parentCategory)
                .include(INCLUDE_COUNT_PARAM)
                .getCategoryChildren();

        restClient.assertStatusCodeIs(OK);
        assertTrue(actualCategories.getEntries().stream()
                .map(RestCategoryModel::onModel)
                .anyMatch(category -> category.getId().equals(categoryLinkedWithFolder.getId()) && category.getCount() == 1));
        assertTrue(actualCategories.getEntries().stream()
                .map(RestCategoryModel::onModel)
                .anyMatch(category -> category.getId().equals(categoryLinkedWithFile.getId()) && category.getCount() == 1));
        assertTrue(actualCategories.getEntries().stream()
                .map(RestCategoryModel::onModel)
                .anyMatch(category -> category.getId().equals(notLinkedCategory.getId()) && category.getCount() == 0));
    }

    /**
     * Create category and verify that its count is 0.
     */
    @Test(groups = {TestGroup.REST_API})
    public void testCreateCategory_includingCount()
    {
        STEP("Create a category under root and verify if count is 0");
        final String categoryName = getRandomName("Category");
        final RestCategoryModel rootCategory = createCategoryModelWithId(ROOT_CATEGORY_ID);
        final RestCategoryModel aCategory = createCategoryModelWithName(categoryName);
        final RestCategoryModel createdCategory = restClient.authenticateUser(dataUser.getAdminUser())
                .withCoreAPI()
                .include(INCLUDE_COUNT_PARAM)
                .usingCategory(rootCategory)
                .createSingleCategory(aCategory);

        STEP("Create a category under root category (as admin)");
        restClient.assertStatusCodeIs(CREATED);
        createdCategory.assertThat().field(FIELD_NAME).is(categoryName);
        createdCategory.assertThat().field(FIELD_COUNT).is(0);
    }

    /**
     * Update category linked to file and folder and verify that its count is 2.
     */
    @Test(groups = {TestGroup.REST_API})
    public void testUpdateCategory_includeCount()
    {
        STEP("Update linked category and verify if count is higher than 0");
        final String categoryNewName = getRandomName("NewCategoryName");
        final RestCategoryModel fixedCategoryModel = createCategoryModelWithName(categoryNewName);
        final RestCategoryModel updatedCategory = restClient.authenticateUser(dataUser.getAdminUser())
                .withCoreAPI()
                .usingCategory(categoryLinkedWithBoth)
                .include(INCLUDE_COUNT_PARAM)
                .updateCategory(fixedCategoryModel);

        restClient.assertStatusCodeIs(OK);
        updatedCategory.assertThat().field(FIELD_ID).is(categoryLinkedWithBoth.getId());
        updatedCategory.assertThat().field(FIELD_COUNT).is(2);
    }

    /**
     * Update category not linked to any content and verify that its count is 0.
     */
    @Test(groups = {TestGroup.REST_API})
    public void testUpdateCategory_includeCountForNonLinkedCategory()
    {
        STEP("Update non-linked category and verify if count is 0");
        final String categoryNewName = getRandomName("NewCategoryName");
        final RestCategoryModel fixedCategoryModel = createCategoryModelWithName(categoryNewName);
        final RestCategoryModel updatedCategory = restClient.authenticateUser(dataUser.getAdminUser())
                .withCoreAPI()
                .usingCategory(notLinkedCategory)
                .include(INCLUDE_COUNT_PARAM)
                .updateCategory(fixedCategoryModel);

        restClient.assertStatusCodeIs(OK);
        updatedCategory.assertThat().field(FIELD_ID).is(notLinkedCategory.getId());
        updatedCategory.assertThat().field(FIELD_COUNT).is(0);
    }

    private RestCategoryModelsCollection linkContentToCategories(final RepoTestModel node, final RestCategoryModel... categories)
    {
        final List<RestCategoryLinkBodyModel> categoryLinkModels = Arrays.stream(categories)
                .map(RestCategoryModel::getId)
                .map(this::createCategoryLinkModelWithId)
                .collect(Collectors.toList());
        final RestCategoryModelsCollection linkedCategories = restClient.authenticateUser(user).withCoreAPI().usingNode(node).linkToCategories(categoryLinkModels);

        restClient.assertStatusCodeIs(CREATED);

        return linkedCategories;
    }

    private RestCategoryModel prepareCategoryUnderRoot()
    {
        return prepareCategoryUnder(createCategoryModelWithId(ROOT_CATEGORY_ID));
    }

    private RestCategoryModel prepareCategoryUnder(final RestCategoryModel parentCategory)
    {
        final RestCategoryModel categoryModel = createCategoryModelWithName(getRandomName(CATEGORY_NAME_PREFIX));
        final RestCategoryModel createdCategory = restClient.authenticateUser(dataUser.getAdminUser())
                .withCoreAPI()
                .usingCategory(parentCategory)
                .createSingleCategory(categoryModel);
        restClient.assertStatusCodeIs(CREATED);

        return createdCategory;
    }

    private RestCategoryModel createCategoryModelWithId(final String id)
    {
        return createCategoryModelWithIdAndName(id, null);
    }

    private RestCategoryModel createCategoryModelWithName(final String name)
    {
        return createCategoryModelWithIdAndName(null, name);
    }

    private RestCategoryModel createCategoryModelWithIdAndName(final String id, final String name)
    {
        return RestCategoryModel.builder()
                .id(id)
                .name(name)
                .create();
    }

    private RestCategoryLinkBodyModel createCategoryLinkModelWithId(final String id)
    {
        return RestCategoryLinkBodyModel.builder()
                .categoryId(id)
                .create();
    }
}
