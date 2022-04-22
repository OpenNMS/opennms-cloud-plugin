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

***Properties***
The default configuration has the following settings:
```
host=localhost
port=5001
tokenKey=x-scope-orgid
tokenValue=acme
mtlsEnabled=false
```

Change configuration via Karaf shell:
```
config:edit org.opennms.plugins.cloud
property-set host localhost
property-set port 5001
property-set tokenKey x-scope-orgid
property-set tokenValue acme
property-set mtlsEnabled false
config:update
```

***Import Certificates***

Before mtls can be enabled, we need to import the certificates via OpenNMS Karaf shell:
```
opennms-tsaas:import-cert --type=publickey --file=/path/to/file/client.crt
opennms-tsaas:import-cert --type=privatekey --file=/path/to/file/client_pkcs8_key.pem
opennms-tsaas:import-cert --type=truststore -file=/path/to/file/truststore.pem
```
The private and public keys are mandatory.
The truststore file is optional, if not supplied, the default java truststore is used.

**Update bundle** automatically (only relevant for development):
```
bundle:watch *
```