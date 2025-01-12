package ru.practicum.ewm.compilation.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.Set;

@Data
public class UpdateCompilationRequest {
    private Set<Integer> events;
    private Boolean pinned;
    @Size(min = 1, max = 50)
    private String title;
}