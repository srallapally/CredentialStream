package org.tampagen.utils

/**
 * Encapsulates all URL constants used throughout the CStream application.
 *
 * The original CStream.groovy defined several static final strings for
 * endpoints and base URLs. To eliminate static fields and centralize
 * configuration, those values have been moved into this class. Each field
 * is an instance field rather than static to conform to the requirement
 * of avoiding static members. Consumers of these constants should
 * instantiate this class and access its properties.
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
}