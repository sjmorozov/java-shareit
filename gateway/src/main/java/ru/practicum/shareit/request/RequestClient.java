package ru.practicum.shareit.request;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import ru.practicum.shareit.client.BaseClient;
import ru.practicum.shareit.request.dto.ItemRequestCreateDto;

@Service
public class RequestClient extends BaseClient {
    private static final String API_PREFIX = "/requests";

    public RequestClient(@Value("${shareit-server.url}") String serverUrl) {
        super(RestClient.builder()
                .baseUrl(serverUrl + API_PREFIX)
                .requestFactory(new HttpComponentsClientHttpRequestFactory())
                .build());
    }

    public ResponseEntity<Object> create(Long userId, ItemRequestCreateDto requestDto) {
        return post("", userId, requestDto);
    }

    public ResponseEntity<Object> getOwn(Long userId) {
        return get("", userId);
    }

    public ResponseEntity<Object> getAll(Long userId) {
        return get("/all", userId);
    }

    public ResponseEntity<Object> getById(Long userId, Long requestId) {
        return get("/" + requestId, userId);
    }
}
