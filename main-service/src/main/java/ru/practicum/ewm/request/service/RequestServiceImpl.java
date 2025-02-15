package ru.practicum.ewm.request.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.practicum.ewm.error.exceptions.ConditionsViolationException;
import ru.practicum.ewm.error.exceptions.EntityExistsException;
import ru.practicum.ewm.error.exceptions.NotFoundException;
import ru.practicum.ewm.event.dto.enums.State;
import ru.practicum.ewm.event.model.Event;
import ru.practicum.ewm.event.repository.EventsRepository;
import ru.practicum.ewm.request.converter.RequestsConverter;
import ru.practicum.ewm.request.dto.ParticipationRequestDto;
import ru.practicum.ewm.request.dto.enums.Status;
import ru.practicum.ewm.request.model.Request;
import ru.practicum.ewm.request.repository.RequestsRepository;
import ru.practicum.ewm.user.model.User;
import ru.practicum.ewm.user.repository.UsersRepository;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class RequestServiceImpl implements RequestService {
    private final RequestsConverter requestsConverter;

    private final RequestsRepository requestsRepository;
    private final EventsRepository eventsRepository;
    private final UsersRepository usersRepository;

    @Override
    public List<ParticipationRequestDto> getAllUsersRequestsById(Integer userId) {
        return requestsRepository.findAllByUserId(userId)
                .stream()
                .map(requestsConverter::toParticipationRequestDto)
                .toList();
    }

    @Override
    public ParticipationRequestDto sendParticipationRequest(Integer userId, Integer eventId) {
        Event event = eventsRepository.findById(eventId).orElseThrow(
                () -> new NotFoundException(String.format("Event with id=%d was not found", eventId))
        );

        User user = usersRepository.findById(userId).orElseThrow(
                () -> new NotFoundException(String.format("User with id=%d was not found", userId))
        );

        if (isRequestAlreadySend(userId, eventId)) {
            log.error("Заявка уже есть в базе данных: повторно подать заявку нельзя");
            throw new EntityExistsException("Нельзя подать повторную заявку на событие");
        }
        if (isParticipantLimitReached(event)) {
            log.error("Лимит участников достигнут; заявка не сохранена в базу данных");
            throw new ConditionsViolationException("К сожалению, требуемое количество участников уже набрано");
        }

        if (event.getInitiator().getId().equals(userId)) {
            log.error("Создатель не может подать заявку на собственное событие");
            throw new ConditionsViolationException("Нельзя подать заявку на собственное событие");
        }
        if (!event.getState().equals(State.PUBLISHED)) {
            log.error("Событие не опубликовано; подать заявку можно только на опубликованное событие");
            throw new ConditionsViolationException("Заявка может быть подана только на опубликованное событие");
        }

        Request request = Request.builder()
                .event(event)
                .user(user)
                .createdOn(LocalDateTime.now())
                .build();

        if (!event.getRequestModeration() || event.getParticipantLimit() == 0) {
            request.setStatus(Status.CONFIRMED);
            log.info("Модерация не требуется. Запросу присвоен статус {}", Status.CONFIRMED);
        } else {
            request.setStatus(Status.PENDING);
            log.info("Модерация необходима. Запросу присвоен статус {}", Status.PENDING);
        }

        requestsRepository.save(request);
        log.info("Создана новая заявка с id={}: {}", event.getId(), event);

        return requestsConverter.toParticipationRequestDto(request);
    }

    @Override
    public ParticipationRequestDto cancelParticipationRequest(Integer userId, Integer requestId) {
        Request request = requestsRepository.findById(requestId).orElseThrow(
                () -> new NotFoundException(String.format("Request with id=%d was not found", requestId))
        );

        if (request.getUser().getId().equals(userId)) {
            request.setStatus(Status.CANCELED);
            requestsRepository.save(request);
        } else {
            log.error("Запрос не может быть удалён: пользователь с id={} не является созлдателем запроса", userId);
            throw new ConditionsViolationException("Удалить запрос может только его создатель");
        }

        return requestsConverter.toParticipationRequestDto(request);
    }

    private boolean isRequestAlreadySend(int userId, int eventId) {
        return requestsRepository.findByUserIdAndEventId(userId, eventId).isPresent();
    }

    private boolean isParticipantLimitReached(Event event) {
        if (event.getParticipantLimit() == 0) {
            return false;
        }
        long participantLimit = event.getParticipantLimit();

        long confirmedRequestCount = requestsRepository.countByStatusAndEventId(Status.CONFIRMED, event.getId());

        return participantLimit - confirmedRequestCount <= 0;
    }
}