package ru.practicum.ewm.event.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import ru.practicum.ewm.event.dto.enums.State;

import java.time.LocalDateTime;
import java.util.List;

@Data
@AllArgsConstructor
public class EventAdminParamDto {
    private List<Integer> users;
    private List<State> states;
    private List<Integer> categories;
    private LocalDateTime rangeStart;
    private LocalDateTime rangeEnd;
    private Integer from;
    private Integer size;
}