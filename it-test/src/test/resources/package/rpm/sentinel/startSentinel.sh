#!/bin/bash

SENTINEL_HOME=/opt/sentinel
chown opennms $SENTINEL_HOME/etc/sentinel-datasources.xml /run/sentinel

cd $SENTINEL_HOME/bin
JAVA_HOME=$(./find-java.sh)
export JAVA_HOME=$JAVA_HOME
sudo -u sentinel ./karaf server