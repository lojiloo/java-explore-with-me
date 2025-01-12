package ru.practicum.ewm.event.service;

import ru.practicum.ewm.event.dto.*;
import ru.practicum.ewm.event.model.Event;
import ru.practicum.ewm.request.dto.EventRequestStatusUpdateRequest;
import ru.practicum.ewm.request.dto.EventRequestStatusUpdateResult;
import ru.practicum.ewm.request.dto.ParticipationRequestDto;

import java.util.List;

public interface EventsService {

    EventFullDto addNewEventPrivate(NewEventDto request, Integer userId);

    List<EventShortDto> getAllEventsPublic(EventPublicParamDto params);

    List<EventFullDto> getAllEventsAdmin(EventAdminParamDto params);

    EventFullDto getEventById(Integer id);

    List<EventShortDto> getUsersEvents(Integer userId, Integer from, Integer size);

    EventFullDto getUsersEventById(Integer userId, Integer eventId);

    List<ParticipationRequestDto> getUsersEventRequestsByEventId(Integer userId, Integer eventId);

    EventFullDto updateUsersEventPrivate(UpdateEventUserRequest request, Integer userId, Integer eventId);

    EventFullDto updateUsersEventAdmin(UpdateEventAdminRequest request, Integer eventId);

    EventRequestStatusUpdateResult updateUsersEventRequestsByEventId(EventRequestStatusUpdateRequest request, Integer userId, Integer eventId);

    void disableCommentsOnEvent(Event event);

    void enableCommentsOnEvent(Event event);

    Event findById(Integer eventId);

    Boolean contains(Integer eventId);

}