package com.biztrackpro.resource;

import com.biztrackpro.service.FinancialService;

import jakarta.inject.Inject;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

/**
 * City analytics and the monthly P&amp;L table.
 */
@Path("/analytics")
@Produces(MediaType.APPLICATION_JSON)
public class AnalyticsResource extends BaseResource {

    @Inject
    private FinancialService financialService;

    @GET
    @Path("/cities")
    public Response cities(@QueryParam("sort") @DefaultValue("revenue") String sort) {
        return ok(financialService.getCities(tenantId(), sort));
    }

    @GET
    @Path("/pnl")
    public Response pnl() {
        return ok(financialService.getPnl(tenantId()));
    }
}
