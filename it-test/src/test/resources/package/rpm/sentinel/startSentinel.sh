#!/bin/bash

SENTINEL_HOME=/opt/sentinel
SENTINEL_VAR=/var/run/sentinel

if [ ! -d "$SENTINEL_VAR" ]; then
  mkdir -p $SENTINEL_VAR
fi

chown opennms $SENTINEL_HOME/etc/sentinel-datasources.xml $SENTINEL_VAR

cd $SENTINEL_HOME/bin
JAVA_HOME=$(./find-java.sh)
export JAVA_HOME=$JAVA_HOME
sudo -u sentinel ./karaf server
