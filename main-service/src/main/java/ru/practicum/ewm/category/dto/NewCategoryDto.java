package ru.practicum.ewm.category.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class NewCategoryDto {
    @NotNull
    @NotBlank
    @Size(min = 1, max = 50, message = "Имя категории должно содержать минимум 1 максимум 50 символов")
    private String name;
}