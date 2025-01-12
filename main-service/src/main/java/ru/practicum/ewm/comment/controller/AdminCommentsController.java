package ru.practicum.ewm.comment.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ru.practicum.ewm.comment.dto.CommentAdminDto;
import ru.practicum.ewm.comment.service.CommentsService;

import java.util.List;

@RestController
@RequestMapping("/admin/comments")
@RequiredArgsConstructor
@Slf4j
public class AdminCommentsController {
    private final CommentsService commentsService;

    @GetMapping("/review")
    @ResponseStatus(HttpStatus.OK)
    public List<CommentAdminDto> getCommentsForReview(@RequestParam(required = false, defaultValue = "0") Integer from,
                                                      @RequestParam(required = false, defaultValue = "10") Integer size) {
        log.info("AdminCommentsController: пришёл запрос на получение комментариев к модерации: from={}, size={}", from, size);
        return commentsService.getCommentsForReview(from, size);
    }

    @GetMapping("/{comId}")
    @ResponseStatus(HttpStatus.OK)
    public CommentAdminDto getCommentById(@PathVariable String comId) {
        log.info("PrivateCommentsController: пришёл запрос на получение комментария с id={}", comId);
        return commentsService.getCommentByIdAdmin(comId);
    }

    @GetMapping("/users/{userId}")
    @ResponseStatus(HttpStatus.OK)
    public List<CommentAdminDto> getAllUsersCommentsAdmin(@PathVariable Integer userId,
                                                          @RequestParam(required = false, defaultValue = "0") Integer from,
                                                          @RequestParam(required = false, defaultValue = "10") Integer size) {
        log.info("AdminCommentsController: пришёл запрос на получение всех комментариев пользователя с id={}", userId);
        return commentsService.getAllUsersCommentsAdmin(userId, from, size);
    }

    @DeleteMapping("/{comId}/delete")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteCommentByIdAdmin(@PathVariable String comId) {
        log.info("AdminCommentsController: пришёл запрос на удаление комментария с id={}", comId);
        commentsService.deleteCommentByIdAdmin(comId);
    }

    @DeleteMapping("users/{userId}/delete")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteAllUsersCommentsAdmin(@PathVariable Integer userId) {
        log.info("AdminCommentsController: пришёл запрос на удаление всех комментариев пользователя с id={}", userId);
        commentsService.deleteAllUsersCommentsAdmin(userId);
    }
}
