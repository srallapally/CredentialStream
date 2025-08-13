package org.tampagen
/**
 * Facilities.groovy
 * Service class responsible for retrieving facilities information for a
 * provider.
 * 
 * @author Sanjay Rallapally
 * @version 1.0
 */
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode
import org.apache.http.client.methods.HttpPost
import org.apache.http.entity.StringEntity
import org.apache.http.impl.client.CloseableHttpClient
import org.apache.http.util.EntityUtils

import java.nio.charset.StandardCharsets

import org.tampagen.utils.CStreamConstants

class Facilities {
    private final CloseableHttpClient httpClient
    private final ObjectMapper objectMapper
    private final CStreamConstants constants

    Facilities() {
        // Initialize with default values or leave null
        this.httpClient = null
        this.objectMapper = null
        this.constants = null
    }

    /**
     * Constructs a Facilities service with the required dependencies.
     *
     * @param httpClient   HTTP client used to execute requests
     * @param objectMapper Jackson object mapper used for JSON serialization
     * @param constants    Container for API URL constants
     */
    Facilities(CloseableHttpClient httpClient, ObjectMapper objectMapper, CStreamConstants constants) {
        this.httpClient = httpClient
        this.objectMapper = objectMapper
        this.constants = constants
    }

    /**
     * Retrieves a list of facilities for a provider. Only facilities with
     * status codes vACT or vTMP are returned. Each element of the list is
     * a map containing the status code, category code and facility code.
     *
     * @param drId     Provider's Dr_Id identifier
     * @param jwtToken Bearer token used for authorization
     * @return list of facilities represented as maps
     * @throws IOException when the API call fails or returns an error code
     */
    List<Map<String, String>> getFacilities(String drId, String jwtToken) throws IOException {
        List<Map<String, String>> facilities = []

        if (drId == null || drId.isEmpty()) {
            throw new IllegalArgumentException("Dr_Id cannot be null or empty")
        }

        // Build the full endpoint URL using the base URL and facilities path
        def url = constants.apiBaseUrl + constants.facilitiesEndpoint

        // Create the request body
        def requestBody = createFacilitiesFilterRequest(drId)
        def jsonString = objectMapper.writeValueAsString(requestBody)

        def request = new HttpPost(url)

        // Set headers
        request.setHeader("Authorization", "Bearer " + jwtToken)
        request.setHeader("Content-Type", "application/json")
        request.setHeader("Accept", "application/json")

        // Attach the JSON payload
        def entity = new StringEntity(jsonString, StandardCharsets.UTF_8)
        request.setEntity(entity)

        try {
            def response = httpClient.execute(request)
            def responseEntity = response.getEntity()
            def responseBody = EntityUtils.toString(responseEntity, StandardCharsets.UTF_8)

            // Check HTTP status
            if (response.getStatusLine().getStatusCode() >= 200 &&
                response.getStatusLine().getStatusCode() < 300) {

                def jsonResponse = objectMapper.readTree(responseBody)

                // Ensure response has expected structure and success code
                def codeNode = jsonResponse.get("Code")
                if (codeNode == null || codeNode.asInt() != 1000) {
                    def description = jsonResponse.has("Description") ?
                            jsonResponse.get("Description").asText() : "Unknown error"
                    throw new IOException("API call failed. Code: " +
                            (codeNode != null ? codeNode.asInt() : "unknown") +
                            ", Description: " + description)
                }

                // Extract the result array from the Value node
                def valueNode = jsonResponse.get("Value")
                if (valueNode != null) {
                    def resultNode = valueNode.get("Result")

                    if (resultNode != null && resultNode.isArray()) {
                        for (int i = 0; i < resultNode.size(); i++) {
                            def facility = resultNode.get(i)

                            // Only include facilities with status code vACT or vTMP
                            def statusCode = facility.has("Status_Code") ? facility.get("Status_Code").asText() : ""
                            if (statusCode == "vACT" || statusCode == "vTMP") {
                                def facilityInfo = [:]
                                facilityInfo.put("Status_Code", statusCode)
                                facilityInfo.put("Category_Code", facility.has("Category_Code") ? facility.get("Category_Code").asText() : "")
                                facilityInfo.put("Facility_Code", facility.has("Facility_Code") ? facility.get("Facility_Code").asText() : "")
                                facilityInfo.put("ApptDate", facility.has("ApptDate") ? facility.get("ApptDate").asText() : "")
                                facilities.add(facilityInfo)
                            }
                        }
                    }
                }

            } else {
                throw new IOException("API call failed. Status: " +
                        response.getStatusLine().getStatusCode() + ", Response: " + responseBody)
            }

        } catch (Exception e) {
            throw new IOException("Error calling facilities filter endpoint for Dr_Id: ${drId}", e)
        }

        return facilities
    }

    /**
     * Builds the JSON filter request used to query facility information.
     *
     * This method mirrors the structure previously defined in
     * VerityStreamApiClient#createFacilitiesFilterRequest. It filters
     * facilities for the specified provider and selects only the active
     * and temporary (vACT or vTMP) records. The returned ObjectNode can
     * be serialized to JSON via the supplied ObjectMapper.
     *
     * @param drId the provider's Dr_Id
     * @return an ObjectNode representing the filter request
     */
    private ObjectNode createFacilitiesFilterRequest(String drId) {
        def requestBody = objectMapper.createObjectNode()
        requestBody.put("Page", 1)
        requestBody.put("PageSize", 100)

        // Main filter grouping using AND logic
        def mainFilter = objectMapper.createObjectNode()
        mainFilter.put("Logic", "AND")

        def mainFilters = objectMapper.createArrayNode()

        // Dr_Id filter
        def drIdFilter = objectMapper.createObjectNode()
        drIdFilter.put("Field", "Dr_Id")
        drIdFilter.put("Operator", "EQUALS")
        drIdFilter.put("Value", drId)
        mainFilters.add(drIdFilter)

        // OR group for Status_Code (vACT or vTMP)
        def statusCodeOrFilter = objectMapper.createObjectNode()
        statusCodeOrFilter.put("Logic", "OR")

        def statusCodeFilters = objectMapper.createArrayNode()

        // vACT filter
        def vActFilter = objectMapper.createObjectNode()
        vActFilter.put("Field", "Status_Code")
        vActFilter.put("Operator", "EQUALS")
        vActFilter.put("Value", "vACT")
        statusCodeFilters.add(vActFilter)

        // vTMP filter
        def vTmpFilter = objectMapper.createObjectNode()
        vTmpFilter.put("Field", "Status_Code")
        vTmpFilter.put("Operator", "EQUALS")
        vTmpFilter.put("Value", "vTMP")
        statusCodeFilters.add(vTmpFilter)

        statusCodeOrFilter.set("Filters", statusCodeFilters)
        mainFilters.add(statusCodeOrFilter)

        mainFilter.set("Filters", mainFilters)
        requestBody.set("Filter", mainFilter)

        // Sort by descending Id
        def sortItem = objectMapper.createObjectNode()
        sortItem.put("Field", "Id")
        sortItem.put("Direction", "DESCENDING")
        requestBody.set("Sort", objectMapper.createArrayNode().add(sortItem))

        return requestBody
    }
}