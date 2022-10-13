# This script recreates all needed certificates for the mtls test.

# use "password" as passphrase
# use "localhost" as common name (CN) or "horizon" in case of IT test
# challenge password: leave empty
# all other questions: use default
# modify CN for your need. (pas server certs are only need for IT test)

# Generate CA key
openssl genrsa -passout pass:"password" -des3 -out ca.key 4096

# Generate CA certificate:
openssl req -new -passin pass:"password" -subj "/CN=localhost" -x509 -days 3650 -key ca.key -out ca.crt

# Generate server key:
openssl genrsa -passout pass:"password" -des3 -out server.key 4096

# Generate server signing request:
openssl req -new -passin pass:"password" -subj "/CN=localhost" -key server.key -out server.csr

# Self-sign server certificate:
openssl x509 -req -passin pass:"password" -days 3650 -in server.csr -CA ca.crt -CAkey ca.key -set_serial 01 -out server.crt

# Convert to pkcs8
openssl pkcs8 -passin pass:"password" -topk8 -nocrypt -in server.key -out server_pkcs8_key.pem

## Generate pas_server key:
#openssl genrsa -passout pass:"password" -des3 -out pas_server.key 4096
#
## Generate pas_server signing request:
#openssl req -new -passin pass:"password" -subj "/CN=cloudMock" -key pas_server.key -out pas_server.csr
#
## Self-sign pas_server certificate:
#openssl x509 -req -passin pass:"password" -days 3650 -in pas_server.csr -CA ca.crt -CAkey ca.key -set_serial 01 -out pas_server.crt
#
## Convert to pkcs8
#openssl pkcs8 -passin pass:"password" -topk8 -nocrypt -in pas_server.key -out pas_server_pkcs8_key.pem

# Create client trust store with public server cert:
keytool -import -trustcacerts -file ca.crt -keypass password -storepass password -keystore clienttruststore.p12 -alias localhost

# Convert client p12 truststore to pem format:
openssl pkcs12 -nodes -in clienttruststore.p12 -out clienttruststore.pem

# Generate client key:
openssl genrsa -passout pass:"password" -des3 -out client.key 4096

# Generate client signing request:
openssl req -new -passin pass:"password" -subj "/CN=localhost" -key client.key -out client.csr

# Self-sign client certificate:
openssl x509 -passin pass:"password" -req -days 3650 -in client.csr -CA ca.crt -CAkey ca.key -set_serial 01 -out client.crt

# Convert client key to pkcs8
openssl pkcs8 -passin pass:"password" -topk8 -nocrypt -in client.key -out client_private_key.key

# Create server trust store with public client cert:
keytool -import -trustcacerts -file ca.crt -keypass password -storepass password -keystore servertruststore.p12 -alias localhost

# Convert server p12 truststore to pem format:
openssl pkcs12 -nodes -in servertruststore.p12 -out servertruststore.pem

# delete immediate files
rm ca.crt
rm ca.key
rm client.csr
rm client.key
rm clienttruststore.p12
rm server.csr
rm server.key
rm servertruststore.p12
rm pas_server.csr
rm pas_server.key
