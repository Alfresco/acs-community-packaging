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
                .orElseThrow(() -> new IllegalArgumentException("Search engine of type '" + type + "' not defined."));
    }
}
