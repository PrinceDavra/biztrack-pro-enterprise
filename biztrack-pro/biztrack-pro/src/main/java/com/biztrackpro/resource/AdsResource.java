package com.biztrackpro.resource;

import java.util.Map;

import com.biztrackpro.dto.Requests.AdRequest;
import com.biztrackpro.service.AdService;

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
 * Ad campaigns: paginated list, manual add, delete.
 */
@Path("/ads")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class AdsResource extends BaseResource {

    @Inject
    private AdService adService;

    @GET
    public Response list(@QueryParam("page") @DefaultValue("1") int page,
                         @QueryParam("size") @DefaultValue("50") int size) {
        return ok(adService.list(tenantId(), page, size));
    }

    @POST
    public Response add(AdRequest req) {
        return ok(adService.add(tenantId(), req), "Campaign added");
    }

    @DELETE
    @Path("/{id}")
    public Response delete(@PathParam("id") Long id) {
        boolean deleted = adService.delete(tenantId(), id);
        return deleted ? ok(Map.of("deleted", true), "Campaign deleted") : notFound("Campaign not found");
    }
}
