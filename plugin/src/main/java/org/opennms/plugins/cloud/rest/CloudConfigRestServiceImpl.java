package org.opennms.plugins.cloud.rest;

import javax.ws.rs.core.Response;

import org.opennms.plugins.cloud.tsaas.TsaasConfig;

public class CloudConfigRestServiceImpl implements CloudConfigRestService {

    private final TsaasConfig config;

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
        // TODO: Patrick implement me properly
        return Response
                .status(Response.Status.OK)
                .entity("{\"status\":\"looking good!\"}")
                .build();
    }
}
