package org.opennms.plugins.cloud.grpc;

import java.util.Objects;

import lombok.Builder;
import lombok.Data;

@Builder(toBuilder = true)
@Data
public class GrpcConnectionConfig {
    private final String host;
    private final int port;
    private final String tokenKey;
    private final String tokenValue;
    private final boolean mtlsEnabled;

    private final String publicKey;
    private final String privateKey;
    private final String clientTrustStore;

    public GrpcConnectionConfig(
            final String host,
            final int port,
            final String tokenKey,
            final String tokenValue,
            final boolean mtlsEnabled,
            final String publicKey,
            final String privateKey,
            final String clientTrustStore) {
        this.host = Objects.requireNonNull(host);
        this.port = requirePositiveNumber(port);
        this.tokenKey = Objects.requireNonNull(tokenKey);
        this.tokenValue = Objects.requireNonNull(tokenValue);
        this.mtlsEnabled = mtlsEnabled;
        this.publicKey = publicKey;
        this.privateKey = privateKey;
        this.clientTrustStore = clientTrustStore;
    }

    private int requirePositiveNumber(int value) {
        if (value < 0) {
            throw new IllegalArgumentException(String.format("A positive number is required but was %s", value));
        }
        return value;
    }

    public static class GrpcConnectionConfigBuilder {
        private String host = "localhost"; // default value
        private int port = 5001; // default value
        private String tokenKey = "token"; // default value
        private String tokenValue = "acme"; // default value
        private boolean mtlsEnabled = false; // default value
    }
}
