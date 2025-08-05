import org.forgerock.openicf.connectors.groovy.OperationType
import org.forgerock.openicf.connectors.groovy.ScriptedConfiguration
import org.identityconnectors.common.logging.Log
import org.identityconnectors.framework.common.objects.Attribute
import org.identityconnectors.framework.common.objects.AttributesAccessor
import org.identityconnectors.framework.common.objects.ObjectClass
import org.identityconnectors.framework.common.objects.OperationOptions
import org.identityconnectors.framework.common.objects.Uid
import org.identityconnectors.framework.common.exceptions.ConnectorException

def operation = operation as OperationType
def configuration = configuration as ScriptedConfiguration

def objectClass = objectClass as ObjectClass
def options = options as OperationOptions
def uid = id as String
def log = log as Log


switch (operation) {
    case OperationType.CREATE:
        println "Entering Create script for " + objectClass
        throw new ConnectorException("CREATE is not supported in this Connector");
}