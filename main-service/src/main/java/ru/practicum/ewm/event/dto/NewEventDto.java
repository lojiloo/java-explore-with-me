package ru.practicum.ewm.event.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.*;
import lombok.Data;
import ru.practicum.ewm.event.model.Location;

import java.time.LocalDateTime;

@Data
public class NewEventDto {
    @NotNull
    @NotBlank
    @Size(min = 20, max = 2000)
    private String annotation;
    @NotNull
    private Integer category;
    @NotNull
    @NotBlank
    @Size(min = 20, max = 7000)
    private String description;
    @NotNull
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Future
    private LocalDateTime eventDate;
    @NotNull
    private Location location;
    private Boolean paid = false;
    @PositiveOrZero
    private Integer participantLimit = 0;
    private Boolean requestModeration = true;
    @NotNull
    @NotBlank
    @Size(min = 3, max = 120)
    private String title;
    private Boolean isCommentPermitted = true;
}