package org.alfresco.elasticsearch;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import com.google.common.collect.Sets;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;

import org.alfresco.rest.core.RestWrapper;
import org.alfresco.rest.search.RestRequestQueryModel;
import org.alfresco.rest.search.RestRequestTemplatesModel;
import org.alfresco.rest.search.SearchNodeModel;
import org.alfresco.rest.search.SearchRequest;
import org.alfresco.rest.search.SearchResponse;
import org.alfresco.utility.Utility;
import org.alfresco.utility.model.UserModel;

/** A class providing methods for testing search queries. */
public class SearchQueryService
{
    /** Maximum time to allow for search query to return correct results. */
    private static final int MAX_TIME = 10000;
    @Autowired
    private RestWrapper client;

    /** Assert that the query returns no results. */
    public void expectNoResultsFromQuery(SearchRequest searchRequest, UserModel testUser)
    {
        expectResultsFromQuery(searchRequest, testUser);
    }

    /** Assert that the query returns something, without checking exactly what it returns. */
    public void expectSomeResultsFromQuery(SearchRequest searchRequest, UserModel testUser)
    {
        Consumer<SearchResponse> assertNotEmpty = searchResponse -> assertFalse(searchResponse.isEmpty());
        expectResultsFromQuery(searchRequest, testUser, assertNotEmpty);
    }

    public void expectResultsInOrder(SearchRequest searchRequest, UserModel user, boolean isAscending, String... expected)
    {
        Consumer<SearchResponse> response = searchResponse -> assertNamesInOrder(searchResponse, isAscending, expected);
        expectResultsFromQuery(searchRequest, user, response);
    }

    public void expectResultsInOrder(SearchRequest searchRequest, UserModel user, String... expectedOrder)
    {
        Consumer<SearchResponse> response = searchResponse -> assertNamesInOrder(searchResponse, expectedOrder);
        expectResultsFromQuery(searchRequest, user, response);
    }

    public void expectResultsStartingWithOneOf(SearchRequest searchRequest, UserModel user, String... expected)
    {
        Consumer<SearchResponse> response = searchResponse -> {
            List<String> expectedFirstElements = List.of(expected);
            String actualFirstElement = searchResponse.getEntries().stream()
                    .map(SearchNodeModel::getModel)
                    .map(SearchNodeModel::getName)
                    .findFirst()
                    .orElse(null);
            assertTrue(expectedFirstElements.contains(actualFirstElement), "Unexpected search results - actual first element: " + actualFirstElement + ", expected one of: " + expectedFirstElements + " |");
        };
        expectResultsFromQuery(searchRequest, user, response);
    }

    public void expectResultsFromQuery(SearchRequest searchRequest, UserModel user, String... expected)
    {
        Consumer<SearchResponse> assertNames = searchResponse -> assertNames(searchResponse, expected);
        expectResultsFromQuery(searchRequest, user, assertNames);
    }

    /** Check that the specified results are all included in the result set. */
    public void expectResultsInclude(SearchRequest searchRequest, UserModel user, String... expected)
    {
        Consumer<SearchResponse> assertNames = searchResponse -> assertNamesInclude(searchResponse, expected);
        expectResultsFromQuery(searchRequest, user, assertNames);
    }

    public void expectNodeTypesFromQuery(SearchRequest searchRequest, UserModel user, String... expected)
    {
        Consumer<SearchResponse> assertNodeTypes = searchResponse -> assertNodeTypes(searchResponse, expected);
        expectResultsFromQuery(searchRequest, user, assertNodeTypes);
    }

    public void expectNodeRefsFromQuery(SearchRequest searchRequest, UserModel user, String... expectedNodeRefs)
    {
        Consumer<SearchResponse> assertNames = searchResponse -> assertNodeRefs(searchResponse, expectedNodeRefs);
        expectResultsFromQuery(searchRequest, user, assertNames);
    }

    public void expectAllResultsFromQuery(SearchRequest searchRequest, UserModel user, Predicate<SearchNodeModel> assertionMethod)
    {
        Function<SearchNodeModel, String> failureMessage = searchNodeModel -> "'" + searchNodeModel.getName() + "' did not satisfy predicate.";
        expectAllResultsFromQuery(searchRequest, user, assertionMethod, failureMessage);
    }

    public void expectAllResultsFromQuery(SearchRequest searchRequest, UserModel user, Predicate<SearchNodeModel> assertionMethod, Function<SearchNodeModel, String> failureMessageFunction)
    {
        expectResultsFromQuery(searchRequest, user, searchResponse -> assertAllSearchResults(searchResponse, assertionMethod, failureMessageFunction));
    }

    public void expectTotalHitsFromQuery(SearchRequest searchRequest, UserModel user, int expected)
    {
        expectResultsFromQuery(searchRequest, user, searchResponse -> assertTotalHitsResults(searchResponse, expected));
    }

    private void expectResultsFromQuery(SearchRequest searchRequest, UserModel user, Consumer<SearchResponse> assertionMethod)
    {
        try
        {
            Utility.sleep(1000, MAX_TIME, () -> {
                SearchResponse response = client.authenticateUser(user)
                        .withSearchAPI()
                        .search(searchRequest);
                client.assertStatusCodeIs(HttpStatus.OK);
                assertionMethod.accept(response);
            });
        }
        catch (InterruptedException e)
        {
            fail("InterruptedException received while waiting for results.");
        }
    }

    public void expectErrorFromQuery(SearchRequest searchRequest, org.alfresco.utility.model.UserModel user,
            HttpStatus expectedStatusCode, String containsErrorString)
    {
        client.authenticateUser(user).withSearchAPI().search(searchRequest);
        client.assertStatusCodeIs(expectedStatusCode);
        client.assertLastError().containsSummary(containsErrorString);
    }

    private void assertNodeRefs(SearchResponse actual, String... expected)
    {
        Set<String> result = actual.getEntries().stream()
                .map(SearchNodeModel::getModel)
                .map(SearchNodeModel::getId)
                .collect(Collectors.toSet());
        Set<String> expectedList = Sets.newHashSet(expected);
        assertEquals(result, expectedList, "Unexpected search results - got " + result + " expected " + expectedList);
    }

    private void assertNames(SearchResponse actual, String... expected)
    {
        Set<String> result = actual.getEntries().stream()
                .map(SearchNodeModel::getModel)
                .map(SearchNodeModel::getName)
                .collect(Collectors.toSet());
        Set<String> expectedList = Sets.newHashSet(expected);
        assertEquals(result, expectedList, "Unexpected search results - got " + result + " expected " + expectedList);
    }

    /** Check that the given names are included in the result set. */
    private void assertNamesInclude(SearchResponse actual, String... expected)
    {
        Set<String> expectedSet = Sets.newHashSet(expected);
        // Filter to only include results that were expected.
        Set<String> filteredResults = actual.getEntries().stream()
                .map(SearchNodeModel::getModel)
                .map(SearchNodeModel::getName)
                .filter(name -> expectedSet.contains(name))
                .collect(Collectors.toSet());
        assertEquals(filteredResults, expectedSet, "Did not receive all the expected search results - got " + filteredResults + " from expected set " + expectedSet);
    }

    private List<String> orderNames(boolean isAscending, String... filename)
    {
        List<String> orderedNames = Arrays.stream(filename).sorted().collect(Collectors.toList());
        if (!isAscending)
        {
            orderedNames = orderedNames.stream().sorted(Comparator.reverseOrder()).collect(Collectors.toList());
        }
        return orderedNames;
    }

    private void assertNamesInOrder(SearchResponse actual, boolean isAscending, String... expected)
    {
        List<String> expectedInOrder = orderNames(isAscending, expected);
        List<String> result = actual.getEntries().stream()
                .map(SearchNodeModel::getModel)
                .map(SearchNodeModel::getName)
                .collect(Collectors.toList());
        assertEquals(result, expectedInOrder, "Unexpected search results - got " + result + " expected " + expectedInOrder);
    }

    private void assertNamesInOrder(SearchResponse actual, String... expectedOrder)
    {
        List<String> expectedInOrder = List.of(expectedOrder);
        List<String> result = actual.getEntries().stream()
                .map(SearchNodeModel::getModel)
                .map(SearchNodeModel::getName)
                .toList();
        assertEquals(result, expectedInOrder, "Unexpected search results - got " + result + " expected " + expectedInOrder);
    }

    private void assertNodeTypes(SearchResponse actual, String... expected)
    {
        Set<String> result = actual.getEntries().stream()
                .map(SearchNodeModel::getModel)
                .map(SearchNodeModel::getNodeType)
                .collect(Collectors.toSet());
        Set<String> expectedList = Sets.newHashSet(expected);
        assertEquals(result, expectedList, "Unexpected search results - got " + result + " expected " + expectedList);
    }

    private void assertAllSearchResults(SearchResponse actual, Predicate<SearchNodeModel> assertion, Function<SearchNodeModel, String> failureMessageFunction)
    {
        String result = actual.getEntries().stream()
                .map(SearchNodeModel::getModel)
                .filter(Predicate.not(assertion))
                .map(failureMessageFunction)
                .collect(Collectors.joining("\n"));
        assertTrue(result.isEmpty(), "assertAllSearchResults failed with these issues:\n" + result);
    }

    private void assertTotalHitsResults(SearchResponse actual, int expected)
    {
        int totalItems = actual.getPagination().getTotalItems();
        assertEquals(totalItems, expected, "Unexpected totalItems results - got " + totalItems + " expected " + expected);
    }

    public static SearchRequest req(String query)
    {
        return req(null, query);
    }

    public static SearchRequest req(String language, String query)
    {
        RestRequestQueryModel restRequestQueryModel = new RestRequestQueryModel();
        restRequestQueryModel.setQuery(query);
        Optional.ofNullable(language).ifPresent(restRequestQueryModel::setLanguage);
        return new SearchRequest(restRequestQueryModel);
    }

    public static SearchRequest req(String language, String query, Map<String, String> templates)
    {
        RestRequestQueryModel restRequestQueryModel = new RestRequestQueryModel();
        restRequestQueryModel.setQuery(query);
        Optional.ofNullable(language).ifPresent(restRequestQueryModel::setLanguage);
        SearchRequest request = new SearchRequest(restRequestQueryModel);
        List<RestRequestTemplatesModel> templatesModels = templates.entrySet().stream()
                .map(entry -> RestRequestTemplatesModel.builder().name(entry.getKey()).template(entry.getValue()).create())
                .toList();
        request.setTemplates(templatesModels);

        return request;
    }
}
