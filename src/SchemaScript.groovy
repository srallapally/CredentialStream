import org.forgerock.openicf.connectors.groovy.OperationType
import org.forgerock.openicf.connectors.groovy.ScriptedConfiguration
import org.identityconnectors.common.logging.Log
import org.identityconnectors.framework.common.objects.ObjectClass
import org.identityconnectors.framework.common.objects.OperationOptionInfoBuilder
import org.identityconnectors.framework.spi.operations.SearchOp

import static org.identityconnectors.framework.common.objects.AttributeInfo.Flags.MULTIVALUED
import static org.identityconnectors.framework.common.objects.AttributeInfo.Flags.REQUIRED

def operation = operation as OperationType
def configuration = configuration as ScriptedConfiguration
def log = log as Log

return builder.schema({
    objectClass {
        type ObjectClass.ACCOUNT_NAME
        attributes {
            "FirstName" String.class, REQUIRED
            "LastName" String.class, REQUIRED
            "NPI" String.class
            "Dr_Id" String.class, REQUIRED
            "ExternalId" String.class, REQUIRED
            "Email" String.class, REQUIRED
            "LastModifiedOn" String.class
            "ProviderTypes_Id" String.class, REQUIRED
            "PrimaryTitles_Code" String.class
            "PrimaryHomeAddressLine1" String.class
            "PrimaryHomeAddressLine2" String.class
            "PrimaryHomeAddressCity" String.class
            "PrimaryHomeAddressState_Code" String.class
            "PrimaryHomeAddressZipcode" String.class
            "Fax" String.class
            facilities Map.class, MULTIVALUED
            affiliations Map.class, MULTIVALUED
            licenses Map.class, MULTIVALUED
        }

    }
   
    defineOperationOption OperationOptionInfoBuilder.buildPagedResultsCookie(), SearchOp
    defineOperationOption OperationOptionInfoBuilder.buildPagedResultsOffset(), SearchOp
    defineOperationOption OperationOptionInfoBuilder.buildPageSize(), SearchOp
    defineOperationOption OperationOptionInfoBuilder.buildSortKeys(), SearchOp
    defineOperationOption OperationOptionInfoBuilder.buildRunWithUser()
    defineOperationOption OperationOptionInfoBuilder.buildRunWithPassword()
    }
)

