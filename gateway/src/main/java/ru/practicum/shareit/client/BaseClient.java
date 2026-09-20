package ru.practicum.shareit.client;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.List;
import java.util.Map;

public class BaseClient {
    protected final RestClient rest;

    public BaseClient(RestClient rest) {
        this.rest = rest;
    }

    protected ResponseEntity<Object> get(String path) {
        return get(path, null, null);
    }

    protected ResponseEntity<Object> get(String path, Long userId) {
        return get(path, userId, null);
    }

    protected ResponseEntity<Object> get(String path, Long userId,
                                         Map<String, Object> parameters) {
        return makeAndSendRequest(HttpMethod.GET, path, userId, parameters, null);
    }

    protected <T> ResponseEntity<Object> post(String path, T body) {
        return post(path, null, null, body);
    }

    protected <T> ResponseEntity<Object> post(String path, Long userId, T body) {
        return post(path, userId, null, body);
    }

    protected <T> ResponseEntity<Object> post(String path, Long userId,
                                              Map<String, Object> parameters, T body) {
        return makeAndSendRequest(HttpMethod.POST, path, userId, parameters, body);
    }

    protected <T> ResponseEntity<Object> put(String path, Long userId, T body) {
        return put(path, userId, null, body);
    }

    protected <T> ResponseEntity<Object> put(String path, Long userId,
                                             Map<String, Object> parameters, T body) {
        return makeAndSendRequest(HttpMethod.PUT, path, userId, parameters, body);
    }

    protected <T> ResponseEntity<Object> patch(String path, T body) {
        return patch(path, null, null, body);
    }

    protected <T> ResponseEntity<Object> patch(String path, Long userId, T body) {
        return patch(path, userId, null, body);
    }

    protected <T> ResponseEntity<Object> patch(String path, Long userId,
                                               Map<String, Object> parameters, T body) {
        return makeAndSendRequest(HttpMethod.PATCH, path, userId, parameters, body);
    }

    protected ResponseEntity<Object> delete(String path) {
        return delete(path, null, null);
    }

    protected ResponseEntity<Object> delete(String path, Long userId) {
        return delete(path, userId, null);
    }

    protected ResponseEntity<Object> delete(String path, Long userId,
                                            Map<String, Object> parameters) {
        return makeAndSendRequest(HttpMethod.DELETE, path, userId, parameters, null);
    }

    private <T> ResponseEntity<Object> makeAndSendRequest(HttpMethod method, String path,
                                                          Long userId,
                                                          Map<String, Object> parameters,
                                                          T body) {
        try {
            RestClient.RequestBodySpec request = parameters == null
                    ? rest.method(method).uri(path)
                    : rest.method(method).uri(path, parameters);
            request.headers(headers -> headers.addAll(defaultHeaders(userId)));
            if (body != null) {
                request.body(body);
            }
            ResponseEntity<Object> response = request.retrieve().toEntity(Object.class);
            return prepareGatewayResponse(response);
        } catch (RestClientResponseException exception) {
            ResponseEntity.BodyBuilder responseBuilder = ResponseEntity.status(exception.getStatusCode());
            HttpHeaders responseHeaders = exception.getResponseHeaders();
            if (responseHeaders != null && responseHeaders.getContentType() != null) {
                responseBuilder.contentType(responseHeaders.getContentType());
            }
            return responseBuilder.body(exception.getResponseBodyAsByteArray());
        }
    }

    private HttpHeaders defaultHeaders(Long userId) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        if (userId != null) {
            headers.set("X-Sharer-User-Id", String.valueOf(userId));
        }
        return headers;
    }

    private static ResponseEntity<Object> prepareGatewayResponse(ResponseEntity<Object> response) {
        ResponseEntity.BodyBuilder responseBuilder = ResponseEntity.status(response.getStatusCode());
        if (response.getHeaders().getContentType() != null) {
            responseBuilder.contentType(response.getHeaders().getContentType());
        }
        if (response.hasBody()) {
            return responseBuilder.body(response.getBody());
        }
        return responseBuilder.build();
    }
}
