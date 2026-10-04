package org.community.bookbox.web;

import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.community.bookbox.model.Box;
import org.community.bookbox.service.ApiException;
import org.community.bookbox.web.dto.BoxView;
import org.community.bookbox.web.dto.Requests;

import java.util.List;

@Path("/api/boxes")
@Produces(MediaType.APPLICATION_JSON)
public class BoxResource {

    /** 箱位列表（含占用情况），读者还书选箱、管理员查看都用它 */
    @GET
    public List<BoxView> list() {
        return Box.<Box>listAll().stream().map(BoxView::of).toList();
    }

    /** 管理员新增箱位 */
    @POST
    @Transactional
    public BoxView create(@Valid Requests.BoxRequest request) {
        String code = request.code().trim().toUpperCase();
        if (Box.findByCode(code) != null) {
            throw ApiException.conflict("箱位编号 " + code + " 已存在");
        }
        Box box = new Box();
        box.code = code;
        box.name = request.name().trim();
        box.location = request.location();
        box.capacity = request.capacity() != null && request.capacity() > 0 ? request.capacity() : 10;
        box.persist();
        return BoxView.of(box);
    }
}
