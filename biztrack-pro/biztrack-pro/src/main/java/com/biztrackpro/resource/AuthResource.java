package com.biztrackpro.resource;

import java.util.Map;

import com.biztrackpro.dto.ApiResponse;
import com.biztrackpro.dto.Requests.LoginRequest;
import com.biztrackpro.dto.Requests.RegisterRequest;
import com.biztrackpro.service.AuthService;

import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

/**
 * Public authentication endpoints (excluded from JWT enforcement by JwtAuthFilter).
 */
@Path("/auth")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class AuthResource {

    @Inject
    private AuthService authService;

    @POST
    @Path("/register")
    public Response register(RegisterRequest req) {
        Map<String, Object> data = authService.register(req);
        return Response.ok(ApiResponse.success(data, "Account created")).build();
    }

    @POST
    @Path("/login")
    public Response login(LoginRequest req) {
        Map<String, Object> data = authService.login(req);
        return Response.ok(ApiResponse.success(data, "Signed in")).build();
    }
}
