package org.tampagen
/**
 * ApprovedAssociates.groovy
 * Service class responsible for retrieving supervising physicians information for a
 * provider.
 * @author Sanjay Rallapally
 * @version 1.0
 */
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode
import org.apache.http.client.methods.HttpPost
import org.apache.http.entity.StringEntity
import org.apache.http.impl.client.CloseableHttpClient
import org.apache.http.util.EntityUtils
import org.tampagen.utils.CStreamConstants

import java.nio.charset.StandardCharsets

class ApprovedAssociates {
    private final CloseableHttpClient httpClient
    private final ObjectMapper objectMapper
    private final CStreamConstants constants

    ApprovedAssociates(){
        // Initialize with default values or leave null
        this.httpClient = null
        this.objectMapper = null
        this.constants = null
    }

    ApprovedAssociates (CloseableHttpClient httpClient, ObjectMapper objectMapper, CStreamConstants constants) {
        this.httpClient = httpClient
        this.objectMapper = objectMapper
        this.constants = constants
    }
    List<String> getSupervisingPhysicians(String drId, String jwtToken) throws IOException {
        List<String> doctors = []

        if (drId == null || drId.isEmpty()) {
            throw new IllegalArgumentException("Dr_Id cannot be null or empty")
        }

        // Build the full endpoint URL using the base URL and facilities path
        def url = constants.apiBaseUrl + constants.approvedAssociatesEndpoint
        // Create the request body
        def requestBody = createApprovedAssociatesFilterRequest(drId)
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
                //println "Value Node: " + valueNode
                if (valueNode != null) {
                    def resultNode = valueNode.get("Result")

                    if (resultNode != null && resultNode.isArray()) {
                        for (int i = 0; i < resultNode.size(); i++) {
                            def associates = resultNode.get(i)
                            def name = associates.has("Name") ? associates.get("Name").asText() : ""
                            // Add the name to the list of doctors
                            doctors.add(name)
                        }
                    }
                }
            } else {
                throw new IOException("API call failed. Status: " +
                        response.getStatusLine().getStatusCode() + ", Response: " + responseBody)
            }
        } catch (Exception e) {
            throw new IOException("Error calling specialities filter endpoint for Dr_Id: ${drId}", e.getMessage())
        }

        return doctors
    }
    private ObjectNode createApprovedAssociatesFilterRequest(String drId) {
        def requestBody = objectMapper.createObjectNode()
        requestBody.put("Page", 1)
        requestBody.put("PageSize", 100)

        // Main filter grouping using AND logic
        def mainFilter = objectMapper.createObjectNode()
        mainFilter.put("Logic", "AND")

        def associatesFilter = objectMapper.createArrayNode()

        // Dr_Id filter
        def drIdFilter = objectMapper.createObjectNode()
        drIdFilter.put("Field", "Dr_Id")
        drIdFilter.put("Operator", "EQUALS")
        drIdFilter.put("Value", drId)
        associatesFilter.add(drIdFilter)

        // only Primary Speciality
        def primaryFilter = objectMapper.createObjectNode()
        primaryFilter.put("Field", "Primary")
        primaryFilter.put("Operator", "EQUALS")
        primaryFilter.put("Value", "True")
        associatesFilter.add(primaryFilter)

        mainFilter.set("Filters", associatesFilter)
        requestBody.set("Filter", mainFilter)

        // Sort by descending Id
        def sortItem = objectMapper.createObjectNode()
        sortItem.put("Field", "Dr_Id")
        sortItem.put("Direction", "DESCENDING")
        requestBody.set("Sort", objectMapper.createArrayNode().add(sortItem))

        return requestBody
    }
}
