package ru.practicum.ewm.comment.repository.complaints;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import ru.practicum.ewm.comment.model.Review;

import java.util.List;
import java.util.Optional;

public interface ReviewRepository extends JpaRepository<Review, Integer> {

    Optional<Review> findByCommentId(String comId);

    @Query("""
            select r from Review r order by r.complaintsCount DESC
            """)
    List<Review> findAllOrderByComplaintsCountDesc(Pageable pageable);

}
