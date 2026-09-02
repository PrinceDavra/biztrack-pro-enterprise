package com.biztrackpro.filter;

import java.io.IOException;

import com.auth0.jwt.interfaces.DecodedJWT;
import com.biztrackpro.dto.ApiResponse;
import com.biztrackpro.security.AuthPrincipal;
import com.biztrackpro.security.JwtUtil;

import jakarta.annotation.Priority;
import jakarta.inject.Inject;
import jakarta.ws.rs.HttpMethod;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;

/**
 * Enforces a valid JWT bearer token on every endpoint except /api/auth/* and
 * CORS pre-flight (OPTIONS). On success it populates {@link AuthPrincipal}.
 */
@Provider
@Priority(Priorities.AUTHENTICATION)
public class JwtAuthFilter implements ContainerRequestFilter {

    private static final String BEARER = "Bearer ";

    @Inject
    private JwtUtil jwtUtil;

    @Inject
    private AuthPrincipal authPrincipal;

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        // Allow CORS pre-flight through untouched.
        if (HttpMethod.OPTIONS.equalsIgnoreCase(requestContext.getMethod())) {
            return;
        }

        String path = requestContext.getUriInfo().getPath();
        if (path != null && (path.startsWith("auth") || path.startsWith("/auth"))) {
            return; // public auth endpoints
        }

        String header = requestContext.getHeaderString(HttpHeaders.AUTHORIZATION);
        if (header == null || !header.startsWith(BEARER)) {
            abort(requestContext, "Missing or malformed Authorization header");
            return;
        }

        String token = header.substring(BEARER.length()).trim();
        try {
            DecodedJWT jwt = jwtUtil.verify(token);
            authPrincipal.setTenantId(jwtUtil.extractTenantId(jwt));
            authPrincipal.setEmail(jwtUtil.extractEmail(jwt));
        } catch (Exception e) {
            abort(requestContext, "Invalid or expired token");
        }
    }

    private void abort(ContainerRequestContext ctx, String message) {
        ctx.abortWith(Response.status(Response.Status.UNAUTHORIZED)
                .entity(ApiResponse.error(message))
                .type(MediaType.APPLICATION_JSON)
                .build());
    }
}
