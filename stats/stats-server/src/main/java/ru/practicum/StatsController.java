package ru.practicum;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.practicum.dto.ReturnStatsDto;
import ru.practicum.dto.SaveStatsDto;
import ru.practicum.service.StatsServiceImpl;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@Slf4j
@Validated
@RequiredArgsConstructor
public class StatsController {
    private final StatsServiceImpl service;

    @PostMapping(path = "/hit")
    @ResponseStatus(value = HttpStatus.CREATED, reason = "Информация сохранена")
    public void saveStats(@RequestBody SaveStatsDto request) {
        service.saveStats(request);
    }

    @GetMapping(path = "/stats")
    @ResponseStatus(value = HttpStatus.OK, reason = "Статистика собрана")
    public List<ReturnStatsDto> getStats(@RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime start,
                                         @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime end,
                                         @RequestParam(defaultValue = "", required = false) List<String> uris,
                                         @RequestParam(defaultValue = "false", required = false) boolean unique) {

        return service.getStats(start, end, uris, unique);
    }

}
