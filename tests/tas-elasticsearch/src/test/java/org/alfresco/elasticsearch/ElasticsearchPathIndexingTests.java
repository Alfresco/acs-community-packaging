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

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import org.alfresco.rest.search.SearchRequest;
import org.alfresco.tas.AlfrescoStackInitializer;
import org.alfresco.utility.data.DataContent;
import org.alfresco.utility.data.DataSite;
import org.alfresco.utility.data.DataUser;
import org.alfresco.utility.model.ContentModel;
import org.alfresco.utility.model.FolderModel;
import org.alfresco.utility.model.TestGroup;
import org.alfresco.utility.network.ServerHealth;
import org.alfresco.utility.report.log.Step;

/**
 * Tests to verify indexing of paths using Elasticsearch.
 */
@ContextConfiguration(locations = "classpath:alfresco-elasticsearch-context.xml",
        initializers = AlfrescoStackInitializer.class)
@SuppressWarnings({"PMD.JUnitTestsShouldIncludeAssert", "PMD.JUnit4TestShouldUseTestAnnotation"}) // these are testng tests and use searchQueryService.expectResultsFromQuery for assertion
public class ElasticsearchPathIndexingTests extends AbstractTestNGSpringContextTests
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
    protected SearchQueryService searchQueryService;

    private org.alfresco.utility.model.UserModel testUser;

    private org.alfresco.utility.model.SiteModel testSite;

    private List<FolderModel> testFolders;

    private String testFileName;
    private String filenameWhichIncludesWhitespace = "TestFile " + UUID.randomUUID() + ".txt";
    private String testFileNameWithWhitespace;

    /**
     * Create a user and a private site containing some nested folders with a document in.
     */
    @BeforeClass(alwaysRun = true)
    public void dataPreparation()
    {
        serverHealth.isServerReachable();
        serverHealth.assertServerIsOnline();

        Step.STEP("Create a test user and private site containing three nested folders and a document.");
        testUser = dataUser.createRandomTestUser();
        testSite = dataSite.usingUser(testUser).createPrivateRandomSite();

        testFolders = createNestedFolders(3);

        testFileName = createDocument(testFolders.get(testFolders.size() - 1));

        testFileNameWithWhitespace = createDocument(testFolders.get(testFolders.size() - 1), filenameWhichIncludesWhitespace);
    }

    @Test(groups = TestGroup.SEARCH)
    public void testSimple()
    {
        SearchRequest query = req("PATH:\"//cm:" + testFileName + "\"");
        searchQueryService.expectResultsFromQuery(query, testUser, testFileName);
    }

    @Test(groups = TestGroup.SEARCH)
    public void testRelativePathQuery()
    {
        SearchRequest query = req("PATH:\"//cm:" + testFileName + "\" ");
        searchQueryService.expectResultsFromQuery(query, testUser, testFileName);
    }

    @Test(groups = TestGroup.SEARCH)
    public void testRelativePathQueryWithoutPrefixes()
    {
        SearchRequest query = req("PATH:\"//" + testFileName + "\" AND name:*");
        searchQueryService.expectResultsFromQuery(query, testUser, testFileName);
    }

    @Test(groups = TestGroup.SEARCH)
    public void testWildcardQuery()
    {
        // The test file should be the only descendent of the last folder.
        SearchRequest query = req("PATH:\"//" + testSite.getId() + "//" + testFolders.get(testFolders.size() - 1).getName() + "/*\" AND name:*");
        searchQueryService.expectResultsFromQuery(query, testUser, testFileName, testFileNameWithWhitespace);
    }

    @Test(groups = TestGroup.SEARCH)
    public void testWildcardQueryWithNamespaces()
    {
        // The test file should be the only descendent of the last folder.
        SearchRequest query = req("PATH:\"//cm:" + testSite.getId() + "//cm:" + testFolders.get(testFolders.size() - 1).getName() + "/*\" AND name:*");
        searchQueryService.expectResultsFromQuery(query, testUser, testFileName, testFileNameWithWhitespace);
    }

    @Test(groups = TestGroup.SEARCH)
    public void testAbsolutePathQuery()
    {
        String folderPath = testFolders.stream().map(folder -> "cm:" + folder.getName()).collect(Collectors.joining("/"));
        SearchRequest query = req("PATH:\"/app:company_home/st:sites/cm:" + testSite.getId() + "/cm:documentLibrary/" + folderPath + "/cm:" + testFileName + "\" AND cm:name:*");
        searchQueryService.expectResultsFromQuery(query, testUser, testFileName);
    }

    @Test(groups = TestGroup.SEARCH)
    public void testAbsolutePathQueryWithoutPrefixes()
    {
        String folderPath = testFolders.stream().map(folder -> folder.getName()).collect(Collectors.joining("/"));
        SearchRequest query = req("PATH:\"/company_home/sites/" + testSite.getId() + "/documentLibrary/" + folderPath + "/" + testFileName + "\" AND name:*");
        searchQueryService.expectResultsFromQuery(query, testUser, testFileName);
    }

    @Test(groups = TestGroup.SEARCH)
    public void testRootNodes()
    {
        SearchRequest query = req("PATH:\"/*\" AND name:*");
        searchQueryService.expectResultsFromQuery(query, testUser, "categories", "Company Home");
    }

    @Test(groups = TestGroup.SEARCH)
    public void testRootNodesWithoutWildcard()
    {
        SearchRequest query = req("PATH:\"/\"");
        searchQueryService.expectNodeTypesFromQuery(query, testUser, "sys:store_root");
    }

    @Test(groups = TestGroup.SEARCH)
    public void testPathNameMismatch()
    {
        SearchRequest query = req("PATH:\"/*\" AND name:" + testFileName + " AND name:*");
        searchQueryService.expectNoResultsFromQuery(query, testUser);
    }

    @Test(groups = TestGroup.SEARCH)
    public void testPathNameIntersect()
    {
        SearchRequest query = req("PATH:\"//*\" AND name:" + testFileName + " AND name:*");
        searchQueryService.expectResultsFromQuery(query, testUser, testFileName);
    }

    @Test(groups = TestGroup.SEARCH)
    public void testAllDescendentsOfFolder()
    {
        SearchRequest query = req("PATH:\"//" + testFolders.get(0).getName() + "//*\" AND name:*");
        searchQueryService.expectResultsFromQuery(query, testUser, testFileName, testFileNameWithWhitespace, testFolders.get(1).getName(), testFolders.get(2).getName());
    }

    @Test(groups = TestGroup.SEARCH)
    public void testSearchAllDescendentsOfFolderQueryWithNamespaces()
    {
        SearchRequest query = req("PATH:\"//cm:" + testFolders.get(0).getName() + "//*\" AND name:*");
        searchQueryService.expectResultsFromQuery(query, testUser, testFileName, testFileNameWithWhitespace, testFolders.get(1).getName(), testFolders.get(2).getName());
    }

    @Test(groups = TestGroup.SEARCH)
    public void testWhereFolderIsAncestor()
    {
        SearchRequest query = req("ANCESTOR:\"" + testFolders.get(2).getNodeRef() + "\" AND name:*");
        searchQueryService.expectResultsFromQuery(query, testUser, testFileName, testFileNameWithWhitespace);
    }

    @Test(groups = TestGroup.SEARCH)
    public void testAncestorWithWorkspaceReference()
    {
        SearchRequest query = req("ANCESTOR:\"workspace://SpacesStore/" + testFolders.get(0).getNodeRef() + "\" AND name:*");
        searchQueryService.expectResultsFromQuery(query, testUser, testFileName, testFileNameWithWhitespace, testFolders.get(1).getName(), testFolders.get(2).getName());
    }

    @Test(groups = TestGroup.SEARCH)
    public void testPrimaryParent()
    {
        SearchRequest query = req("PRIMARYPARENT:\"" + testFolders.get(1).getNodeRef() + "\" AND name:*");
        searchQueryService.expectResultsFromQuery(query, testUser, testFolders.get(2).getName());
    }

    @Test(groups = TestGroup.SEARCH)
    public void testParent()
    {
        SearchRequest query = req("PARENT:\"" + testFolders.get(1).getNodeRef() + "\" AND name:*");
        searchQueryService.expectResultsFromQuery(query, testUser, testFolders.get(2).getName());
    }

    @Test(groups = TestGroup.SEARCH)
    public void testAllFoldersInSite()
    {
        SearchRequest query = req("PATH:\"/*/sites/" + testSite.getId() + "/*//*\" AND TYPE:\"cm:folder\" AND name:*");
        String[] folderNames = testFolders.stream().map(ContentModel::getName).toArray(String[]::new);
        searchQueryService.expectResultsFromQuery(query, testUser, folderNames);
    }

    @Test(groups = TestGroup.SEARCH)
    public void testSearchAllFoldersInSiteQueryWithNamespaces()
    {
        SearchRequest query = req("PATH:\"/*/st:sites/cm:" + testSite.getId() + "/*//*\" AND TYPE:\"cm:folder\" AND name:*");
        String[] folderNames = testFolders.stream().map(ContentModel::getName).toArray(String[]::new);
        searchQueryService.expectResultsFromQuery(query, testUser, folderNames);
    }

    @Test(groups = TestGroup.SEARCH, enabled = false)
    public void testUpdatePath()
    {
        // disabled test: find a way to rename or move a folder on repository
        String folderPath = testFolders.stream().map(folder -> "cm:" + folder.getName()).collect(Collectors.joining("/"));
        SearchRequest query = req("PATH:\"/app:company_home/cm:" + testFileName + "\" AND cm:name:*");
        searchQueryService.expectResultsFromQuery(query, testUser, testFileName);
    }

    /**
     * Create a set of nested folders in the test site using the test user.
     *
     * @return The folder objects (containing the randomly generated names) in order of depth.
     */
    private List<FolderModel> createNestedFolders(int maxDepth)
    {
        List<FolderModel> folders = new ArrayList<>();
        dataContent.usingSite(testSite);
        for (int depth = 0; depth < maxDepth; depth++)
        {
            String folderName = "TestFolder" + depth + "_" + UUID.randomUUID();
            FolderModel folderModel = new FolderModel(folderName);
            folders.add(folderModel);
            if (depth != 0)
            {
                dataContent.usingResource(folders.get(depth - 1));
            }
            dataContent.usingUser(testUser)
                    .createFolder(folderModel);
        }
        return folders;
    }

    /**
     * Create a document in the given folder using the test user and a random filename.
     *
     * @param folderModel
     *            The location to create the document.
     * @return The randomly generated name of the new document.
     */
    private String createDocument(FolderModel folderModel)
    {
        return createDocument(folderModel, "TestFile" + UUID.randomUUID() + ".txt");
    }

    /**
     * Create a document in the given folder using the test user and the given filename.
     *
     * @param folderModel
     *            The location to create the document.
     * @param filename
     *            the filename.
     * @return the passed filename.
     */
    private String createDocument(FolderModel folderModel, String filename)
    {
        dataContent.usingUser(testUser)
                .usingResource(folderModel)
                .createContent(new org.alfresco.utility.model.FileModel(filename, org.alfresco.utility.model.FileType.TEXT_PLAIN, "content"));
        return filename;
    }
}
