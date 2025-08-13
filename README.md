<pre>
Deployment Instructions for Ping AIC
**1. Create a folder in RCS as shown below**
CredentialStream
└── org
    └── tampagen
        └── utils
            ├── CStreamConstants.groovy
            ├── DemographicSchema.groovy
            ├── Affiliations.groovy
            ├── ApprovedAssociates.groovy
            ├── Facilities.groovy
            ├── Licenses.groovy
            ├── OfficeLocations.groovy
            ├── Providers.groovy
            └── Specialities.groovy
        ├── AuthenticateScript.groovy
        ├── CreateScript.groovy
        ├── DeleteScript.groovy
        ├── SchemaScript.groovy
        ├── SearchScript.groovy
        ├── TestScript.groovy
        └── UpdateScript.groovy

**Modify SearchScript.groovy and set the following parameters**
// Configuration
apiKey = "<API Key>"
requesterId = "<REQUESTER _ID>"
requesterSecret = "<SECRET>"
resource = "<RESOURCE>"
**Define a Scripted Groovy Authoritative App**
Select the RCS (in case you have multiple)
Specify the file paths
</pre>
