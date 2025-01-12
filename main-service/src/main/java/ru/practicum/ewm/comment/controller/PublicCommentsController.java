package ru.practicum.ewm.comment.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ru.practicum.ewm.comment.dto.CommentFullDto;
import ru.practicum.ewm.comment.dto.enums.CommentsSort;
import ru.practicum.ewm.comment.service.CommentsService;

import java.util.List;

@RestController
@RequestMapping("/comments")
@RequiredArgsConstructor
@Slf4j
public class PublicCommentsController {
    private final CommentsService commentsService;

    @GetMapping("/events/{eventId}")
    public List<CommentFullDto> getCommentsByEvent(@PathVariable Integer eventId,
                                                   @RequestParam(required = false, defaultValue = "BY_CREATION_DATE_DESC") CommentsSort sort,
                                                   @RequestParam(required = false, defaultValue = "0") Integer from,
                                                   @RequestParam(required = false, defaultValue = "10") Integer size) {
        log.info("PublicCommentsController: пришёл запрос на получение всех комментариев для события с id={}", eventId);
        return commentsService.getCommentsByEventPublic(eventId, sort, from, size);
    }

    @GetMapping("/{comId}")
    @ResponseStatus(HttpStatus.OK)
    public CommentFullDto getCommentById(@PathVariable String comId) {
        log.info("PrivateCommentsController: пришёл запрос на получение комментария с id={}", comId);
        return commentsService.getCommentByIdPublic(comId);
    }
}
