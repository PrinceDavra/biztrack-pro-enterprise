package com.biztrackpro.resource;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import org.glassfish.jersey.media.multipart.FormDataBodyPart;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataParam;

import com.biztrackpro.dto.ImportResultDTO;
import com.biztrackpro.service.ImportService;

import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

/**
 * CSV upload endpoints. Each validates MIME/extension and a 10MB size cap before
 * handing a fresh stream to ImportService.
 */
@Path("/import")
@Produces(MediaType.APPLICATION_JSON)
public class ImportResource extends BaseResource {

    private static final long MAX_BYTES = 10L * 1024 * 1024;

    @Inject
    private ImportService importService;

    @POST
    @Path("/shopify")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public Response shopify(@FormDataParam("file") InputStream in,
                            @FormDataParam("file") FormDataContentDisposition meta,
                            @FormDataParam("file") FormDataBodyPart body) {
        byte[] bytes = validateAndRead(in, meta, body);
        ImportResultDTO r = importService.importShopify(tenantId(), new ByteArrayInputStream(bytes));
        return ok(r, r.message);
    }

    @POST
    @Path("/meta")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public Response meta(@FormDataParam("file") InputStream in,
                         @FormDataParam("file") FormDataContentDisposition meta,
                         @FormDataParam("file") FormDataBodyPart body) {
        byte[] bytes = validateAndRead(in, meta, body);
        ImportResultDTO r = importService.importMeta(tenantId(), new ByteArrayInputStream(bytes));
        return ok(r, r.message);
    }

    @POST
    @Path("/shipping")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public Response shipping(@FormDataParam("file") InputStream in,
                             @FormDataParam("file") FormDataContentDisposition meta,
                             @FormDataParam("file") FormDataBodyPart body) {
        byte[] bytes = validateAndRead(in, meta, body);
        ImportResultDTO r = importService.importShipping(tenantId(), new ByteArrayInputStream(bytes));
        return ok(r, r.message);
    }

    @POST
    @Path("/costs")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public Response costs(@FormDataParam("file") InputStream in,
                          @FormDataParam("file") FormDataContentDisposition meta,
                          @FormDataParam("file") FormDataBodyPart body) {
        byte[] bytes = validateAndRead(in, meta, body);
        ImportResultDTO r = importService.importCosts(tenantId(), new ByteArrayInputStream(bytes));
        return ok(r, r.message);
    }

    // ----- validation -----

    private byte[] validateAndRead(InputStream in, FormDataContentDisposition meta, FormDataBodyPart body) {
        if (in == null) {
            throw new IllegalArgumentException("No file was uploaded");
        }
        validateType(meta, body);
        byte[] bytes = readLimited(in);
        if (bytes.length == 0) {
            throw new IllegalArgumentException("The uploaded file is empty");
        }
        return bytes;
    }

    private void validateType(FormDataContentDisposition meta, FormDataBodyPart body) {
        String fileName = meta != null ? meta.getFileName() : null;
        String mediaType = body != null && body.getMediaType() != null
                ? body.getMediaType().toString().toLowerCase() : "";
        boolean okExt = fileName != null && fileName.toLowerCase().endsWith(".csv");
        boolean okMime = mediaType.contains("csv") || mediaType.contains("excel")
                || mediaType.contains("octet-stream") || mediaType.contains("text/plain");
        if (!okExt && !okMime) {
            throw new IllegalArgumentException("Please upload a .csv file");
        }
    }

    private byte[] readLimited(InputStream in) {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buf = new byte[8192];
            int read;
            long total = 0;
            while ((read = in.read(buf)) != -1) {
                total += read;
                if (total > MAX_BYTES) {
                    throw new IllegalArgumentException("File exceeds the 10MB limit");
                }
                out.write(buf, 0, read);
            }
            return out.toByteArray();
        } catch (IOException e) {
            throw new IllegalArgumentException("Could not read the uploaded file");
        }
    }
}
