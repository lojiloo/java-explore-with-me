package ru.practicum.ewm.user.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class NewUserDto {
    @NotEmpty
    @Email
    @Size(min = 6, max = 254)
    private String email;
    @NotBlank
    @NotNull
    @Size(min = 2, max = 250)
    private String name;
}