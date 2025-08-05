package org.tampagen.utils

/**
 * Defines the list of demographic attributes that should be extracted
 * for each provider record when processing the demographics endpoint.
 *
 * This class provides an instance-level list of attribute names instead
 * of hard-coding them in the main client. New attributes can be added
 * to this list without modifying the extraction logic in CStream.groovy.
 */
class DemographicSchema {

    /**
     * Ordered list of provider attribute names to extract from the
     * demographics result. Each entry corresponds to a field expected
     * in the provider record returned from the API.
     */
    final List<String> providerAttributes
    final List<String> affiliationAttributes

    DemographicSchema() {
        // Initialize the list with the attribute names. This list can
        // easily be modified to include additional demographic fields
        // without touching the extraction logic in VerityStreamApiClient.
        this.providerAttributes = [
                "FirstName",
                "LastName",
                "NPI",
                "Dr_Id",
                "ExternalId",
                "Email",
                "LastModifiedOn",
                "ProviderTypes_Id",
                "PrimaryTitles_Code",
                "PrimaryHomeAddressLine1",
                "PrimaryHomeAddressLine2",
                "PrimaryHomeAddressCity",
                "PrimaryHomeAddressState_Code",
                "PrimaryHomeAddressZipcode",
                "Fax"
        ]
        this.affiliationAttributes = [
                "Id",
                "Type_Code",
                "Speciality_Code",
                "Institution_Code",
                "Position_Code"
        ]
    }
}