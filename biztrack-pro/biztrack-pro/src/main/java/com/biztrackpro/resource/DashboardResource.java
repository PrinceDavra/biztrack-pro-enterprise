package com.biztrackpro.resource;

import com.biztrackpro.service.FinancialService;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

/**
 * Dashboard data: headline KPIs, the monthly revenue/profit series (sales months
 * only), and the expense breakdown (categories + COGS + Ad Spend).
 */
@Path("/dashboard")
@Produces(MediaType.APPLICATION_JSON)
public class DashboardResource extends BaseResource {

    @Inject
    private FinancialService financialService;

    @GET
    @Path("/kpis")
    public Response kpis() {
        return ok(financialService.getKpis(tenantId()));
    }

    @GET
    @Path("/monthly")
    public Response monthly() {
        return ok(financialService.getMonthly(tenantId()));
    }

    @GET
    @Path("/expenses/breakdown")
    public Response expenseBreakdown() {
        return ok(financialService.getExpenseBreakdown(tenantId()));
    }
}
