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
 * Service class responsible for retrieving affiliation information for a
 * provider.  Similar in structure to the Licenses and Facilities
 * services, this class encapsulates the filter request and parsing
 * logic for the affiliations endpoint.
 */
class Affiliations {

    private final CloseableHttpClient httpClient
    private final ObjectMapper objectMapper
    private final CStreamConstants constants
    private final DemographicSchema demographicSchema

    Affiliations(){
        // Initialize with default values or leave null
        this.httpClient = null
        this.objectMapper = null
        this.constants = null
        this.demographicSchema = new DemographicSchema()
    }

    Affiliations(CloseableHttpClient httpClient, ObjectMapper objectMapper,
                CStreamConstants constants) {
        this.httpClient = httpClient
        this.objectMapper = objectMapper
        this.constants = constants
        this.demographicSchema = new DemographicSchema()
    }

    /**
     * Retrieves all primary affiliations for a given provider.  The
     * returned list contains maps with only the selected fields: Id,
     * Type_Code, Specialty_Code, Institution_Code and Position_Code.
     *
     * @param drId     the provider identifier
     * @param jwtToken the authentication token
     * @return list of affiliation maps
     * @throws IOException if the API call fails
     */
    List<Map<String, Object>> getAffiliations(String drId, String jwtToken) throws IOException {
        List<Map<String, Object>> affiliations = []
        // Build the request body
        ObjectNode requestBody = createAffiliationsFilterRequest(drId)
        String jsonBody = objectMapper.writeValueAsString(requestBody)

        // Create the HTTP POST request
        String url = constants.apiBaseUrl + constants.affiliationsEndpoint
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
                        def affiliation = resultNode.get(i)
                        Map<String, Object> affiliationData = [:]
                        for (String attrName : demographicSchema.affiliationAttributes) {
                            // Check for existence of the field and convert to text; default to an empty string
                            affiliationData[attrName] = affiliation.has(attrName) ? affiliation.get(attrName).asText() : ""
                        }
                        // Extract only selected fields
                        affiliationData.each { key, value ->
                            affiliationData[key] = affiliation.has(key) ? affiliation.get(key).asText() : null
                        }
                        /*
                        affiliationData["Id"] = affiliation.has("Id") ? affiliation.get("Id").asText() : null
                        affiliationData["Type_Code"] = affiliation.has("Type_Code") ? affiliation.get("Type_Code").asText() : null
                        affiliationData["Specialty_Code"] = affiliation.has("Specialty_Code") ? affiliation.get("Specialty_Code").asText() : null
                        affiliationData["Institution_Code"] = affiliation.has("Institution_Code") ? affiliation.get("Institution_Code").asText() : null
                        affiliationData["Position_Code"] = affiliation.has("Position_Code") ? affiliation.get("Position_Code").asText() : null
                        */
                        affiliations.add(affiliationData)

                    }
                }
            }
        } else {
            throw new IOException("API call failed. Status: " +
                    response.getStatusLine().getStatusCode() + ", Response: " + responseBody)
        }

        return affiliations
    }

    /**
     * Creates the filter request body used to retrieve provider affiliations.
     * The filter requires the Dr_Id and limits results to primary affiliations.
     *
     * @param drId the provider identifier
     * @return ObjectNode representing the request JSON
     */
    private ObjectNode createAffiliationsFilterRequest(String drId) {
        ObjectNode requestBody = objectMapper.createObjectNode()
        requestBody.put("Page", 1)
        requestBody.put("PageSize", 100)

        // Build filter with two conditions: Dr_Id equals and Primary equals true
        ObjectNode filter = objectMapper.createObjectNode()
        filter.put("Logic", "AND")

        ObjectNode drIdFilter = objectMapper.createObjectNode()
        drIdFilter.put("Field", "Dr_Id")
        drIdFilter.put("Operator", "EQUALS")
        drIdFilter.put("Value", drId)

        ObjectNode primaryFilter = objectMapper.createObjectNode()
        primaryFilter.put("Field", "Primary")
        primaryFilter.put("Operator", "EQUALS")
        primaryFilter.put("Value", "true")

        filter.set("Filters", objectMapper.createArrayNode().add(drIdFilter).add(primaryFilter))
        requestBody.set("Filter", filter)

        // Sort by Id descending
        ObjectNode sortItem = objectMapper.createObjectNode()
        sortItem.put("Field", "Id")
        sortItem.put("Direction", "DESCENDING")
        requestBody.set("Sort", objectMapper.createArrayNode().add(sortItem))

        return requestBody
    }
}