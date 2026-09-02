package com.biztrackpro.resource;

import com.biztrackpro.dto.ApiResponse;
import com.biztrackpro.security.AuthPrincipal;

import jakarta.inject.Inject;
import jakarta.ws.rs.core.Response;

/**
 * Shared plumbing for all authenticated resources: the current tenant (set by
 * JwtAuthFilter) and helpers that wrap payloads in the standard JSON envelope.
 */
public abstract class BaseResource {

    @Inject
    protected AuthPrincipal principal;

    protected Long tenantId() {
        Long t = principal != null ? principal.getTenantId() : null;
        if (t == null) {
            throw new SecurityException("Authentication required");
        }
        return t;
    }

    protected Response ok(Object data) {
        return Response.ok(ApiResponse.success(data)).build();
    }

    protected Response ok(Object data, String message) {
        return Response.ok(ApiResponse.success(data, message)).build();
    }

    protected Response notFound(String message) {
        return Response.status(Response.Status.NOT_FOUND)
                .entity(ApiResponse.error(message))
                .build();
    }
}
