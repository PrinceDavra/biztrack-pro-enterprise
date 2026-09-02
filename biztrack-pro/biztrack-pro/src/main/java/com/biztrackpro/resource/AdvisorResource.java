package com.biztrackpro.resource;

import com.biztrackpro.service.AnalysisService;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

/**
 * Rule-based AI Advisor endpoint.
 */
@Path("/advisor")
@Produces(MediaType.APPLICATION_JSON)
public class AdvisorResource extends BaseResource {

    @Inject
    private AnalysisService analysisService;

    @GET
    @Path("/analyse")
    public Response analyse() {
        return ok(analysisService.analyse(tenantId()));
    }
}
