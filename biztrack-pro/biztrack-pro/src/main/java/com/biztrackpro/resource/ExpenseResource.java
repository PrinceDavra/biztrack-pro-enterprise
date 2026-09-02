package com.biztrackpro.resource;

import java.util.Map;

import com.biztrackpro.dto.Requests.ExpenseRequest;
import com.biztrackpro.service.ExpenseService;

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
 * Operating expenses: paginated list, manual add, delete.
 */
@Path("/expenses")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ExpenseResource extends BaseResource {

    @Inject
    private ExpenseService expenseService;

    @GET
    public Response list(@QueryParam("page") @DefaultValue("1") int page,
                         @QueryParam("size") @DefaultValue("50") int size) {
        return ok(expenseService.list(tenantId(), page, size));
    }

    @POST
    public Response add(ExpenseRequest req) {
        return ok(expenseService.add(tenantId(), req), "Expense added");
    }

    @DELETE
    @Path("/{id}")
    public Response delete(@PathParam("id") Long id) {
        boolean deleted = expenseService.delete(tenantId(), id);
        return deleted ? ok(Map.of("deleted", true), "Expense deleted") : notFound("Expense not found");
    }
}
