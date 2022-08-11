# Time Series as a Service

These classes provide access to _Time Series as a Service_.

## Prerequisites
* [Timeseries Integration Layer](https://docs.opennms.com/horizon/latest/deployment/time-series-storage/timeseries/ts-integration-layer.html) is enabled in OpenNMS.

## Interfaces exposed to OpenNMS:
Interfaces exposed via OpenNMS Integration API (OIA):
* _TsaasStorage_ implements _TimeSeriesStorage_ will be picked up by [TimeseriesStorageManager](https://github.com/OpenNMS/opennms/blob/develop/features/timeseries/src/main/java/org/opennms/netmgt/timeseries/TimeseriesStorageManager.java)