package org.community.bookbox.web;

import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.community.bookbox.model.Book;
import org.community.bookbox.model.BookCondition;
import org.community.bookbox.service.ApiException;
import org.community.bookbox.service.BookService;
import org.community.bookbox.web.dto.BookAdminView;
import org.community.bookbox.web.dto.Requests;

/** 捐赠登记（管理员录入居民捐书） */
@Path("/api/donations")
@Produces(MediaType.APPLICATION_JSON)
public class DonationResource {

    @Inject
    BookService bookService;

    @POST
    @Transactional
    public Response donate(@Valid Requests.DonationRequest request) {
        BookCondition condition = parseCondition(request.condition());
        Book book = bookService.registerDonation(
                request.title(), request.isbn(), request.author(), condition,
                request.donorName(), request.donorContact(),
                Boolean.TRUE.equals(request.anonymous()), request.note());
        return Response.status(Response.Status.CREATED)
                .entity(BookAdminView.of(book, null))
                .build();
    }

    static BookCondition parseCondition(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return BookCondition.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw ApiException.badRequest("无效的书况: " + value + "（可选 NEW/GOOD/FAIR/POOR）");
        }
    }
}
