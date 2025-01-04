package ru.practicum.ewm.error;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.springframework.http.HttpStatus;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
public class ApiError {
    List<StackTraceElement> errors;
    String message;
    String reason;
    HttpStatus status;
    String timestamp;
}
