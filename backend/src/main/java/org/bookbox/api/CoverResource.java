package org.bookbox.api;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.nio.file.Files;

/** 封面图：从本地数据目录读取 */
@Path("/api/covers")
public class CoverResource {

    @ConfigProperty(name = "bookbox.data-dir", defaultValue = "data")
    String dataDir;

    @GET
    @Path("/{fileName}")
    public Response cover(@PathParam("fileName") String fileName) throws Exception {
        if (fileName.contains("..") || fileName.contains("/")) {
            throw new NotFoundException();
        }
        java.nio.file.Path path = java.nio.file.Path.of(dataDir, "covers", fileName);
        if (!Files.isRegularFile(path)) {
            throw new NotFoundException();
        }
        String type = fileName.endsWith(".png") ? "image/png"
                : fileName.endsWith(".webp") ? "image/webp"
                : fileName.endsWith(".gif") ? "image/gif"
                : "image/jpeg";
        return Response.ok(Files.readAllBytes(path), type).build();
    }
}
