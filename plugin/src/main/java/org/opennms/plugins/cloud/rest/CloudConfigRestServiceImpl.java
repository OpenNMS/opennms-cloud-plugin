package org.opennms.plugins.cloud.rest;

import javax.ws.rs.core.Response;

import org.opennms.plugins.cloud.tsaas.TsaasConfig;

public class CloudConfigRestServiceImpl implements CloudConfigRestService {

    private final TsaasConfig config;
    private boolean configured = false;

    public CloudConfigRestServiceImpl(final TsaasConfig config) {
        this.config = config;
    }

    @Override
    public Response getConfig() {
        return Response
                .status(Response.Status.OK)
                .entity(config).build();
    }

    @Override
    public Response putActivationKey(final String key) {
        configured = true;
        // TODO: Patrick implement me properly
        return getStatus();
    }

    @Override
    public Response getStatus() {
        // TODO: Patrick implement me properly
        String status = configured ? "configured" : "notConfigured";
        return Response
                .status(Response.Status.OK)
                .entity("{\"status\":\"" + status + "\"}")
                .build();
    }
}
