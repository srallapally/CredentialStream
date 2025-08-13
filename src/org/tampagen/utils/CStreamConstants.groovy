package org.tampagen.utils

/**
 * CStreamConstants.groovy
 * This class contains constants used throughout the CredentialStream application.
 * It includes API endpoints, page size, and other configuration values.
 *
 * @author Sanjay Rallapally
 * @version 1.0
 */
public class CStreamConstants {

    CStreamConstants(){}

        /**
     * Page Size
     */
    final int pageSize = 50
    /**
     * OAuth2 token validation endpoint for obtaining JWT tokens.
     */
    final String tokenUrl =
            "https://api.veritystream.cloud/services/oauth/api/authentication/jwt/validate"

    /**
     * Base URL for all API calls to VerityStream.
     */
    final String apiBaseUrl = "https://api.veritystream.cloud"

    /**
     * Endpoint for getting a single Provider record
     */
    final String demographicsFindEndpoint =
            "/services/verityconnect/api/core/v1/Demographics/find"
    /**
     * Endpoint for filtering demographic records (pagination of providers).
     */
    final String demographicsFilterEndpoint =
            "/services/verityconnect/api/core/v1/Demographics/filter"

    /**
     * Endpoint for retrieving credential/license data for a provider.
     */
    final String credentialsLicensesEndpoint =
            "/services/verityconnect/api/core/v1/Demographic/CredentialsLicenses/Filter"

    /**
     * Endpoint for retrieving facility data for a provider.
     */
    final String facilitiesEndpoint =
            "/services/verityconnect/api/core/v1/Demographic/Facilities/Filter"

    /**
     * Endpoint for retrieving affiliation data for a provider.
     */
    final String affiliationsEndpoint =
            "/services/verityconnect/api/core/v1/Demographic/Affiliation/Filter"

    /**
     * Endpoint for retrieving Specialities data for a provider.
     */
    final String specialitiesEndpoint =
            "/services/verityconnect/api/core/v1/Demographic/Specialties/Filter"

    /**
    * Endpoint for retrieving Approved Associates data for a provider.
    */
    final String approvedAssociatesEndpoint =
            "/services/verityconnect/api/core/v1/Demographic/ApprovedAssociates/Filter"

   /**
    * Endpoint for retrieving Office Locations data for a provider.
    */
    final String officeLocationsEndpoint =
            "/services/verityconnect/api/core/v1/Demographic/OfficesLocations/Filter"

   /**
    * Endpoint for retrieving Locations data for a location.
    */
    final String locationsEndpoint =
            "/services/verityconnect/api/core/v1/lookup/locations/find"
}