package ru.practicum.ewm.comment.repository.likes;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import ru.practicum.ewm.comment.model.Like;

import java.util.List;
import java.util.Optional;

public interface LikesRepository extends JpaRepository<Like, Integer> {

    @Query("""
            select new ru.practicum.ewm.comment.repository.likes.LikesQueryResult(l.comment, count(l.user.id)) from Like l
            where l.comment.author.id = :userId
            group by l.comment
            order by count(l.user.id) DESC
            """)
    List<LikesQueryResult> findAllByCommentAuthorIdOrderByLikes(int userId, Pageable pageable);

    @Query("""
            select new ru.practicum.ewm.comment.repository.likes.LikesQueryResult(l.comment, count(l.comment.event.id)) from Like l
            where l.comment.event.id = :eventId
            group by l.comment
            order by count(l.comment.event.id) DESC
            """)
    List<LikesQueryResult> findAllByCommentEventIdOrderByLikes(int eventId, Pageable pageable);

    @Query("""
            select new ru.practicum.ewm.comment.repository.likes.LikesQueryResult(l.comment, count(l.user.id)) from Like l
            where l.comment.id in :ids
            group by l.comment
            """)
    List<LikesQueryResult> findAllByCommentIdIn(List<String> ids);

    @Query("""
            select new ru.practicum.ewm.comment.repository.likes.LikesQueryResult(l.comment, count(l.user.id)) from Like l
            where l.comment.id = :id
            group by l.comment
            """)
    Optional<LikesQueryResult> findByCommentId(String id);

    Optional<Like> findByUserIdAndCommentId(int userId, String comId);

}
