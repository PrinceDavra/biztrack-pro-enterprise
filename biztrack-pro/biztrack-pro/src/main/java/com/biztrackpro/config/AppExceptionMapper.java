package com.biztrackpro.config;

import com.biztrackpro.dto.ApiResponse;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

/**
 * Maps uncaught exceptions to the standard JSON envelope
 * { "status":"error", "data":null, "message":"..." }.
 * Monetary data and personal identifiers are never included in messages.
 */
@Provider
public class AppExceptionMapper implements ExceptionMapper<Throwable> {

    @Override
    public Response toResponse(Throwable ex) {
        int status = 500;
        String message = "Internal server error";

        if (ex instanceof WebApplicationException wae) {
            status = wae.getResponse().getStatus();
            message = ex.getMessage() != null ? ex.getMessage() : wae.getResponse().getStatusInfo().getReasonPhrase();
        } else if (ex instanceof IllegalArgumentException) {
            status = 400;
            message = ex.getMessage();
        } else if (ex instanceof SecurityException) {
            status = 401;
            message = ex.getMessage();
        } else if (ex.getMessage() != null) {
            message = ex.getMessage();
        }

        return Response.status(status)
                .entity(ApiResponse.error(message))
                .type(MediaType.APPLICATION_JSON)
                .build();
    }
}
