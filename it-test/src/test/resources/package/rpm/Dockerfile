FROM redhat/ubi8

RUN dnf install -y curl sudo gnupg2 && \
    curl -1sLf 'https://packages.opennms.com/public/common/setup.rpm.sh' | sudo -E bash && \
    curl -1sLf 'https://packages.opennms.com/public/develop/setup.rpm.sh' | sudo -E bash

COPY opennms-plugin-cloud.rpm /tmp/opennms-plugin-cloud.rpm
COPY sentinel-plugin-cloud.rpm /tmp/sentinel-plugin-cloud.rpm

RUN dnf install -y https://download.postgresql.org/pub/repos/yum/reporpms/EL-8-x86_64/pgdg-redhat-repo-latest.noarch.rpm && \
    dnf install -y opennms /tmp/opennms-plugin-cloud.rpm \
                   opennms-sentinel  /tmp/sentinel-plugin-cloud.rpm

COPY --chown=opennms horizon/org.apache.karaf.shell.cfg /opt/opennms/etc/org.apache.karaf.shell.cfg
COPY --chown=sentinel sentinel/org.apache.karaf.shell.cfg /opt/sentinel/etc/org.apache.karaf.shell.cfg

COPY --chown=opennms horizon/startHorizon.sh /startHorizon.sh
COPY --chown=opennms sentinel/startSentinel.sh /startSentinel.sh

RUN chmod 755 /startHorizon.sh /startSentinel.sh
