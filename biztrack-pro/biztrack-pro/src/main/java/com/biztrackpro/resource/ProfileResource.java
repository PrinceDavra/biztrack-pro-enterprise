package com.biztrackpro.resource;

import com.biztrackpro.dto.ProfileDTO;
import com.biztrackpro.service.ProfileService;

import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

/**
 * Business profile used by CA exports.
 */
@Path("/profile")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ProfileResource extends BaseResource {

    @Inject
    private ProfileService profileService;

    @GET
    public Response get() {
        return ok(profileService.get(tenantId()));
    }

    @PUT
    public Response update(ProfileDTO dto) {
        return ok(profileService.update(tenantId(), dto), "Profile saved");
    }
}
