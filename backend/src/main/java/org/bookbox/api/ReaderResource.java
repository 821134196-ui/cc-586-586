package org.bookbox.api;

import jakarta.transaction.Transactional;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import org.bookbox.api.Dtos.*;
import org.bookbox.model.*;
import org.bookbox.service.ApiException;
import org.bookbox.service.LibraryService;

import java.util.List;

/**
 * 读者接口：查书、借书、还书、看流转。
 * 所有响应均不含捐赠者信息。
 */
@Path("/api")
@Produces(MediaType.APPLICATION_JSON)
@Transactional
public class ReaderResource {

    final LibraryService library;

    public ReaderResource(LibraryService library) {
        this.library = library;
    }

    /** 箱列表（含占用数），还书选箱、找书定位用 */
    @GET
    @Path("/boxes")
    public List<BoxView> boxes() {
        return BookBox.<BookBox>listAll().stream().map(BoxView::of).toList();
    }

    /** 按编号 / 书名 / ISBN / 作者检索；只展示在架与已借出的书 */
    @GET
    @Path("/books/search")
    public List<CopyView> search(@QueryParam("q") String q) {
        if (q == null || q.isBlank()) {
            return BookCopy.<BookCopy>list("status in ?1",
                            List.of(CopyStatus.ON_SHELF, CopyStatus.BORROWED))
                    .stream().map(CopyView::of).toList();
        }
        String like = "%" + q.trim() + "%";
        return BookCopy.<BookCopy>list(
                        "status in ?1 and (copyCode like ?2 or title like ?2 or isbn like ?2 or author like ?2)",
                        List.of(CopyStatus.ON_SHELF, CopyStatus.BORROWED), like)
                .stream().map(CopyView::of).toList();
    }

    /** 按实体编号查书：当前在哪个箱，或是否已借出 */
    @GET
    @Path("/copies/{copyCode}")
    public CopyView copyDetail(@PathParam("copyCode") String copyCode) {
        return CopyView.of(library.requireCopy(copyCode));
    }

    /** 完整流转轨迹（跨箱归还后仍可追查） */
    @GET
    @Path("/copies/{copyCode}/history")
    public List<MovementView> history(@PathParam("copyCode") String copyCode) {
        BookCopy copy = library.requireCopy(copyCode);
        return MovementEvent.historyOf(copy).stream().map(MovementView::of).toList();
    }

    /** 扫码借书 */
    @POST
    @Path("/loans")
    public LoanView borrow(BorrowRequest req) {
        if (req == null) {
            throw ApiException.badRequest("请求体不能为空");
        }
        return LoanView.of(library.borrow(req.copyCode(), req.borrowerName(), req.borrowerContact()));
    }

    /** 扫码还书：可还到任意有空位的箱 */
    @POST
    @Path("/loans/return")
    public LoanView returnBook(ReturnRequest req) {
        if (req == null) {
            throw ApiException.badRequest("请求体不能为空");
        }
        return LoanView.of(library.returnBook(req.copyCode(), req.boxCode(), req.damageNote()));
    }
}
