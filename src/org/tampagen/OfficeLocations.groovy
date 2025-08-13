package org.tampagen
/**
 * OfficeLocations.groovy
 * Service class responsible for retrieving office location information for a
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
import org.tampagen.utils.CStreamConstants
import org.tampagen.utils.DemographicSchema

import java.nio.charset.StandardCharsets

class OfficeLocations {

    private final CloseableHttpClient httpClient
    private final ObjectMapper objectMapper
    private final CStreamConstants constants
    private final DemographicSchema demographicSchema

    OfficeLocations() {
        // Initialize with default values or leave null
        this.httpClient = null
        this.objectMapper = null
        this.constants = null
        this.demographicSchema = new DemographicSchema()
    }

    OfficeLocations(CloseableHttpClient httpClient, ObjectMapper objectMapper,
                    CStreamConstants constants) {
        this.httpClient = httpClient
        this.objectMapper = objectMapper
        this.constants = constants
        this.demographicSchema = new DemographicSchema()
    }

    List<String> getOfficeLocations(String drId, String jwtToken) throws IOException {
        List<String> officeLocations = []
        
        // Build the request body
        ObjectNode requestBody = createOfficeLocationsFilterRequest(drId)
        String jsonString = objectMapper.writeValueAsString(requestBody)

    

        // Create the HTTP POST request
        String url = constants.apiBaseUrl + constants.officeLocationsEndpoint
       
        HttpPost request = new HttpPost(url)
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
                            def location = resultNode.get(i)
                            // Only include records where location exists and is not empty
                            def locationid = location.has("Location_Id") ? location.get("Location_Id") : null
                            println "Location_Id: ${locationid}"
                            if (locationid  != null && !locationid.isNull() && !locationid .asText().isEmpty()) {
                                def locationName = getLocationName(locationid.asText(), jwtToken)
                                if (locationName != null && !locationName.isEmpty()) {
                                    officeLocations.add(locationName)
                                } else {
                                    throw new IOException("Location name is null or empty for Location_Id: " + locationid.asText())
                                }
                            }
                        }
                    }
                }

            } else {
                throw new IOException("API call failed. Status: " +
                        response.getStatusLine().getStatusCode() + ", Response: " + responseBody)
            }

        } catch (Exception e) {
            throw new IOException("Error calling office locations filter endpoint for Dr_Id: ${drId}", e)
        }

        return officeLocations
    }

    private getLocationName(String locationId,String jwtToken) throws IOException {
        //String locationName = null;

       // Build the request body
        ObjectNode requestBody = createLocationFilterRequest(locationId)
        String jsonString = objectMapper.writeValueAsString(requestBody)
        
        // Create the HTTP POST request
        String url = constants.apiBaseUrl + constants.locationsEndpoint
        println "URL: ${url}"
        HttpPost request = new HttpPost(url)
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
                            def location = resultNode.get(i)
                            // Only include records where location exists and is not empty
                            def locationName = location.has("Name") ? location.get("Name") : null
                            if (locationName != null && !locationName.isNull() && !locationName.asText().isEmpty()) {
                                return locationName.asText()
                            }
                        }
                    }
                }

            } else {
                throw new IOException("API call failed. Status: " +
                        response.getStatusLine().getStatusCode() + ", Response: " + responseBody)
            }

        } catch (Exception e) {
            //throw new IOException("Error calling Locations endpoint: ${locationId}", e)
            e.printStackTrace()
        }
        return null
    }

    private ObjectNode createOfficeLocationsFilterRequest(String drId) {
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

        filter.set("Filters", objectMapper.createArrayNode().add(drIdFilter))
        requestBody.set("Filter", filter)

        // Sort by Id descending
        ObjectNode sortItem = objectMapper.createObjectNode()
        sortItem.put("Field", "Id")
        sortItem.put("Direction", "DESCENDING")
        requestBody.set("Sort", objectMapper.createArrayNode().add(sortItem))

        return requestBody
    }

    private ObjectNode createLocationFilterRequest(String locationId){
        ObjectNode requestBody = objectMapper.createObjectNode()
        requestBody.put("Id", locationId)
    
        return requestBody

    }
}