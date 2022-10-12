# OpenNMS Cloud Plugin
[![CircleCI](https://circleci.com/gh/OpenNMS/opennms-cloud-plugin.svg?style=svg)](https://circleci.com/gh/OpenNMS/opennms-cloud-plugin)
[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=OpenNMS_opennms-cloud-plugin&metric=alert_status)](https://sonarcloud.io/summary/new_code?id=OpenNMS_opennms-cloud-plugin)

The OpenNMS Cloud Plugin enables OpenNMS to store data in the cloud.
Initially it will provide storage for time series data (tsaas).

# Installation
## Prerequisites
### OpenNMS System
The cloud plugin requires an [OpenNMS installation](https://docs.opennms.com/horizon/latest/deployment/core/getting-started.html).

### System Id
The default system id of older OpenNMS installations is '00000000-0000-0000-0000-000000000000'.
It is advisable to change it to a true UUID.
This will allow to distinguish different systems in the cloud logs.

To change the system id into a UID execute this sql against your database:
```
CREATE EXTENSION pgcrypto;
UPDATE monitoringsystems SET id=gen_random_uuid () WHERE id = '00000000-0000-0000-0000-000000000000' AND type='OpenNMS' AND location='Default';
```

## Installation and Initialization
###
### Install
Stop OpenNMS.

The plugin is as `deb` and `rpm` archives available.
Install with your favorite package manager:

`apt-get install opennms-cloud-plugin-core`

or

`apt-get install opennms-cloud-plugin-sentinel`

### Enabling Timeseries Integration Layer (TSS)
TSS needs to be activated in order to use the plugin for sending time series data.
Therefore set `org.opennms.timeseries.strategy=integration` in `[opennms.home]/etc/opennms.properties`
For more details on TSS and its configuration see [here](https://docs.opennms.com/horizon/latest/deployment/time-series-storage/timeseries/ts-integration-layer.html).

Start OpenNMS.

### Initialization
Before the Cloud Plugin can be used with the OpenNMS Cloud it needs to be initialized.
In order to start the initialization process you need to obtain an access key for your organisation.

TODO: Link to cloud portal where to get access key 

#### Via Web Interface
Once you have the access key you can enter it into the cloud plugin configuration page within OpenNMS.

#### Via Karaf Shell
An alternative is to use the command line in the Karaf shell:
```
opennms-cloud:init <access token>
```

# Verify / Monitoring

## Health Check
Check the cloud status with:
```
opennms:health-check
```

# Development
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
### Configuration Properties
The initial properties should be good to go.
But for development purposes it is possible to change properties via Karaf shell:

```
config:edit org.opennms.plugins.cloud
```
TLS configuration to PAS.
Used for the init process (get client certificates):
```
property-set pas.tls.host access-test.staging.nonprod.dataservice.opennms.com 
property-set pas.tls.port 443
property-set pas.tls.security TLS
```

MTLS configuration to PAS.
Used for the configuration process (get enabled services, get access token):
```
property-set pas.mtls.host authenticate.access-test.staging.nonprod.dataservice.opennms.com 
property-set pas.mtls.port 443 
property-set pas.mtls.security MTLS
```

If using a self signed cert you can set the truststore.
This is the actual trust store as a string, not a file path.
Example: https://github.com/OpenNMS/opennms-cloud-plugin/blob/jira/DC-342/it-test/src/test/java/org/opennms/plugins/cloud/tsaas/TsaasStorageIT.java#L142
```
property-set grpc.truststore
```

Specific settings for TSaaS:
```
property-set tsaas.batchSize 1000 
property-set tsaas.maxBatchWaitTimeInMilliSeconds 5000
```

```
config:update
```

### Initialization Sequence
1. The Cloud Plugin contacts PAS (Platform Access Service) to obtain certificates for MTLS.
   From now on all communication is MTLS secured.
2. The Cloud Plugin retrieves all enabled services from PAS.
3. A JWT token is received from PAS.
4. All enabled services are configured and enabled in OpenNMS.
