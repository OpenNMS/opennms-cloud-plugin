package org.opennms.plugins.cloud.tsaas.config;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CloudGatewayConfig {
    private String publicKey;
    private String privateKey;
    private String token;
    private String securedGrpcEndpoint;
}
