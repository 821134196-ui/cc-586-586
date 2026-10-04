package org.community.bookbox.web;

import jakarta.transaction.Transactional;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import org.community.bookbox.model.Loan;
import org.community.bookbox.model.LoanStatus;
import org.community.bookbox.web.dto.LoanView;

import java.util.List;

@Path("/api/loans")
@Produces(MediaType.APPLICATION_JSON)
public class LoanResource {

    /** 管理员查看借阅记录；active=true 只看当前未还的 */
    @GET
    @Transactional
    public List<LoanView> list(@QueryParam("active") Boolean active) {
        List<Loan> loans = Boolean.TRUE.equals(active)
                ? Loan.list("status", LoanStatus.ACTIVE)
                : Loan.listAll();
        return loans.stream()
                .sorted((a, b) -> b.borrowedAt.compareTo(a.borrowedAt))
                .map(LoanView::of)
                .toList();
    }
}
