# Doing a Release

## Update the POMs and Integration Tests for Release

```bash
export FROMVERSION=1.0.6-SNAPSHOT TOVERSION=1.0.6
mvn versions:set -DnewVersion="${TOVERSION}"
perl -pi -e "s,${FROMVERSION},${TOVERSION},g" it-test/src/test/resources/docker-compose.yaml it-test/src/test/java/org/opennms/plugins/cloud/ittest/EndToEndIt.java
git commit -a -m "OpenNMS Cloud Plugin v${TOVERSION}"
```

## Tag and Push

```bash
git tag "v${TOVERSION}"
git push origin "v${TOVERSION}"
```

## Update the POMs and Integration Tests for Development

```bash
export FROMVERSION=1.0.6 TOVERSION=1.0.7-SNAPSHOT
mvn versions:set -DnewVersion="${TOVERSION}"
perl -pi -e "s,${FROMVERSION},${TOVERSION},g" it-test/src/test/resources/docker-compose.yaml it-test/src/test/java/org/opennms/plugins/cloud/ittest/EndToEndIt.java
git commit -a -m "update poms: ${FROMVERSION} -> ${TOVERSION}"
```
