package com.biztrackpro.resource;

import java.util.Map;

import com.biztrackpro.dto.Requests.CostRequest;
import com.biztrackpro.service.CostService;

import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

/**
 * Product costs catalogue plus the "apply all costs" action that re-matches COGS
 * onto every order.
 */
@Path("/costs")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class CostsResource extends BaseResource {

    @Inject
    private CostService costService;

    @GET
    public Response list() {
        return ok(costService.list(tenantId()));
    }

    @POST
    public Response add(CostRequest req) {
        return ok(costService.add(tenantId(), req), "Cost saved");
    }

    @DELETE
    @Path("/{id}")
    public Response delete(@PathParam("id") Long id) {
        boolean deleted = costService.delete(tenantId(), id);
        return deleted ? ok(Map.of("deleted", true), "Cost deleted") : notFound("Product cost not found");
    }

    @POST
    @Path("/apply")
    public Response apply() {
        int applied = costService.applyAllCosts(tenantId());
        return ok(Map.of("applied", applied), "Applied costs to " + applied + " order(s)");
    }
}
