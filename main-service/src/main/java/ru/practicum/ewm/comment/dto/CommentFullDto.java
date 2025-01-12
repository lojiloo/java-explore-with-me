package ru.practicum.ewm.comment.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Data;
import ru.practicum.ewm.event.dto.EventShortDto;
import ru.practicum.ewm.user.dto.UserShortDto;

import java.time.LocalDateTime;

@Data
@JsonPropertyOrder({"id", "text", "likes", "author", "event", "created_on", "is_updated"})
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CommentFullDto {
    private String id;
    private String text;
    private UserShortDto author;
    private EventShortDto event;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonProperty("created_on")
    private LocalDateTime createdOn;
    @JsonProperty("is_updated")
    private Boolean isUpdated;
    private Long likes;
}
