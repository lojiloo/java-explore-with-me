package ru.practicum.ewm.comment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateCommentDto {
    @NotNull
    private String commentId;
    @NotNull
    @NotBlank
    @Size(min = 1, max = 4000)
    private String text;
}
