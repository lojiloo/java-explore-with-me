package ru.practicum.ewm.event.service;

import com.querydsl.core.types.ExpressionUtils;
import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.dsl.BooleanExpression;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;
import ru.practicum.StatsClient;
import ru.practicum.dto.ReturnStatsDto;
import ru.practicum.ewm.category.model.Category;
import ru.practicum.ewm.category.repository.CategoriesRepository;
import ru.practicum.ewm.error.exceptions.BadRequestException;
import ru.practicum.ewm.error.exceptions.ConditionsViolationException;
import ru.practicum.ewm.error.exceptions.NotFoundException;
import ru.practicum.ewm.error.exceptions.ValidationException;
import ru.practicum.ewm.event.converter.EventsConverter;
import ru.practicum.ewm.event.dto.*;
import ru.practicum.ewm.event.dto.enums.State;
import ru.practicum.ewm.event.dto.enums.StateAction;
import ru.practicum.ewm.event.model.Event;
import ru.practicum.ewm.event.model.QEvent;
import ru.practicum.ewm.event.repository.EventsRepository;
import ru.practicum.ewm.request.converter.RequestsConverter;
import ru.practicum.ewm.request.dto.EventRequestStatusUpdateRequest;
import ru.practicum.ewm.request.dto.EventRequestStatusUpdateResult;
import ru.practicum.ewm.request.dto.ParticipationRequestDto;
import ru.practicum.ewm.request.dto.enums.Status;
import ru.practicum.ewm.request.model.Request;
import ru.practicum.ewm.request.repository.RequestsRepository;
import ru.practicum.ewm.user.model.User;
import ru.practicum.ewm.user.repository.UsersRepository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class EventsServiceImpl implements EventsService {
    public static final String SORT_BY_VIEWS_PROPERTY = "views";
    public static final String SORT_BY_DATE_PROPERTY = "eventDate";
    public static final Integer VIEWS_ANALYSIS_MONTHS = 1;
    private final StatsClient client;
    private final EventsConverter eventsConverter = new EventsConverter();
    private final RequestsConverter requestsConverter = new RequestsConverter();
    private final EventsRepository eventsRepository;
    private final RequestsRepository requestsRepository;
    private final CategoriesRepository categoriesRepository;
    private final UsersRepository usersRepository;

    @Override
    public EventFullDto addNewEventPrivate(NewEventDto request, Integer userId) {
        Category category = categoriesRepository.findById(request.getCategory()).orElseThrow(
                () -> new NotFoundException(String.format("Category with id=%d was not found", request.getCategory()))
        );

        User initiator = usersRepository.findById(userId).orElseThrow(
                () -> new NotFoundException(String.format("User with id=%d was not found", userId))
        );

        LocalDateTime eventDate = request.getEventDate();
        if (eventDate.isBefore(LocalDateTime.now().plusHours(2))) {
            log.error("Начало события должно быть не ранее, чем через два часа: {}", eventDate);
            throw new ConditionsViolationException(String.format("Начало события должно быть не ранее, чем через два часа: %s", eventDate));
        }

        Event event = Event.builder()
                .initiator(initiator)
                .annotation(request.getAnnotation())
                .category(category)
                .description(request.getDescription())
                .eventDate(eventDate)
                .state(State.PENDING)
                .location(request.getLocation())
                .participantLimit(request.getParticipantLimit() == null ? 0 : request.getParticipantLimit())
                .requestModeration(request.getRequestModeration() == null || request.getRequestModeration())
                .title(request.getTitle())
                .createdOn(LocalDateTime.now())
                .paid(request.getPaid() != null && request.getPaid())
                .isCommentPermitted(request.getIsCommentPermitted() == null || request.getIsCommentPermitted())
                .build();

        eventsRepository.save(event);
        log.info("Создано новое событие с id={}: {}", event.getId(), event);

        return eventsConverter.toFullDto(event);
    }

    @Override
    public List<EventShortDto> getAllEventsPublic(EventPublicParamDto params) {
        if (params.getRangeStart() != null
                && params.getRangeEnd() != null
                && !params.getRangeStart().isBefore(params.getRangeEnd())) {
            log.error("Время начала должно предшествовать времени окончания: start={}, end={}", params.getRangeStart(), params.getRangeEnd());
            throw new ValidationException("Время начала должно предшествовать времени окончания");
        }

        BooleanExpression byState = QEvent.event.state.eq(State.PUBLISHED);

        String text = params.getText();
        BooleanExpression byText = (text != null && !text.isEmpty() ? QEvent.event.description.containsIgnoreCase(text)
                .or(QEvent.event.annotation.containsIgnoreCase(text)) : null);

        List<Integer> categories = params.getCategories();
        BooleanExpression byCategory = (categories != null && !categories.isEmpty()
                ? QEvent.event.category.id.in(params.getCategories()) : null);


        Boolean paid = params.getPaid();
        BooleanExpression byPaid = (paid != null ? QEvent.event.paid.eq(paid) : null);

        LocalDateTime rangeStart = params.getRangeStart();
        LocalDateTime rangeEnd = params.getRangeEnd();
        BooleanExpression byEventDate = (rangeStart != null && rangeEnd != null
                ? QEvent.event.eventDate.between(rangeStart, rangeEnd) : QEvent.event.eventDate.after(LocalDateTime.now()));

        Predicate predicate = ExpressionUtils.allOf(byState, byText, byCategory, byPaid, byEventDate);
        Pageable pageable = PageRequest.of(params.getFrom(), params.getSize(), extractSort(params.getSort()));

        List<Event> events = (predicate != null)
                ? eventsRepository.findAll(predicate, pageable).stream().collect(Collectors.toList()) : eventsRepository.findAll(pageable).stream().collect(Collectors.toList());

        log.info("По заданным параметрам найдено событий: всего {}", events.size());

        if (events.isEmpty()) {
            return List.of();
        }

        setConfirmedRequestsCountForEventList(events);

        if (params.getOnlyAvailable() != null && params.getOnlyAvailable()) {
            events = filterByAvailability(events);
        }

        return mapListToShort(events);
    }

    @Override
    public List<EventFullDto> getAllEventsAdmin(EventAdminParamDto params) {
        if (params.getRangeStart() != null
                && params.getRangeEnd() != null
                && !params.getRangeStart().isBefore(params.getRangeEnd())) {
            throw new ValidationException("Время начала должно предшествовать времени окончания");
        }

        List<Integer> initiatorsIds = params.getUsers();
        BooleanExpression byUsers = (initiatorsIds != null && !initiatorsIds.isEmpty())
                ? QEvent.event.initiator.id.in(initiatorsIds) : null;

        List<State> states = params.getStates();
        BooleanExpression byStates = (states != null) ? QEvent.event.state.in(states) : null;

        List<Integer> categories = params.getCategories();
        BooleanExpression byCategory = (categories != null && !categories.isEmpty()
                ? QEvent.event.category.id.in(params.getCategories()) : null);

        LocalDateTime rangeStart = params.getRangeStart();
        LocalDateTime rangeEnd = params.getRangeEnd();
        BooleanExpression byEventDate = (rangeStart != null && rangeEnd != null
                ? QEvent.event.eventDate.between(rangeStart, rangeEnd) : QEvent.event.eventDate.after(LocalDateTime.now()));

        Predicate predicate = ExpressionUtils.allOf(byUsers, byStates, byCategory, byEventDate);
        Pageable pageable = PageRequest.of(params.getFrom(), params.getSize());

        List<Event> events = (predicate != null)
                ? eventsRepository.findAll(predicate, pageable).stream().collect(Collectors.toList()) : eventsRepository.findAll(pageable).stream().collect(Collectors.toList());

        log.info("По заданным параметрам найдено событий: всего {}", events.size());

        events = setViewsForEventsList(events);
        setConfirmedRequestsCountForEventList(events);

        return mapListToFull(events);
    }

    @Override
    public EventFullDto getEventById(Integer id) {
        Event event = findById(id);

        if (!event.getState().equals(State.PUBLISHED)) {
            log.error("Запрос отклонён, поскольку событие ещё не опубликовано: {}", event.getState());
            throw new NotFoundException(String.format("Event with id=%d was not found", id));
        }

        setViewsForEvent(event);
        setConfirmedRequestsCountForEvent(event);

        return eventsConverter.toFullDto(event);
    }

    @Override
    public List<EventShortDto> getUsersEvents(Integer userId, Integer from, Integer size) {
        List<Event> events = eventsRepository.findAllByInitiatorId(userId, PageRequest.of(from, size));

        events = setViewsForEventsList(events);
        setConfirmedRequestsCountForEventList(events);

        return mapListToShort(events);
    }

    @Override
    public EventFullDto getUsersEventById(Integer userId, Integer eventId) {
        Event event = findById(eventId);

        setViewsForEvent(event);
        setConfirmedRequestsCountForEvent(event);

        return eventsConverter.toFullDto(event);
    }

    @Override
    public List<ParticipationRequestDto> getUsersEventRequestsByEventId(Integer userId, Integer eventId) {
        return requestsRepository.findAllByEventId(eventId)
                .stream()
                .map(requestsConverter::toParticipationRequestDto)
                .toList();
    }

    @Override
    public EventFullDto updateUsersEventPrivate(UpdateEventUserRequest request, Integer userId, Integer eventId) {
        Event event = findById(eventId);

        if (event.getState().equals(State.PUBLISHED)) {
            log.error("Событие уже опубликовано, изменения запрещены: event.getState={}", event.getState());
            throw new ConditionsViolationException("Only pending or canceled events can be changed");
        }

        if (request.getAnnotation() != null) {
            event.setAnnotation(request.getAnnotation());
        }
        if (request.getCategory() != null) {
            Category category = categoriesRepository.findById(request.getCategory()).orElseThrow(
                    () -> new NotFoundException(String.format("Category with id=%d was not found", request.getCategory()))
            );
            event.setCategory(category);
        }
        if (request.getDescription() != null) {
            event.setDescription(request.getDescription());
        }
        if (request.getEventDate() != null) {
            LocalDateTime eventDate = request.getEventDate();
            if (eventDate.isBefore(LocalDateTime.now().plusHours(2))) {
                log.error("Начало события должно быть не ранее, чем через два часа: {}", eventDate);
                throw new ConditionsViolationException(String.format("Начало события должно быть не ранее, чем через два часа: %s", eventDate));
            }
            event.setEventDate(eventDate);
        }
        if (request.getLocation() != null) {
            event.setLocation(request.getLocation());
        }
        if (request.getPaid() != null) {
            event.setPaid(request.getPaid());
        }
        if (request.getParticipantLimit() != null) {
            event.setParticipantLimit(request.getParticipantLimit());
        }
        if (request.getRequestModeration() != null) {
            event.setRequestModeration(request.getRequestModeration());
        }
        if (request.getTitle() != null) {
            event.setTitle(request.getTitle());
        }
        if (request.getIsCommentPermitted() != null) {
            event.setIsCommentPermitted(request.getIsCommentPermitted());
        }

        eventsRepository.save(event);

        setViewsForEvent(event);
        setConfirmedRequestsCountForEvent(event);

        if (request.getStateAction() != null) {
            StateAction stateAction = request.getStateAction();
            switch (stateAction) {
                case SEND_TO_REVIEW -> event.setState(State.PENDING);
                case CANCEL_REVIEW -> event.setState(State.CANCELED);
            }
        }

        return eventsConverter.toFullDto(event);
    }

    @Override
    public EventFullDto updateUsersEventAdmin(UpdateEventAdminRequest request, Integer eventId) {
        Event event = findById(eventId);

        if (request.getAnnotation() != null) {
            event.setAnnotation(request.getAnnotation());
        }
        if (request.getCategory() != null) {
            Category category = categoriesRepository.findById(request.getCategory()).orElseThrow(
                    () -> new NotFoundException(String.format("Category with id=%d was not found", request.getCategory()))
            );
            event.setCategory(category);
        }
        if (request.getDescription() != null) {
            event.setDescription(request.getDescription());
        }
        if (request.getEventDate() != null) {
            LocalDateTime eventDate = request.getEventDate();
            if (eventDate.isBefore(LocalDateTime.now().plusHours(1))) {
                log.error("Начало события должно быть не ранее, чем через два часа: {}", eventDate);
                throw new ConditionsViolationException(String.format("Начало события должно быть не ранее, чем через час: %s", eventDate));
            }
            event.setEventDate(eventDate);
        }
        if (request.getLocation() != null) {
            event.setLocation(request.getLocation());
        }
        if (request.getPaid() != null) {
            event.setPaid(request.getPaid());
        }
        if (request.getParticipantLimit() != null) {
            event.setParticipantLimit(request.getParticipantLimit());
        }
        if (request.getRequestModeration() != null) {
            event.setRequestModeration(request.getRequestModeration());
        }
        if (request.getTitle() != null) {
            event.setTitle(request.getTitle());
        }
        if (request.getIsCommentPermitted() != null) {
            event.setIsCommentPermitted(request.getIsCommentPermitted());
        }

        if (request.getStateAction() != null) {
            StateAction stateAction = request.getStateAction();
            switch (stateAction) {
                case PUBLISH_EVENT:
                    if (!event.getState().equals(State.PENDING)) {
                        throw new ConditionsViolationException(String.format("Cannot publish the eventId because it's not in the right state: %s", event.getState()));
                    }
                    event.setState(State.PUBLISHED);
                    event.setPublishedOn(LocalDateTime.now());
                    break;
                case REJECT_EVENT:
                    if (event.getState().equals(State.PUBLISHED)) {
                        throw new ConditionsViolationException(String.format("Cannot reject the eventId because it's not in the right state: %s", event.getState()));
                    }
                    event.setState(State.REJECTED);
                    break;
            }
        }

        eventsRepository.save(event);

        setViewsForEvent(event);
        setConfirmedRequestsCountForEvent(event);

        return eventsConverter.toFullDto(event);
    }

    @Override
    public EventRequestStatusUpdateResult updateUsersEventRequestsByEventId(EventRequestStatusUpdateRequest request, Integer userId, Integer eventId) {
        Event event = findById(eventId);
        if (isParticipantLimitReached(event)) {
            throw new ConditionsViolationException("На данное событие набор окончен: лимит заявок достигнут");
        }

        List<Request> requests = requestsRepository.findAllById(request.getRequestIds());
        for (Request r : requests) {
            if (!r.getStatus().equals(Status.PENDING)) {
                throw new ConditionsViolationException("Статус заявки может быть изменён только для заявок, ожидающих модерацию");
            }
        }

        if (!event.getRequestModeration()) {
            requestsRepository.updateStatus(Status.CONFIRMED, request.getRequestIds());
        }

        Status status = request.getStatus();
        switch (status) {
            case CONFIRMED:
                return processConfirmedRequests(event, requests);
            default:
                return processRejectedRequests(event, requests);
        }
    }

    @Override
    public void disableCommentsOnEvent(Event event) throws BadRequestException {
        if (!event.getIsCommentPermitted()) {
            throw new BadRequestException("Комментарии для данного события уже отключены");
        }

        event.setIsCommentPermitted(false);
        eventsRepository.save(event);
        log.info("Комментирование события с id={} отключено", event.getId());
    }

    @Override
    public void enableCommentsOnEvent(Event event) throws BadRequestException {
        if (event.getIsCommentPermitted()) {
            throw new BadRequestException("Комментарии для данного события уже разрешены");
        }

        event.setIsCommentPermitted(true);
        eventsRepository.save(event);
        log.info("Комментирование события с id={} вновь доступно", event.getId());
    }

    @Override
    public Event findById(Integer eventId) {
        return eventsRepository.findById(eventId).orElseThrow(
                () -> new NotFoundException(String.format("Event with id=%d was not found", eventId))
        );
    }

    @Override
    public Boolean contains(Integer eventId) {
        return eventsRepository.existsById(eventId);
    }

    private Sort extractSort(EventPublicParamDto.Sort s) {
        Sort sort;
        switch (s) {
            case VIEWS:
                sort = Sort.by(SORT_BY_VIEWS_PROPERTY);
                break;
            default:
                sort = Sort.by(SORT_BY_DATE_PROPERTY);
        }
        return sort;
    }

    private String extractUri(int eventId) {
        return UriComponentsBuilder.fromUriString("/events/").pathSegment(String.valueOf(eventId)).toUriString();
    }

    private List<EventShortDto> mapListToShort(List<Event> events) {
        return events.stream().map(eventsConverter::toShortDto).collect(Collectors.toList());

    }

    private List<EventFullDto> mapListToFull(List<Event> events) {
        return events.stream().map(eventsConverter::toFullDto).collect(Collectors.toList());
    }

    private List<Event> setViewsForEventsList(List<Event> events) {
        Map<String, Event> urisMap = events.stream()
                .collect(Collectors.toMap(dto -> UriComponentsBuilder.fromUriString("/events")
                        .pathSegment(String.valueOf(dto.getId()))
                        .toUriString(), Function.identity()));

        List<ReturnStatsDto> stats = client.getStats(
                LocalDateTime.now().minusMonths(VIEWS_ANALYSIS_MONTHS),
                LocalDateTime.now(),
                new ArrayList<>(urisMap.keySet()),
                true
        );

        for (ReturnStatsDto stat : stats) {
            Event event = urisMap.get(stat.getUri());
            if (event != null) {
                event.setViews(stat.getHits());
                log.info("Для события с id={} установлено количество просмотров views={}", event.getId(), event.getViews());
            }
        }

        return new ArrayList<>(urisMap.values());
    }

    private void setViewsForEvent(Event event) {
        String uri = extractUri(event.getId());
        List<ReturnStatsDto> stats = client.getStats(
                LocalDateTime.now().minusMonths(VIEWS_ANALYSIS_MONTHS),
                LocalDateTime.now(),
                List.of(uri),
                true);

        if (!stats.isEmpty()) {
            Integer views = stats.getFirst().getHits();
            event.setViews(views);
        } else {
            event.setViews(0);
        }
    }

    private boolean isParticipantLimitReached(Event event) {
        if (event.getParticipantLimit() == 0) {
            return false;
        }
        long participantLimit = event.getParticipantLimit();

        long confirmedRequestCount = requestsRepository.countByStatusAndEventId(Status.CONFIRMED, event.getId());

        return participantLimit - confirmedRequestCount <= 0;
    }

    private List<Event> setConfirmedRequestsCountForEventList(List<Event> events) {
        List<Integer> eventsIds = events.stream().map(Event::getId).collect(Collectors.toList());

        Map<Integer, List<Request>> confirmedRequestsByEventId = requestsRepository.findByStatusAndEventIdIn(Status.CONFIRMED, eventsIds)
                .stream()
                .collect(Collectors.groupingBy(request -> request.getEvent().getId()));

        for (Event event : events) {
            int count = confirmedRequestsByEventId.getOrDefault(event.getId(), Collections.emptyList()).size();
            event.setConfirmedRequests(count);
        }

        return events;
    }

    private void setConfirmedRequestsCountForEvent(Event event) {
        Integer confirmedRequestsCount = requestsRepository.countByStatusAndEventId(Status.CONFIRMED, event.getId());

        event.setConfirmedRequests(confirmedRequestsCount);
    }

    private List<Event> filterByAvailability(List<Event> events) {
        return events.stream()
                .filter(event -> event.getParticipantLimit() < event.getConfirmedRequests())
                .collect(Collectors.toList());
    }

    private EventRequestStatusUpdateResult processConfirmedRequests(Event event, List<Request> requests) {
        List<Request> confirmedRequests = new ArrayList<>();
        List<Request> rejectedRequests = new ArrayList<>();

        int limit = event.getParticipantLimit();
        int confirmedRequestsCount = requestsRepository.countByStatusAndEventId(Status.CONFIRMED, event.getId());

        for (Request request : requests) {
            if (confirmedRequestsCount >= limit) {
                request.setStatus(Status.REJECTED);
                rejectedRequests.add(request);
                continue;
            }

            request.setStatus(Status.CONFIRMED);
            confirmedRequests.add(request);

            confirmedRequestsCount++;
        }

        List<Integer> rejectedRequestsIds = rejectedRequests.stream().map(Request::getId).collect(Collectors.toList());
        if (!rejectedRequestsIds.isEmpty()) {
            requestsRepository.updateStatus(Status.REJECTED, rejectedRequestsIds);
        }

        List<Integer> confirmedRequestsIds = confirmedRequests.stream().map(Request::getId).collect(Collectors.toList());
        if (!confirmedRequestsIds.isEmpty()) {
            requestsRepository.updateStatus(Status.CONFIRMED, confirmedRequestsIds);
        }

        List<ParticipationRequestDto> confirmedRequestsDto = confirmedRequests.stream()
                .map(requestsConverter::toParticipationRequestDto)
                .collect(Collectors.toList());
        List<ParticipationRequestDto> rejectedRequestsDto = rejectedRequests.stream()
                .map(requestsConverter::toParticipationRequestDto)
                .collect(Collectors.toList());

        log.info("Идентификаторы отклонённых запросов на событие с id={}: {}", event.getId(), rejectedRequestsIds);
        log.info("Идентификаторы принятых запросов на событие с id={}: {}", event.getId(), confirmedRequestsIds);

        return new EventRequestStatusUpdateResult(confirmedRequestsDto, rejectedRequestsDto);
    }

    private EventRequestStatusUpdateResult processRejectedRequests(Event event, List<Request> rejectedRequests) {
        List<Integer> rejectedRequestsIds = rejectedRequests.stream().map(Request::getId).collect(Collectors.toList());
        requestsRepository.updateStatus(Status.REJECTED, rejectedRequestsIds);

        List<ParticipationRequestDto> rejectedRequestsDto = rejectedRequests.stream()
                .map(requestsConverter::toParticipationRequestDto)
                .peek(request -> request.setStatus(Status.REJECTED))
                .collect(Collectors.toList());

        return new EventRequestStatusUpdateResult(new ArrayList<>(), rejectedRequestsDto);
    }
}