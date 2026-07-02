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

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public enum DatabaseType
{
    POSTGRESQL_DB("postgresql", "org.postgresql.Driver", "jdbc:postgresql://postgres:5432/alfresco", "alfresco", "alfresco"), MYSQL_DB("mysql", "com.mysql.cj.jdbc.Driver", "jdbc:mysql://mysql:3306/alfresco", "alfresco", "alfresco"), MARIA_DB("mariadb", "org.mariadb.jdbc.Driver", "jdbc:mariadb://mariadb:3306/alfresco", "alfresco", "alfresco");

    private final String type;
    private final String driver;
    private final String url;
    private final String username;
    private final String password;
    private final Map<String, String> additionalDbSettings;

    DatabaseType(String type, String driver, String url, String username, String password, Map<String, String> additionalDbSettings)
    {
        this.type = type;
        this.driver = driver;
        this.url = url;
        this.username = username;
        this.password = password;
        this.additionalDbSettings = additionalDbSettings;
    }

    DatabaseType(String type, String driver, String url, String username, String password)
    {
        this.type = type;
        this.driver = driver;
        this.url = url;
        this.username = username;
        this.password = password;
        this.additionalDbSettings = new HashMap<>(0);
    }

    public String getType()
    {
        return this.type;
    }

    public String getDriver()
    {
        return driver;
    }

    public String getUrl()
    {
        return url;
    }

    public String getUsername()
    {
        return username;
    }

    public String getPassword()
    {
        return password;
    }

    public Map<String, String> getAdditionalDbSettings()
    {
        return additionalDbSettings;
    }

    public static DatabaseType from(String type)
    {
        return Arrays.stream(DatabaseType.values())
                .filter(database -> database.getType().equals(type.toLowerCase()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Database of type + '" + type + "' not defined."));
    }
}
