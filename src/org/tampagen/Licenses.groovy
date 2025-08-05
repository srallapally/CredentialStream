package org.tampagen

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode
import org.apache.http.client.methods.HttpPost
import org.apache.http.entity.StringEntity
import org.apache.http.impl.client.CloseableHttpClient
import org.apache.http.util.EntityUtils

import java.nio.charset.StandardCharsets

import org.tampagen.utils.CStreamConstants

/**
 * Provides functionality to retrieve license information for a provider.
 *
 * This class encapsulates the API call that fetches credentials and licenses
 * for a given Dr_Id. The logic originally resided inside VerityStreamApiClient.
 * It has been extracted here to keep the client focused on higher level
 * orchestration. The class accepts required dependencies via its constructor
 * rather than relying on static members. Callers must ensure that the
 * JWT token used for authorization is valid prior to invoking getLicenses.
 */
class Licenses {
    private final CloseableHttpClient httpClient
    private final ObjectMapper objectMapper
    private final CStreamConstants constants

    Licenses(){
        // Initialize with default values or leave null
        this.httpClient = null
        this.objectMapper = null
        this.constants = null
    }
    /**
     * Constructs a Licenses service with the necessary dependencies.
     *
     * @param httpClient   HTTP client used to execute requests
     * @param objectMapper Jackson object mapper used for JSON serialization
     * @param constants    Container for API URL constants
     */
    Licenses(CloseableHttpClient httpClient, ObjectMapper objectMapper, CStreamConstants constants) {
        this.httpClient = httpClient
        this.objectMapper = objectMapper
        this.constants = constants
    }

    /**
     * Retrieves a list of license numbers for a provider.
     *
     * The caller must supply a valid Dr_Id and a non‑null JWT token. The JWT
     * token is passed as a parameter to avoid coupling this class to
     * token management. Any HTTP or parsing errors will result in an
     * IOException being thrown.
     *
     * @param drId     Provider's Dr_Id identifier
     * @param jwtToken Bearer token used for authorization
     * @return list of license numbers for the provider
     * @throws IOException when the API call fails or returns an error code
     */
    List<String> getLicenses(String drId, String jwtToken) throws IOException {
        List<String> licenseNumbers = []

        if (drId == null || drId.isEmpty()) {
            throw new IllegalArgumentException("Dr_Id cannot be null or empty")
        }

        // Build the full endpoint URL using the base URL and credentials/licensing path
        def url = constants.apiBaseUrl + constants.credentialsLicensesEndpoint

        // Create the request body using the helper method
        def requestBody = createLicensesFilterRequest(drId)
        def jsonString = objectMapper.writeValueAsString(requestBody)

        def request = new HttpPost(url)

        // Set headers: bearer token for auth and standard content types
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
                            def license = resultNode.get(i)
                            // Only include records where LicenseNumber exists and is not empty
                            def licenseNumber = license.has("LicenseNumber") ? license.get("LicenseNumber") : null
                            if (licenseNumber != null && !licenseNumber.isNull() && !licenseNumber.asText().isEmpty()) {
                                licenseNumbers.add(licenseNumber.asText())
                            }
                        }
                    }
                }

            } else {
                throw new IOException("API call failed. Status: " +
                        response.getStatusLine().getStatusCode() + ", Response: " + responseBody)
            }

        } catch (Exception e) {
            throw new IOException("Error calling credentials licenses filter endpoint for Dr_Id: ${drId}", e)
        }

        return licenseNumbers
    }

    /**
     * Builds the JSON filter request used to query license information.
     *
     * This method mirrors the structure previously defined in
     * VerityStreamApiClient#createLicensesFilterRequest but is no longer static.
     * It constructs nested filter criteria selecting active certificates and
     * DEA registrations for the specified provider. The returned ObjectNode
     * can be serialized to JSON via the supplied ObjectMapper.
     *
     * @param drId the provider's Dr_Id
     * @return an ObjectNode representing the filter request
     */
    private ObjectNode createLicensesFilterRequest(String drId) {
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

        // Active filter: only active licenses
        def activeFilter = objectMapper.createObjectNode()
        activeFilter.put("Field", "Active")
        activeFilter.put("Operator", "EQUALS")
        activeFilter.put("Value", "true")
        mainFilters.add(activeFilter)

        // OR group for Type_Code (vCERT or vDEA)
        def typeCodeOrFilter = objectMapper.createObjectNode()
        typeCodeOrFilter.put("Logic", "OR")

        def typeCodeFilters = objectMapper.createArrayNode()

        // vCERT filter
        def vCertFilter = objectMapper.createObjectNode()
        vCertFilter.put("Field", "Type_Code")
        vCertFilter.put("Operator", "EQUALS")
        vCertFilter.put("Value", "vCERT")
        typeCodeFilters.add(vCertFilter)

        // vDEA filter
        def vDeaFilter = objectMapper.createObjectNode()
        vDeaFilter.put("Field", "Type_Code")
        vDeaFilter.put("Operator", "EQUALS")
        vDeaFilter.put("Value", "vDEA")
        typeCodeFilters.add(vDeaFilter)

        typeCodeOrFilter.set("Filters", typeCodeFilters)
        mainFilters.add(typeCodeOrFilter)

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