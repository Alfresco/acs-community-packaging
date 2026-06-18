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
package org.alfresco.elasticsearch.utility;

import static org.alfresco.utility.model.FileType.TEXT_PLAIN;

import org.springframework.beans.factory.annotation.Autowired;

import org.alfresco.rest.core.RestWrapper;
import org.alfresco.rest.model.RestCategoryLinkBodyModel;
import org.alfresco.rest.model.RestCategoryModel;
import org.alfresco.utility.data.DataContent;
import org.alfresco.utility.data.DataSite;
import org.alfresco.utility.data.DataUser;
import org.alfresco.utility.data.RandomData;
import org.alfresco.utility.model.ContentModel;
import org.alfresco.utility.model.FileModel;
import org.alfresco.utility.model.FolderModel;
import org.alfresco.utility.model.SiteModel;
import org.alfresco.utility.model.UserModel;

/** Helper methods for Elasticsearch E2E tests. */
public class ElasticsearchRESTHelper
{
    /** The alias for the root of the category hierarchy. */
    public static final String ROOT_CATEGORY_ALIAS = "-root-";
    /** The root of the category hierarchy. */
    private static final RestCategoryModel ROOT_CATEGORY = RestCategoryModel.builder().id(ROOT_CATEGORY_ALIAS).name(ROOT_CATEGORY_ALIAS).create();

    @Autowired
    private RestWrapper client;
    @Autowired
    private DataSite dataSite;
    @Autowired
    private DataContent dataContent;
    @Autowired
    private DataUser dataUser;

    /**
     * Create a private site.
     *
     * @param user
     *            The user to use.
     * @return The new site.
     */
    public SiteModel createPrivateSite(UserModel user)
    {
        return dataSite.usingUser(user).createPrivateRandomSite();
    }

    /**
     * Create a folder in a site.
     *
     * @param user
     *            The user to use.
     * @param site
     *            The site to create the folder in.
     * @return The new folder.
     */
    public FolderModel createFolderInSite(UserModel user, SiteModel site)
    {
        return dataContent.usingUser(user).usingSite(site).createFolder();
    }

    /**
     * Create a text file in a site.
     *
     * @param user
     *            The user to use.
     * @param site
     *            The site to create the file in.
     * @return The new file.
     */
    public FileModel createFileInSite(UserModel user, SiteModel site)
    {
        FileModel fileModel = FileModel.getRandomFileModel(TEXT_PLAIN);
        return dataContent.usingUser(user).usingSite(site).createContent(fileModel);
    }

    /**
     * Create a file in a folder.
     *
     * @param user
     *            The user to use.
     * @param folder
     *            The folder to create the file in.
     * @return The new file.
     */
    public FileModel createFileInFolder(UserModel user, FolderModel folder)
    {
        FileModel fileModel = FileModel.getRandomFileModel(TEXT_PLAIN);
        return dataContent.usingUser(user).usingResource(folder).createContent(fileModel);
    }

    /**
     * Create a category.
     *
     * @param ancestorCategories
     *            The path of categories between the root and the new category, or leave blank to create the category at the "-root-".
     * @return The newly created category.
     */
    public RestCategoryModel createCategory(RestCategoryModel... ancestorCategories)
    {
        RestCategoryModel parent = (ancestorCategories.length > 0 ? ancestorCategories[ancestorCategories.length - 1] : ROOT_CATEGORY);
        return client.authenticateUser(dataUser.getAdminUser()).withCoreAPI()
                .usingCategory(parent)
                .createSingleCategory(RestCategoryModel.builder().name(RandomData.getRandomAlphanumeric()).create());
    }

    /**
     * Link a file or folder to a category.
     *
     * @param user
     *            The user who should create the link.
     * @param node
     *            The file or folder to be linked.
     * @param categoryHierarchy
     *            The full list of categories from the root (excluding "-root-") to the category to use.
     * @return The category that was linked to.
     */
    public RestCategoryModel linkToCategory(UserModel user, ContentModel node, RestCategoryModel... categoryHierarchy)
    {
        RestCategoryModel linkedToCategory = (categoryHierarchy.length > 0 ? categoryHierarchy[categoryHierarchy.length - 1] : ROOT_CATEGORY);
        return client.authenticateUser(user).withCoreAPI().usingNode(node)
                .linkToCategory(RestCategoryLinkBodyModel.builder().categoryId(linkedToCategory.getId()).create());
    }

    /**
     * Unlink a node from a category.
     *
     * @param user
     *            The user who should remove the link.
     * @param node
     *            The node to unlink.
     * @param categoryHierarchy
     *            The full list of categories from the root (excluding "-root-") to the category to use.
     */
    public void unlinkFromCategory(UserModel user, ContentModel node, RestCategoryModel... categoryHierarchy)
    {
        RestCategoryModel linkedToCategory = (categoryHierarchy.length > 0 ? categoryHierarchy[categoryHierarchy.length - 1] : ROOT_CATEGORY);
        client.authenticateUser(user).withCoreAPI().usingNode(node).unlinkFromCategory(linkedToCategory.getId());
    }
}

