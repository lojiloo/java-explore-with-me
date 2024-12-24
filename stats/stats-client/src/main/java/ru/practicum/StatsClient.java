package ru.practicum;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import ru.practicum.dto.ReturnStatsDto;
import ru.practicum.dto.SaveStatsDto;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

@Component
public class StatsClient {
    private static final String STATS_URI = "http://localhost:9090";
    protected final WebClient client;

    @Autowired
    public StatsClient() {
        this.client = WebClient.create(STATS_URI);
    }

    public SaveStatsDto saveStats(SaveStatsDto body) {
        return client.post()
                .uri("/hit")
                .contentType(MediaType.APPLICATION_JSON)
                .retrieve()
                .bodyToMono(SaveStatsDto.class)
                .block();
    }

    public List<ReturnStatsDto> getStats(LocalDateTime start, LocalDateTime end, List<String> uris, boolean unique) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd hh:mm:ss");
        String startEncoded = URLEncoder.encode(start.format(formatter), StandardCharsets.UTF_8);
        String endEncoded = URLEncoder.encode(end.format(formatter), StandardCharsets.UTF_8);

        return client.get()
                .uri(builder -> builder.path("/stats")
                        .queryParam("start", startEncoded)
                        .queryParam("end", endEncoded)
                        .queryParamIfPresent("uris", Optional.of(uris))
                        .queryParam("unique", unique).build())
                .retrieve()
                .bodyToFlux(ReturnStatsDto.class)
                .collectList()
                .block();
    }
}
