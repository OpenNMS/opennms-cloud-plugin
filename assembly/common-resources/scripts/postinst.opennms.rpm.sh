#!/bin/bash

OPENNMS_HOME="/opt/opennms"

if [ -f "$OPENNMS_HOME/etc/opennms.conf" ]; then
  . "$OPENNMS_HOME/etc/opennms.conf"
else
  RUNAS="opennms"
fi

chown -R $RUNAS "$OPENNMS_HOME/etc/examples/opennms.properties.d"

if [ -f "$OPENNMS_HOME/etc/featuresBoot.d/plugin-cloud.boot" ]; then
  chown -R $RUNAS "$OPENNMS_HOME/etc/featuresBoot.d/plugin-cloud.boot"
fi

echo "Please make sure org.opennms.timeseries.strategy=integration"
echo "Example config is located at $OPENNMS_HOME/etc/examples/opennms.properties.d/timeseries.properties"
