package org.alfresco.elasticsearch.reindexing;

import static org.alfresco.utility.report.log.Step.STEP;

import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import org.alfresco.dataprep.CMISUtil;
import org.alfresco.rest.core.RestWrapper;
import org.alfresco.rest.model.RestTagModel;
import org.alfresco.rest.model.RestTagModelsCollection;
import org.alfresco.tas.AlfrescoStackInitializer;
import org.alfresco.utility.Utility;
import org.alfresco.utility.data.DataContent;
import org.alfresco.utility.data.DataSite;
import org.alfresco.utility.data.DataUser;
import org.alfresco.utility.data.RandomData;
import org.alfresco.utility.model.FileModel;
import org.alfresco.utility.model.SiteModel;
import org.alfresco.utility.model.TestGroup;
import org.alfresco.utility.model.UserModel;
import org.alfresco.utility.network.ServerHealth;

/**
 * Tests to verify batch indexing of tags using Elasticsearch.
 */
@ContextConfiguration(locations = "classpath:alfresco-elasticsearch-context.xml",
        initializers = AlfrescoStackInitializer.class)
public class ElasticsearchGetTagsTests extends AbstractTestNGSpringContextTests
{

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
    private RestTagModel apple, banana, pineapple, winegrape, grapefruit, orange;

    @BeforeClass(alwaysRun = true)
    public void dataPreparation() throws InterruptedException
    {
        serverHealth.isServerReachable();
        serverHealth.assertServerIsOnline();

        STEP("Create user and site");
        user = dataUser.createRandomTestUser();
        site = dataSite.usingUser(user).createPublicRandomSite();
        final FileModel document = dataContent.usingAdmin().usingSite(site).createContent(CMISUtil.DocumentType.TEXT_PLAIN);

        STEP("Create few tags");
        apple = restClient.authenticateUser(user).withCoreAPI().usingResource(document)
                .addTag(RandomData.getRandomName("apple"));
        banana = restClient.authenticateUser(dataUser.getAdminUser()).withCoreAPI()
                .createSingleTag(RestTagModel.builder().tag(RandomData.getRandomName("banana")).create());
        pineapple = restClient.authenticateUser(dataUser.getAdminUser()).withCoreAPI()
                .createSingleTag(RestTagModel.builder().tag(RandomData.getRandomName("pineapple")).create());
        winegrape = restClient.authenticateUser(dataUser.getAdminUser()).withCoreAPI()
                .createSingleTag(RestTagModel.builder().tag(RandomData.getRandomName("winegrape")).create());
        grapefruit = restClient.authenticateUser(dataUser.getAdminUser()).withCoreAPI()
                .createSingleTag(RestTagModel.builder().tag(RandomData.getRandomName("grapefruit")).create());
        orange = restClient.authenticateUser(dataUser.getAdminUser()).withCoreAPI()
                .createSingleTag(RestTagModel.builder().tag(RandomData.getRandomName("orange")).create());

        STEP("Wait for indexing to complete");
        Utility.sleep(500, 10000, () -> restClient.authenticateUser(dataUser.getAdminUser())
                .withParams("where=(tag MATCHES ('oran*'))")
                .withCoreAPI()
                .getTags()
                .assertThat()
                .entrySetContains("tag", orange.getTag().toLowerCase()));
    }

    @AfterClass
    public void dataCleanup()
    {
        STEP("Remove created tags");
        restClient.authenticateUser(dataUser.getAdminUser()).withCoreAPI().usingTag(apple).deleteTag();
        restClient.authenticateUser(dataUser.getAdminUser()).withCoreAPI().usingTag(banana).deleteTag();
        restClient.authenticateUser(dataUser.getAdminUser()).withCoreAPI().usingTag(pineapple).deleteTag();
        restClient.authenticateUser(dataUser.getAdminUser()).withCoreAPI().usingTag(winegrape).deleteTag();
        restClient.authenticateUser(dataUser.getAdminUser()).withCoreAPI().usingTag(grapefruit).deleteTag();
        restClient.authenticateUser(dataUser.getAdminUser()).withCoreAPI().usingTag(orange).deleteTag();

        STEP("Remove site and user");
        dataSite.usingUser(user).deleteSite(site);
        dataUser.deleteUser(user);
    }

    /**
     * Verify if exact name filter can be applied.
     */
    @Test(groups = {TestGroup.REST_API, TestGroup.TAGS, TestGroup.REGRESSION})
    public void testGetTags_withSingleNameFilter()
    {
        STEP("Get tags with names filter using EQUALS and expect one item in result");
        final RestTagModelsCollection returnedCollection = restClient.authenticateUser(user)
                .withParams("where=(tag='" + apple.getTag() + "')")
                .withCoreAPI()
                .getTags();

        restClient.assertStatusCodeIs(HttpStatus.OK);
        returnedCollection.assertThat()
                .entrySetMatches("tag", Set.of(apple.getTag().toLowerCase()));
    }

    /**
     * Verify if multiple names can be applied as a filter.
     */
    @Test(groups = {TestGroup.REST_API, TestGroup.TAGS, TestGroup.REGRESSION})
    public void testGetTags_withTwoNameFilters()
    {
        STEP("Get tags with names filter using IN and expect two items in result");
        final RestTagModelsCollection returnedCollection = restClient.authenticateUser(user)
                .withParams("where=(tag IN ('" + apple.getTag() + "', '" + banana.getTag() + "'))")
                .withCoreAPI()
                .getTags();

        restClient.assertStatusCodeIs(HttpStatus.OK);
        returnedCollection.assertThat()
                .entrySetMatches("tag", Set.of(apple.getTag().toLowerCase(), banana.getTag().toLowerCase()));
    }

    /**
     * Verify if alike name filter can be applied.
     */
    @Test(groups = {TestGroup.REST_API, TestGroup.TAGS, TestGroup.REGRESSION})
    public void testGetTags_whichNamesStartsWithOrphan()
    {
        STEP("Get tags with names filter using MATCHES and expect one item in result");
        final RestTagModelsCollection returnedCollection = restClient.authenticateUser(user)
                .withParams("where=(tag MATCHES ('*an*'))")
                .withCoreAPI()
                .getTags();

        restClient.assertStatusCodeIs(HttpStatus.OK);
        returnedCollection.assertThat()
                .entrySetContains("tag", banana.getTag().toLowerCase(), orange.getTag().toLowerCase());
    }

    /**
     * Verify that tags can be filtered by exact name and alike name at the same time.
     */
    @Test(groups = {TestGroup.REST_API, TestGroup.TAGS, TestGroup.REGRESSION})
    public void testGetTags_withExactNameAndAlikeFilters()
    {
        STEP("Get tags with names filter using EQUALS and MATCHES and expect four items in result");
        final RestTagModelsCollection returnedCollection = restClient.authenticateUser(user)
                .withParams("where=(tag='" + orange.getTag() + "' OR tag MATCHES ('*grape*'))")
                .withCoreAPI()
                .getTags();

        restClient.assertStatusCodeIs(HttpStatus.OK);
        returnedCollection.assertThat()
                .entrySetMatches("tag", Set.of(orange.getTag().toLowerCase(), grapefruit.getTag().toLowerCase(), winegrape.getTag().toLowerCase()));
    }

    /**
     * Verify if multiple alike filters can be applied.
     */
    @Test(groups = {TestGroup.REST_API, TestGroup.TAGS, TestGroup.REGRESSION})
    public void testGetTags_withTwoAlikeFilters()
    {
        STEP("Get tags applying names filter using MATCHES twice and expect four items in result");
        final RestTagModelsCollection returnedCollection = restClient.authenticateUser(user)
                .withParams("where=(tag MATCHES ('*apple*') OR tag MATCHES ('grape*'))")
                .withCoreAPI()
                .getTags();

        restClient.assertStatusCodeIs(HttpStatus.OK);
        returnedCollection.assertThat()
                .entrySetMatches("tag", Set.of(apple.getTag().toLowerCase(), pineapple.getTag().toLowerCase(), grapefruit.getTag().toLowerCase()));
    }

    /**
     * Verify that providing incorrect field name in where query will result with 400 (Bad Request).
     */
    @Test(groups = {TestGroup.REST_API, TestGroup.TAGS, TestGroup.REGRESSION})
    public void testGetTags_withWrongWherePropertyNameAndExpect400()
    {
        STEP("Try to get tags with names filter using EQUALS and wrong property name and expect 400");
        restClient.authenticateUser(user)
                .withParams("where=(name=apple)")
                .withCoreAPI()
                .getTags();

        restClient.assertStatusCodeIs(HttpStatus.BAD_REQUEST)
                .assertLastError().containsSummary("Where query error: property with name: name is not expected");
    }

    /**
     * Verify tht AND operator is not supported in where query and expect 400 (Bad Request).
     */
    @Test(groups = {TestGroup.REST_API, TestGroup.TAGS, TestGroup.REGRESSION})
    public void testGetTags_queryAndOperatorNotSupported()
    {
        STEP("Try to get tags applying names filter using AND operator and expect 400");
        restClient.authenticateUser(user)
                .withParams("where=(tag=apple AND tag IN ('banana', 'melon'))")
                .withCoreAPI()
                .getTags();

        restClient.assertStatusCodeIs(HttpStatus.BAD_REQUEST)
                .assertLastError().containsSummary("An invalid WHERE query was received. Unsupported Predicate");
    }
}
