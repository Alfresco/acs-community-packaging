/*
 * #%L
 * Alfresco Tas Elasticsearch
 * %%
 * Copyright (C) 2026 Alfresco Software Limited
 * %%
 * This file is part of the Alfresco software.
 * If the software was purchased under a paid Alfresco license, the terms of
 * the paid license agreement will prevail.  Otherwise, the software is
 * provided under the following open source license terms:
 *
 * Alfresco is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Alfresco is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Alfresco. If not, see <http://www.gnu.org/licenses/>.
 * #L%
 */
package org.alfresco.elasticsearch;

import static org.alfresco.elasticsearch.SearchQueryService.req;
import static org.alfresco.utility.report.log.Step.STEP;

import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import org.alfresco.rest.search.SearchRequest;
import org.alfresco.utility.model.FileModel;
import org.alfresco.utility.model.FolderModel;
import org.alfresco.utility.model.TestGroup;

/**
 * Tests verifying indexing of secondary children and ANCESTOR index in Elasticsearch.
 */
@SuppressWarnings({"PMD.JUnitTestsShouldIncludeAssert"}) // these are TAS E2E tests and use searchQueryService.expectResultsFromQuery for assertion
public class NodesSecondaryAncestorIndexingTests extends NodesSecondaryChildrenRelatedTests
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
    }

    @Test(groups = TestGroup.SEARCH)
    public void testSecondaryAncestorWithNodeHavingOneSecondaryChild()
    {
        // then
        STEP("Verify that searching by ANCESTOR and folderM will find one descendant node: folderC.");
        SearchRequest query = req("ANCESTOR:" + folders.get(M).getNodeRef());
        searchQueryService.expectResultsFromQuery(query, testUser,
                folders.get(C).getName());
    }

    @Test(groups = TestGroup.SEARCH)
    public void testSecondaryAncestorWithNodeHavingTwoSecondaryChildren()
    {
        // then
        STEP("Verify that searching by ANCESTOR and folderL will find nodes: folderM, folderC, folderY and folderZ.");
        SearchRequest queryAncestorL = req("ANCESTOR:" + folders.get(L).getNodeRef());
        searchQueryService.expectResultsFromQuery(queryAncestorL, testUser,
                // primary descendant
                folders.get(M).getName(),
                // secondary descendants
                folders.get(C).getName(),
                folders.get(Y).getName(),
                folders.get(Z).getName());
    }

    @Test(groups = TestGroup.SEARCH)
    public void testSecondaryAncestorWithDocumentAsSecondaryChild()
    {
        // then
        STEP("Verify that searching by ANCESTOR and folderA will find nodes: folderB, folderC and fileInP.");
        SearchRequest query = req("ANCESTOR:" + folders.get(A).getNodeRef());
        searchQueryService.expectResultsFromQuery(query, testUser,
                // primary descendants
                folders.get(B).getName(),
                folders.get(C).getName(),
                // secondary descendant
                fileInP.getName());
    }

    @Test(groups = TestGroup.SEARCH)
    public void testSecondaryAncestorWithNodeHavingComplexSecondaryRelationship()
    {
        // then
        STEP("Verify that all descendant of folderX can be found.");
        SearchRequest query = req("ANCESTOR:" + folders.get(X).getNodeRef());
        searchQueryService.expectResultsFromQuery(query, testUser,
                // primary descendants
                folders.get(Y).getName(),
                folders.get(Z).getName(),
                // secondary descendants
                folders.get(B).getName(),
                folders.get(C).getName(),
                folders.get(K).getName(),
                folders.get(L).getName(),
                folders.get(M).getName());
    }

    /**
     * Verify that removing secondary parent-child relationship will result in updating ES index: ANCESTOR. Test changes below folders hierarchy:
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
    public void testSecondaryAncestorWithDeletedSecondaryRelationship()
    {
        // given
        STEP("Add to folderQ a secondary child folderR and verify if it can be found using ANCESTOR index and secondary child node reference.");
        folders.modify(Q).add().secondaryContent(folders.get(R));

        STEP("Verify that searching by ANCESTOR and folderQ will find secondary descendant node: folderR.");
        SearchRequest query = req("ANCESTOR:" + folders.get(Q).getNodeRef());
        searchQueryService.expectResultsFromQuery(query, testUser,
                // secondary descendant
                folders.get(R).getName());

        // when
        STEP("Delete the secondary parent-child relationship between folderQ and FolderR.");
        folders.modify(Q).remove().secondaryContent(folders.get(R));

        // then
        STEP("Verify that folderQ cannot be found by ANCESTOR and folderQ anymore.");
        searchQueryService.expectNoResultsFromQuery(query, testUser);
    }

    /**
     * Verify that removing a node D (fD) having a secondary children relationship will remove the relationships and update ANCESTOR index in ES. Test changes below folders hierarchy from:
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
    public void testSecondaryAncestorWithDeletedSecondaryParentNode()
    {
        // given
        STEP("Create two nested folders (E and F) in Document Library.");
        FolderModel folderE = folders.add().randomFolder("E").create();
        FolderModel folderF = folders.modify(folderE).add().randomFolder("F").create();
        STEP("Make folderE a secondary children of folderQ and folderR a secondary children of folderE.");
        folders.modify(Q).add().secondaryContent(folderE);
        folders.modify(folderE).add().secondaryContent(folders.get(R));

        STEP("Verify that searching by ANCESTOR and folderQ will find its secondary descendant: folderE, folderF and folderR.");
        SearchRequest queryAncestorQ = req("ANCESTOR:" + folders.get(Q).getNodeRef());
        searchQueryService.expectResultsFromQuery(queryAncestorQ, testUser,
                // secondary descendants
                folderE.getName(),
                folderF.getName(),
                folders.get(R).getName());

        // when
        STEP("Delete folderE with its content.");
        folders.modify(folderE).delete();

        // then
        STEP("Verify that searching by ANCESTOR and folderQ will not find any nodes.");
        searchQueryService.expectNoResultsFromQuery(queryAncestorQ, testUser);
    }

    /**
     * Verify that moving folderD (fD) containing secondary children from hierarchy:
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
     * will update ANCESTOR index in ES.
     */
    @Test(groups = TestGroup.SEARCH)
    public void testSecondaryAncestorWithMovedSecondaryParentNode()
    {
        // given
        STEP("Create folderD inside folderQ, and add folderP to D as a secondary child.");
        FolderModel folderD = folders.modify(Q).add().randomFolder("D").create();
        folders.modify(folderD).add().secondaryContent(folders.get(P));

        STEP("Verify that searching by ANCESTOR and folderQ will find its primary and secondary descendant nodes: folderD, folderP and file.");
        SearchRequest queryAncestorQ = req("ANCESTOR:" + folders.get(Q).getNodeRef());
        searchQueryService.expectResultsFromQuery(queryAncestorQ, testUser,
                // primary descendant
                folderD.getName(),
                // secondary descendants
                folders.get(P).getName(),
                fileInP.getName());
        STEP("Verify that searching by ANCESTOR and folderR will not find any descendant nodes.");
        SearchRequest queryAncestorR = req("ANCESTOR:" + folders.get(R).getNodeRef());
        searchQueryService.expectNoResultsFromQuery(queryAncestorR, testUser);

        // when
        STEP("Move folderD from folderQ to folderR.");
        folders.modify(folderD).moveTo(folders.get(R));

        // then
        STEP("Verify that search result for ANCESTOR and folderQ will not find any descendant anymore.");
        searchQueryService.expectNoResultsFromQuery(queryAncestorQ, testUser);
        STEP("Verify that searching by ANCESTOR and folderR will find its primary and secondary descendant nodes: folderD, folderP and file.");
        searchQueryService.expectResultsFromQuery(queryAncestorR, testUser,
                // primary descendant
                folderD.getName(),
                // secondary descendants
                folders.get(P).getName(),
                fileInP.getName());

        STEP("Clean-up - delete folderD.");
        folders.modify(folderD).delete();
    }

    /**
     * Verify that copying folder will also result in copying folder's secondary children and update ANCESTOR index in ES. Test changes below folders hierarchy:
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
    public void testSecondaryAncestorWithCopiedSecondaryParentNode() throws InterruptedException {
        // given
        STEP("Create nested folders (G and H) inside folderS and folderT in Document Library. Make folderP a secondary child of folderG.");
        FolderModel folderG = folders.modify(S).add().randomFolder("G").create();
        FolderModel folderH = folders.modify(folderG).add().randomFolder("H").create();
        FolderModel folderT = folders.add().randomFolder("T").create();
        folders.modify(folderG).add().secondaryContent(folders.get(P));

        Thread.sleep(30_000);

        STEP("Verify that searching by ANCESTOR and folderS will find its descendant nodes: folderG, folderH, folderP and file in P.");
        SearchRequest queryAncestorS = req("ANCESTOR:" + folders.get(S).getNodeRef());
        searchQueryService.expectResultsFromQuery(queryAncestorS, testUser,
                // primary descendants
                folderG.getName(),
                folderH.getName(),
                // secondary descendants
                folders.get(P).getName(),
                fileInP.getName());
        STEP("Verify that searching by ANCESTOR and folderT will not find any nodes.");
        SearchRequest queryAncestorT = req("ANCESTOR:" + folderT.getNodeRef());
        searchQueryService.expectNoResultsFromQuery(queryAncestorT, testUser);

        // when
        STEP("Copy folderG with its content to folderT.");
        FolderModel folderGCopy = folders.modify(folderG).copyTo(folderT);

        // then
        STEP("Verify that searching by ANCESTOR and folderS will find its descendant nodes: folderG, folderH, folderP and file in P.");
        searchQueryService.expectResultsFromQuery(queryAncestorS, testUser,
                // primary descendants
                folderG.getName(),
                folderH.getName(),
                // secondary descendants
                folders.get(P).getName(),
                fileInP.getName());
        STEP("Verify that searching by ANCESTOR and folderT will find its descendant nodes: folderG-copy, folderH-copy, folderP, file.");
        searchQueryService.expectResultsFromQuery(queryAncestorT, testUser,
                // primary descendants
                folderGCopy.getName(),
                folderH.getName(), // the same name as folderH-copy
                // secondary descendants
                folders.get(P).getName(),
                fileInP.getName());

        STEP("Clean-up - delete folderG and folderT (with G's copy).");
        folders.modify(folderG).delete();
        folders.modify(folderT).delete();
    }
}
