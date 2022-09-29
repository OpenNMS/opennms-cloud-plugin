# OpenNMS Cloud Plugin [![CircleCI](https://circleci.com/gh/OpenNMS/opennms-cloud-plugin.svg?style=svg)](https://circleci.com/gh/OpenNMS/opennms-cloud-plugin)

The OpenNMS Cloud Plugin enables OpenNMS to store data in the cloud.
Initially it will provide storage for time series data (tsaas).

# Installation
## Prerequisites
### System Id
The default system id of older OpenNMS installations is '00000000-0000-0000-0000-000000000000'.
It is advisable to change it to a true UUID.
This will allow to distinguish different systems in the cloud logs.

Execute this sql against your database:
```
CREATE EXTENSION pgcrypto;
UPDATE monitoringsystems SET id=gen_random_uuid () WHERE id = '00000000-0000-0000-0000-000000000000' AND type='OpenNMS' AND location='Default';
```

## Build and Install
Build and install the plugin into your local Maven repository using:
```
mvn clean install
```

Install into OpenNMS via Karaf shell:
```
feature:repo-add mvn:org.opennms.plugins.cloud/karaf-features/1.0.0-SNAPSHOT/xml
```

for Core (OpenNMS main system): 
```
feature:install opennms-cloud-plugin-core
```

for Sentinel:
```
feature:install opennms-cloud-plugin-sentinel
```

Check if it was properly installed and started:
```
feature:list | grep opennms-cloud-plugin
```
we expect it to say: _Started_

## Configuration
### Initializing
Before the Cloud Plugin can be used with the OpenNMS Cloud it needs to be configured.
In order to configure it you need to obtain an access key for your organisation.

#### Via Web Interface
Once you have the access key you can enter it into the cloud plugin configuration page within OpenNMS.

#### Via Karaf Shell
An alternative is to use the command line in the Karaf shell:
```
opennms-cloud:init <access token>
```

#### Configuration sequence
In either case the following happens:
* The Cloud Plugin contacts PAS (Platform Access Service) to obtain certificates for MTLS.
  From now on all communication is MTLS secured.
* The Cloud Plugin retrieves all enabled services from PAS.
* All enabled services are configured and enabled in OpenNMS.

### Configuration Properties
The initial properties should be good to go.
However it is possible to change properties via Karaf shell:

```
config:edit org.opennms.plugins.cloud
property-set pas.host access-test.staging.nonprod.dataservice.opennms.com
property-set pas.port 443
property-set pas.security TLS
property-set tsaas.batchSize 1000
property-set tsaas.maxBatchWaitTimeInMilliSeconds 5000
config:update
```

# Verify / Monitoring

## Health Check
Check the cloud status with:
```
opennms:health-check
```

# Development
## Import Certificates

The initializing can happen via a zip file as an alternative to using PAS.

Move the cloud credentials file to: `[opennms.home]/etc/cloud-credentials.zip`.
When the plugin is started it will import the cloud credentials automatically and delete the file after successful import.
