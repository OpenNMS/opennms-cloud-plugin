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

Before mtls can be enabled, we need to import the certificates via OpenNMS Karaf shell:
```
opennms-cloud:import-cert /path/to/credentials/cloud-credentials.zip
```
The zip will be deleted automatically after a successful import.

An alternative to the karaf comment is placing the zip file into `[opennms.home]/etc/cloud-credentials.zip`.
The plugin will import the cloud credentials automatically and delete the file after successful import.

***Properties***

The default configuration has the following settings:
```
host=localhost
port=5001
tokenKey=token
mtlsEnabled=false
```

Change configuration via Karaf shell:
```
config:edit org.opennms.plugins.cloud
property-set host localhost
property-set port 5001
property-set tokenKey token
property-set mtlsEnabled false
config:update
```

***Verify***

Check the cloud status with: 
```
opennms:health-check
```

**Update bundle** automatically (only relevant for development):
```
bundle:watch *
```