package org.alfresco.elasticsearch;

import static org.alfresco.elasticsearch.SearchQueryService.req;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import org.alfresco.tas.AlfrescoStackInitializer;
import org.alfresco.utility.data.DataContent;
import org.alfresco.utility.data.DataSite;
import org.alfresco.utility.data.DataUser;
import org.alfresco.utility.model.FileModel;
import org.alfresco.utility.model.FileType;
import org.alfresco.utility.model.SiteModel;
import org.alfresco.utility.model.TestGroup;
import org.alfresco.utility.model.UserModel;
import org.alfresco.utility.network.ServerHealth;

@ContextConfiguration(locations = "classpath:alfresco-elasticsearch-context.xml",
        initializers = AlfrescoStackInitializer.class)
/**
 * Tests verifying a range of simple AFTS queries using the cm:name field.
 */
public class ElasticsearchTokenisationTests extends AbstractTestNGSpringContextTests
{
    private static final String ALPHABETIC_NO_SPACE = "TestFileabc.txt";
    private static final String NUMERIC_NO_SPACE = "TestFile123.txt";
    private static final String ALPHABETIC_WITH_SPACE = "TestFile abc.txt";
    private static final String NUMERIC_WITH_SPACE = "TestFile 123.txt";

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

    /**
     * Create a site containing four documents.
     */
    @BeforeClass(alwaysRun = true)
    public void dataPreparation()
    {
        serverHealth.assertServerIsOnline();

        user = dataUser.createRandomTestUser();
        siteModel = dataSite.usingUser(user).createPrivateRandomSite();

        createContent(ALPHABETIC_NO_SPACE, "", siteModel, user);
        createContent(NUMERIC_NO_SPACE, "", siteModel, user);
        createContent(ALPHABETIC_WITH_SPACE, "", siteModel, user);
        createContent(NUMERIC_WITH_SPACE, "", siteModel, user);
    }

    /** Check that searching for "TestFileabc.txt" only returns that file. */
    @Test(groups = TestGroup.SEARCH)
    public void searchNoSpaceAlphabetic()
    {
        searchQueryService.expectResultsFromQuery(req("name:\"TestFileabc.txt\""), user, ALPHABETIC_NO_SPACE);
    }

    /** Check that searching for "TestFile abc.txt" only returns that file. */
    @Test(groups = TestGroup.SEARCH)
    public void searchWithSpaceAlphabetic()
    {
        searchQueryService.expectResultsFromQuery(req("name:\"TestFile abc.txt\""), user, ALPHABETIC_WITH_SPACE);
    }

    /** Check that searching for "TestFile123.txt" returns both files with numbers in. */
    @Test(groups = TestGroup.SEARCH)
    public void searchNoSpaceNumeric()
    {
        searchQueryService.expectResultsFromQuery(req("name:\"TestFile123.txt\""), user, NUMERIC_NO_SPACE, NUMERIC_WITH_SPACE);
    }

    /** Check that searching for "TestFile 123.txt" returns both files with numbers in. */
    @Test(groups = TestGroup.SEARCH)
    public void searchWithSpaceNumeric()
    {
        searchQueryService.expectResultsFromQuery(req("name:\"TestFile 123.txt\""), user, NUMERIC_NO_SPACE, NUMERIC_WITH_SPACE);
    }

    /** Check that searching for "TestFile" doesn't return TestFileabc.txt. */
    @Test(groups = TestGroup.SEARCH)
    public void searchBaseFileName()
    {
        searchQueryService.expectResultsFromQuery(req("name:\"TestFile\""), user, NUMERIC_NO_SPACE, ALPHABETIC_WITH_SPACE, NUMERIC_WITH_SPACE);
    }

    /** Check that searching for TestFileabc.txt with no quotes returns just that file. */
    @Test(groups = TestGroup.SEARCH)
    public void searchNoQuotesNoSpaceAlphabetic()
    {
        searchQueryService.expectResultsFromQuery(req("name:TestFileabc.txt"), user, ALPHABETIC_NO_SPACE);
    }

    /** Check that searching for TestFile123.txt with no quotes returns both files with numbers in. */
    @Test(groups = TestGroup.SEARCH)
    public void searchNoQuotesNoSpaceNumeric()
    {
        searchQueryService.expectResultsFromQuery(req("name:TestFile123.txt"), user, NUMERIC_NO_SPACE, NUMERIC_WITH_SPACE);
    }

    /** Check that searching for TestFile doesn't return TestFileabc.txt. */
    @Test(groups = TestGroup.SEARCH)
    public void searchBaseFileNameNoQuotes()
    {
        searchQueryService.expectResultsFromQuery(req("name:TestFile"), user, NUMERIC_NO_SPACE, ALPHABETIC_WITH_SPACE, NUMERIC_WITH_SPACE);
    }

    /** Check that including an escaped space (_x0020_) in double quotes means that nothing is matched. */
    @Test(groups = TestGroup.SEARCH)
    public void searchEncodedSpaceInQuotes()
    {
        searchQueryService.expectNoResultsFromQuery(req("name:\"TestFile_x0020_abc.txt\""), user);
    }

    /** Check that searching for 'TestFileabc.txt' only returns that file. */
    @Test(groups = TestGroup.SEARCH)
    public void searchNoSpaceAlphabeticSingleQuote()
    {
        searchQueryService.expectResultsFromQuery(req("name:'TestFileabc.txt'"), user, ALPHABETIC_NO_SPACE);
    }

    /** Check that searching for 'TestFile abc.txt' only returns that file. */
    @Test(groups = TestGroup.SEARCH)
    public void searchWithSpaceAlphabeticSingleQuote()
    {
        searchQueryService.expectResultsFromQuery(req("name:'TestFile abc.txt'"), user, ALPHABETIC_WITH_SPACE);
    }

    /** Check that searching for 'TestFile123.txt' returns both files with numbers in. */
    @Test(groups = TestGroup.SEARCH)
    public void searchNoSpaceNumericSingleQuote()
    {
        searchQueryService.expectResultsFromQuery(req("name:'TestFile123.txt'"), user, NUMERIC_NO_SPACE, NUMERIC_WITH_SPACE);
    }

    /** Check that searching for 'TestFile 123.txt' returns both files with numbers in. */
    @Test(groups = TestGroup.SEARCH)
    public void searchWithSpaceNumericSingleQuote()
    {
        searchQueryService.expectResultsFromQuery(req("name:'TestFile 123.txt'"), user, NUMERIC_NO_SPACE, NUMERIC_WITH_SPACE);
    }

    /** Check that searching for 'TestFile' doesn't return TestFileabc.txt. */
    @Test(groups = TestGroup.SEARCH)
    public void searchBaseFileNameSingleQuote()
    {
        searchQueryService.expectResultsFromQuery(req("name:'TestFile'"), user, NUMERIC_NO_SPACE, ALPHABETIC_WITH_SPACE, NUMERIC_WITH_SPACE);
    }

    private FileModel createContent(String filename, String content, SiteModel site, UserModel user)
    {
        FileModel fileModel = new FileModel(filename, FileType.TEXT_PLAIN, content);
        return dataContent.usingUser(user).usingSite(site)
                .createContent(fileModel);
    }
}
