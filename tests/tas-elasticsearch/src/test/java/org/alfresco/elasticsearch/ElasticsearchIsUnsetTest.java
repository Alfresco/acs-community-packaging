package org.alfresco.elasticsearch;

import static java.util.Objects.requireNonNull;
import static java.util.Optional.ofNullable;
import static java.util.function.Predicate.not;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Stream.concat;
import static java.util.stream.Stream.of;

import static org.apache.http.entity.ContentType.APPLICATION_JSON;
import static org.assertj.core.api.Assertions.assertThat;

import static org.alfresco.elasticsearch.SearchQueryService.req;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import com.google.common.io.CharStreams;
import com.google.gson.Gson;
import org.apache.http.HeaderElement;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.StringEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import org.alfresco.dataprep.AlfrescoHttpClient;
import org.alfresco.dataprep.AlfrescoHttpClientFactory;
import org.alfresco.rest.search.SearchRequest;
import org.alfresco.tas.AlfrescoStackInitializer;
import org.alfresco.utility.data.DataUser;

@ContextConfiguration(locations = "classpath:alfresco-elasticsearch-context.xml",
        initializers = AlfrescoStackInitializer.class)
public class ElasticsearchIsUnsetTest extends AbstractTestNGSpringContextTests
{
    private static final String CM_TITLED_ASPECT = "cm:titled";
    private static final String CM_TITLE_PROPERTY = "cm:title";
    private static final String CM_DESCRIPTION_PROPERTY = "cm:description";

    private final Gson gson = new Gson();

    @Autowired
    SearchQueryService searchQueryService;

    @Autowired
    private AlfrescoHttpClientFactory alfrescoHttpClientFactory;

    @Autowired
    private DataUser dataUser;

    private AlfrescoHttpClient alfrescoClient;
    private AlfNode testNode;

    @BeforeClass
    public void setUp() throws IOException
    {
        alfrescoClient = alfrescoHttpClientFactory.getObject();
        testNode = createNodeWithTitleButWithoutDescription();
    }

    @AfterClass
    public void tearDown()
    {
        alfrescoClient.close();
    }

    @Test
    public void testElasticsearchIsUnsetQuery() throws Exception
    {
        assertNodeIsReadyForTesting();

        // cm:title is set, we should have no result
        shouldNotFindNode("ISUNSET:\"" + CM_TITLE_PROPERTY + "\"");
        shouldFindNode("NOT ISUNSET:\"" + CM_TITLE_PROPERTY + "\"");

        // cm:description is not set, we should have a result
        shouldFindNode("ISUNSET:\"" + CM_DESCRIPTION_PROPERTY + "\"");
        shouldNotFindNode("NOT ISUNSET:\"" + CM_DESCRIPTION_PROPERTY + "\"");

        removeCmTitledAspect();

        // ISUNSET is type aware, no cm:titled aspect so no result
        shouldNotFindNode("ISUNSET:\"" + CM_TITLE_PROPERTY + "\"");
        shouldNotFindNode("ISUNSET:\"" + CM_DESCRIPTION_PROPERTY + "\"");
        shouldFindNode("NOT ISUNSET:\"" + CM_TITLE_PROPERTY + "\"");
        shouldFindNode("NOT ISUNSET:\"" + CM_DESCRIPTION_PROPERTY + "\"");

        restoreCmTitledAspect();

        // cm:titled aspect is back but cm:title property has been "unset"
        shouldFindNode("ISUNSET:\"" + CM_TITLE_PROPERTY + "\"");
        shouldFindNode("ISUNSET:\"" + CM_DESCRIPTION_PROPERTY + "\"");
        shouldNotFindNode("NOT ISUNSET:\"" + CM_TITLE_PROPERTY + "\"");
        shouldNotFindNode("NOT ISUNSET:\"" + CM_DESCRIPTION_PROPERTY + "\"");
    }

    private void restoreCmTitledAspect() throws IOException
    {
        setAspects(testNode.getAspects());
    }

    private void removeCmTitledAspect() throws IOException
    {
        final List<String> noCmTitled = testNode.getAspects().stream().filter(not(CM_TITLED_ASPECT::equals)).toList();
        setAspects(noCmTitled);
    }

    private void assertNodeIsReadyForTesting()
    {
        assertThat(testNode.getAspects()).contains(CM_TITLED_ASPECT);
        shouldFindNode();
    }

    private void shouldFindNode(String... conjunctions)
    {
        searchQueryService.expectResultsInclude(searchByName(conjunctions), dataUser.getAdminUser(), testNode.getName());
    }

    private void shouldNotFindNode(String... conjunctions)
    {
        searchQueryService.expectNoResultsFromQuery(searchByName(conjunctions), dataUser.getAdminUser());
    }

    private SearchRequest searchByName(String... conjunctions)
    {
        return req(concat(of(testNode.getName()), of(conjunctions)).collect(joining(" AND ")));
    }

    private void setAspects(Collection<String> aspectsToSet) throws IOException
    {
        final URI nodeURI = URI.create(alfrescoClient.getApiVersionUrl()).resolve("nodes/" + testNode.getId());
        final HttpPut putRequest = new HttpPut(nodeURI);

        final String jsonBody = toJsonString(Map.of("aspectNames", aspectsToSet));
        putRequest.setEntity(new StringEntity(jsonBody, APPLICATION_JSON));

        nodeApiRequest(putRequest, 200);
    }

    private AlfNode createNodeWithTitleButWithoutDescription() throws IOException
    {
        final URI parentURI = URI.create(alfrescoClient.getApiVersionUrl()).resolve("nodes/-my-/children");
        final HttpPost postRequest = new HttpPost(parentURI);
        final String jsonBody = toJsonString(
                Map.of("name", uniqueName(),
                        "nodeType", "cm:content",
                        "properties", Map.of("cm:title", "UnsetTestingDocument")));
        postRequest.setEntity(new StringEntity(jsonBody, APPLICATION_JSON));

        return nodeApiRequest(postRequest, 201);
    }

    private AlfNode nodeApiRequest(HttpRequestBase request, int expectedResponseCode) throws IOException
    {
        final HttpResponse response = alfrescoClient.executeAsAdmin(request);
        assertThat(response.getStatusLine().getStatusCode()).isEqualTo(expectedResponseCode);

        final HttpEntity entity = response.getEntity();
        assertThat(entity).isNotNull();
        assertThat(entity.getContentType()).isNotNull();
        assertThat(entity.getContentType().getElements()).hasSize(1);

        final HeaderElement contentTypeHeader = entity.getContentType().getElements()[0];
        assertThat(contentTypeHeader.getName()).isEqualTo(APPLICATION_JSON.getMimeType());

        final Charset contentCharset = ofNullable(contentTypeHeader.getParameterByName("charset"))
                .map(NameValuePair::getValue)
                .map(Charset::forName)
                .orElse(StandardCharsets.UTF_8);

        try (var reader = new InputStreamReader(entity.getContent(), contentCharset))
        {
            return asNode((Map<String, ?>) fromJsonString(CharStreams.toString(reader)).get("entry"));
        }
    }

    private AlfNode asNode(Map<String, ?> map)
    {
        return new AlfNode(map);
    }

    private String uniqueName()
    {
        return "Name-" + UUID.randomUUID();
    }

    private String toJsonString(Map<String, ?> map)
    {
        return gson.toJson(map);
    }

    private Map<String, ?> fromJsonString(String string)
    {
        return gson.fromJson(string, Map.class);
    }

    private static class AlfNode
    {
        private final Map<String, ?> nodeMap;

        private AlfNode(Map<String, ?> nodeMap)
        {
            this.nodeMap = requireNonNull(nodeMap);
        }

        public String getId()
        {
            return requireNonNull((String) nodeMap.get("id"));
        }

        public Set<String> getAspects()
        {
            return ofNullable(nodeMap.get("aspectNames"))
                    .map(c -> (Collection<String>) c)
                    .map(Set::copyOf)
                    .orElseGet(Set::of);
        }

        public String getName()
        {
            return (String) nodeMap.get("name");
        }
    }
}
