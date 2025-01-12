package ru.practicum.ewm.comment.converter;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.util.UUID;

@Converter
public class UUIDConverter implements AttributeConverter<String, UUID> {
    @Override
    public UUID convertToDatabaseColumn(String s) {
        return UUID.fromString(s);
    }

    @Override
    public String convertToEntityAttribute(UUID u) {
        return u.toString();
    }
}