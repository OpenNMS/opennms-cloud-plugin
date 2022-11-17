FROM ubuntu:20.04

RUN apt-get update && \
    DEBIAN_FRONTEND=noninteractive apt-get install -y curl sudo gnupg2 && \
    curl -1sLf 'https://packages.opennms.com/public/common/setup.deb.sh' | sudo -E bash && \
    curl -1sLf 'https://packages.opennms.com/public/develop/setup.deb.sh' | sudo -E bash && \
    apt-get update

COPY opennms-plugin-cloud.deb /tmp/opennms-plugin-cloud.deb
COPY sentinel-plugin-cloud.deb /tmp/sentinel-plugin-cloud.deb

RUN DEBIAN_FRONTEND=noninteractive apt-get install -y opennms /tmp/opennms-plugin-cloud.deb \
                                                      opennms-sentinel  /tmp/sentinel-plugin-cloud.deb

COPY --chown=opennms horizon/org.apache.karaf.shell.cfg /usr/share/opennms/etc/org.apache.karaf.shell.cfg
COPY --chown=sentinel sentinel/org.apache.karaf.shell.cfg /usr/share/sentinel/etc/org.apache.karaf.shell.cfg

COPY --chown=opennms horizon/startHorizon.sh /startHorizon.sh
COPY --chown=opennms sentinel/startSentinel.sh /startSentinel.sh

RUN chmod 755 /startHorizon.sh /startSentinel.sh
