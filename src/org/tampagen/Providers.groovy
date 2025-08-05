package org.tampagen

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode
import org.apache.http.client.methods.HttpPost
import org.apache.http.entity.StringEntity
import org.apache.http.impl.client.CloseableHttpClient
import org.apache.http.util.EntityUtils
import org.tampagen.utils.CStreamConstants
import org.tampagen.utils.DemographicSchema

import java.nio.charset.StandardCharsets

/**
 * Service class responsible for retrieving detailed provider information
 * from the demographics filter endpoint.  Similar in design to the
 * Licenses and Facilities services, this class encapsulates the
 * request/response handling for provider lookups.
 */
class Providers {

    private final CloseableHttpClient httpClient
    private final ObjectMapper objectMapper
    private final CStreamConstants constants
    private final DemographicSchema demographicSchema

    Providers(CloseableHttpClient httpClient, ObjectMapper objectMapper,
              CStreamConstants constants, DemographicSchema demographicSchema) {
        this.httpClient = httpClient
        this.objectMapper = objectMapper
        this.constants = constants
        this.demographicSchema = demographicSchema
    }

    /**
     * Retrieves provider details for a given Dr_Id.  The returned list
     * contains a single map of field names to values according to the
     * configured demographic schema.  A list is used for consistency
     * with other service methods.
     *
     * @param drId the unique provider identifier
     * @param jwtToken the bearer token for authentication
     * @return list containing one map of provider attributes
     * @throws IOException if the API call fails
     */
    List<Map<String, Object>> getProvider(String drId, String jwtToken) throws IOException {
        List<Map<String, Object>> providers = []

        // Build request body
        ObjectNode requestBody = createProviderFilterRequest(drId)
        String jsonBody = objectMapper.writeValueAsString(requestBody)

        // Construct URL for demographics filter
        String url = constants.apiBaseUrl + constants.demographicsFilterEndpoint
        HttpPost request = new HttpPost(url)
        request.setHeader("Authorization", "Bearer " + jwtToken)
        request.setHeader("Content-Type", "application/json")
        request.setHeader("Accept", "application/json")
        request.setEntity(new StringEntity(jsonBody, StandardCharsets.UTF_8))

        def response = httpClient.execute(request)
        def responseEntity = response.getEntity()
        def responseBody = EntityUtils.toString(responseEntity, StandardCharsets.UTF_8)

        if (response.getStatusLine().getStatusCode() >= 200 &&
                response.getStatusLine().getStatusCode() < 300) {
            def jsonResponse = objectMapper.readTree(responseBody)
            def codeNode = jsonResponse.get("Code")
            if (codeNode == null || codeNode.asInt() != 1000) {
                def description = jsonResponse.has("Description") ?
                        jsonResponse.get("Description").asText() : "Unknown error"
                throw new IOException("API call failed. Code: " +
                        (codeNode != null ? codeNode.asInt() : "unknown") +
                        ", Description: " + description)
            }
            def valueNode = jsonResponse.get("Value")
            if (valueNode != null) {
                def resultNode = valueNode.get("Result")
                if (resultNode != null && resultNode.isArray()) {
                    for (int i = 0; i < resultNode.size(); i++) {
                        def provider = resultNode.get(i)
                        Map<String, Object> providerData = [:]
                        // Only extract fields defined in the demographic schema
                        for (String attrName : demographicSchema.providerAttributes) {
                            providerData[attrName] = provider.has(attrName) ? provider.get(attrName).asText() : null
                        }
                        providers.add(providerData)
                    }
                }
            }
        } else {
            throw new IOException("API call failed. Status: " +
                    response.getStatusLine().getStatusCode() + ", Response: " + responseBody)
        }

        return providers
    }

    /**
     * Builds the JSON request body for filtering a single provider by Dr_Id.
     *
     * @param drId the provider identifier
     * @return ObjectNode representing the filter request
     */
    private ObjectNode createProviderFilterRequest(String drId) {
        ObjectNode requestBody = objectMapper.createObjectNode()
        // Request only the first page with minimal size; only one record is expected
        requestBody.put("Page", 1)
        requestBody.put("PageSize", 5)

        // Filter section
        ObjectNode filter = objectMapper.createObjectNode()
        filter.put("Logic", "AND")
        ObjectNode filterItem = objectMapper.createObjectNode()
        filterItem.put("Field", "Dr_Id")
        filterItem.put("Operator", "EQUALS")
        filterItem.put("Value", drId)
        filter.set("Filters", objectMapper.createArrayNode().add(filterItem))
        requestBody.set("Filter", filter)

        // Sort by first name descending, although sorting has no effect for a single record
        ObjectNode sortItem = objectMapper.createObjectNode()
        sortItem.put("Field", "FirstName")
        sortItem.put("Direction", "DESCENDING")
        requestBody.set("Sort", objectMapper.createArrayNode().add(sortItem))

        return requestBody
    }
}