package com.biztrackpro.resource;

import java.util.Map;

import com.biztrackpro.dto.Requests.SaleRequest;
import com.biztrackpro.service.SalesService;

import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

/**
 * Orders: paginated list, manual add, delete.
 */
@Path("/sales")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class SalesResource extends BaseResource {

    @Inject
    private SalesService salesService;

    @GET
    public Response list(@QueryParam("page") @DefaultValue("1") int page,
                         @QueryParam("size") @DefaultValue("50") int size) {
        return ok(salesService.list(tenantId(), page, size));
    }

    @POST
    public Response add(SaleRequest req) {
        return ok(salesService.add(tenantId(), req), "Sale added");
    }

    @DELETE
    @Path("/{id}")
    public Response delete(@PathParam("id") Long id) {
        boolean deleted = salesService.delete(tenantId(), id);
        return deleted ? ok(Map.of("deleted", true), "Sale deleted") : notFound("Order not found");
    }
}
