package ru.practicum.ewm.comment.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ru.practicum.ewm.comment.dto.CommentFullDto;
import ru.practicum.ewm.comment.dto.CommentShortDto;
import ru.practicum.ewm.comment.dto.NewCommentDto;
import ru.practicum.ewm.comment.dto.UpdateCommentDto;
import ru.practicum.ewm.comment.dto.enums.CommentsSort;
import ru.practicum.ewm.comment.service.CommentsService;

import java.util.List;

@RestController
@RequestMapping("/users/{userId}/comments")
@RequiredArgsConstructor
@Slf4j
public class PrivateCommentsController {
    private final CommentsService commentsService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CommentFullDto addNewComment(@RequestBody @Valid NewCommentDto request,
                                        @PathVariable Integer userId,
                                        @RequestParam @Positive Integer eventId) {
        log.info("PrivateCommentsController: пришёл запрос на добавление нового комментария к событию с id={} от пользователя с id={}: {}", eventId, userId, request);
        return commentsService.addNewCommentPrivate(request, userId, eventId);
    }

    @PostMapping("/{comId}/like")
    @ResponseStatus(HttpStatus.CREATED)
    public CommentFullDto likeComment(@PathVariable Integer userId,
                                      @PathVariable String comId) {
        log.info("PrivateCommentsController: пользователь с id={} поставил лайк комментарию с id={}", userId, comId);
        return commentsService.likeComment(userId, comId);
    }

    @PostMapping("/{comId}/complain")
    @ResponseStatus(HttpStatus.CREATED)
    public void submitCommentForAdminModeration(@PathVariable Integer userId,
                                                @PathVariable String comId) {
        log.info("PrivateCommentsController: комментарий с id={} нуждается в модерации", comId);
        commentsService.submitCommentForAdminModeration(userId, comId);
    }

    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    public List<CommentShortDto> getAllUsersComments(@PathVariable Integer userId,
                                                     @RequestParam(required = false, defaultValue = "BY_CREATION_DATE_DESC") CommentsSort sort,
                                                     @RequestParam(required = false, defaultValue = "0") Integer from,
                                                     @RequestParam(required = false, defaultValue = "10") Integer size) {
        log.info("PrivateCommentsController: пришёл запрос на получение всех собственных комментариев от пользователя с id={}", userId);
        return commentsService.getAllUsersCommentsPrivate(userId, sort, from, size);
    }

    @PatchMapping
    @ResponseStatus(HttpStatus.OK)
    public CommentFullDto updateUsersComment(@RequestBody @Valid UpdateCommentDto request,
                                             @PathVariable Integer userId) {
        log.info("PrivateCommentsController: пришёл запрос на обновление собственного комментария от пользователя с id={}: {}", userId, request);
        return commentsService.updateUsersCommentPrivate(request, userId);
    }

    @PatchMapping("/disable")
    @ResponseStatus(HttpStatus.OK)
    public void disableCommentsOnEvent(@PathVariable Integer userId,
                                       @RequestParam @Positive Integer eventId) {
        log.info("PrivateCommentsController: пришёл запрос на отключение возможности комментирования события с id={} от пользователя с id={}", eventId, userId);
        commentsService.disableCommentsOnEvent(userId, eventId);
    }

    @PatchMapping("/enable")
    @ResponseStatus(HttpStatus.OK)
    public void enableCommentsOnEvent(@PathVariable Integer userId,
                                      @RequestParam @Positive Integer eventId) {
        log.info("PrivateCommentsController: пришёл запрос на разрешение возможности комментирования события с id={} от пользователя с id={}", eventId, userId);
        commentsService.enableCommentsOnEvent(userId, eventId);
    }

    @DeleteMapping("/{comId}/delete")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteUsersComment(@PathVariable Integer userId,
                                   @PathVariable String comId) {
        log.info("PrivateCommentsController: пришёл запрос на удаление собственного комментария с id={} от пользователя с id={}", comId, userId);
        commentsService.deleteCommentByIdPrivate(userId, comId);
    }
}