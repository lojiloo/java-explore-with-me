package ru.practicum.ewm.request.dto;

import lombok.Data;
import ru.practicum.ewm.request.dto.enums.Status;

import java.util.List;

@Data
public class EventRequestStatusUpdateRequest {
    private List<Integer> requestIds;
    private Status status;
}