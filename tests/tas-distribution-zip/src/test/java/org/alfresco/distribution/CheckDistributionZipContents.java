/*
 * #%L
 * Alfresco Remote API
 * %%
 * Copyright (C) 2005 - 2021 Alfresco Software Limited
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
package org.alfresco.distribution;

import org.testng.annotations.Test;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;

public class CheckDistributionZipContents
{
    public static final String ACS_PREFIX = "alfresco-content-services-community-distribution";
    public static final String ACS_DIR_NAME = "distribution";
    public static final String AGS_PREFIX = "alfresco-governance-services-community";
    public static final String AGS_DIR_NAME = "distribution-ags";
    public static final String FORMAT = ".zip";

    @Test
    public void testAcsDistributionZipContents() throws Exception
    {
        File filesList[] = getDistributionZip(ACS_DIR_NAME, ACS_PREFIX);
        for (File file : filesList)
        {
            List<String> zipEntries = getZipEntries(file.getAbsolutePath());
            assertThat(zipEntries).
                    contains(
                            "keystore/metadata-keystore/keystore-passwords.properties",
                            "keystore/metadata-keystore/keystore",
                            "keystore/generate_keystores.bat",
                            "keystore/generate_keystores.sh",
                            "bin/alfresco-mmt.jar",
                            "bin/apply_amps.bat",
                            "bin/apply_amps.sh",
                            "web-server/webapps/ROOT.war",
                            "web-server/webapps/alfresco.war",
                            "web-server/webapps/share.war",
                            "web-server/webapps/_vti_bin.war",
                            "web-server/conf/Catalina/localhost/alfresco.xml",
                            "web-server/shared/classes/alfresco/web-extension/share-config-custom.xml"
                    );
        }
    }

    @Test
    public void testAgsDistributionZipContents() throws Exception
    {
        String repoVersion = getPomValues().getProperties().getProperty("dependency.alfresco-community-repo.version");
        String shareVersion = getPomValues().getProperties().getProperty("dependency.alfresco-community-share.version");
        File filesList[] = getDistributionZip(AGS_DIR_NAME, AGS_PREFIX);
        for (File file : filesList)
        {
            List<String> zipEntries = getZipEntries(file.getAbsolutePath());
            assertThat(zipEntries).
                    contains(
                            "alfresco-governance-services-community-repo-" + repoVersion  + ".amp",
                            "alfresco-governance-services-community-rest-api-explorer-" + repoVersion + ".war",
                            "alfresco-governance-services-community-share-" + shareVersion + ".amp"
                    );
        }
    }

    private File[] getDistributionZip(String dirName, String prefix) throws Exception
    {
        String resourcePath = Paths.get("").toAbsolutePath().getParent().getParent().toString() + "/" + dirName + "/" + "target" + "/";
        File distributionZip = new File(resourcePath);
        FilenameFilter zipFileFilter = (dir, name) -> {
            if (name.startsWith(prefix) && name.endsWith(FORMAT))
            {
                return true;
            }
            else
            {
                return false;
            }
        };

        return distributionZip.listFiles(zipFileFilter);
    }

    private List<String> getZipEntries(String filePath) throws Exception
    {
        List<String> zipEntries = new ArrayList<>();
        ZipFile zipFile = new ZipFile(new File(filePath));
        Enumeration<? extends ZipEntry> entries = zipFile.entries();
        while (entries.hasMoreElements())
        {
            ZipEntry entry = entries.nextElement();
            zipEntries.add(entry.toString());
        }
        return zipEntries;
    }

    private Model getPomValues() throws Exception
    {
        String parentPom = Paths.get("").toAbsolutePath().getParent().getParent().toString() + "/pom.xml";
        MavenXpp3Reader reader = new MavenXpp3Reader();
        Model model = reader.read(new FileReader(parentPom));
        return model;
    }
}
