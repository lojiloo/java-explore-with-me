package ru.practicum.ewm.event.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@AllArgsConstructor
public class EventPublicParamDto {
    private String text;
    private List<Integer> categories;
    private Boolean paid;
    private LocalDateTime rangeStart;
    private LocalDateTime rangeEnd;
    private Boolean onlyAvailable;
    private Sort sort;
    private Integer from;
    private Integer size;

    public enum Sort {
        EVENT_DATE, VIEWS
    }
}