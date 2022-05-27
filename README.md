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
opennms-cloud:import-cert /path/to/credentials/credentials.zip
```
The zip will be deleted automatically after a successful import.

***Properties***

The default configuration has the following settings:
```
host=localhost
port=5001
tokenKey=x-scope-orgid
mtlsEnabled=false
```

Change configuration via Karaf shell:
```
config:edit org.opennms.plugins.cloud
property-set host localhost
property-set port 5001
property-set tokenKey x-scope-orgid
property-set mtlsEnabled false
config:update
```

**Update bundle** automatically (only relevant for development):
```
bundle:watch *
```