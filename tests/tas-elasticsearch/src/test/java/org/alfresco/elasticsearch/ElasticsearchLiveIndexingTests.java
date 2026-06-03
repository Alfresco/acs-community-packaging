package org.alfresco.elasticsearch;

import static java.util.Arrays.asList;

import static org.alfresco.elasticsearch.SearchQueryService.req;
import static org.alfresco.tas.TestDataUtility.getAlphabeticUUID;

import java.util.Map;
import java.util.function.Predicate;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.json.simple.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import org.alfresco.dataprep.AlfrescoHttpClient;
import org.alfresco.dataprep.AlfrescoHttpClientFactory;
import org.alfresco.rest.search.RestRequestQueryModel;
import org.alfresco.rest.search.SearchNodeModel;
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

@ContextConfiguration(locations = "classpath:alfresco-elasticsearch-context.xml",
        initializers = AlfrescoStackInitializer.class)
/**
 * In this test we are verifying end-to-end the indexing and search in Elasticsearch. In order to test ACLs we created 2 sites and 3 users.
 */
public class ElasticsearchLiveIndexingTests extends AbstractTestNGSpringContextTests
{
    private static final String PREFIX = getAlphabeticUUID() + "_";
    private static final String UNIQUE_WORD = getAlphabeticUUID();
    private static final String FILE_0_NAME = PREFIX + "test.txt";
    private static final String FILE_1_NAME = PREFIX + "another.txt";
    private static final String FILE_2_NAME = PREFIX + "user1.txt";
    private static final String FILE_3_NAME = PREFIX + "user1Old.txt";
    public static final String BEFORE_1970_TXT = "before1970.txt";

    @Autowired
    private DataUser dataUser;

    @Autowired
    private DataContent dataContent;

    @Autowired
    private DataSite dataSite;

    @Autowired
    private AlfrescoHttpClientFactory alfrescoHttpClientFactory;

    @Autowired
    private ServerHealth serverHealth;

    @Autowired
    protected SearchQueryService searchQueryService;

    private UserModel userSite1;
    private UserModel userSite2;
    private UserModel userMultiSite;
    private SiteModel siteModel1;
    private SiteModel siteModel2;

    /**
     * Data will be prepared using the schema below:
     * <p>
     * Site1: - Users: userSite1, userMultiSite - Documents: FILE_0_NAME (owner: userSite1), FILE_1_NAME (owner: userSite1), FILE_3_NAME (owner: userSite2)
     * <p>
     * Site2: - Users: userSite2, userMultiSite - Documents: FILE_2_NAME (owner: userSite2)
     */
    @BeforeClass(alwaysRun = true)
    public void dataPreparation()
    {
        serverHealth.assertServerIsOnline();

        userSite1 = dataUser.createRandomTestUser();
        userSite2 = dataUser.createRandomTestUser();
        userMultiSite = dataUser.createRandomTestUser();

        siteModel1 = dataSite.usingUser(userSite1).createPrivateRandomSite();
        siteModel2 = dataSite.usingUser(userSite2).createPrivateRandomSite();

        dataUser.addUserToSite(userSite2, siteModel1, UserRole.SiteContributor);
        dataUser.addUserToSite(userMultiSite, siteModel1, UserRole.SiteContributor);
        dataUser.addUserToSite(userMultiSite, siteModel2, UserRole.SiteContributor);

        createContent(FILE_0_NAME, "This is the first test containing the unique word " + UNIQUE_WORD, siteModel1, userSite1);
        createContent(FILE_1_NAME, "This is another TEST file", siteModel1, userSite1);
        createContent(FILE_2_NAME, "This is another test file", siteModel2, userSite2);
        createContent(FILE_3_NAME, "This is another Test file", siteModel1, userSite2);
        // remove the user from site, but he keeps ownership on FILE_3_NAME
        dataUser.removeUserFromSite(userSite2, siteModel1);
    }

    @TestRail(section = TestGroup.SEARCH,
            executionType = ExecutionType.REGRESSION,
            description = "Verify that the include parameter work with Elasticsearch search as expected.")
    @Test(groups = TestGroup.SEARCH)
    public void searchCanFindAFileUsingIncludeParameter()
    {
        SearchRequest queryWithoutIncludes = req(UNIQUE_WORD);
        Predicate<SearchNodeModel> allFieldsNull = searchNodeModel -> searchNodeModel.getProperties() == null
                && searchNodeModel.getPath() == null
                && searchNodeModel.getAspectNames() == null
                && searchNodeModel.getAllowableOperations() == null
                && searchNodeModel.getPermissions() == null
                && searchNodeModel.getAssociation() == null
                && searchNodeModel.isLocked() == null
                && searchNodeModel.isLink() == null;
        searchQueryService.expectAllResultsFromQuery(queryWithoutIncludes, userSite1, allFieldsNull);

        SearchRequest queryWithIncludes = new SearchRequest();
        // A full list of all fields that can be included is declared in constant:
        // org.alfresco.rest.api.search.impl.SearchMapper.PERMITTED_INCLUDES
        queryWithIncludes.setInclude(asList("properties", "path", "aspectNames", "isLocked", "allowableOperations",
                "permissions", "isLink", "association"));
        RestRequestQueryModel queryReq = new RestRequestQueryModel();
        queryReq.setQuery(UNIQUE_WORD);
        queryWithIncludes.setQuery(queryReq);
        Predicate<SearchNodeModel> noFieldsNull = searchNodeModel -> searchNodeModel.getProperties() != null
                && searchNodeModel.getPath() != null
                && searchNodeModel.getAspectNames() != null
                && searchNodeModel.getAllowableOperations() != null
                && searchNodeModel.getPermissions() != null
                && searchNodeModel.getAssociation() != null
                && searchNodeModel.isLocked() != null
                && searchNodeModel.isLink() != null;
        searchQueryService.expectAllResultsFromQuery(queryWithIncludes, userSite1, noFieldsNull);
    }

    @TestRail(section = TestGroup.SEARCH,
            executionType = ExecutionType.REGRESSION,
            description = "Verify that the simpler Elasticsearch search works as expected.")
    @Test(groups = TestGroup.SEARCH)
    public void searchCanFindAFile()
    {
        // this test must found only one documents, while documents in the system are four because
        // only one contains the unique word.
        searchQueryService.expectResultsFromQuery(req(UNIQUE_WORD), userSite1, FILE_0_NAME);
    }

    @TestRail(section = TestGroup.SEARCH,
            executionType = ExecutionType.REGRESSION,
            description = "Verify that Elasticsearch search works as expected using a user that has access to only one site.")
    @Test(groups = TestGroup.SEARCH)
    public void searchCanFindFilesOnASite()
    {
        searchQueryService.expectResultsFromQuery(req(PREFIX), userSite1, FILE_0_NAME, FILE_1_NAME, FILE_3_NAME);
    }

    @TestRail(section = TestGroup.SEARCH,
            executionType = ExecutionType.REGRESSION,
            description = "Verify that Elasticsearch search works as expected when the user can search a file because he is the owner.")
    @Test(groups = TestGroup.SEARCH)
    public void searchCanFindAFileOnMultipleSitesWithOwner()
    {
        searchQueryService.expectResultsFromQuery(req(PREFIX), userSite2, FILE_3_NAME, FILE_2_NAME);
    }

    @TestRail(section = TestGroup.SEARCH,
            executionType = ExecutionType.REGRESSION,
            description = "Verify that Elasticsearch search works as expected when a user has permission on multiple sites.")
    @Test(groups = TestGroup.SEARCH)
    public void searchCanFindAFileOnMultipleSites()
    {
        searchQueryService.expectResultsFromQuery(req(PREFIX), userMultiSite, FILE_0_NAME, FILE_1_NAME, FILE_3_NAME, FILE_2_NAME);
    }

    @TestRail(section = TestGroup.SEARCH,
            executionType = ExecutionType.REGRESSION,
            description = "Verify that wildcard field queries work inside quotes with Elasticsearch.")
    @Test(groups = TestGroup.SEARCH)
    public void wildcardWorksInsideQuotes()
    {
        searchQueryService.expectResultsFromQuery(req("cm:name:\"" + PREFIX + "user1*\""), userMultiSite, FILE_2_NAME, FILE_3_NAME);
    }

    @TestRail(section = TestGroup.SEARCH,
            executionType = ExecutionType.REGRESSION,
            description = "Verify that wildcard field queries work without quotes with Elasticsearch.")
    @Test(groups = TestGroup.SEARCH)
    public void wildcardWorksWithoutQuotes()
    {
        searchQueryService.expectResultsFromQuery(req("cm:name:" + PREFIX + "user1*"), userMultiSite, FILE_2_NAME, FILE_3_NAME);
    }

    @TestRail(section = TestGroup.SEARCH,
            executionType = ExecutionType.REGRESSION,
            description = "Verify that wildcard queries work against noderefs.")
    @Test(groups = TestGroup.SEARCH, enabled = false) // Test should be re-enabled within: ACS-6068
    public void wildcardNodeRefQuery()
    {
        searchQueryService.expectResultsFromQuery(req("ANCESTOR:\"" + siteModel2.getGuid().substring(0, 10) + "*\""), userMultiSite, "documentLibrary", FILE_2_NAME);
    }

    @TestRail(section = TestGroup.SEARCH,
            executionType = ExecutionType.REGRESSION,
            description = "Verify that a range query can return a document from Elasticsearch.")
    @Test(groups = TestGroup.SEARCH)
    public void findFileWithRangeQuery()
    {
        searchQueryService.expectResultsFromQuery(req("cm:created:[NOW-1YEAR TO MAX] AND name:" + FILE_0_NAME), userSite1, FILE_0_NAME);
    }

    @TestRail(section = TestGroup.SEARCH,
            executionType = ExecutionType.REGRESSION,
            description = "Verify that a range query doesn't return all documents from Elasticsearch.")
    @Test(groups = TestGroup.SEARCH)
    public void omitFileWithRangeQuery()
    {
        searchQueryService.expectNoResultsFromQuery(req("cm:created:[MIN TO NOW-2YEARS] AND name:" + FILE_0_NAME), userSite1);
    }

    @TestRail(section = TestGroup.SEARCH,
            executionType = ExecutionType.REGRESSION,
            description = "Verify that the simpler Elasticsearch search works as expected.")
    @Test(groups = TestGroup.SEARCH)
    public void indexAndSearchForDateBefore1970()
    {
        // Elasticsearch doesn't accept numbers for dates before 1970, so we create and search for a specific document in order to verify that.
        createNodeWithProperties(siteModel1, new FileModel(BEFORE_1970_TXT, FileType.TEXT_PLAIN), userSite1,
                Map.of("cm:from", -2637887000L));

        searchQueryService.expectResultsInclude(req("cm:from:1969-12-01T11:15:13Z"), userSite1, BEFORE_1970_TXT);
    }

    private FileModel createContent(String filename, String content, SiteModel site, UserModel user)
    {
        FileModel fileModel = new FileModel(filename, FileType.TEXT_PLAIN, content);
        return dataContent.usingUser(user).usingSite(site)
                .createContent(fileModel);
    }

    private void createNodeWithProperties(SiteModel parentSite, FileModel fileModel, UserModel currentUser, Map<String, Object> properties)
    {
        AlfrescoHttpClient client = alfrescoHttpClientFactory.getObject();
        String reqUrl = client.getApiVersionUrl() + "nodes/" + parentSite.getGuid() + "/children";
        String name = fileModel.getName();

        HttpPost post = new HttpPost(reqUrl);
        JSONObject body = new JSONObject();
        body.put("name", name);
        body.put("nodeType", "cm:content");

        JSONObject jsonProperties = new JSONObject();
        jsonProperties.putAll(properties);
        body.put("properties", jsonProperties);

        post.setEntity(client.setMessageBody(body));

        // Send Request
        logger.info(String.format("POST: '%s'", reqUrl));
        HttpResponse response = client.execute(currentUser.getUsername(), currentUser.getPassword(), post);
        if (org.apache.http.HttpStatus.SC_CREATED != response.getStatusLine().getStatusCode())
        {
            throw new RuntimeException("Could not create file. Request response: " + client.getParameterFromJSON(response, "briefSummary", "error"));
        }
    }
}
