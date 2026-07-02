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

import java.io.FileReader;
import java.util.Properties;

import org.testng.Assert;

/** A helper class with methods to access properties from the .env file. */
public class EnvHelper
{
    /** The location of the .env file (relative to the root of this maven submodule). */
    private static final String ENV_FILE_NAME = "../environment/.env";

    /** Private constructor for helper class. */
    private EnvHelper()
    {}

    /**
     * Load the value of a property from the .env file.
     * 
     * @param key
     *            The key to look up.
     * @return The value as a string.
     */
    public static String getEnvProperty(String key)
    {
        return loadEnvProperties().getProperty(key);
    }

    /**
     * Load all the properties from the .env file.
     * 
     * @return The properties.
     */
    public static Properties loadEnvProperties()
    {
        Properties env = new Properties();
        try (FileReader reader = new FileReader(ENV_FILE_NAME))
        {
            env.load(reader);
        }
        catch (Exception e)
        {
            Assert.fail("unable to load .env property file ");
        }
        return env;
    }
}
