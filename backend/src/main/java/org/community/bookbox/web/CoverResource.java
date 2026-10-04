package org.community.bookbox.web;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.core.Response;
import org.community.bookbox.service.ApiException;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.io.IOException;
import java.nio.file.Files;

/** 本地存放的封面图 */
@Path("/api/covers")
public class CoverResource {

    @ConfigProperty(name = "bookbox.covers-dir", defaultValue = "data/covers")
    String coversDir;

    @GET
    @Path("/{fileName}")
    public Response get(@PathParam("fileName") String fileName) throws IOException {
        java.nio.file.Path base = java.nio.file.Path.of(coversDir).normalize();
        java.nio.file.Path file = base.resolve(fileName).normalize();
        if (!file.startsWith(base) || !Files.isRegularFile(file)) {
            throw ApiException.notFound("封面不存在");
        }
        String contentType = Files.probeContentType(file);
        return Response.ok(file.toFile())
                .type(contentType != null ? contentType : "application/octet-stream")
                .build();
    }
}
