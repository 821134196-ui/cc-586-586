package org.community.bookbox.web.dto;

import org.community.bookbox.model.Box;

/** 箱位视图，含占用情况 */
public record BoxView(
        String code,
        String name,
        String location,
        int capacity,
        long load,
        long free
) {
    public static BoxView of(Box box) {
        long load = box.currentLoad();
        return new BoxView(box.code, box.name, box.location, box.capacity, load, box.capacity - load);
    }
}
