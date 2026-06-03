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
