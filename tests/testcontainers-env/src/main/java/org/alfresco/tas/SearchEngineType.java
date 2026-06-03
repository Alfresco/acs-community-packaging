package org.alfresco.tas;

import java.util.Arrays;

public enum SearchEngineType
{
    OPENSEARCH_ENGINE("opensearch"), ELASTICSEARCH_ENGINE("elasticsearch");

    private final String type;

    SearchEngineType(String type)
    {
        this.type = type;
    }

    public String getType()
    {
        return this.type;
    }

    public static SearchEngineType from(String type)
    {
        return Arrays.stream(SearchEngineType.values())
                .filter(engine -> engine.getType().equals(type.toLowerCase()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Search engine of type + '" + type + "' not defined."));
    }
}
