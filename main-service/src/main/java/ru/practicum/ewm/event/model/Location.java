package ru.practicum.ewm.event.model;

import jakarta.persistence.Embeddable;
import lombok.Data;

@Data
@Embeddable
public class Location {
    private Float lat;
    private Float lon;
}