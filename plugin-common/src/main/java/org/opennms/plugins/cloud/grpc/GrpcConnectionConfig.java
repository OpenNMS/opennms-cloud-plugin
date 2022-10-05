package org.opennms.plugins.cloud.grpc;

import static org.opennms.plugins.cloud.config.ConfigStore.TOKEN_KEY;

import java.util.Objects;

import lombok.Builder;
import lombok.Data;

@Builder(toBuilder = true)
@Data
public class GrpcConnectionConfig {

    public enum Security {
        PLAIN_TEXT,
        TLS,
        MTLS
    }
    private final String host;
    private final int port;
    private final String tokenKey;
    private final String tokenValue;
    private final Security security;

    private final String publicKey;
    private final String privateKey;
    private final String clientTrustStore;

    /** Called by blueprint.xml. */
    public GrpcConnectionConfig(
            final String host,
            final int port,
            final Security security,
            final String clientTrustStore) {
        this(
                host,
                port,
                null,
                null,
                security,
                null,
                null,
                clientTrustStore);
    }

    @SuppressWarnings("java:S107") // this constructor is only used by builder => ok to have many parameters
    private GrpcConnectionConfig(
            final String host,
            final int port,
            final String tokenKey,
            final String tokenValue,
            final Security security,
            final String publicKey,
            final String privateKey,
            final String clientTrustStore) {
        this.host = Objects.requireNonNull(host);
        this.port = requirePositiveNumber(port);
        this.tokenKey = tokenKey;
        this.tokenValue = tokenValue;
        this.security = security;
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

    @SuppressWarnings("java:S1068") // fields are not unused but part of lombok builder
    public static class GrpcConnectionConfigBuilder {
        private String host = "localhost"; // default value
        private String tokenKey = TOKEN_KEY; // default value
        private Security security = Security.TLS; // default value
    }
}
