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
