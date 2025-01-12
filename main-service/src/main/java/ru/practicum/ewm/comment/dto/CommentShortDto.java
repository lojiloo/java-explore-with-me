package ru.practicum.ewm.comment.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@JsonPropertyOrder({"author_name", "event_title", "text", "likes", "created_on", "is_updated"})
public class CommentShortDto {
    private String text;
    private Long likes;
    @JsonProperty("author_name")
    private String authorName;
    @JsonProperty("event_title")
    private String eventTitle;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonProperty("created_on")
    private LocalDateTime createdOn;
    @JsonProperty("is_updated")
    private Boolean isUpdated;
}
