#!/bin/bash

OPENNMS_HOME=/opt/opennms
OPENNMS_VAR=/var/run/opennms

if [ ! -d "$OPENNMS_VAR" ]; then
  mkdir -p $OPENNMS_VAR
fi

chown opennms $OPENNMS_HOME/etc/opennms-datasources.xml $OPENNMS_VAR

cd $OPENNMS_HOME
bin/install -dis
bin/opennms -f start
