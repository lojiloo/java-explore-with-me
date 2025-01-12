package ru.practicum.ewm.comment.service;

import ru.practicum.ewm.comment.dto.*;
import ru.practicum.ewm.comment.dto.enums.CommentsSort;

import java.util.List;

public interface CommentsService {

    CommentFullDto addNewCommentPrivate(NewCommentDto request, Integer userId, Integer eventId);

    CommentFullDto getCommentByIdPublic(String comId);

    CommentAdminDto getCommentByIdAdmin(String comId);

    List<CommentFullDto> getCommentsByEventPublic(Integer eventId, CommentsSort sort, Integer from, Integer size);

    List<CommentShortDto> getAllUsersCommentsPrivate(Integer userId, CommentsSort sort, Integer from, Integer size);

    List<CommentAdminDto> getAllUsersCommentsAdmin(Integer userId, Integer from, Integer size);

    List<CommentAdminDto> getCommentsForReview(Integer from, Integer size);

    CommentFullDto updateUsersCommentPrivate(UpdateCommentDto request, Integer userId);

    void disableCommentsOnEvent(Integer userId, Integer eventId);

    void enableCommentsOnEvent(Integer userId, Integer eventId);

    void submitCommentForAdminModeration(Integer userId, String comId);

    CommentFullDto likeComment(Integer userId, String comId);

    void deleteCommentByIdPrivate(Integer userId, String comId);

    void deleteCommentByIdAdmin(String comId);

    void deleteAllUsersCommentsAdmin(Integer userId);

}
