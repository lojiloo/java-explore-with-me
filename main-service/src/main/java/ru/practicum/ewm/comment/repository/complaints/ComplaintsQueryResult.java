package ru.practicum.ewm.comment.repository.complaints;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import ru.practicum.ewm.comment.model.Comment;

@Setter
@Getter
@ToString
@AllArgsConstructor
public class ComplaintsQueryResult {
    Comment comment;
    Long complaintsCount;
}
