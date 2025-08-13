import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode
import org.apache.http.client.methods.HttpPost
import org.apache.http.entity.StringEntity
import org.apache.http.impl.client.CloseableHttpClient
import org.apache.http.impl.client.HttpClients
import org.apache.http.util.EntityUtils
import org.forgerock.openicf.connectors.groovy.OperationType
import org.forgerock.openicf.connectors.groovy.ScriptedConfiguration
import org.identityconnectors.common.logging.Log
import org.identityconnectors.framework.common.exceptions.ConnectorException
import org.identityconnectors.framework.common.objects.ObjectClass
import org.identityconnectors.framework.common.objects.OperationOptions
import org.identityconnectors.framework.common.objects.SearchResult
import org.identityconnectors.framework.common.objects.filter.EqualsFilter
import org.identityconnectors.framework.common.objects.filter.Filter
import org.identityconnectors.framework.common.objects.filter.OrFilter

import org.tampagen.Licenses
import org.tampagen.utils.CStreamConstants
import org.tampagen.utils.DemographicSchema
import org.tampagen.Affiliations
import org.tampagen.Facilities
import org.tampagen.Licenses
import org.tampagen.ApprovedAssociates
import org.tampagen.Specialities
import org.tampagen.OfficeLocations

import java.nio.charset.StandardCharsets

// Configuration
apiKey = "<API Key>"
requesterId = "<REQUESTER _ID>"
requesterSecret = "<SECRET>"
resource = "<RESOURCE>"

// Global objects
httpClient = HttpClients.createDefault()
objectMapper = new ObjectMapper()
jwtToken = null as String
debugMode = true
constants = new CStreamConstants()
demographicSchema = new DemographicSchema()

def operation = operation as OperationType
def configuration = configuration as ScriptedConfiguration
def filter = filter as Filter
def log = log as Log
def objectClass = objectClass as ObjectClass
def options = options as OperationOptions

println "########## Entering " + operation + " Script"
println "########## ObjectClass: " + objectClass.objectClassValue
def map = [:]
//def queryFilter = 'true'

switch (objectClass.objectClassValue)  {
    case '__ACCOUNT__':

        String pagedResultsCookie = options.getPagedResultsCookie();
        //def remainingPagedResults = -1
        if(filter != null){
            def drId = null
            def provider = null
            if (filter instanceof EqualsFilter){
                def attrName = ((EqualsFilter) filter).getAttribute()
                if (attrName.is(Uid.NAME) || attrName.is(Name.NAME)) {
                    drId = ((EqualsFilter) filter).getAttribute().getValue().get(0)
                }
                println "Filtering by Dr_Id: " + drId
                // Ensure we have a valid JWT token before making the API call
                ensureJwtToken()
                def filterNode = objectMapper.createObjectNode()
                filterNode.put("Dr_Id", drId)
                def jsonString = objectMapper.writeValueAsString(filterNode)
                println(jsonString)
                def url = constants.apiBaseUrl + constants.demographicsFindEndpoint
                def request = new HttpPost(url)
                // Set headers
                request.setHeader("Authorization", "Bearer " + jwtToken)
                request.setHeader("Content-Type", "application/json")
                request.setHeader("Accept", "application/json")

                // Set request body
                def entity = new StringEntity(jsonString, StandardCharsets.UTF_8)
                request.setEntity(entity)

                try {
                    def response = httpClient.execute(request)
                    def responseEntity = response.getEntity()
                    def responseBody = EntityUtils.toString(responseEntity, StandardCharsets.UTF_8)

                    if (response.getStatusLine().getStatusCode() >= 200 &&
                            response.getStatusLine().getStatusCode() < 300) {

                        // Parse response to check for more data
                        def jsonResponse = objectMapper.readTree(responseBody)

                        // Check if response has the expected structure
                        def codeNode = jsonResponse.get("Code")
                        if (codeNode == null || codeNode.asInt() != 1000) {
                            def description = jsonResponse.has("Description") ?
                                    jsonResponse.get("Description").asText() : "Unknown error"
                            throw new ConnectorException("API call failed. Code: " +
                                    (codeNode != null ? codeNode.asInt() : "unknown") +
                                    ", Description: " + description)
                        }

                        // Get the Value object containing the actual data
                        def valueNode = jsonResponse.get("Value")
                        if (valueNode != null) {
                            // Get pagination info
                            def paginationNode = valueNode.get("Pagination")
                            def resultNode = valueNode.get("Result")
                            provider = resultNode.get(0)
                            Map<String, String> providerData = [:]
                            for (String attributeName : demographicSchema.providerAttributes) {
                                // Check for existence of the field and convert to text; default to an empty string
                                providerData[attributeName] = provider.has(attributeName) ? provider.get(attributeName).asText() : ""
                            }
                            def title = providerData["PrimaryTitles_Code"] ?: "NA"
                            def dr_Id = providerData["Dr_Id"] ?: "NA"
                            println "Processing Dr_Id: " + dr_Id + ", Title: " + title
                            // Get affiliations, licenses, facilities, associates
                            def associates = []
                            if(title == "PA"){
                                try {
                                    associates = getAssociates(httpClient,objectMapper,jwtToken,constants,dr_Id)
                                } catch (Exception e){
                                        println "   Warning: Could not retrieve associates for Dr_Id ${dr_Id}: ${e.getMessage()}"
                                }
                            }
                            // Get licenses for this provider
                            def licenses = []
                            if (dr_Id && dr_Id != "NA") {
                                try {
                                    licenses = getLicenses(httpClient,objectMapper,jwtToken,constants,dr_Id)
                                } catch (Exception e) {
                                    println "   Warning: Could not retrieve licenses for Dr_Id ${dr_Id}: ${e.getMessage()}"                        
                                }
                            }
                            // Get facilities for this provider
                            def facilities = []
                            if (dr_Id && dr_Id != "NA") {
                                try {
                                    facilities = getFacilities(httpClient,objectMapper,jwtToken,constants,dr_Id)
                                } catch (Exception e) {
                                    println "   Warning: Could not retrieve facilities for Dr_Id ${dr_Id}: ${e.getMessage()}"
                                }
                            }
                            // Get specialities for this provider
                            def specialities = []
                            if (dr_Id && dr_Id != "NA") {
                                try {
                                    specialities = getSpecialities(httpClient,objectMapper,jwtToken,constants,dr_Id)
                                } catch (Exception e) {
                                    println "   Warning: Could not retrieve facilities for Dr_Id ${dr_Id}: ${e.getMessage()}"
                                }
                            }

                            // Get Physical Office Locations/ Physician Group Name for this provider
                            def officeLocations = []
                            if (dr_Id && dr_Id != "NA") {
                                try {
                                    officeLocations = getOfficeLocations(httpClient,objectMapper,jwtToken,constants,dr_Id)
                                } catch (Exception e) {
                                    println "   Warning: Could not retrieve office locations for Dr_Id ${dr_Id}: ${e.getMessage()}"
                                }
                            }        

                            handler {
                                uid                     providerData["Dr_Id"] ?: "NA"
                                id                      providerData["Dr_Id"] ?: "NA"
                                attribute 'FirstName',  providerData["FirstName"]
                                attribute 'LastName',   providerData["LastName"]
                                attribute 'NPI',        providerData["NPI"]
                                attribute 'ExternalId', providerData["ExternalId"]
                                attribute 'Email',      providerData["Email"]
                                attribute 'LastModifiedOn', providerData["LastModifiedOn"]
                                attribute 'ProviderTypes_Id', providerData["ProviderTypes_Id"]
                                attribute 'PrimaryTitles_Code', providerData["PrimaryTitles_Code"]
                                attribute 'PrimaryHomeAddressLine1', providerData["PrimaryHomeAddressLine1"]
                                attribute 'PrimaryHomeAddressLine2', providerData["PrimaryHomeAddressLine2"]
                                attribute 'PrimaryHomeAddressCity', providerData["PrimaryHomeAddressCity"]
                                attribute 'PrimaryHomeAddressState_Code', providerData["PrimaryHomeAddressState_Code"]
                                attribute 'PrimaryHomeAddressZipcode', providerData["PrimaryHomeAddressZipcode"]
                                attribute 'Fax',         providerData["Fax"]
                                attribute 'Gender',      providerData["Gender"]
                                attribute 'associates', associates
                                attribute 'licenses', licenses
                                attribute 'specialities', specialities
                                attribute 'officelocations', officeLocations
                                attribute 'facilities', facilities
                            }
                        }
                    }
                } catch (Exception e) {
                    throw new IOException("Error calling demographics filter",e)
                }

            } else if(filter instanceof OrFilter){
                println "Filter to type " + filter.getClass().getName() + " not supported"
            }
            return new SearchResult()
        } else {
           
            int currentPage = 1
            String providerTypeId = "1"
            boolean hasMoreData = true
            int totalRecordsProcessed = 0
            int pageSize = constants.pageSize
           
            String currentPageCookie = null
           
            if(null != options.getPagedResultsOffset() && options.getPagedResultsOffset() > 0){
                int offset = options.getPagedResultsOffset()
                currentPage = (offset/pageSize) + 1
            }
            
            if (null != options.pagedResultsCookie) {
                currentPage = Integer.parseInt(options.pagedResultsCookie.toString())
            } else {
                println "No Cookie found"
            }   
           
    
            def requestBody = null
            try {
                    requestBody = createDemographicsFilterRequest(currentPage, pageSize, providerTypeId)
            } catch (Exception e){
                    e.printStackTrace
            }
            def jsonString = objectMapper.writeValueAsString(requestBody)
            println "Filter "+ jsonString

            def url = constants.apiBaseUrl + constants.demographicsFilterEndpoint
            println "Provider URL "+url
            def request = new HttpPost(url)
            ensureJwtToken()
            // Set headers
            request.setHeader("Authorization", "Bearer " + jwtToken)
            request.setHeader("Content-Type", "application/json")
            request.setHeader("Accept", "application/json")

            // Set request body
            def entity = new StringEntity(jsonString, StandardCharsets.UTF_8)
            request.setEntity(entity)
   
            try {
                    def response = httpClient.execute(request)
                    def responseEntity = response.getEntity()
                    def responseBody = EntityUtils.toString(responseEntity, StandardCharsets.UTF_8)

                    if (response.getStatusLine().getStatusCode() >= 200 &&
                            response.getStatusLine().getStatusCode() < 300) {
                            
                        // Parse response to check for more data
                        def jsonResponse = objectMapper.readTree(responseBody)
                        
                        // Check if response has the expected structure
                        def codeNode = jsonResponse.get("Code")
                        if (codeNode == null || codeNode.asInt() != 1000) {
                            def description = jsonResponse.has("Description") ?
                                    jsonResponse.get("Description").asText() : "Unknown error"
                            throw new IOException("API call failed. Code: " +
                                    (codeNode != null ? codeNode.asInt() : "unknown") +
                                    ", Description: " + description)
                        }
                        // Get the Value object containing the actual data
                        def valueNode = jsonResponse.get("Value")
                        if (valueNode != null) {
                            // Get pagination info
                            def paginationNode = valueNode.get("Pagination")
                            def resultNode = valueNode.get("Result")

                            if (paginationNode != null && resultNode != null && resultNode.isArray()) {
                                int recordsInThisPage = resultNode.size()
                                int totalRecords = paginationNode.has("TotalRecords") ? paginationNode.get("TotalRecords").asInt() : 0
                                //println "Total Records "+totalRecords
                                int currentPageFromResponse = paginationNode.has("Page") ? paginationNode.get("Page").asInt() : currentPage
                                totalRecordsProcessed += recordsInThisPage
                                //println "Total Records Processed "+ totalRecordsProcessed
                                for (int i = 0; i < resultNode.size(); i++) {
                                    def provider = resultNode.get(i)
                                    Map<String, String> providerData = [:]
                                    for (String attrName : demographicSchema.providerAttributes) {
                                        // Check for existence of the field and convert to text; default to an empty string
                                        //println "Attribute "+ attrName 
                                        //println "Value " + provider.has(attrName) ? provider.get(attrName).asText() : "NA"
                                        providerData[attrName] = provider.has(attrName) ? provider.get(attrName).asText() : "NA"
                                    }
                                    def title = providerData["PrimaryTitles_Code"] ?: "NA"
                                    def dr_Id = providerData["Dr_Id"] ?: "NA"
                                    //println "Processing Dr_Id: " + dr_Id + ", Title: " + title
                                    // Get affiliations, licenses, facilities, associates
                    
                                    def associates = []
                                    if(title == "PA"){
                                        try {
                                            associates = getAssociates(httpClient,objectMapper,jwtToken,constants,dr_Id)
                                        } catch (Exception e){
                                            println "   Warning: Could not retrieve associates for Dr_Id ${dr_Id}: ${e.getMessage()}"
                                        }
                                    }
                                    // Get licenses for this provider
                                    def licenses = []
                                    if (dr_Id && dr_Id != "NA") {
                                        try {
                                            licenses = getLicenses(httpClient,objectMapper,jwtToken,constants,dr_Id)
                                        } catch (Exception e) {
                                            println "   Warning: Could not retrieve licenses for Dr_Id ${dr_Id}: ${e.getMessage()}"
                        
                                        }
                                    }
                                    // Get facilities for this provider
                                    def facilities = []
                                    if (dr_Id && dr_Id != "NA") {
                                        try {
                                            facilities = getFacilities(httpClient,objectMapper,jwtToken,constants,dr_Id)
                                        } catch (Exception e) {
        
                                            println "   Warning: Could not retrieve facilities for Dr_Id ${dr_Id}: ${e.getMessage()}"
                                        }
                                    }
                                    // Get specialities for this provider
                                    def specialities = []
                                    if (dr_Id && dr_Id != "NA") {
                                        try {
                                            specialities = getSpecialities(httpClient,objectMapper,jwtToken,constants,dr_Id)
                                        } catch (Exception e) {
                                            println "   Warning: Could not retrieve facilities for Dr_Id ${dr_Id}: ${e.getMessage()}"
                                        }
                                    }

                                    // Get Physical Office Locations/ Physician Group Name for this provider
                                    def officeLocations = []
                                    if (dr_Id && dr_Id != "NA") {
                                        try {
                                             officeLocations = getOfficeLocations(httpClient,objectMapper,jwtToken,constants,dr_Id)
                                        } catch (Exception e) {
                                            println "   Warning: Could not retrieve office locations for Dr_Id ${dr_Id}: ${e.getMessage()}"
                                        }
                                    }    

                                    handler {
                                        uid                     providerData["Dr_Id"] ?: "NA"
                                        id                      providerData["Dr_Id"] ?: "NA"
                                        attribute 'FirstName',  providerData["FirstName"]
                                        attribute 'LastName',   providerData["LastName"]
                                        attribute 'NPI',        providerData["NPI"]
                                        attribute 'ExternalId', providerData["ExternalId"]
                                        attribute 'Email',      providerData["Email"]
                                        attribute 'LastModifiedOn', providerData["LastModifiedOn"]
                                        attribute 'ProviderTypes_Id', providerData["ProviderTypes_Id"]
                                        attribute 'PrimaryTitles_Code', providerData["PrimaryTitles_Code"]
                                        attribute 'PrimaryHomeAddressLine1', providerData["PrimaryHomeAddressLine1"]
                                        attribute 'PrimaryHomeAddressLine2', providerData["PrimaryHomeAddressLine2"]
                                        attribute 'PrimaryHomeAddressCity', providerData["PrimaryHomeAddressCity"]
                                        attribute 'PrimaryHomeAddressState_Code', providerData["PrimaryHomeAddressState_Code"]
                                        attribute 'PrimaryHomeAddressZipcode', providerData["PrimaryHomeAddressZipcode"]
                                        attribute 'Fax',         providerData["Fax"]
                                        attribute 'Gender',      providerData["Gender"]
                                        attribute 'associates', associates
                                        attribute 'licenses', licenses
                                        attribute 'specialities', specialities
                                        attribute 'officelocations', officeLocations
                                        attribute 'facilities', facilities
                                    }
                                }
                                // Check if we've processed all available records
                                if (totalRecordsProcessed >= totalRecords) {
                                    //println "Done"
                                    return new SearchResult()
                                } else {
                                    currentPage++
                                    //println "Current Page after rows " + currentPage
                                    return new SearchResult(currentPage.toString(),-1)
                                }
                               
                            } 
                        }
                    }
                } catch (Exception e){
                    e.printStackTrace()
                }
            //}
            return new SearchResult()
        }
        break
    default:
        break
}
def getJwtToken() {
    def tokenRequest = new HttpPost(constants.tokenUrl)
    // Set headers
    tokenRequest.setHeader("Content-Type", "application/json")

    // Create request body JSON
    def requestBody = objectMapper.createObjectNode()
    def requester = objectMapper.createObjectNode()
    requester.put("Key", apiKey)
    requester.put("Id", requesterId)
    requester.put("Secret", requesterSecret)
    requester.put("Resource", resource)

    def parameters = objectMapper.createObjectNode()

    requestBody.set("Requester", requester)
    requestBody.set("Parameters", parameters)

    def jsonString = objectMapper.writeValueAsString(requestBody)
    def entity = new StringEntity(jsonString, StandardCharsets.UTF_8)
    tokenRequest.setEntity(entity)

    try {
        def response = httpClient.execute(tokenRequest)
        def responseEntity = response.getEntity()
        def responseBody = EntityUtils.toString(responseEntity, StandardCharsets.UTF_8)

        if (response.getStatusLine().getStatusCode() == 200) {
            def jsonResponse = objectMapper.readTree(responseBody)

            // Check response code
            def codeNode = jsonResponse.get("Code")
            if (codeNode != null && codeNode.asInt() == 1000) {
                // Extract token from Value field
                def valueNode = jsonResponse.get("Value")
                if (valueNode != null) {
                    this.jwtToken = valueNode.asText()
                    //return this.jwtToken
                } else {
                    throw new IOException("Token value not found in response")
                }
            } else {
                def description = jsonResponse.has("Description") ?
                        jsonResponse.get("Description").asText() : "Unknown error"
                throw new IOException("Authentication failed. Code: " +
                        (codeNode != null ? codeNode.asInt() : "unknown") +
                        ", Description: " + description)
            }
        } else {
            throw new IOException("Failed to obtain JWT token. Status: " +
                    response.getStatusLine().getStatusCode() + ", Response: " + responseBody)
        }
    } catch (Exception e) {
        throw new IOException("Error obtaining JWT token", e)
    }
}

def ensureJwtToken() {
    if (jwtToken == null || jwtToken.isEmpty()) {
        getJwtToken()
    }
}

def getAffiliations(CloseableHttpClient httpClient, ObjectMapper objectMapper, String token, CStreamConstants constants, String drId) throws IOException {
    //ensureJwtToken()
    affiliationsService = new Affiliations(httpClient,objectMapper,constants)
    return affiliationsService.getAffiliations(drId, token)
}
def getLicenses(CloseableHttpClient httpClient, ObjectMapper objectMapper, String token, CStreamConstants constants,String drId) throws IOException {
    //ensureJwtToken()
    licensesService = new Licenses(httpClient,objectMapper,constants)
    return licensesService.getLicenses(drId, token)
}
//(httpClient,objectMapper,jwtToken,constants,dr_Id)
def getFacilities(CloseableHttpClient httpClient, ObjectMapper objectMapper, String token, CStreamConstants constants,String drId) throws IOException {
    //ensureJwtToken()
    facilitiesService = new Facilities(httpClient,objectMapper,constants)
    return facilitiesService.getFacilities(drId, token)
}

def getAssociates(CloseableHttpClient httpClient, ObjectMapper objectMapper, String token, CStreamConstants constants,String drId) throws IOException {
    //ensureJwtToken()
    //println "Getting Associates for Dr_Id: " + drId
    approvedAssociatesService = new ApprovedAssociates(httpClient,objectMapper,constants)
    return approvedAssociatesService.getSupervisingPhysicians(drId, token)
}

def getSpecialities(CloseableHttpClient httpClient, ObjectMapper objectMapper, String token, CStreamConstants constants,String drId) throws IOException {
    //ensureJwtToken()
   specialitiesService = new Specialities(httpClient,objectMapper,constants)
    return specialitiesService.getSpecialities(drId, token)
}

def getOfficeLocations(CloseableHttpClient httpClient, ObjectMapper objectMapper, String token, CStreamConstants constants,String drId) throws IOException {
    //ensureJwtToken()
   officeLocationsService = new OfficeLocations(httpClient,objectMapper,constants)
   return officeLocationsService. getOfficeLocations(drId, token)
}

def ObjectNode createDemographicsFilterRequest(int page, int pageSize, String providerTypeId) {
    def requestBody = objectMapper.createObjectNode()
    requestBody.put("Page", page)
    requestBody.put("PageSize", pageSize)

    // Create Filter object
    def filter = objectMapper.createObjectNode()
    filter.put("Logic", "OR")

    // Create Filters array
    def filterItem = objectMapper.createObjectNode()
    filterItem.put("Field", "ProviderTypes_Id")
    filterItem.put("Operator", "EQUALS")
    filterItem.put("Value", providerTypeId)

    filter.set("Filters", objectMapper.createArrayNode().add(filterItem))
    requestBody.set("Filter", filter)

    // Create Sort array
    def sortItem = objectMapper.createObjectNode()
    sortItem.put("Field", "FirstName")
    sortItem.put("Direction", "DESCENDING")

    requestBody.set("Sort", objectMapper.createArrayNode().add(sortItem))

    return requestBody
}