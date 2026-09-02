package com.biztrackpro.resource;

import java.nio.charset.StandardCharsets;

import com.biztrackpro.service.ExportService;
import com.biztrackpro.service.ExportService.ExportFile;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Response;

/**
 * CA export module. Every endpoint streams a UTF-8 CSV (BOM + metadata row) as a
 * file download.
 */
@Path("/export")
public class ExportResource extends BaseResource {

    private static final String CSV = "text/csv; charset=UTF-8";

    @Inject
    private ExportService exportService;

    @GET
    @Path("/pnl")
    @Produces(CSV)
    public Response pnl() {
        return csv(exportService.pnl(tenantId()));
    }

    @GET
    @Path("/sales")
    @Produces(CSV)
    public Response sales() {
        return csv(exportService.sales(tenantId()));
    }

    @GET
    @Path("/expenses")
    @Produces(CSV)
    public Response expenses() {
        return csv(exportService.expenses(tenantId()));
    }

    @GET
    @Path("/ads")
    @Produces(CSV)
    public Response ads() {
        return csv(exportService.ads(tenantId()));
    }

    @GET
    @Path("/full")
    @Produces(CSV)
    public Response full() {
        return csv(exportService.full(tenantId()));
    }

    @GET
    @Path("/summary")
    @Produces(CSV)
    public Response summary() {
        return csv(exportService.summary(tenantId()));
    }

    @GET
    @Path("/backup")
    @Produces(CSV)
    public Response backup() {
        return csv(exportService.backup(tenantId()));
    }

    private Response csv(ExportFile f) {
        return Response.ok(f.content.getBytes(StandardCharsets.UTF_8))
                .type(CSV)
                .header("Content-Disposition", "attachment; filename=\"" + f.filename + "\"")
                .build();
    }
}
