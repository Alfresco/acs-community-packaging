package org.alfresco.elasticsearch.reindexing;

import static java.lang.String.format;

import static org.alfresco.elasticsearch.SearchQueryService.req;
import static org.alfresco.utility.report.log.Step.STEP;

import org.springframework.beans.factory.annotation.Autowired;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import org.alfresco.rest.repo.resource.Categories;
import org.alfresco.rest.search.SearchRequest;
import org.alfresco.utility.model.TestGroup;

/**
 * Tests verifying live indexing of secondary children and ANCESTOR index in Elasticsearch.
 */
@SuppressWarnings({"PMD.JUnitTestsShouldIncludeAssert", "PMD.MethodNamingConventions", "PMD.LocalVariableNamingConventions"}) // these are testng tests and use searchQueryService.expectResultsFromQuery for assertion
public class NodeWithCategoryIndexingTests extends NodesSecondaryChildrenRelatedTests
{

    @Autowired
    private Categories categories;

    /* A --- B (folders) \____ \ K --- L (categories) */
    @BeforeClass(alwaysRun = true)
    @Override
    public void dataPreparation()
    {
        super.dataPreparation();

        // given
        STEP("Create nested folders in site's Document Library.");
        folders.add().nestedRandomFolders(A, B).create();

        STEP("Create nested categories.");
        categories.add().nestedRandomCategories(K, L).create();

        STEP("Link folders to category.");
        folders.modify(A).linkTo(categories.get(L));
    }

    @Test(groups = TestGroup.SEARCH)
    public void testParentQueryAgainstCategory()
    {
        // then
        STEP("Verify that searching by PARENT and category will find one descendant node: categoryL.");
        SearchRequest query = req("PARENT:" + categories.get(K).getId());
        searchQueryService.expectResultsFromQuery(query, testUser, categories.get(L).getName());
    }

    @Test(groups = TestGroup.SEARCH)
    public void testParentQueryAgainstFolder()
    {
        // then
        STEP("Verify that searching by PARENT and category will find one descendant node: folderA.");
        SearchRequest query = req("PARENT:" + categories.get(L).getId());
        searchQueryService.expectResultsFromQuery(query, testUser, folders.get(A).getName());
    }

    @Test(groups = TestGroup.SEARCH)
    public void testParentQueryAgainstFolderAfterCategoryDeletion()
    {
        // given
        STEP("Create nested folders in site's Document Library.");
        folders.add().randomFolder(C).create();

        STEP("Create nested categories.");
        categories.add().randomCategory(M).create();

        STEP("Link folders to category.");
        folders.modify(C).linkTo(categories.get(M));

        // when
        STEP("Verify that searching by PARENT and category will find one descendant node: folderC.");
        SearchRequest query = req("PARENT:" + categories.get(M).getId());
        searchQueryService.expectResultsFromQuery(query, testUser, folders.get(C).getName());

        // then
        STEP("Delete categoryM.");
        categories.delete(categories.get(M));

        STEP("Verify that searching by PARENT and deleted category will find no descendant nodes.");
        searchQueryService.expectResultsFromQuery(query, testUser);
    }

    @Test(groups = TestGroup.SEARCH, enabled = false) // re-enable after ACS-6588, ACS-6592
    public void testParentQueryAgainstFolderAfterParentCategoryDeletion()
    {
        // given
        STEP("Create nested folders in site's Document Library.");
        folders.add().randomFolder(X).create();

        STEP("Create nested categories.");
        categories.add().nestedRandomCategories(P, Q).create();

        STEP("Link folders to category.");
        folders.modify(X).linkTo(categories.get(Q));

        // when
        STEP("Verify that searching by PARENT and category will find one descendant node: folderC.");
        SearchRequest query = req("PARENT:" + categories.get(Q).getId());
        searchQueryService.expectResultsFromQuery(query, testUser, folders.get(X).getName());

        // then
        STEP("Delete categoryM.");
        categories.delete(categories.get(P));

        STEP("Verify that searching by PARENT and deleted category will find no descendant nodes.");
        searchQueryService.expectResultsFromQuery(query, testUser);
    }

    @Test(groups = TestGroup.SEARCH)
    public void testSearchByPath()
    {
        // given
        String Kname = categories.get(K).getName();
        String Lname = categories.get(L).getName();
        String Aname = folders.get(A).getName();

        // then
        STEP("Verify that searching by PATH and category will find: folderA");
        SearchRequest query = req(format("PATH:'/cm:categoryRoot/cm:generalclassifiable/cm:%s/cm:%s/cm:%s'", Kname, Lname, Aname));
        searchQueryService.expectResultsFromQuery(query, testUser, folders.get(A).getName());
    }

    @Test(groups = TestGroup.SEARCH)
    public void ensureCategoriesAreNotTransitive_lookingForExactChildren()
    {
        // given
        String Kname = categories.get(K).getName();
        String Lname = categories.get(L).getName();
        String Aname = folders.get(A).getName();
        String Bname = folders.get(B).getName();

        // then
        STEP("Verify that searching by PATH for nested folder will return no results (Dependency to category is not transitive)");
        SearchRequest query = req(format("PATH:'/cm:categoryRoot/cm:generalclassifiable/cm:%s/cm:%s/cm:%s/cm:%s'", Kname, Lname, Aname, Bname));
        searchQueryService.expectNoResultsFromQuery(query, testUser);
    }

    @Test(groups = TestGroup.SEARCH)
    public void ensureCategoriesAreNotTransitive_lookingForAllDescendants()
    {
        // given
        String Kname = categories.get(K).getName();
        String Lname = categories.get(L).getName();

        // then
        STEP("Verify that searching by PATH and category will find: folderA");
        SearchRequest query = req(format("PATH:'/cm:categoryRoot/cm:generalclassifiable/cm:%s/cm:%s//*'", Kname, Lname));
        searchQueryService.expectResultsFromQuery(query, testUser, folders.get(A).getName());
    }

    @Test(groups = TestGroup.SEARCH)
    public void ensureCategoriesAreNotTransitive_lookingForAllDescendants_byParentCategory()
    {
        // given
        String Kname = categories.get(K).getName();

        // then
        STEP("Verify that searching by PATH and category will find: folderA");
        SearchRequest query = req(format("PATH:'/cm:categoryRoot/cm:generalclassifiable/cm:%s//*'", Kname));
        searchQueryService.expectResultsFromQuery(query, testUser, categories.get(L).getName(), folders.get(A).getName());
    }
}
