package org.opennms.plugins.cloud.rest;

import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("/cloud/config")
public interface CloudConfigRestService {
    @GET()
    @Produces(value={MediaType.APPLICATION_JSON})
    Response getConfig();

    @PUT
    @Path("/activationkey")
    Response putActivationKey(final String key);

    @GET
    @Path("/status")
    @Produces(value={MediaType.APPLICATION_JSON})
    Response getStatus();
}
