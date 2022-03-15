# use "password" as passphrase
# use localhost as common name (CN)
# challenge password: leave empty
# rest doesnt matter

# Generate CA key
openssl genrsa -des3 -out ca.key 4096

# Generate CA certificate:
openssl req -new -x509 -days 3650 -key ca.key -out ca.crt

# Generate server key:
openssl genrsa -des3 -out server.key 4096

# Generate server signing request:
openssl req -new -key server.key -out server.csr

# Self-sign server certificate:
openssl x509 -req -days 3650 -in server.csr -CA ca.crt -CAkey ca.key -set_serial 01 -out server.crt

# Convert to pkcs8
openssl pkcs8 -topk8 -nocrypt -in server.key -out server_pkcs8_key.pem

# Create client trust store with public server cert:
keytool -import -trustcacerts -file ca.crt -keypass password -storepass password -keystore clienttruststore.p12 -alias localhost

# Convert client p12 truststore to pem format:
openssl pkcs12 -nodes -in clienttruststore.p12 -out clienttruststore.pem

# Generate client key:
openssl genrsa -des3 -out client.key 4096

# Generate client signing request:
openssl req -new -key client.key -out client.csr

# Self-sign client certificate:
openssl x509 -req -days 3650 -in client.csr -CA ca.crt -CAkey ca.key -set_serial 01 -out client.crt

# Create server trust store with public client cert:
keytool -import -trustcacerts -file ca.crt -keypass password -storepass password -keystore servertruststore.p12 -alias localhost

# Convert server p12 truststore to pem format:
openssl pkcs12 -nodes -in servertruststore.p12 -out servertruststore.pem