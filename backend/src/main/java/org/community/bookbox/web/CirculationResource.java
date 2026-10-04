package org.community.bookbox.web;

import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.community.bookbox.model.Loan;
import org.community.bookbox.service.BookService;
import org.community.bookbox.web.dto.LoanView;
import org.community.bookbox.web.dto.Requests;

/** 读者扫码借还 */
@Path("/api")
@Produces(MediaType.APPLICATION_JSON)
public class CirculationResource {

    @Inject
    BookService bookService;

    /** 扫码借书 */
    @POST
    @Path("/borrow")
    @Transactional
    public LoanView borrow(@Valid Requests.BorrowRequest request) {
        Loan loan = bookService.borrow(request.bookCode(), request.borrowerName(), request.borrowerContact());
        return LoanView.of(loan);
    }

    /** 扫码还书：可归还到任意有空位的箱；破损上报会留处理记录 */
    @POST
    @Path("/returns")
    @Transactional
    public LoanView returnBook(@Valid Requests.ReturnRequest request) {
        Loan loan = bookService.returnBook(
                request.bookCode(), request.boxCode(),
                Boolean.TRUE.equals(request.damaged()), request.note());
        return LoanView.of(loan);
    }
}
