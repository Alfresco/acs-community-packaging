package org.alfresco.elasticsearch;

import static java.lang.String.format;

import static org.alfresco.elasticsearch.SearchQueryService.req;

import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import org.alfresco.rest.search.SearchRequest;
import org.alfresco.tas.AlfrescoStackInitializer;
import org.alfresco.utility.data.DataContent;
import org.alfresco.utility.data.DataUser;
import org.alfresco.utility.model.ContentModel;
import org.alfresco.utility.model.FileModel;
import org.alfresco.utility.model.FileType;
import org.alfresco.utility.model.TestGroup;
import org.alfresco.utility.model.UserModel;
import org.alfresco.utility.network.ServerHealth;
import org.alfresco.utility.testrail.ExecutionType;
import org.alfresco.utility.testrail.annotation.TestRail;

@ContextConfiguration(locations = "classpath:alfresco-elasticsearch-context.xml",
        initializers = AlfrescoStackInitializer.class)
public class ElasticsearchProximitySearchTests extends AbstractTestNGSpringContextTests
{
    @Autowired
    ServerHealth serverHealth;

    @Autowired
    DataUser dataUser;

    @Autowired
    DataContent dataContent;

    @Autowired
    SearchQueryService searchQueryService;

    private UserModel testUser;
    private String contentA;
    private String contentB;
    private String contentC;
    private String contentD;
    private String nameA;
    private String nameB;
    private String nameC;
    private String nameD;
    private String file_NameA_ContentABCD;
    private String file_NameAB_ContentABC;
    private String file_NameABC_ContentAB;
    private String file_NameABCD_ContentA;

    @BeforeClass(alwaysRun = true)
    public void dataPreparation()
    {
        serverHealth.assertServerIsOnline();

        nameA = uniqueString();
        nameB = uniqueString();
        nameC = uniqueString();
        nameD = uniqueString();

        contentA = uniqueString();
        contentB = uniqueString();
        contentC = uniqueString();
        contentD = uniqueString();

        file_NameA_ContentABCD = createContent(join(nameA), join(contentA, contentB, contentC, contentD));
        file_NameAB_ContentABC = createContent(join(nameA, nameB), join(contentA, contentB, contentC));
        file_NameABC_ContentAB = createContent(join(nameA, nameB, nameC), join(contentA, contentB));
        file_NameABCD_ContentA = createContent(join(nameA, nameB, nameC, nameD), join(contentA));

        testUser = dataUser.createRandomTestUser();
    }

    @TestRail(section = {TestGroup.SEARCH, TestGroup.TAGS}, executionType = ExecutionType.REGRESSION,
            description = "Verify proximity queries with AFTS syntax work correctly")
    @Test(groups = {TestGroup.SEARCH, TestGroup.TAGS, TestGroup.REGRESSION})
    public void testProximitySearchUsingAFTSSyntax()
    {
        final String AFTS = "%s *%s %s";

        assertAFTS(format(AFTS, contentA, "(0)", contentB), file_NameA_ContentABCD, file_NameAB_ContentABC, file_NameABC_ContentAB);
        assertAFTS(format(AFTS, contentA, "(1)", contentB), file_NameA_ContentABCD, file_NameAB_ContentABC, file_NameABC_ContentAB);
        assertAFTS(format(AFTS, contentA, "(2)", contentB), file_NameA_ContentABCD, file_NameAB_ContentABC, file_NameABC_ContentAB);
        assertAFTS(format(AFTS, contentA, "(3)", contentB), file_NameA_ContentABCD, file_NameAB_ContentABC, file_NameABC_ContentAB);
        assertAFTS(format(AFTS, contentA, "", contentB), file_NameA_ContentABCD, file_NameAB_ContentABC, file_NameABC_ContentAB);

        assertAFTS(format(AFTS, contentA, "(0)", contentC));
        assertAFTS(format(AFTS, contentA, "(1)", contentC), file_NameA_ContentABCD, file_NameAB_ContentABC);
        assertAFTS(format(AFTS, contentA, "(2)", contentC), file_NameA_ContentABCD, file_NameAB_ContentABC);
        assertAFTS(format(AFTS, contentA, "(3)", contentC), file_NameA_ContentABCD, file_NameAB_ContentABC);
        assertAFTS(format(AFTS, contentA, "", contentC), file_NameA_ContentABCD, file_NameAB_ContentABC);

        assertAFTS(format(AFTS, contentA, "(0)", contentD));
        assertAFTS(format(AFTS, contentA, "(1)", contentD));
        assertAFTS(format(AFTS, contentA, "(2)", contentD), file_NameA_ContentABCD);
        assertAFTS(format(AFTS, contentA, "(3)", contentD), file_NameA_ContentABCD);
        assertAFTS(format(AFTS, contentA, "", contentD), file_NameA_ContentABCD);
    }

    @TestRail(section = {TestGroup.SEARCH, TestGroup.TAGS}, executionType = ExecutionType.REGRESSION,
            description = "Verify proximity queries with AFTS syntax work correctly for specific property")
    @Test(groups = {TestGroup.SEARCH, TestGroup.TAGS, TestGroup.REGRESSION})
    public void testProximitySearchUsingAFTSSyntaxForSpecificProperty()
    {
        final String AFTS = "cm:name:(%s *%s %s)";

        assertAFTS(format(AFTS, nameA, "(0)", nameB), file_NameAB_ContentABC, file_NameABC_ContentAB, file_NameABCD_ContentA);
        assertAFTS(format(AFTS, nameA, "(1)", nameB), file_NameAB_ContentABC, file_NameABC_ContentAB, file_NameABCD_ContentA);
        assertAFTS(format(AFTS, nameA, "(2)", nameB), file_NameAB_ContentABC, file_NameABC_ContentAB, file_NameABCD_ContentA);
        assertAFTS(format(AFTS, nameA, "(3)", nameB), file_NameAB_ContentABC, file_NameABC_ContentAB, file_NameABCD_ContentA);
        assertAFTS(format(AFTS, nameA, "", nameB), file_NameAB_ContentABC, file_NameABC_ContentAB, file_NameABCD_ContentA);

        assertAFTS(format(AFTS, nameA, "(0)", nameC));
        assertAFTS(format(AFTS, nameA, "(1)", nameC), file_NameABC_ContentAB, file_NameABCD_ContentA);
        assertAFTS(format(AFTS, nameA, "(2)", nameC), file_NameABC_ContentAB, file_NameABCD_ContentA);
        assertAFTS(format(AFTS, nameA, "(3)", nameC), file_NameABC_ContentAB, file_NameABCD_ContentA);
        assertAFTS(format(AFTS, nameA, "", nameC), file_NameABC_ContentAB, file_NameABCD_ContentA);

        assertAFTS(format(AFTS, nameA, "(0)", nameD));
        assertAFTS(format(AFTS, nameA, "(1)", nameD));
        assertAFTS(format(AFTS, nameA, "(2)", nameD), file_NameABCD_ContentA);
        assertAFTS(format(AFTS, nameA, "(3)", nameD), file_NameABCD_ContentA);
        assertAFTS(format(AFTS, nameA, "", nameD), file_NameABCD_ContentA);
    }

    @TestRail(section = {TestGroup.SEARCH, TestGroup.TAGS}, executionType = ExecutionType.REGRESSION,
            description = "Verify proximity queries with Lucene syntax work correctly")
    @Test(groups = {TestGroup.SEARCH, TestGroup.TAGS, TestGroup.REGRESSION})
    public void testProximitySearchUsingLuceneSyntax()
    {
        final String lucene = "\"%s %s\"~%d";

        assertLucene(format(lucene, contentA, contentB, 0), file_NameA_ContentABCD, file_NameAB_ContentABC, file_NameABC_ContentAB);
        assertLucene(format(lucene, contentA, contentB, 1), file_NameA_ContentABCD, file_NameAB_ContentABC, file_NameABC_ContentAB);
        assertLucene(format(lucene, contentA, contentB, 2), file_NameA_ContentABCD, file_NameAB_ContentABC, file_NameABC_ContentAB);
        assertLucene(format(lucene, contentA, contentB, 3), file_NameA_ContentABCD, file_NameAB_ContentABC, file_NameABC_ContentAB);

        assertLucene(format(lucene, contentA, contentC, 0));
        assertLucene(format(lucene, contentA, contentC, 1), file_NameA_ContentABCD, file_NameAB_ContentABC);
        assertLucene(format(lucene, contentA, contentC, 2), file_NameA_ContentABCD, file_NameAB_ContentABC);
        assertLucene(format(lucene, contentA, contentC, 3), file_NameA_ContentABCD, file_NameAB_ContentABC);

        assertLucene(format(lucene, contentA, contentD, 0));
        assertLucene(format(lucene, contentA, contentD, 1));
        assertLucene(format(lucene, contentA, contentD, 2), file_NameA_ContentABCD);
        assertLucene(format(lucene, contentA, contentD, 3), file_NameA_ContentABCD);
    }

    @TestRail(section = {TestGroup.SEARCH, TestGroup.TAGS}, executionType = ExecutionType.REGRESSION,
            description = "Verify proximity queries with Lucene syntax work correctly for specific property")
    @Test(groups = {TestGroup.SEARCH, TestGroup.TAGS, TestGroup.REGRESSION})
    public void testProximitySearchUsingLuceneSyntaxForSpecificProperty()
    {
        final String lucene = "@cm\\:name:\"%s %s\"~%d";

        assertLucene(format(lucene, nameA, nameB, 0), file_NameAB_ContentABC, file_NameABC_ContentAB, file_NameABCD_ContentA);
        assertLucene(format(lucene, nameA, nameB, 1), file_NameAB_ContentABC, file_NameABC_ContentAB, file_NameABCD_ContentA);
        assertLucene(format(lucene, nameA, nameB, 2), file_NameAB_ContentABC, file_NameABC_ContentAB, file_NameABCD_ContentA);
        assertLucene(format(lucene, nameA, nameB, 3), file_NameAB_ContentABC, file_NameABC_ContentAB, file_NameABCD_ContentA);

        assertLucene(format(lucene, nameA, nameC, 0));
        assertLucene(format(lucene, nameA, nameC, 1), file_NameABC_ContentAB, file_NameABCD_ContentA);
        assertLucene(format(lucene, nameA, nameC, 2), file_NameABC_ContentAB, file_NameABCD_ContentA);
        assertLucene(format(lucene, nameA, nameC, 3), file_NameABC_ContentAB, file_NameABCD_ContentA);

        assertLucene(format(lucene, nameA, nameD, 0));
        assertLucene(format(lucene, nameA, nameD, 1));
        assertLucene(format(lucene, nameA, nameD, 2), file_NameABCD_ContentA);
        assertLucene(format(lucene, nameA, nameD, 3), file_NameABCD_ContentA);
    }

    private void assertLucene(String query, String... expected)
    {
        assertQueryResult("lucene", query, expected);
    }

    private void assertAFTS(String query, String... expected)
    {
        assertQueryResult("afts", query, expected);
    }

    private void assertQueryResult(String language, String query, String... expected)
    {
        final SearchRequest searchRequest = req(language, query);
        if (expected.length == 0)
        {
            searchQueryService.expectNoResultsFromQuery(searchRequest, testUser);
        }
        else
        {
            searchQueryService.expectNodeRefsFromQuery(searchRequest, testUser, expected);
        }
    }

    private String createContent(String filename, String content)
    {
        return dataContent
                .usingAdmin()
                .usingResource(contentRoot())
                .createContent(new FileModel(filename, FileType.TEXT_PLAIN, content))
                .getNodeRef();
    }

    private static ContentModel contentRoot()
    {
        final ContentModel root = new ContentModel("-root-");
        root.setNodeRef(root.getName());
        return root;
    }

    private String join(String... parts)
    {
        return String.join(" ", parts);
    }

    private static String uniqueString()
    {
        final UUID unique = UUID.randomUUID();
        final StringBuilder result = new StringBuilder(Long.SIZE / 2);
        for (long l : List.of(unique.getMostSignificantBits(), unique.getLeastSignificantBits()))
        {
            for (int i = Long.SIZE - 4; i >= 0; i -= 4)
            {
                int ch = 'a' + (byte) ((((l & (0xFL << i)) >> i)) & 0xFL);
                result.append(Character.toChars(ch));
            }
        }
        return result.toString();
    }
}
