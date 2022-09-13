# Cloud Plugin ReST API

## Appliances

### Get Appliance List
GET http://localhost:8980/opennms/rest/plugin/cloud/appliance


### Launch Configuration Process

POST http://localhost:8980/opennms/rest/plugin/cloud/appliance/configure


## Configuration

### Set Cloud Portal Activation Key

PUT http://localhost:8980/opennms/rest/plugin/cloud/config/activationkey

### Get Cloud Portal Configuration Status

GET http://localhost:8980/opennms/rest/plugin/cloud/config/status



## Additional Information

see also: https://docs.opennms.com/meridian/2022/development/rest/osgi.html