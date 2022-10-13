#!/bin/bash

OPENNMS_HOME="/usr/share/opennms"

if [ -f "$OPENNMS_HOME/etc/opennms.conf" ]; then
  . "$OPENNMS_HOME/etc/opennms.conf"
else
  RUNAS="opennms"
fi

chown -R $RUNAS "$OPENNMS_HOME/etc/examples/opennms.properties.d"

echo "Please make sure org.opennms.timeseries.strategy=integration"
echo "Example config is located at $OPENNMS_HOME/etc/examples/opennms.properties.d/timeseries.properties"
