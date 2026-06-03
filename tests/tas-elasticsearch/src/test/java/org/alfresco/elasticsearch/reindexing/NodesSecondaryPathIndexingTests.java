package org.alfresco.elasticsearch.reindexing;

import static org.alfresco.elasticsearch.SearchQueryService.req;
import static org.alfresco.utility.report.log.Step.STEP;

import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import org.alfresco.rest.search.SearchRequest;
import org.alfresco.utility.model.FileModel;
import org.alfresco.utility.model.FolderModel;
import org.alfresco.utility.model.TestGroup;

/**
 * Tests verifying live indexing of secondary children and PATH index in Elasticsearch.
 */
@SuppressWarnings({"PMD.JUnitTestsShouldIncludeAssert"}) // these are TAS E2E tests and use searchQueryService.expectResultsFromQuery for assertion
public class NodesSecondaryPathIndexingTests extends NodesSecondaryChildrenRelatedTests
{

    private FileModel fileInP;

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
     *  += fP += file -+ fA
     *  += fQ
     *  += fR
     *  += fS
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
        STEP("Create few sets of nested folders in site's Document Library.");
        folders.add().nestedRandomFolders(A, B, C).create();
        folders.add().nestedRandomFolders(K, L, M).create();
        folders.add().nestedRandomFolders(X, Y, Z).create();
        folders.add().randomFolders(P, Q, R, S).create();
        fileInP = folders.modify(P).add().randomFile().create();

        STEP("Create few secondary parent-child relationships.");
        folders.modify(K).add().secondaryContent(folders.get(B));
        folders.modify(X).add().secondaryContent(folders.get(K));
        folders.modify(L).add().secondaryContent(folders.get(C), folders.get(Y));
        folders.modify(M).add().secondaryContent(folders.get(C));
        folders.modify(A).add().secondaryContent(fileInP);

        STEP("Add to folderQ a secondary child folderR.");
        folders.modify(Q).add().secondaryContent(folders.get(R));

        // when
        STEP("Delete the secondary parent relationship between folderQ and FolderR.");
        folders.modify(Q).remove().secondaryContent(folders.get(R));
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
    public void testSecondaryPathWithNodeHavingDocumentAsSecondaryChild()
    {
        // then
        STEP("Verify that a file being a secondary child of folderA can be found using PATH index.");
        SearchRequest query = req("PATH:\"//cm:" + folders.get(A).getName() + "//*\"");
        searchQueryService.expectResultsFromQuery(query, testUser,
                // primary path
                folders.get(B).getName(),
                folders.get(C).getName(),
                // secondary path
                fileInP.getName());
    }

    @Test(groups = TestGroup.SEARCH)
    public void testSecondaryPathWithNodeHavingComplexSecondaryRelationship()
    {
        // then
        STEP("Verify that all secondary children of folderX can be found.");
        SearchRequest query = req("PATH:\"//cm:" + folders.get(X).getName() + "//*\"");
        searchQueryService.expectResultsFromQuery(query, testUser,
                // primary path
                folders.get(Y).getName(),
                folders.get(Z).getName(),
                // secondary path
                folders.get(B).getName(),
                folders.get(C).getName(),
                folders.get(K).getName(),
                folders.get(L).getName(),
                folders.get(M).getName());
    }

    /**
     * Verify that removing secondary parent-child relationship will result in updating ES index: PATH. Test changes below folders hierarchy:
     * 
     * <pre>
     * DL
     *  += fQ
     *     +
     *     |
     *  += fR
     * </pre>
     * 
     * into:
     * 
     * <pre>
     * DL += fQ += fR
     * </pre>
     */
    @Test(groups = TestGroup.SEARCH)
    public void testSecondaryPathWithDeletedSecondaryRelationship()
    {
        // then
        STEP("Verify that folderR cannot be found by PATH and folderQ anymore.");
        SearchRequest query = req("PATH:\"//cm:" + folders.get(Q).getName() + "//*\"");
        searchQueryService.expectNoResultsFromQuery(query, testUser);
    }

    /**
     * Verify that removing a node D (fD) having a secondary children relationship will remove the relationships and update PATH index in ES. Test changes below folders hierarchy from:
     * 
     * <pre>
     * DL
     *  += fQ
     *     +
     *     |
     *  += fE += fF
     *     +
     *     |
     *  += fR
     * </pre>
     * 
     * into:
     * 
     * <pre>
     * DL += fQ += fR
     * </pre>
     */
    @Test(groups = TestGroup.SEARCH)
    public void testSecondaryPathWithDeletedSecondaryParentNode()
    {
        // given
        STEP("Create two nested folders (E and F) in Document Library.");
        FolderModel folderE = folders.add().randomFolder("E").create();
        FolderModel folderF = folders.modify(folderE).add().randomFolder("F").create();
        STEP("Make folderE a secondary children of folderQ and folderR a secondary children of folderE.");
        folders.modify(Q).add().secondaryContent(folderE);
        folders.modify(folderE).add().secondaryContent(folders.get(R));

        STEP("Verify that searching by PATH and folderQ will find nodes: folderE, folderF and folderR.");
        SearchRequest queryPathQ = req("PATH:\"//cm:" + folders.get(Q).getName() + "//*\"");
        searchQueryService.expectResultsFromQuery(queryPathQ, testUser,
                // secondary path
                folderE.getName(),
                folderF.getName(),
                folders.get(R).getName());
        STEP("Verify that searching by PATH and folderE will find its primary and secondary children: folderF and folderR.");
        SearchRequest queryPathD = req("PATH:\"//cm:" + folderE.getName() + "//*\"");
        searchQueryService.expectResultsFromQuery(queryPathD, testUser,
                // primary path
                folderF.getName(),
                // secondary path
                folders.get(R).getName());

        // when
        STEP("Delete folderE and verify that PATH was updated for nodes folderQ and folderR.");
        folders.modify(folderE).delete();

        // then
        searchQueryService.expectNoResultsFromQuery(queryPathQ, testUser);
        searchQueryService.expectNoResultsFromQuery(queryPathD, testUser);
    }

    /**
     * Verify that moving folder D (fD) containing secondary children from hierarchy:
     * 
     * <pre>
     * DL
     *  += fQ += fD +- fP += file
     *  += fR
     * </pre>
     * 
     * to:
     * 
     * <pre>
     * DL
     *  += fQ
     *  += fR += fD +- fP += file
     * </pre>
     * 
     * will update PATH index in ES.
     */
    @Test(groups = TestGroup.SEARCH)
    public void testSecondaryPathWithMovedSecondaryParentNode()
    {
        // given
        STEP("Create folderD inside folderQ, and add folderP as a secondary child.");
        FolderModel folderD = folders.modify(Q).add().randomFolder("D").create();
        folders.modify(folderD).add().secondaryContent(folders.get(P));
        folders.modify(M).add().secondaryContent(folderD);

        STEP("Verify that searching by PATH and folderD will find nodes: folderP and file.");
        SearchRequest queryPathD = req("PATH:\"//cm:" + folderD.getName() + "//*\"");
        searchQueryService.expectResultsFromQuery(queryPathD, testUser,
                // secondary path
                folders.get(P).getName(),
                fileInP.getName());
        STEP("Verify that searching by PATH and folderQ will find nodes: folderD, folderP and file.");
        SearchRequest queryPathQ = req("PATH:\"//cm:" + folders.get(Q).getName() + "//*\"");
        searchQueryService.expectResultsFromQuery(queryPathQ, testUser,
                // primary path
                folderD.getName(),
                // secondary path
                folders.get(P).getName(),
                fileInP.getName());

        // when
        STEP("Move folderD from folderQ to folderR.");
        folders.modify(folderD).moveTo(folders.get(R));

        // then
        STEP("Verify that search result for PATH and folderD didn't change.");
        searchQueryService.expectResultsFromQuery(queryPathD, testUser,
                // secondary path
                folders.get(P).getName(),
                fileInP.getName());
        STEP("Verify that searching by PATH and folderQ doesn't return any node anymore.");
        searchQueryService.expectNoResultsFromQuery(queryPathQ, testUser);
        STEP("Verify that searching by PATH and folderR will find nodes: folderD, folderP and file.");
        SearchRequest queryPathR = req("PATH:\"//cm:" + folders.get(R).getName() + "//*\"");
        searchQueryService.expectResultsFromQuery(queryPathR, testUser,
                // primary path
                folderD.getName(),
                // secondary path
                folders.get(P).getName(),
                fileInP.getName());

        STEP("Clean-up - delete folderD.");
        folders.modify(folderD).delete();
    }

    /**
     * Verify that copying folder will also result in copying folder's secondary children and update PATH index in ES. Test changes below folders hierarchy:
     * 
     * <pre>
     * DL
     *  += fS += fG += fH
     *         +
     *        /
     *  += fP += file
     *  += fT
     * </pre>
     * 
     * into:
     * 
     * <pre>
     * DL
     *  += fS += fG += fH
     *         +
     *        /
     *  += fP += file
     *        \
     *         +
     *  += fT += fG-c += fH-c
     * </pre>
     */
    @Test(groups = TestGroup.SEARCH)
    public void testSecondaryParentWithCopiedSecondaryParentNode()
    {
        // given
        STEP("Create nested folders (G and H) inside folderS and folderT in Document Library. Make folderP a secondary child of folderG.");
        FolderModel folderG = folders.modify(S).add().randomFolder("G").create();
        FolderModel folderH = folders.modify(folderG).add().randomFolder("H").create();
        FolderModel folderT = folders.add().randomFolder("T").create();
        folders.modify(folderG).add().secondaryContent(folders.get(P));

        STEP("Verify that searching by PATH and folderG will find nodes: folderH, folderP and file.");
        SearchRequest queryPathG = req("PATH:\"//cm:" + folderG.getName() + "//*\"");
        searchQueryService.expectResultsFromQuery(queryPathG, testUser,
                // primary path
                folderH.getName(),
                // secondary path
                folders.get(P).getName(),
                fileInP.getName());
        STEP("Verify that searching by PATH and folderS will find nodes: folderG, folderH, folderP and file.");
        SearchRequest queryPathS = req("PATH:\"//cm:" + folders.get(S).getName() + "//*\"");
        searchQueryService.expectResultsFromQuery(queryPathS, testUser,
                // primary path
                folderG.getName(),
                folderH.getName(),
                // secondary path
                folders.get(P).getName(),
                fileInP.getName());

        // when
        STEP("Copy folderG with its content to folderT.");
        FolderModel folderGCopy = folders.modify(folderG).copyTo(folderT);

        // then
        STEP("Verify that search result for PATH and folderS didn't change.");
        searchQueryService.expectResultsFromQuery(queryPathS, testUser,
                // primary path
                folderH.getName(),
                folderG.getName(),
                // secondary path
                folders.get(P).getName(),
                fileInP.getName());
        STEP("Verify that searching by PATH and folderS/folderG will find nodes: folderH, folderP and file in P.");
        SearchRequest queryPathSG = req("PATH:\"//cm:" + folders.get(S).getName() + "/cm:" + folderG.getName() + "//*\"");
        searchQueryService.expectResultsFromQuery(queryPathSG, testUser,
                // primary path
                folderH.getName(),
                // secondary path
                folders.get(P).getName(),
                fileInP.getName());
        STEP("Verify that folderG was copied with secondary parent-child relationship and PATH reflects that - search by folderT/folderGCopy should find nodes: folderH, folderP and file in P.");
        SearchRequest queryPathTGCopy = req("PATH:\"//cm:" + folderT.getName() + "/cm:" + folderGCopy.getName() + "//*\"");
        searchQueryService.expectResultsFromQuery(queryPathTGCopy, testUser,
                // primary path
                folderH.getName(), // the same name as folderH-copy
                // secondary path
                folders.get(P).getName(),
                fileInP.getName());

        STEP("Clean-up - delete folderG and folderT (with G's copy).");
        folders.modify(folderG).delete();
        folders.modify(folderT).delete();
    }
}
