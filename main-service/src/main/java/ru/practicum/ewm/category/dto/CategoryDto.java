package ru.practicum.ewm.category.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CategoryDto {
    private Integer id;
    @Size(min = 1, max = 50, message = "Имя категории должно содержать минимум 1 максимум 50 символов")
    private String name;
}