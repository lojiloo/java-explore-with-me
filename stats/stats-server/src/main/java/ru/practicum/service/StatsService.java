package ru.practicum.service;

import ru.practicum.dto.ReturnStatsDto;
import ru.practicum.dto.SaveStatsDto;

import java.time.LocalDateTime;
import java.util.List;

public interface StatsService {

    void saveStats(SaveStatsDto request);

    List<ReturnStatsDto> getStats(LocalDateTime start, LocalDateTime end, List<String> uris, boolean unique);

}
