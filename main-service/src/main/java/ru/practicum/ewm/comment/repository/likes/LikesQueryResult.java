package ru.practicum.ewm.comment.repository.likes;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import ru.practicum.ewm.comment.model.Comment;

@Setter
@Getter
@ToString
@AllArgsConstructor
public class LikesQueryResult {
    Comment comment;
    Long likesCount;
}
