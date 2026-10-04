package org.community.bookbox.web;

import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.community.bookbox.model.HandlingRecord;
import org.community.bookbox.service.BookService;
import org.community.bookbox.web.dto.HandlingRecordView;

import java.util.List;
import java.util.Map;

@Path("/api/admin")
@Produces(MediaType.APPLICATION_JSON)
public class AdminResource {

    @Inject
    BookService bookService;

    /** 触发逾期检查：发送模拟提醒并留处理记录 */
    @POST
    @Path("/remind-overdue")
    public Map<String, Object> remindOverdue() {
        int reminded = bookService.checkOverdueAndRemind();
        return Map.of("reminded", reminded);
    }

    /** 处理记录列表（逾期、破损） */
    @GET
    @Path("/handling-records")
    @Transactional
    public List<HandlingRecordView> handlingRecords() {
        return HandlingRecord.<HandlingRecord>listAll().stream()
                .sorted((a, b) -> b.createdAt.compareTo(a.createdAt))
                .map(HandlingRecordView::of)
                .toList();
    }
}
