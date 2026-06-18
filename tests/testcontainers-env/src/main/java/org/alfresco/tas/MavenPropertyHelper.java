/*
 * #%L
 * Alfresco Testcontainers Environment
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
package org.alfresco.tas;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Properties;

import org.testng.Assert;

/** A helper class with methods to access properties from Maven pom files. */
public class MavenPropertyHelper
{
    /** The relative path to the maven property file resource. */
    private static final String MAVEN_PROPERTIES_FILE = "maven.properties";

    /** Private constructor for helper class. */
    private MavenPropertyHelper()
    {}

    /**
     * Load the value of a property from Maven.
     *
     * @param key
     *            The key to look up.
     * @return The value as a string.
     */
    public static String getMavenProperty(String key)
    {
        String value = loadMavenProperties().getProperty(key);
        // If Maven filtering hasn't run (e.g. running directly from IDE), the value will be an
        // unresolved placeholder like "${database.type}". Fall back to the JVM system property instead.
        if (value != null && value.startsWith("${") && value.endsWith("}"))
        {
            value = System.getProperty(key);
        }
        return value;
    }

    /**
     * Load all the properties from Maven.
     *
     * @return The properties.
     */
    public static Properties loadMavenProperties()
    {
        Properties properties = new Properties();
        try (InputStream inputStream = MavenPropertyHelper.class.getClassLoader().getResourceAsStream(MAVEN_PROPERTIES_FILE))
        {
            Reader reader = new InputStreamReader(inputStream);
            properties.load(reader);
        }
        catch (Exception e)
        {
            Assert.fail("Unable to load maven.properties file ");
        }
        return properties;
    }
}
