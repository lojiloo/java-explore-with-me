package ru.practicum.ewm.comment.repository.comments;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import ru.practicum.ewm.comment.model.Comment;

import java.util.List;

public interface CommentsRepository extends JpaRepository<Comment, String> {

    List<Comment> findAllByAuthorId(int userId, PageRequest pageRequest);

    List<Comment> findAllByAuthorId(int userId);

    List<Comment> findAllByAuthorIdOrderByCreatedOnDesc(int userId, Pageable pageable);

    List<Comment> findAllByAuthorIdOrderByCreatedOn(int userId, Pageable pageable);

    List<Comment> findAllByEventIdOrderByCreatedOnDesc(int eventId, Pageable pageable);

    List<Comment> findAllByEventIdOrderByCreatedOn(int eventId, Pageable pageable);

    List<Comment> findAllByIdInOrderByCreatedOnDesc(List<String> ids);

}
