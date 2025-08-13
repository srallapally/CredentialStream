package org.tampagen.utils

/**
 * Defines the list of demographic attributes that should be extracted
 * for each provider record when processing the demographics endpoint.
 * 
 * @author Sanjay Rallapally
 * @version 1.0
 */
class DemographicSchema {

    /**
     * Ordered list of provider attribute names to extract from the
     * demographics result. Each entry corresponds to a field expected
     * in the provider record returned from the API.
     */
    final List<String> providerAttributes
    final List<String> affiliationAttributes
    final List<String> specialitiesAttributes
    final List<String> officeLocationsAttributes
    
    DemographicSchema() {

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
                "Fax",
                "Gender"
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