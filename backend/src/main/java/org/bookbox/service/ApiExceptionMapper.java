package org.bookbox.service;

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

@Provider
public class ApiExceptionMapper implements ExceptionMapper<ApiException> {

    public record ErrorBody(String error) {
    }

    @Override
    public Response toResponse(ApiException exception) {
        return Response.status(exception.status)
                .type(MediaType.APPLICATION_JSON)
                .entity(new ErrorBody(exception.getMessage()))
                .build();
    }
}
