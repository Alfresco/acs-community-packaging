package org.alfresco.elasticsearch;

import static java.util.stream.Collectors.toSet;

import static org.alfresco.elasticsearch.SearchQueryService.req;
import static org.alfresco.utility.report.log.Step.STEP;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import org.alfresco.rest.search.ResponseHighlightModel;
import org.alfresco.rest.search.RestRequestFieldsModel;
import org.alfresco.rest.search.RestRequestHighlightModel;
import org.alfresco.rest.search.SearchNodeModel;
import org.alfresco.rest.search.SearchRequest;
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
// These are TestNG tests and the assertions are hidden in searchQueryService.
@SuppressWarnings({"PMD.JUnitTestsShouldIncludeAssert"})
public class ElasticsearchPlainHighlightingTests extends AbstractTestNGSpringContextTests
{
    static final Logger LOGGER = LoggerFactory.getLogger(ElasticsearchPlainHighlightingTests.class);
    static final String FILE_A = "fileA.txt";
    static final String FILE_B = "fileB.txt";
    static final String FILE_C = "fileC.txt";
    static final String CONTENT_A = "The quick brown fox jumps over the lazy dog.";
    static final String CONTENT_B = """
            The lazy dog sleeps under the quick brown fox. The middle of the document is quite long:
            Lorem ipsum dolor sit amet. In veniam tempore hic provident sunt et distinctio velit et reprehenderit
            officiis id sapiente omnis et quisquam aliquam et porro perferendis. In sequi placeat ut quaerat voluptatem
            ea consequuntur impedit aut eaque enim aut atque rerum.
            The end of the document mentions the dog again!
            """;
    static final String CONTENT_C = """
            The rabbit made a ring in the center of the field. He said to the wolf, "Now, you dance around this ring, and sing just as I do."
            Rabbit made a larger ring for himself and danced around just beyond the wolf.
            The wolf thought that this was the finest dance he had ever seen.
            He and the rabbit danced faster and faster, and sang louder and louder.
            As the rabbit danced, he moved nearer and nearer to the edge of the field.
            The wolf was dancing so fast and singing so loud that he did not notice this.
            At last, Brother Rabbit reached the edge of the field; then he jumped into the blackberry bushes and ran away.
            The wolf tried to give chase, but he was so dizzy that he could not run. And the rabbit got away without having his ears cut off.
            """;

    @Autowired
    ServerHealth serverHealth;
    @Autowired
    DataUser dataUser;
    @Autowired
    DataContent dataContent;
    @Autowired
    DataSite dataSite;
    @Autowired
    SearchQueryService searchQueryService;

    UserModel user;
    SiteModel site;
    FileModel fileA;
    FileModel fileB;
    FileModel fileC;

    @BeforeClass(alwaysRun = true)
    public void dataPreparation()
    {
        serverHealth.assertServerIsOnline();

        STEP("Create a test user and a private site.");
        user = dataUser.createRandomTestUser();
        site = dataSite.usingUser(user).createPrivateRandomSite();

        STEP("Create two test files with highlightable content");
        FileModel fileModelA = new FileModel(FILE_A, FileType.TEXT_PLAIN, CONTENT_A);
        fileA = dataContent.usingUser(user).usingSite(site).createContent(fileModelA);
        FileModel fileModelB = new FileModel(FILE_B, FileType.TEXT_PLAIN, CONTENT_B);
        fileB = dataContent.usingUser(user).usingSite(site).createContent(fileModelB);
        FileModel fileModelC = new FileModel(FILE_C, FileType.TEXT_PLAIN, CONTENT_C);
        fileC = dataContent.usingUser(user).usingSite(site).createContent(fileModelC);
    }

    @AfterClass
    public void dataCleanup()
    {
        STEP("Remove test site and user");
        dataSite.usingAdmin().deleteSite(site);
        dataUser.usingAdmin().deleteUser(user);
    }

    @Test(groups = {TestGroup.SEARCH})
    public void testHighlightsInContent()
    {
        STEP("Search for files mentioning 'dog'");
        String query = "cm:content:dog AND SITE:" + site.getId();
        SearchRequest searchRequest = req("afts", query);
        RestRequestHighlightModel highlightModel = RestRequestHighlightModel.builder().fields(List.of("cm:content")).build();
        searchRequest.setHighlight(highlightModel);
        // Configure the expected highlights for each document.
        Predicate<SearchNodeModel> assertionMethod = highlightAssert(
                Map.of(FILE_A, Map.of("cm:content", List.of("The quick brown fox jumps over the lazy <em>dog</em>.")),
                        FILE_B, Map.of("cm:content", List.of("The lazy <em>dog</em> sleeps under the quick brown fox. The middle of the document is quite long:\nLorem",
                                "\nea consequuntur impedit aut eaque enim aut atque rerum.\nThe end of the document mentions the <em>dog</em> again!\n"))));
        searchQueryService.expectAllResultsFromQuery(searchRequest, user, assertionMethod);
    }

    @Test(groups = {TestGroup.SEARCH})
    public void testHighlightsInTwoFields()
    {
        STEP("Search for files with 'file' in the name that mention 'middle'");
        String query = "cm:content:middle AND cm:name:file AND SITE:" + site.getId();
        SearchRequest searchRequest = req("afts", query);
        RestRequestHighlightModel highlightModel = RestRequestHighlightModel.builder().fields(List.of("cm:name", "cm:content")).build();
        searchRequest.setHighlight(highlightModel);
        // Configure the expected highlights for each document.
        Predicate<SearchNodeModel> assertionMethod = highlightAssert(
                Map.of(FILE_B, Map.of("cm:name", List.of("<em>file</em>B.txt"),
                        "cm:content", List.of("The lazy dog sleeps under the quick brown fox. The <em>middle</em> of the document is quite long:\nLorem"))));
        searchQueryService.expectAllResultsFromQuery(searchRequest, user, assertionMethod);
    }

    @Test(groups = {TestGroup.SEARCH})
    public void testHighlightsWithCustomPrefixAndPostfix()
    {
        STEP("Search for files mentioning 'dog' with custom prefix and postfix");
        String query = "cm:content:dog AND SITE:" + site.getId();
        SearchRequest searchRequest = req("afts", query);
        RestRequestHighlightModel highlightModel = RestRequestHighlightModel.builder()
                .prefix("<b>")
                .postfix("</b>")
                .fields(List.of("cm:content"))
                .build();
        searchRequest.setHighlight(highlightModel);
        // Configure the expected highlights for each document.
        Predicate<SearchNodeModel> assertionMethod = highlightAssert(
                Map.of(FILE_A, Map.of("cm:content", List.of("The quick brown fox jumps over the lazy <b>dog</b>.")),
                        FILE_B, Map.of("cm:content", List.of("The lazy <b>dog</b> sleeps under the quick brown fox. The middle of the document is quite long:\nLorem",
                                "\nea consequuntur impedit aut eaque enim aut atque rerum.\nThe end of the document mentions the <b>dog</b> again!\n"))));
        searchQueryService.expectAllResultsFromQuery(searchRequest, user, assertionMethod);
    }

    @Test(groups = {TestGroup.SEARCH})
    public void testHighlightsWithBlankPrefixAndPostfix()
    {
        STEP("Search for files mentioning 'middle' with blank mark as custom prefix and postfix");
        String query = "cm:content:middle AND SITE:" + site.getId();
        SearchRequest searchRequest = req("afts", query);
        RestRequestHighlightModel highlightModel = RestRequestHighlightModel.builder()
                .prefix(" ")
                .postfix(" ")
                .fields(List.of("cm:content"))
                .build();
        searchRequest.setHighlight(highlightModel);
        // Configure the expected highlights for each document.
        Predicate<SearchNodeModel> assertionMethod = highlightAssert(
                Map.of(FILE_B, Map.of("cm:content", List.of("The lazy dog sleeps under the quick brown fox. The  middle  of the document is quite long:\nLorem"))));
        searchQueryService.expectAllResultsFromQuery(searchRequest, user, assertionMethod);
    }

    @Test(groups = {TestGroup.SEARCH})
    public void testHighlightsWithGeneralAndFieldSpecificPrefixesAndPostfixes()
    {
        STEP("Search for files with 'file' in the name that mention 'dog' with general and field specific prefixes or postfixes");
        String query = "cm:content:dog AND cm:name:file AND SITE:" + site.getId();
        SearchRequest searchRequest = req("afts", query);
        RestRequestHighlightModel highlightModel = RestRequestHighlightModel.builder()
                .prefix("¿")
                .postfix("?")
                .fields(
                        RestRequestFieldsModel.of("cm:name"),
                        RestRequestFieldsModel.of("cm:content", "(", ")"))
                .build();
        searchRequest.setHighlight(highlightModel);
        // Configure the expected highlights for each document.
        Predicate<SearchNodeModel> assertionMethod = highlightAssert(
                Map.of(FILE_A, Map.of("cm:name", List.of("¿file?A.txt"), "cm:content", List.of("The quick brown fox jumps over the lazy (dog).")),
                        FILE_B, Map.of("cm:name", List.of("¿file?B.txt"), "cm:content", List.of("The lazy (dog) sleeps under the quick brown fox. The middle of the document is quite long:\nLorem",
                                "\nea consequuntur impedit aut eaque enim aut atque rerum.\nThe end of the document mentions the (dog) again!\n"))));
        searchQueryService.expectAllResultsFromQuery(searchRequest, user, assertionMethod);
    }

    @Test(groups = {TestGroup.SEARCH})
    public void testHighlightsWithOneFieldSpecificPrefixAndPostfix()
    {
        STEP("Search for files with 'file' in the name that mention 'dog' with one field specific prefix or postfix");
        String query = "cm:content:dog AND cm:name:file AND SITE:" + site.getId();
        SearchRequest searchRequest = req("afts", query);
        RestRequestHighlightModel highlightModel = RestRequestHighlightModel.builder()
                .fields(
                        RestRequestFieldsModel.of("cm:name"),
                        RestRequestFieldsModel.of("cm:content", "(", ")"))
                .build();
        searchRequest.setHighlight(highlightModel);
        // Configure the expected highlights for each document.
        Predicate<SearchNodeModel> assertionMethod = highlightAssert(
                Map.of(FILE_A, Map.of("cm:name", List.of("<em>file</em>A.txt"), "cm:content", List.of("The quick brown fox jumps over the lazy (dog).")),
                        FILE_B, Map.of("cm:name", List.of("<em>file</em>B.txt"), "cm:content", List.of("The lazy (dog) sleeps under the quick brown fox. The middle of the document is quite long:\nLorem",
                                "\nea consequuntur impedit aut eaque enim aut atque rerum.\nThe end of the document mentions the (dog) again!\n"))));
        searchQueryService.expectAllResultsFromQuery(searchRequest, user, assertionMethod);
    }

    @Test(groups = {TestGroup.SEARCH})
    public void testHighlightsInTwoFieldsWithPrefixAndWithoutPostfix()
    {
        STEP("Search for files with 'file' in the name that mention 'middle' with custom prefix");
        String query = "cm:content:middle AND cm:name:file AND SITE:" + site.getId();
        SearchRequest searchRequest = req("afts", query);
        RestRequestHighlightModel highlightModel = RestRequestHighlightModel.builder()
                .fields(List.of("cm:name", "cm:content"))
                .prefix("(")
                .build();
        searchRequest.setHighlight(highlightModel);
        // Configure the expected highlights for each document.
        Predicate<SearchNodeModel> assertionMethod = highlightAssert(
                Map.of(FILE_B, Map.of("cm:name", List.of("(file</em>B.txt"),
                        "cm:content", List.of("The lazy dog sleeps under the quick brown fox. The (middle</em> of the document is quite long:\nLorem"))));
        searchQueryService.expectAllResultsFromQuery(searchRequest, user, assertionMethod);
    }

    @Test(groups = {TestGroup.SEARCH})
    public void testHighlightsInTwoFieldsWithoutPrefixAndWithPostfix()
    {
        STEP("Search for files with 'file' in the name that mention 'middle' with custom postfix");
        String query = "cm:content:middle AND cm:name:file AND SITE:" + site.getId();
        SearchRequest searchRequest = req("afts", query);
        RestRequestHighlightModel highlightModel = RestRequestHighlightModel.builder()
                .fields(List.of("cm:name", "cm:content"))
                .postfix(")")
                .build();
        searchRequest.setHighlight(highlightModel);
        // Configure the expected highlights for each document.
        Predicate<SearchNodeModel> assertionMethod = highlightAssert(
                Map.of(FILE_B, Map.of("cm:name", List.of("<em>file)B.txt"),
                        "cm:content", List.of("The lazy dog sleeps under the quick brown fox. The <em>middle) of the document is quite long:\nLorem"))));
        searchQueryService.expectAllResultsFromQuery(searchRequest, user, assertionMethod);
    }

    @Test(groups = {TestGroup.SEARCH})
    public void testHighlightsInContentWithGeneralSnippetCount()
    {
        STEP("Search for files mentioning 'rabbit' and expect 2 snippets in result");
        String query = "cm:content:rabbit AND SITE:" + site.getId();
        SearchRequest searchRequest = req("afts", query);
        RestRequestHighlightModel highlightModel = RestRequestHighlightModel.builder()
                .fields(RestRequestFieldsModel.of("cm:content"))
                .snippetCount(2)
                .build();
        searchRequest.setHighlight(highlightModel);
        // Configure the expected highlights for each document.
        Predicate<SearchNodeModel> assertionMethod = highlightAssert(
                Map.of(FILE_C, Map.of("cm:content", List.of("The <em>rabbit</em> made a ring in the center of the field. He said to the wolf, \"Now, you dance around",
                        " this ring, and sing just as I do.\"\n<em>Rabbit</em> made a larger ring for himself and danced around just beyond"))));
        searchQueryService.expectAllResultsFromQuery(searchRequest, user, assertionMethod);
    }

    @Test(groups = {TestGroup.SEARCH})
    public void testHighlightsInContentWithFieldSpecificSnippetCount()
    {
        STEP("Search for files mentioning 'rabbit' and expect 3 snippets in result");
        String query = "cm:content:rabbit AND SITE:" + site.getId();
        SearchRequest searchRequest = req("afts", query);
        RestRequestHighlightModel highlightModel = RestRequestHighlightModel.builder()
                .fields(RestRequestFieldsModel.builder().field("cm:content").snippetCount(3).build())
                .snippetCount(1)
                .build();
        searchRequest.setHighlight(highlightModel);
        // Configure the expected highlights for each document.
        Predicate<SearchNodeModel> assertionMethod = highlightAssert(
                Map.of(FILE_C, Map.of("cm:content", List.of("The <em>rabbit</em> made a ring in the center of the field. He said to the wolf, \"Now, you dance around",
                        " this ring, and sing just as I do.\"\n<em>Rabbit</em> made a larger ring for himself and danced around just beyond",
                        " the wolf.\nThe wolf thought that this was the finest dance he had ever seen.\nHe and the <em>rabbit</em> danced"))));
        searchQueryService.expectAllResultsFromQuery(searchRequest, user, assertionMethod);
    }

    @Test(groups = {TestGroup.SEARCH})
    public void testHighlightsInContentWithZeroSnippetCount()
    {
        STEP("Search for files mentioning 'rabbit' and expect default 5 snippets in result");
        String query = "cm:content:rabbit AND SITE:" + site.getId();
        SearchRequest searchRequest = req("afts", query);
        RestRequestHighlightModel highlightModel = RestRequestHighlightModel.builder()
                .fields(RestRequestFieldsModel.of("cm:content"))
                .snippetCount(0)
                .build();
        searchRequest.setHighlight(highlightModel);
        // Configure the expected highlights for each document.
        Predicate<SearchNodeModel> assertionMethod = highlightAssert(
                Map.of(FILE_C, Map.of("cm:content", List.of("The <em>rabbit</em> made a ring in the center of the field. He said to the wolf, \"Now, you dance around",
                        " this ring, and sing just as I do.\"\n<em>Rabbit</em> made a larger ring for himself and danced around just beyond",
                        " the wolf.\nThe wolf thought that this was the finest dance he had ever seen.\nHe and the <em>rabbit</em> danced",
                        " faster and faster, and sang louder and louder.\nAs the <em>rabbit</em> danced, he moved nearer and nearer",
                        " this.\nAt last, Brother <em>Rabbit</em> reached the edge of the field; then he jumped into the blackberry bushes"))));
        searchQueryService.expectAllResultsFromQuery(searchRequest, user, assertionMethod);
    }

    @Test(groups = {TestGroup.SEARCH})
    public void testHighlightsInContentWithNegativeSnippetCount()
    {
        STEP("Search for files mentioning 'rabbit' and expect default 5 snippets in result");
        String query = "cm:content:rabbit AND SITE:" + site.getId();
        SearchRequest searchRequest = req("afts", query);
        RestRequestHighlightModel highlightModel = RestRequestHighlightModel.builder()
                .fields(RestRequestFieldsModel.of("cm:content"))
                .snippetCount(-1)
                .build();
        searchRequest.setHighlight(highlightModel);
        // Configure the expected highlights for each document.
        Predicate<SearchNodeModel> assertionMethod = highlightAssert(
                Map.of(FILE_C, Map.of("cm:content", List.of("The <em>rabbit</em> made a ring in the center of the field. He said to the wolf, \"Now, you dance around",
                        " this ring, and sing just as I do.\"\n<em>Rabbit</em> made a larger ring for himself and danced around just beyond",
                        " the wolf.\nThe wolf thought that this was the finest dance he had ever seen.\nHe and the <em>rabbit</em> danced",
                        " faster and faster, and sang louder and louder.\nAs the <em>rabbit</em> danced, he moved nearer and nearer",
                        " this.\nAt last, Brother <em>Rabbit</em> reached the edge of the field; then he jumped into the blackberry bushes"))));
        searchQueryService.expectAllResultsFromQuery(searchRequest, user, assertionMethod);
    }

    @Test(groups = {TestGroup.SEARCH})
    public void testHighlightsInContentWithGeneralFragmentSize()
    {
        STEP("Search for files mentioning 'dog' and expect snippets about 10 characters long");
        String query = "cm:content:dog AND SITE:" + site.getId();
        SearchRequest searchRequest = req("afts", query);
        RestRequestHighlightModel highlightModel = RestRequestHighlightModel.builder()
                .fields(RestRequestFieldsModel.of("cm:content"))
                .fragmentSize(10)
                .build();
        searchRequest.setHighlight(highlightModel);
        // Configure the expected highlights for each document.
        Predicate<SearchNodeModel> assertionMethod = highlightAssert(
                Map.of(FILE_A, Map.of("cm:content", List.of(" over the lazy <em>dog</em>.")),
                        FILE_B, Map.of("cm:content", List.of(" <em>dog</em> sleeps", " the <em>dog</em> again!\n"))));
        searchQueryService.expectAllResultsFromQuery(searchRequest, user, assertionMethod);
    }

    @Test(groups = {TestGroup.SEARCH})
    public void testHighlightsInContentWithFieldSpecificFragmentSize()
    {
        STEP("Search for files mentioning 'dog' and expect snippets about 15 characters long");
        String query = "cm:content:dog AND SITE:" + site.getId();
        SearchRequest searchRequest = req("afts", query);
        RestRequestHighlightModel highlightModel = RestRequestHighlightModel.builder()
                .fields(RestRequestFieldsModel.builder().field("cm:content").fragmentSize(15).build())
                .fragmentSize(10)
                .build();
        searchRequest.setHighlight(highlightModel);
        // Configure the expected highlights for each document.
        Predicate<SearchNodeModel> assertionMethod = highlightAssert(
                Map.of(FILE_A, Map.of("cm:content", List.of(" over the lazy <em>dog</em>.")),
                        FILE_B, Map.of("cm:content", List.of("The lazy <em>dog</em>", " the <em>dog</em> again!\n"))));
        searchQueryService.expectAllResultsFromQuery(searchRequest, user, assertionMethod);
    }

    @Test(groups = {TestGroup.SEARCH})
    public void testHighlightsInContentWithZeroFragmentSize()
    {
        STEP("Search for files mentioning 'dog' and expect snippets having default characters length");
        String query = "cm:content:dog AND SITE:" + site.getId();
        SearchRequest searchRequest = req("afts", query);
        RestRequestHighlightModel highlightModel = RestRequestHighlightModel.builder()
                .fields(RestRequestFieldsModel.of("cm:content"))
                .fragmentSize(0)
                .build();
        searchRequest.setHighlight(highlightModel);
        // Configure the expected highlights for each document.
        Predicate<SearchNodeModel> assertionMethod = highlightAssert(
                Map.of(FILE_A, Map.of("cm:content", List.of("The quick brown fox jumps over the lazy <em>dog</em>.")),
                        FILE_B, Map.of("cm:content", List.of("The lazy <em>dog</em> sleeps under the quick brown fox. The middle of the document is quite long:\nLorem",
                                "\nea consequuntur impedit aut eaque enim aut atque rerum.\nThe end of the document mentions the <em>dog</em> again!\n"))));
        searchQueryService.expectAllResultsFromQuery(searchRequest, user, assertionMethod);
    }

    @Test(groups = {TestGroup.SEARCH})
    public void testHighlightsInContentWithNegativeFragmentSize()
    {
        STEP("Search for files mentioning 'dog' and expect snippets having default characters length");
        String query = "cm:content:dog AND SITE:" + site.getId();
        SearchRequest searchRequest = req("afts", query);
        RestRequestHighlightModel highlightModel = RestRequestHighlightModel.builder()
                .fields(RestRequestFieldsModel.of("cm:content"))
                .fragmentSize(-1)
                .build();
        searchRequest.setHighlight(highlightModel);
        // Configure the expected highlights for each document.
        Predicate<SearchNodeModel> assertionMethod = highlightAssert(
                Map.of(FILE_A, Map.of("cm:content", List.of("The quick brown fox jumps over the lazy <em>dog</em>.")),
                        FILE_B, Map.of("cm:content", List.of("The lazy <em>dog</em> sleeps under the quick brown fox. The middle of the document is quite long:\nLorem",
                                "\nea consequuntur impedit aut eaque enim aut atque rerum.\nThe end of the document mentions the <em>dog</em> again!\n"))));
        searchQueryService.expectAllResultsFromQuery(searchRequest, user, assertionMethod);
    }

    /**
     * Create a predicate that returns true if the highlights for a received document match the given expectation.
     *
     * @param allExpectedHighlights
     *            The expected highlights for all documents keyed by document name and then field name.
     * @return The predicate.
     */
    private Predicate<SearchNodeModel> highlightAssert(Map<String, Map<String, List<String>>> allExpectedHighlights)
    {
        return document -> {
            if (!allExpectedHighlights.containsKey(document.getName()))
            {
                LOGGER.error("Unexpected entry in results: {}", document.getName());
                return false;
            }
            Map<String, List<String>> expectedHighlights = allExpectedHighlights.get(document.getName());
            List<ResponseHighlightModel> actualHighlights = document.getSearch().getHighlight();
            Set<String> actualFields = actualHighlights.stream()
                    .map(ResponseHighlightModel::getField)
                    .collect(toSet());
            if (!actualFields.equals(expectedHighlights.keySet()))
            {
                LOGGER.error("Unexpected field set for {}: {}", document.getName(), actualFields);
                return false;
            }
            Set<ResponseHighlightModel> expectedHighlightResponse = new HashSet<>();
            for (String expectedField : expectedHighlights.keySet())
            {
                ResponseHighlightModel expectedHighlight = new ResponseHighlightModel();
                List<String> expectedSnippets = expectedHighlights.get(expectedField);
                expectedHighlight.setField(expectedField);
                expectedHighlight.setSnippets(expectedSnippets);
                expectedHighlightResponse.add(expectedHighlight);
            }
            if (!new HashSet<>(actualHighlights).equals(expectedHighlightResponse))
            {
                LOGGER.error("Unexpected highlights for {}, {}", document.getName(), actualHighlights);
                return false;
            }
            return true;
        };
    }
}
