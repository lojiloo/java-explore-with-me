package ru.practicum.ewm.comment.converter;

import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;
import ru.practicum.ewm.comment.dto.CommentAdminDto;
import ru.practicum.ewm.comment.dto.CommentFullDto;
import ru.practicum.ewm.comment.dto.CommentShortDto;
import ru.practicum.ewm.comment.model.Comment;

@Component
public class CommentsConverter {
    private final ModelMapper mapper;

    public CommentsConverter() {
        this.mapper = new ModelMapper();
    }

    public CommentFullDto toFull(Comment comment) {
        return mapper.map(comment, CommentFullDto.class);
    }

    public CommentShortDto toShort(Comment comment) {
        return mapper.map(comment, CommentShortDto.class);
    }

    public CommentAdminDto toAdmin(Comment comment) {
        return mapper.map(comment, CommentAdminDto.class);
    }

}
