package org.opennms.plugins.cloud.grpc;

import static org.opennms.plugins.cloud.config.SecureCredentialsVaultUtil.TOKEN_KEY;

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
            final Security security) {
        this(
                host,
                port,
                null,
                null,
                security,
                null,
                null,
                null);
    }
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

    public static class GrpcConnectionConfigBuilder {
        private String host = "localhost"; // default value
        private int port = 5001; // default value
        private String tokenKey = "token"; // default value
        private String tokenValue = TOKEN_KEY; // default value
        private Security security = Security.PLAIN_TEXT; // default value
    }
}
