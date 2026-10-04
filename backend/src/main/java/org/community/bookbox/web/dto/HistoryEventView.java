package org.community.bookbox.web.dto;

import org.community.bookbox.model.LocationEvent;

import java.time.LocalDateTime;

/** 流转事件视图 */
public record HistoryEventView(
        String type,
        String fromBoxCode,
        String fromBoxName,
        String toBoxCode,
        String toBoxName,
        String actor,
        String note,
        LocalDateTime occurredAt
) {
    public static HistoryEventView of(LocationEvent event) {
        return new HistoryEventView(
                event.type.name(),
                event.fromBox != null ? event.fromBox.code : null,
                event.fromBox != null ? event.fromBox.name : null,
                event.toBox != null ? event.toBox.code : null,
                event.toBox != null ? event.toBox.name : null,
                event.actor,
                event.note,
                event.occurredAt
        );
    }
}
