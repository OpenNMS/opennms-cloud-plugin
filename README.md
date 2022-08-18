# OpenNMS Cloud Plugin [![CircleCI](https://circleci.com/gh/OpenNMS/opennms-cloud-plugin.svg?style=svg)](https://circleci.com/gh/OpenNMS/opennms-cloud-plugin)

The OpenNMS Cloud Plugin enables OpenNMS to store data in the cloud.
Initially it will provide storage for time series data (tsaas).

**Build** and install the plugin into your local Maven repository using:
```
mvn clean install
```

**Install** via OpenNMS Karaf shell:
```
feature:repo-add mvn:org.opennms.plugins.cloud/karaf-features/1.0.0-SNAPSHOT/xml
feature:install opennms-cloud-plugin
```
**Configure**

***Import Certificates***

Before mtls can be enabled, we need to import the certificates.
Move the cloud credentials file to: `[opennms.home]/etc/cloud-credentials.zip`.
When the plugin is started it will import the cloud credentials automatically and delete the file after successful import.

***Properties***

Change configuration via Karaf shell for the initial setting:
```
config:edit org.opennms.plugins.cloud
property-set pas.host access-test.staging.nonprod.dataservice.opennms.com
property-set pas.port 443
property-set pas.security TLS
property-set tsaas.batchSize 1000
property-set tsaas.maxBatchWaitTimeInMilliSeconds 5000
config:update
```

Configure cloud access via Karaf shell:
```
opennms-cloud:configure <access token>
```
or via web interface.

This will get the cloud credentials from the Platform Acess Service (PAS) and configure the enabled services.

***Verify***

Check the cloud status with: 
```
opennms:health-check
```

**Update bundle** automatically (only relevant for development):
```
bundle:watch *
```