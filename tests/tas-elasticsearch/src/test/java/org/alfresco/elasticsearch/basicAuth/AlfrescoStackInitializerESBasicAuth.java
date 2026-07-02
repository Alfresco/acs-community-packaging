/*
 * #%L
 * Alfresco Tas Elasticsearch
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
package org.alfresco.elasticsearch.basicAuth;

import java.io.IOException;

import org.testcontainers.containers.GenericContainer;

import org.alfresco.tas.AlfrescoStackInitializer;
import org.alfresco.tas.SearchEngineType;

/**
 * ACS Stack Docker Compose initializer with Basic Authentication for Search Engine service (Opensearch or Elasticsearch).
 */
public class AlfrescoStackInitializerESBasicAuth extends AlfrescoStackInitializer
{

    // Default Elasticsearch credentials
    private static final String SEARCH_ENGINE_USERNAME = "elastic";
    private static final String SEARCH_ENGINE_PASSWORD = "bob123"; // pragma: allowlist secret
    private static final String OPENSEARCH_TEST_ROLE = "test_role";

    @Override
    public void configureSecuritySettings(GenericContainer searchEngineContainer)
    {
        SearchEngineType usedEngine = getImagesConfig().getSearchEngineType();

        if (SearchEngineType.OPENSEARCH_ENGINE.equals(usedEngine))
        {
            configureNewUser(searchEngineContainer);
        }
    }

    private void configureNewUser(GenericContainer opensearchContainer)
    {
        try
        {
            // Using password hash for setting up test only
            String passwordHash = hashPassword(opensearchContainer, SEARCH_ENGINE_PASSWORD);

            addNewUser(opensearchContainer, SEARCH_ENGINE_USERNAME, passwordHash);
            addNewRole(opensearchContainer, OPENSEARCH_TEST_ROLE, CUSTOM_ALFRESCO_INDEX);
            addNewRoleMapping(opensearchContainer, OPENSEARCH_TEST_ROLE, SEARCH_ENGINE_USERNAME);
            applyNewSecurityConfigs(opensearchContainer);
        }
        catch (IOException | InterruptedException e)
        {
            e.printStackTrace();
        }
    }

    private String hashPassword(GenericContainer opensearchContainer, String password) throws IOException, InterruptedException
    {
        return opensearchContainer.execInContainer("/usr/share/opensearch/plugins/opensearch-security/tools/hash.sh", "-p", password)
                .getStdout().strip();
    }

    private void applyNewSecurityConfigs(GenericContainer opensearchContainer) throws IOException, InterruptedException
    {
        opensearchContainer.execInContainer("sh", "-c", "/usr/share/opensearch/plugins/opensearch-security/tools/securityadmin.sh " +
                "-cd /usr/share/opensearch/config/opensearch-security " +
                "-icl -nhnv " +
                "-cert /usr/share/opensearch/config/kirk.pem " +
                "-cacert /usr/share/opensearch/config/root-ca.pem " +
                "-key /usr/share/opensearch/config/kirk-key.pem");
    }

    private void addNewRoleMapping(GenericContainer opensearchContainer, String role, String username) throws IOException, InterruptedException
    {
        opensearchContainer.execInContainer(
                "sh", "-c", "echo '\n\n" + newOpensearchRoleMapping(role, username) + "' >> /usr/share/opensearch/config/opensearch-security/roles_mapping.yml");
    }

    private void addNewRole(GenericContainer opensearchContainer, String role, String index) throws IOException, InterruptedException
    {
        opensearchContainer.execInContainer(
                "sh", "-c", "echo '\n\n" + newOpensearchRule(role, index) + "' >> /usr/share/opensearch/config/opensearch-security/roles.yml");
    }

    private void addNewUser(GenericContainer opensearchContainer, String username, String passwordHash) throws IOException, InterruptedException
    {
        opensearchContainer.execInContainer(
                "sh", "-c", "echo '\n\n" + newOpensearchUser(username, passwordHash) + "' >> /usr/share/opensearch/config/opensearch-security/internal_users.yml");
    }

    private String newOpensearchUser(String username, String passwordHash)
    {
        return username + ":\n" +
                "  hash: \"" + passwordHash + "\"\n" +
                "  reserved: false\n" +
                "  backend_roles:\n" +
                "  - \"all_access\"\n" +
                "  description: \"New user for testing purposes\"";
    }

    private String newOpensearchRule(String role, String indexName)
    {
        return role + ":\n" +
                "  cluster_permissions:\n" +
                "    - cluster_all\n" +
                "  index_permissions:\n" +
                "    - index_patterns:\n" +
                "      - \"" + indexName + "\"\n" +
                "      - \"alfresco-reindex-state\"\n" +
                "      allowed_actions:\n" +
                "        - \"*\"\n";
    }

    private String newOpensearchRoleMapping(String role, String user)
    {
        return role + ":\n" +
                "  reserved: true\n" +
                "  users:\n" +
                "  - \"" + user + "\"";
    }

    @Override
    protected GenericContainer createBatchIndexingContainer()
    {
        GenericContainer container = super.createBatchIndexingContainer();
        container.withEnv("SPRING_ELASTICSEARCH_REST_USERNAME", SEARCH_ENGINE_USERNAME);
        container.withEnv("SPRING_ELASTICSEARCH_REST_PASSWORD", SEARCH_ENGINE_PASSWORD);
        return container;
    }

    @Override
    protected GenericContainer createSearchEngineContainer()
    {
        SearchEngineType usedEngine = getImagesConfig().getSearchEngineType();

        if (SearchEngineType.OPENSEARCH_ENGINE.equals(usedEngine))
        {
            return super.createOpensearchContainer()
                    .withEnv("plugins.security.disabled", "false")
                    .withEnv("plugins.security.ssl.http.enabled", "false");
        }
        else
        {
            return super.createElasticContainer()
                    .withEnv("xpack.security.enabled", "true")
                    .withEnv("ELASTIC_PASSWORD", SEARCH_ENGINE_PASSWORD);
        }
    }

    @Override
    protected GenericContainer createAlfrescoContainer()
    {
        GenericContainer container = super.createAlfrescoContainer();
        String javaOpts = (String) container.getEnvMap().get("JAVA_OPTS");
        javaOpts = javaOpts + " -Delasticsearch.user=" + SEARCH_ENGINE_USERNAME + " " +
                "-Delasticsearch.password=" + SEARCH_ENGINE_PASSWORD;
        container.getEnvMap().put("JAVA_OPTS", javaOpts);
        return container;
    }
}
