package ru.practicum.dao;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import ru.practicum.dto.ReturnStatsDto;
import ru.practicum.model.Action;

import java.time.LocalDateTime;
import java.util.List;

public interface StatsRepository extends JpaRepository<Action, Long> {

    @Query("select new ru.practicum.dto.ReturnStatsDto(a.app, a.uri, count(a.ip)) " +
            "from Action as a " +
            "where a.timestamp between :start and :end " +
            "group by a.app, a.uri " +
            "order by count(a.ip) desc")
    List<ReturnStatsDto> getStatsByTimestampBetween(LocalDateTime start, LocalDateTime end);

    @Query("select new ru.practicum.dto.ReturnStatsDto(a.app, a.uri, count(a.ip)) " +
            "from Action as a where a.timestamp between :start and :end " +
            "and a.uri in :uris " +
            "group by a.app, a.uri " +
            "order by count(a.ip) desc")
    List<ReturnStatsDto> getStatsByTimestampBetweenAndUriIn(LocalDateTime start, LocalDateTime end, List<String> uris);

    @Query("select new ru.practicum.dto.ReturnStatsDto(a.app, a.uri, count(DISTINCT a.ip)) " +
            "from Action as a " +
            "where a.timestamp between :start and :end " +
            "group by a.app, a.uri " +
            "order by count(DISTINCT a.ip) desc")
    List<ReturnStatsDto> getStatsByTimestampBetweenAndIpUnique(LocalDateTime start, LocalDateTime end);

    @Query("select new ru.practicum.dto.ReturnStatsDto(a.app, a.uri, count(DISTINCT a.ip)) " +
            "from Action as a where a.timestamp between :start and :end " +
            "and a.uri in :uris " +
            "group by a.app, a.uri " +
            "order by count(DISTINCT a.ip) desc")
    List<ReturnStatsDto> getStatsByTimestampBetweenAndUriInAndIpUnique(LocalDateTime start, LocalDateTime end, List<String> uris);

}
