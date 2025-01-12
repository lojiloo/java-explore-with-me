package ru.practicum.ewm.compilation.dto;

import lombok.Data;
import ru.practicum.ewm.event.dto.EventShortDto;

import java.util.List;

@Data
public class CompilationDto {
    private Integer id;
    private Boolean pinned;
    private String title;
    private List<EventShortDto> events;
}