# OpenNMS Cloud Plugin [![CircleCI](https://circleci.com/gh/OpenNMS/opennms-cloud-plugin.svg?style=svg)](https://circleci.com/gh/OpenNMS/opennms-cloud-plugin)

The OpenNMS Cloud Plugin enables OpenNMS to store data in the cloud.
Initially it will provide storage for time series data.

**Build** and install the plugin into your local Maven repository using:
```
mvn clean install
```

**Install** via OpenNMS Karaf shell:
```
feature:repo-add mvn:org.opennms.plugins.cloud/karaf-features/1.0.0-SNAPSHOT/xml
feature:install opennms-plugins-cloud
```
**Configure**

The default configuration has the following settings:
```
host=localhost
port=5001
tokenKey=x-scope-orgid
tokenValue=acme
mtlsEnabled=false
certificatePath=${OPENNMS_HOME}/etc
```

Change configuration via Karaf shell:
```
config:edit opennms-plugins-cloud
property-set host localhost
property-set port 5001
property-set tokenKey x-scope-orgid
property-set tokenValue acme
property-set mtlsEnabled false
property-set certificatePath /etc/bla
config:update
```

**Update bundle** automatically (only relevant for development):
```
bundle:watch *
```