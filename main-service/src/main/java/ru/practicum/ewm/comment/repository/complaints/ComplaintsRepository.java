package ru.practicum.ewm.comment.repository.complaints;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import ru.practicum.ewm.comment.model.Complaint;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ComplaintsRepository extends JpaRepository<Complaint, Integer> {

    Optional<Complaint> findByUserIdAndCommentId(int userId, UUID comId);

    @Query("""
            select new ru.practicum.ewm.comment.repository.complaints.ComplaintsQueryDto(c.comment, count(c.user.id)) from Complaint c
            where c.comment.id in :ids
            group by c.comment
            """)
    List<ComplaintsQueryDto> findAllByCommentIdIn(List<UUID> ids);

    @Query("""
            select new ru.practicum.ewm.comment.repository.complaints.ComplaintsQueryDto(c.comment, count(c.user.id)) from Complaint c
            where c.comment.id = :comId
            group by c.comment
            """)
    Optional<ComplaintsQueryDto> findByCommentId(UUID comId);

    Integer countByCommentId(UUID comId);

}
