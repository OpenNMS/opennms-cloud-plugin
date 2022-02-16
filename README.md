# OpenNMS Cloud Plugin [![CircleCI](https://circleci.com/gh/OpenNMS/opennms-cloud-plugin.svg?style=svg)](https://circleci.com/gh/OpenNMS/opennms-cloud-plugin)

The OpenNMS Cloud Plugin enables OpenNMS to store data in the cloud.
Initially it will provide storage for time series data.

Build and install the plugin into your local Maven repository using:
```
mvn clean install
```

> OpenNMS normally runs as root, so make sure the artifacts are installed in `/root/.m2` or try making `/root/.m2` symlink to your user's repository

From the OpenNMS Karaf shell:
```
feature:repo-add mvn:org.opennms.plugins.cloud/karaf-features/1.0.0-SNAPSHOT/xml
feature:install opennms-plugins-cloud
```
Configure:
```
config:edit opennms-plugins-cloud
property-set host grpc-server.7760e3a2553b4cc7ac31.eastus.aksapp.io
property-set port 443
config:update
```

Update automatically:
```
bundle:watch *
```