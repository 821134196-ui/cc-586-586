package org.community.bookbox.web;

import io.quarkus.hibernate.orm.panache.PanacheQuery;
import io.quarkus.panache.common.Parameters;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import org.community.bookbox.model.Book;
import org.community.bookbox.model.BookCondition;
import org.community.bookbox.model.BookStatus;
import org.community.bookbox.model.Loan;
import org.community.bookbox.model.LocationEvent;
import org.community.bookbox.service.ApiException;
import org.community.bookbox.service.BookService;
import org.community.bookbox.web.dto.BookAdminView;
import org.community.bookbox.web.dto.BookView;
import org.community.bookbox.web.dto.HistoryEventView;
import org.community.bookbox.web.dto.Requests;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.resteasy.reactive.RestForm;
import org.jboss.resteasy.reactive.multipart.FileUpload;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Locale;

@Path("/api/books")
@Produces(MediaType.APPLICATION_JSON)
public class BookResource {

    @Inject
    BookService bookService;

    @ConfigProperty(name = "bookbox.covers-dir", defaultValue = "data/covers")
    String coversDir;

    /**
     * 按编号/书名/ISBN/作者检索。编号精确命中时直接定位到那一本实体书，
     * 不会把同书名的其他副本混在一起。
     */
    @GET
    @Transactional
    public List<BookView> search(@QueryParam("q") String q, @QueryParam("status") String status) {
        PanacheQuery<Book> query;
        if (q != null && !q.isBlank()) {
            String like = "%" + q.trim().toLowerCase(Locale.ROOT) + "%";
            query = Book.find(
                    "lower(code) like :kw or lower(title) like :kw or lower(isbn) like :kw or lower(author) like :kw",
                    Parameters.with("kw", like));
        } else {
            query = Book.findAll();
        }
        List<Book> books = query.list();
        return books.stream()
                .filter(b -> status == null || status.isBlank() || b.status.name().equalsIgnoreCase(status))
                .map(b -> BookView.of(b, Loan.findActiveByBook(b)))
                .toList();
    }

    /** 按实体编号查询一本书当前在哪（在哪个箱 / 是否已借出） */
    @GET
    @Path("/{code}")
    @Transactional
    public BookView getByCode(@PathParam("code") String code) {
        Book book = bookService.mustFindBook(code);
        return BookView.of(book, Loan.findActiveByBook(book));
    }

    /** 管理员视图：含捐赠人身份与借阅人信息 */
    @GET
    @Path("/{code}/admin")
    @Transactional
    public BookAdminView getAdminView(@PathParam("code") String code) {
        Book book = bookService.mustFindBook(code);
        return BookAdminView.of(book, Loan.findActiveByBook(book));
    }

    /** 完整流转历史：跨箱归还后仍可追查 */
    @GET
    @Path("/{code}/history")
    @Transactional
    public List<HistoryEventView> history(@PathParam("code") String code) {
        Book book = bookService.mustFindBook(code);
        return LocationEvent.historyOf(book).stream().map(HistoryEventView::of).toList();
    }

    /** 管理员检查并上架 */
    @POST
    @Path("/{code}/shelve")
    @Transactional
    public BookAdminView shelve(@PathParam("code") String code, @Valid Requests.ShelveRequest request) {
        BookCondition condition = DonationResource.parseCondition(request.condition());
        Book book = bookService.shelve(code, request.boxCode(), condition, request.note());
        return BookAdminView.of(book, null);
    }

    /** 上传封面图，本地存储 */
    @POST
    @Path("/{code}/cover")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Transactional
    public BookAdminView uploadCover(@PathParam("code") String code, @RestForm("file") FileUpload file) {
        Book book = bookService.mustFindBook(code);
        if (file == null || file.uploadedFile() == null) {
            throw ApiException.badRequest("缺少上传文件");
        }
        String original = file.fileName() != null ? file.fileName() : "";
        String ext = original.contains(".")
                ? original.substring(original.lastIndexOf('.')).toLowerCase(Locale.ROOT) : ".jpg";
        if (!List.of(".jpg", ".jpeg", ".png", ".webp", ".gif").contains(ext)) {
            throw ApiException.badRequest("仅支持 jpg/png/webp/gif 图片");
        }
        String fileName = book.code + ext;
        try {
            java.nio.file.Path dir = java.nio.file.Path.of(coversDir);
            Files.createDirectories(dir);
            Files.copy(file.uploadedFile(), dir.resolve(fileName), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw ApiException.badRequest("封面保存失败: " + e.getMessage());
        }
        book.coverImage = fileName;
        return BookAdminView.of(book, Loan.findActiveByBook(book));
    }

    /** 待上架（待检查）列表，管理后台用 */
    @GET
    @Path("/pending")
    @Transactional
    public List<BookAdminView> pending() {
        return Book.<Book>list("status", BookStatus.PENDING_INSPECTION).stream()
                .map(b -> BookAdminView.of(b, null))
                .toList();
    }
}
