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

/** A helper class with methods to access system properties, e.g. those passed via -D maven command line arguments. */
public class SystemPropertyHelper
{
    /** Private constructor for helper class. */
    private SystemPropertyHelper()
    {}

    /**
     * Load the value of a property.
     *
     * @param key
     *            The key to look up.
     * @param defaultValue
     *            The default value to use if the property is not set.
     * @return The value as a string.
     */
    public static String getSystemProperty(String key, String defaultValue)
    {
        String value = System.getProperty(key);
        if (value == null)
        {
            return defaultValue;
        }
        return value;
    }
}
