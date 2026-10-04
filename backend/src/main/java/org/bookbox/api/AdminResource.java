package org.bookbox.api;

import jakarta.transaction.Transactional;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import org.bookbox.api.Dtos.*;
import org.bookbox.model.*;
import org.bookbox.service.ApiException;
import org.bookbox.service.LibraryService;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.resteasy.reactive.multipart.FileUpload;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** 管理接口：捐赠审核上架、箱位、借阅记录、逾期破损处理、封面上传、提醒触发 */
@Path("/api/admin")
@Produces(MediaType.APPLICATION_JSON)
@Transactional
public class AdminResource {

    private static final Set<String> COVER_TYPES =
            Set.of("image/jpeg", "image/png", "image/webp", "image/gif");

    final LibraryService library;

    @ConfigProperty(name = "bookbox.data-dir", defaultValue = "data")
    String dataDir;

    public AdminResource(LibraryService library) {
        this.library = library;
    }

    // ---------- 箱 ----------

    @GET
    @Path("/boxes")
    public List<BoxView> boxes() {
        return BookBox.<BookBox>listAll().stream().map(BoxView::of).toList();
    }

    @POST
    @Path("/boxes")
    public BoxView createBox(BoxRequest req) {
        if (req == null || req.code() == null || req.code().isBlank()
                || req.name() == null || req.name().isBlank()) {
            throw ApiException.badRequest("箱编号与名称不能为空");
        }
        return BoxView.of(library.createBox(req.code().trim(), req.name().trim(),
                req.location(), req.capacity()));
    }

    // ---------- 捐赠 ----------

    @POST
    @Path("/donations")
    public DonationAdminView registerDonation(DonationRequest req) {
        if (req == null) {
            throw ApiException.badRequest("请求体不能为空");
        }
        return DonationAdminView.of(library.registerDonation(req.title(), req.author(),
                req.isbn(), req.condition(), req.donorName(), req.anonymous(), req.note()));
    }

    @GET
    @Path("/donations")
    public List<DonationAdminView> donations(@QueryParam("status") DonationStatus status) {
        List<Donation> list = status == null
                ? Donation.listAll()
                : Donation.list("status", status);
        return list.stream().map(DonationAdminView::of).toList();
    }

    @POST
    @Path("/donations/{id}/approve")
    public DonationAdminView approve(@PathParam("id") long id, ApproveRequest req) {
        if (req == null) {
            throw ApiException.badRequest("请求体不能为空");
        }
        return DonationAdminView.of(library.approveDonation(id, req.boxCode()));
    }

    @POST
    @Path("/donations/{id}/reject")
    public DonationAdminView reject(@PathParam("id") long id, NoteRequest req) {
        return DonationAdminView.of(library.rejectDonation(id, req == null ? null : req.note()));
    }

    // ---------- 实体书 ----------

    @GET
    @Path("/copies")
    public List<CopyView> copies() {
        return BookCopy.<BookCopy>listAll().stream().map(CopyView::of).toList();
    }

    @POST
    @Path("/copies/{copyCode}/reshelve")
    public CopyView reshelve(@PathParam("copyCode") String copyCode, ReshelveRequest req) {
        if (req == null) {
            throw ApiException.badRequest("请求体不能为空");
        }
        return CopyView.of(library.reshelve(copyCode, req.boxCode(), req.note()));
    }

    /** 上传封面：本地存放到数据目录 covers/ 下 */
    @POST
    @Path("/copies/{copyCode}/cover")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public CopyView uploadCover(@PathParam("copyCode") String copyCode,
                                @org.jboss.resteasy.reactive.RestForm("file") FileUpload file) {
        BookCopy copy = library.requireCopy(copyCode);
        if (file == null) {
            throw ApiException.badRequest("缺少文件字段 file");
        }
        String contentType = file.contentType() == null ? "" : file.contentType();
        if (!COVER_TYPES.contains(contentType)) {
            throw ApiException.badRequest("仅支持 JPG/PNG/WebP/GIF 图片");
        }
        String ext = switch (contentType) {
            case "image/png" -> ".png";
            case "image/webp" -> ".webp";
            case "image/gif" -> ".gif";
            default -> ".jpg";
        };
        String fileName = copy.copyCode + ext;
        try {
            java.nio.file.Path dir = java.nio.file.Path.of(dataDir, "covers");
            Files.createDirectories(dir);
            Files.copy(file.uploadedFile(), dir.resolve(fileName),
                    StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        copy.coverFile = fileName;
        return CopyView.of(copy);
    }

    // ---------- 借阅 ----------

    /** 当前借阅记录（默认仅在借；all=true 含历史） */
    @GET
    @Path("/loans")
    public List<LoanView> loans(@QueryParam("all") @DefaultValue("false") boolean all) {
        List<Loan> list = all
                ? Loan.listAll()
                : Loan.list("returnedAt is null");
        return list.stream().map(LoanView::of).toList();
    }

    // ---------- 处理记录 ----------

    @GET
    @Path("/incidents")
    public List<IncidentView> incidents(@QueryParam("open") @DefaultValue("false") boolean openOnly) {
        List<IncidentRecord> list = openOnly
                ? IncidentRecord.list("resolved = false")
                : IncidentRecord.listAll();
        return list.stream().map(IncidentView::of).toList();
    }

    @POST
    @Path("/incidents/{id}/resolve")
    public IncidentView resolveIncident(@PathParam("id") long id, NoteRequest req) {
        return IncidentView.of(library.resolveIncident(id, req == null ? null : req.note()));
    }

    /** 手动触发逾期扫描与模拟提醒 */
    @POST
    @Path("/reminders/run")
    public Map<String, Integer> runReminders() {
        return Map.of("created", library.scanOverdueAndNotify());
    }
}
