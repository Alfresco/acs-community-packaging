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
