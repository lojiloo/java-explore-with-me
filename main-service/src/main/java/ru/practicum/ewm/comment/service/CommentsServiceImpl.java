package ru.practicum.ewm.comment.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import ru.practicum.ewm.comment.converter.CommentsConverter;
import ru.practicum.ewm.comment.dto.*;
import ru.practicum.ewm.comment.dto.enums.CommentsSort;
import ru.practicum.ewm.comment.model.Comment;
import ru.practicum.ewm.comment.model.Complaint;
import ru.practicum.ewm.comment.model.Like;
import ru.practicum.ewm.comment.model.Review;
import ru.practicum.ewm.comment.repository.comments.CommentsRepository;
import ru.practicum.ewm.comment.repository.complaints.ComplaintsQueryResult;
import ru.practicum.ewm.comment.repository.complaints.ComplaintsRepository;
import ru.practicum.ewm.comment.repository.complaints.ReviewRepository;
import ru.practicum.ewm.comment.repository.likes.LikesQueryResult;
import ru.practicum.ewm.comment.repository.likes.LikesRepository;
import ru.practicum.ewm.error.exceptions.BadRequestException;
import ru.practicum.ewm.error.exceptions.ConditionsViolationException;
import ru.practicum.ewm.error.exceptions.EntityExistsException;
import ru.practicum.ewm.error.exceptions.NotFoundException;
import ru.practicum.ewm.event.dto.enums.State;
import ru.practicum.ewm.event.model.Event;
import ru.practicum.ewm.event.service.EventsService;
import ru.practicum.ewm.user.model.User;
import ru.practicum.ewm.user.service.UserService;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CommentsServiceImpl implements CommentsService {
    private static final Integer COMPLAINTS_COUNT_FOR_REVIEW = 2; //такое маленькое число для удобства тестирования

    private final CommentsRepository commentsRepository;
    private final LikesRepository likesRepository;
    private final ComplaintsRepository complaintsRepository;
    private final ReviewRepository reviewRepository;

    private final EventsService eventsService;
    private final UserService userService;

    private final CommentsConverter commentsConverter = new CommentsConverter();

    @Override
    public CommentFullDto addNewCommentPrivate(NewCommentDto request, Integer userId, Integer eventId) {
        Event event = eventsService.findById(eventId);
        if (event.getState() != State.PUBLISHED) {
            log.error("Оставить комментарий можно только для опубликованного события: событие с id={}, state={}", eventId, event.getState());
            throw new ConditionsViolationException("It is possible to comment only published events");
        }
        if (!event.getIsCommentPermitted()) {
            log.error("Инициатор события ограничил возможность комментирования");
            throw new ConditionsViolationException("Comments are not permitted by initiator");
        }

        User author = userService.findById(userId);

        Comment comment = Comment.builder()
                .text(request.getText())
                .author(author)
                .event(event)
                .createdOn(LocalDateTime.now())
                .isUpdated(false)
                .build();
        commentsRepository.save(comment);
        log.info("Комментарий с id={} сохранён: {}", comment.getId(), comment.getText());

        return commentsConverter.toFull(comment);
    }

    @Override
    public CommentFullDto getCommentByIdPublic(String comId) {
        Comment comment = findById(comId);
        setLikes(comment);

        return commentsConverter.toFull(comment);
    }

    @Override
    public CommentAdminDto getCommentByIdAdmin(String comId) {
        Comment comment = findById(comId);
        setLikes(comment);
        setComplaints(comment);

        return commentsConverter.toAdmin(comment);
    }

    @Override
    public List<CommentFullDto> getCommentsByEventPublic(Integer eventId, CommentsSort sort, Integer from, Integer size) {
        if (!eventsService.contains(eventId)) {
            log.error("Событие с id={} не найдено", eventId);
            throw new NotFoundException(String.format("Event with id=%d was not found", eventId));
        }

        List<Comment> comments;
        switch (sort) {
            case BY_LIKES:

                return processLikeSortByEvent(eventId, from, size).stream().map(commentsConverter::toFull).toList();
            case BY_CREATION_DATE_ASC:
                comments = commentsRepository.findAllByEventIdOrderByCreatedOn(eventId, PageRequest.of(from, size));

                List<CommentFullDto> dtos = setLikes(comments)
                        .stream()
                        .map(commentsConverter::toFull)
                        .collect(Collectors.toList());

                return dtos;
            case BY_CREATION_DATE_DESC:
                comments = commentsRepository.findAllByEventIdOrderByCreatedOnDesc(eventId, PageRequest.of(from, size));

                return setLikes(comments)
                        .stream()
                        .map(commentsConverter::toFull)
                        .collect(Collectors.toList());
            default:
                throw new BadRequestException("Sort type is unprocessed");
        }
    }

    @Override
    public List<CommentShortDto> getAllUsersCommentsPrivate(Integer userId, CommentsSort sort, Integer from, Integer size) {
        List<Comment> comments;
        switch (sort) {
            case BY_LIKES:

                return processLikeSortByUser(userId, from, size).stream().map(commentsConverter::toShort).toList();
            case BY_CREATION_DATE_ASC:
                comments = commentsRepository.findAllByAuthorIdOrderByCreatedOn(userId, PageRequest.of(from, size));

                List<CommentShortDto> dtos = setLikes(comments)
                        .stream()
                        .map(commentsConverter::toShort)
                        .collect(Collectors.toList());

                return dtos;
            case BY_CREATION_DATE_DESC:
                comments = commentsRepository.findAllByAuthorIdOrderByCreatedOnDesc(userId, PageRequest.of(from, size));

                return setLikes(comments)
                        .stream()
                        .map(commentsConverter::toShort)
                        .collect(Collectors.toList());
            default:
                throw new BadRequestException("Sort type is unprocessed");
        }
    }

    @Override
    public List<CommentAdminDto> getAllUsersCommentsAdmin(Integer userId, Integer from, Integer size) {
        List<Comment> comments = commentsRepository.findAllByAuthorIdOrderByCreatedOnDesc(userId, PageRequest.of(from, size));

        if (comments.isEmpty()) {
            return List.of();
        }

        setLikes(comments);
        setComplaints(comments);

        return comments.stream().map(commentsConverter::toAdmin).toList();
    }

    @Override
    public List<CommentAdminDto> getCommentsForReview(Integer from, Integer size) {
        Pageable pageable = PageRequest.of(from, size);
        List<Comment> comments = reviewRepository.findAllOrderByComplaintsCountDesc(pageable)
                .stream()
                .map(query -> {
                    long complaintsCount = query.getComplaintsCount();
                    Comment comment = query.getComment();
                    comment.setComplaints(complaintsCount);

                    return comment;
                })
                .collect(Collectors.toList());

        setLikes(comments);

        return comments.stream().map(commentsConverter::toAdmin).toList();
    }

    @Override
    public CommentFullDto updateUsersCommentPrivate(UpdateCommentDto request, Integer userId) {
        Comment comment = findById(request.getCommentId());
        if (!comment.getAuthor().getId().equals(userId)) {
            log.error("Обновить комментарий может только его автор");
            throw new ConditionsViolationException("Update comment operation is allowed only for comment's author");
        }

        comment.setText(request.getText());
        comment.setIsUpdated(true);
        setLikes(comment);

        commentsRepository.save(comment);
        log.info("Комментарий с id={} успешно обновлён: {}", request.getCommentId(), request.getText());

        return commentsConverter.toFull(comment);
    }

    @Override
    public void disableCommentsOnEvent(Integer userId, Integer eventId) {
        Event event = eventsService.findById(eventId);
        if (!event.getInitiator().getId().equals(userId)) {
            log.error("Запретить комментирование события может только его инициатор: пользователь с id={} не является создателем события с id={}", userId, eventId);
            throw new ConditionsViolationException("Only initiator of the event can disable comments");
        }

        eventsService.disableCommentsOnEvent(event);
    }

    @Override
    public void enableCommentsOnEvent(Integer userId, Integer eventId) {
        Event event = eventsService.findById(eventId);
        if (!event.getInitiator().getId().equals(userId)) {
            log.error("Разрешить комментирование события может только его инициатор: пользователь с id={} не является создателем события с id={}", userId, eventId);
            throw new ConditionsViolationException("Only initiator of the event can disable comments");
        }

        eventsService.enableCommentsOnEvent(event);
    }

    @Override
    public void submitCommentForAdminModeration(Integer userId, String comId) {
        Optional<Complaint> maybeComplaint = complaintsRepository.findByUserIdAndCommentId(userId, comId);

        if (maybeComplaint.isPresent()) {
            log.error("Пометить комментарий как нежелательный можно только один раз: пользователь с id={} уже отправил заявку на модерацию для комментария с id={}", userId, comId);
            throw new EntityExistsException("User's complaint has been already registered");
        }

        User user = userService.findById(userId);
        Comment comment = findById(comId);
        Complaint complaint = Complaint.builder().user(user).comment(comment).build();

        complaintsRepository.save(complaint);

        int complaintsCount = complaintsRepository.countByCommentId(comId);
        if (complaintsCount >= COMPLAINTS_COUNT_FOR_REVIEW) {
            Optional<Review> maybeReview = reviewRepository.findByCommentId(comment.getId());
            if (maybeReview.isPresent()) {
                Review review = maybeReview.get();
                review.setComplaintsCount(complaintsCount);

                reviewRepository.save(review);
            } else {
                Review review = Review.builder().comment(comment).complaintsCount(complaintsCount).build();
                reviewRepository.save(review);
            }
        }
    }

    @Override
    public CommentFullDto likeComment(Integer userId, String comId) {
        Comment comment = findById(comId);

        Optional<Like> maybeLike = likesRepository.findByUserIdAndCommentId(userId, comId);
        if (maybeLike.isEmpty()) {
            User user = userService.findById(userId);
            Like like = Like.builder().comment(comment).user(user).build();

            likesRepository.save(like);
        } else {
            likesRepository.delete(maybeLike.get());
        }
        setLikes(comment);

        return commentsConverter.toFull(comment);
    }

    @Override
    public void deleteCommentByIdPrivate(Integer userId, String comId) {
        Comment comment = findById(comId);
        if (!comment.getAuthor().getId().equals(userId)) {
            log.error("Удалить комментарий может только его создатель: пользователь с id={} не является автором комментария с id={}", userId, comId);
            throw new ConditionsViolationException("Delete comment operation is allowed only for comment's author");
        }

        commentsRepository.delete(comment);
    }

    @Override
    public void deleteCommentByIdAdmin(String comId) {
        Comment comment = findById(comId);
        commentsRepository.delete(comment);
    }

    @Override
    public void deleteAllUsersCommentsAdmin(Integer userId) {
        List<Comment> comments = commentsRepository.findAllByAuthorId(userId); //page request all
        commentsRepository.deleteAll(comments);
    }

    private Comment findById(String comId) {
        return commentsRepository.findById(comId).orElseThrow(
                () -> new NotFoundException(String.format("Comment with id=%s was not found", comId))
        );
    }

    private List<Comment> processLikeSortByUser(Integer userId, Integer from, Integer size) {
        Pageable pageable = PageRequest.of(from, size);
        ArrayList<Comment> likedCommentsTop = likesRepository.findAllByCommentAuthorIdOrderByLikes(userId, pageable)
                .stream()
                .map(query -> {
                    long likesCount = query.getLikesCount();
                    Comment comment = query.getComment();
                    comment.setLikes(likesCount);

                    return comment;
                }).collect(Collectors.toCollection(ArrayList::new));

        if (likedCommentsTop.isEmpty()) {
            return commentsRepository.findAllByAuthorIdOrderByCreatedOnDesc(userId, pageable)
                    .stream()
                    .peek(dto -> dto.setLikes(0L))
                    .toList();
        }

        int remainSize = size - likedCommentsTop.size();
        if (remainSize > 0) {
            Pageable remainPageable = PageRequest.of(0, remainSize);

            List<String> ids = likedCommentsTop.stream().map(Comment::getId).toList();
            commentsRepository.findAllByAuthorIdOrderByCreatedOnDesc(userId, remainPageable)
                    .stream()
                    .filter(comment -> !ids.contains(comment.getId()))
                    .peek(comment -> comment.setLikes(0L))
                    .forEach(likedCommentsTop::add);
        }

        return likedCommentsTop;
    }

    private List<Comment> processLikeSortByEvent(Integer eventId, Integer from, Integer size) {
        Pageable pageable = PageRequest.of(from, size);
        ArrayList<Comment> likedCommentsTop = likesRepository.findAllByCommentEventIdOrderByLikes(eventId, pageable)
                .stream()
                .map(query -> {
                    long likesCount = query.getLikesCount();
                    Comment comment = query.getComment();
                    comment.setLikes(likesCount);

                    return comment;
                }).collect(Collectors.toCollection(ArrayList::new));

        if (likedCommentsTop.isEmpty()) {
            return commentsRepository.findAllByEventIdOrderByCreatedOnDesc(eventId, pageable)
                    .stream()
                    .peek(dto -> dto.setLikes(0L))
                    .toList();
        }

        int remainSize = size - likedCommentsTop.size();
        if (remainSize > 0) {
            Pageable remainPageable = PageRequest.of(0, remainSize);

            List<String> ids = likedCommentsTop.stream().map(Comment::getId).toList();
            commentsRepository.findAllByEventIdOrderByCreatedOnDesc(eventId, remainPageable)
                    .stream()
                    .filter(comment -> !ids.contains(comment.getId()))
                    .peek(comment -> comment.setLikes(0L))
                    .forEach(likedCommentsTop::add);
        }

        return likedCommentsTop;
    }

    private List<Comment> setLikes(List<Comment> comments) {
        ArrayList<String> ids = comments.stream().map(Comment::getId).collect(Collectors.toCollection(ArrayList::new));
        Map<String, Long> likesForComments = likesRepository.findAllByCommentIdIn(ids)
                .stream()
                .collect(Collectors.toMap(like -> like.getComment().getId(), LikesQueryResult::getLikesCount));

        for (Comment comment : comments) {
            String id = comment.getId();
            comment.setLikes(likesForComments.getOrDefault(id, 0L));
        }

        return comments;
    }

    private Comment setLikes(Comment comment) {
        Optional<LikesQueryResult> maybeLikes = likesRepository.findByCommentId(comment.getId());
        if (maybeLikes.isPresent()) {
            Long likesCount = maybeLikes.get().getLikesCount();
            comment.setLikes(likesCount);
        } else {
            comment.setLikes(0L);
        }

        return comment;
    }

    private List<Comment> setComplaints(List<Comment> comments) {
        ArrayList<String> ids = comments.stream().map(Comment::getId).collect(Collectors.toCollection(ArrayList::new));
        Map<String, Long> complaintsForComments = complaintsRepository.findAllByCommentIdIn(ids)
                .stream()
                .collect(Collectors.toMap(comp -> comp.getComment().getId(), ComplaintsQueryResult::getComplaintsCount));

        for (Comment comment : comments) {
            String id = comment.getId();
            comment.setComplaints(complaintsForComments.getOrDefault(id, 0L));
        }

        return comments;
    }

    private Comment setComplaints(Comment comment) {
        Optional<ComplaintsQueryResult> maybeComplaints = complaintsRepository.findByCommentId(comment.getId());
        if (maybeComplaints.isPresent()) {
            Long complaintsCount = maybeComplaints.get().getComplaintsCount();
            comment.setComplaints(complaintsCount);
        } else {
            comment.setLikes(0L);
        }

        return comment;
    }

}
