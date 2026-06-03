package org.alfresco.elasticsearch.reindexing;

import static org.alfresco.elasticsearch.SearchQueryService.req;
import static org.alfresco.utility.report.log.Step.STEP;

import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import org.alfresco.rest.search.SearchRequest;
import org.alfresco.tas.AlfrescoStackInitializer;
import org.alfresco.utility.model.TestGroup;

@SuppressWarnings({"PMD.JUnitTestsShouldIncludeAssert"}) // these are TAS E2E tests and use searchQueryService.expectResultsFromQuery for assertion
public class PathFieldsReindexingTests extends NodesSecondaryChildrenRelatedTests
{
    /**
     * Creates a user and a private site containing below hierarchy of folders.
     * 
     * <pre>
     * Site
     * DL (Document Library)
     *  += fA += fB += fC (folderC)
     *         /     / |
     *        +     +  +
     *  += fK += fL += fM
     *     |     +
     *     +     |
     *  += fX += fY += fZ
     * </pre>
     * 
     * Parent += Child - primary parent-child relationship Parent +- Child - secondary parent-child relationship
     */
    @BeforeClass(alwaysRun = true)
    @Override
    public void dataPreparation()
    {
        super.dataPreparation();

        // given
        STEP("Create some nested folders.");
        folders.add().nestedRandomFolders(A, B, C).create();
        folders.add().nestedRandomFolders(K, L, M).create();
        folders.add().nestedRandomFolders(X, Y, Z).create();

        STEP("Add some extra secondary parent relationships.");
        folders.modify(K).add().secondaryContent(folders.get(B));
        folders.modify(X).add().secondaryContent(folders.get(K));
        folders.modify(L).add().secondaryContent(folders.get(C), folders.get(Y));
        folders.modify(M).add().secondaryContent(folders.get(C));
    }

    @Test(groups = TestGroup.SEARCH)
    public void testPrimaryParentField()
    {
        STEP("Check that only M has L as a primary parent.");
        SearchRequest query = req("PRIMARYPARENT:" + folders.get(L).getNodeRef());
        searchQueryService.expectResultsFromQuery(query, testUser,
                folders.get(M).getName());
    }

    @Test(groups = TestGroup.SEARCH)
    public void testParentFieldIncludesSecondaryParents()
    {
        STEP("Check that three nodes have L as a primary or secondary parent.");
        SearchRequest query = req("PARENT:" + folders.get(L).getNodeRef());
        searchQueryService.expectResultsFromQuery(query, testUser,
                folders.get(C).getName(), folders.get(M).getName(), folders.get(Y).getName());
    }

    @Test(groups = TestGroup.SEARCH)
    public void testAncestorFieldIncludesSecondaryAssociations()
    {
        STEP("Check which nodes have K as an ancestor.");
        SearchRequest query = req("ANCESTOR:" + folders.get(K).getNodeRef());
        searchQueryService.expectResultsFromQuery(query, testUser,
                folders.get(B).getName(), folders.get(C).getName(), folders.get(L).getName(), folders.get(M).getName(),
                folders.get(Y).getName(), folders.get(Z).getName());
    }

    @Test(groups = TestGroup.SEARCH)
    public void testSecondaryPathWithNodeHavingOneSecondaryChild()
    {
        // then
        STEP("Verify that folderC can be found by secondary PATH using secondary parent folderM.");
        SearchRequest query = req("PATH:\"//cm:" + folders.get(M).getName() + "//*\"");
        searchQueryService.expectResultsFromQuery(query, testUser,
                // secondary path
                folders.get(C).getName());
    }

    @Test(groups = TestGroup.SEARCH)
    public void testSecondaryPathWithNodeHavingOnePrimaryAndTwoSecondaryChildren()
    {
        // then
        STEP("Verify that primary and secondary children of folderL can be found using PATH index.");
        SearchRequest query = req("PATH:\"//cm:" + folders.get(L).getName() + "//*\"");
        searchQueryService.expectResultsFromQuery(query, testUser,
                // primary path
                folders.get(M).getName(),
                // secondary path
                folders.get(C).getName(),
                folders.get(Y).getName(),
                folders.get(Z).getName());
    }

    @Test(groups = TestGroup.SEARCH)
    public void testQueryThroughSecondaryPath()
    {
        // then
        STEP("Verify we can get direct children of a secondary paths (X-K=L) referenced in a query.");
        SearchRequest query = req("PATH:\"//cm:" + folders.get(X).getName() + "//cm:" + folders.get(L).getName() + "/*\"");
        searchQueryService.expectResultsFromQuery(query, testUser,
                // primary path from L
                folders.get(M).getName(),
                // secondary path from L
                folders.get(C).getName(),
                folders.get(Y).getName());
    }

    @Test(groups = TestGroup.SEARCH)
    public void testSecondaryPathWithNodeHavingComplexSecondaryRelationship()
    {
        // then
        STEP("Verify we can find all secondary descendents (excluding direct children) of folderX.");
        SearchRequest query = req("PATH:\"//cm:" + folders.get(X).getName() + "//*//*\"");
        searchQueryService.expectResultsFromQuery(query, testUser,
                // primary path
                folders.get(Z).getName(),
                // secondary path
                folders.get(Y).getName(), // Y _is_ a direct primary child, but this query should only find it via the secondary path.
                folders.get(B).getName(),
                folders.get(C).getName(),
                folders.get(L).getName(),
                folders.get(M).getName());
    }
}
