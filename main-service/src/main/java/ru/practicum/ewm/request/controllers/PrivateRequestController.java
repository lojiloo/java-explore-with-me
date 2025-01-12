package ru.practicum.ewm.request.controllers;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ru.practicum.ewm.request.dto.ParticipationRequestDto;
import ru.practicum.ewm.request.service.RequestService;

import java.util.List;

@RestController
@RequestMapping("/users/{userId}")
@RequiredArgsConstructor
@Slf4j
public class PrivateRequestController {
    private final RequestService requestService;

    @PostMapping("/requests")
    @ResponseStatus(HttpStatus.CREATED)
    public ParticipationRequestDto sendParticipationRequest(@PathVariable Integer userId,
                                                            @RequestParam Integer eventId) {
        log.info("PrivateRequestController: пришёл запрос от пользователя с id={} на участие в событии id={}", userId, eventId);
        return requestService.sendParticipationRequest(userId, eventId);
    }

    @GetMapping("/requests")
    @ResponseStatus(HttpStatus.OK)
    public List<ParticipationRequestDto> getAllUsersRequestsById(@PathVariable Integer userId) {
        return requestService.getAllUsersRequestsById(userId);
    }

    @PatchMapping("/requests/{requestId}/cancel")
    @ResponseStatus(HttpStatus.OK)
    public ParticipationRequestDto cancelParticipationRequest(@PathVariable Integer userId,
                                                              @PathVariable Integer requestId) {
        return requestService.cancelParticipationRequest(userId, requestId);
    }
}