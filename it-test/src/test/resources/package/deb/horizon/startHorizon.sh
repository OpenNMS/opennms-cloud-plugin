#!/bin/bash

mkdir -p /run/opennms

OPENNMS_HOME=/usr/share/opennms
chown opennms $OPENNMS_HOME/etc/opennms-datasources.xml /run/opennms

cd $OPENNMS_HOME
bin/install -dis
bin/opennms -f start
