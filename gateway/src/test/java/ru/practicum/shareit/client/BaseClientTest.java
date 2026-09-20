package ru.practicum.shareit.client;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class BaseClientTest {
    private HttpServer server;
    private TestClient client;
    private RequestRecord lastRequest;

    @BeforeEach
    void setUp() throws IOException {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/", this::handleRequest);
        server.start();

        String serverUrl = "http://localhost:" + server.getAddress().getPort();
        client = new TestClient(serverUrl);
    }

    @AfterEach
    void tearDown() {
        server.stop(0);
    }

    @Test
    void shouldSendEverySupportedRequestVariant() {
        assertThat(client.getSimple("/get").getStatusCode()).isEqualTo(HttpStatus.OK);
        assertRequest("GET", "/get", null);

        client.getForUser("/get-user", 1L);
        assertRequest("GET", "/get-user", "1");

        client.getWithParameters("/get-param?value={value}", 2L, Map.of("value", "found"));
        assertRequest("GET", "/get-param?value=found", "2");

        client.postSimple("/post", Map.of("name", "body"));
        assertRequest("POST", "/post", null);

        client.postForUser("/post-user", 3L, Map.of("name", "body"));
        assertRequest("POST", "/post-user", "3");

        client.postWithParameters("/post-param?value={value}", 4L,
                Map.of("value", "saved"), Map.of("name", "body"));
        assertRequest("POST", "/post-param?value=saved", "4");

        client.putForUser("/put", 5L, Map.of("name", "body"));
        assertRequest("PUT", "/put", "5");

        client.putWithParameters("/put-param?value={value}", 6L,
                Map.of("value", "updated"), Map.of("name", "body"));
        assertRequest("PUT", "/put-param?value=updated", "6");

        client.patchSimple("/patch", Map.of("name", "body"));
        assertRequest("PATCH", "/patch", null);

        client.patchForUser("/patch-user", 7L, Map.of("name", "body"));
        assertRequest("PATCH", "/patch-user", "7");

        client.patchWithParameters("/patch-param?value={value}", 8L,
                Map.of("value", "patched"), null);
        assertRequest("PATCH", "/patch-param?value=patched", "8");

        client.deleteSimple("/delete");
        assertRequest("DELETE", "/delete", null);

        client.deleteForUser("/delete-user", 9L);
        assertRequest("DELETE", "/delete-user", "9");

        client.deleteWithParameters("/delete-param?value={value}", 10L, Map.of("value", "removed"));
        assertRequest("DELETE", "/delete-param?value=removed", "10");
    }

    @Test
    void shouldPreserveSuccessfulAndErrorResponses() {
        ResponseEntity<Object> success = client.getSimple("/success");
        ResponseEntity<Object> noContent = client.getSimple("/no-content");
        ResponseEntity<Object> badRequest = client.getSimple("/bad-request");
        ResponseEntity<Object> notFound = client.getSimple("/not-found");

        assertThat(success.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(success.getHeaders().getContentType()).hasToString("application/json");
        assertThat(success.getBody()).isInstanceOf(Map.class);
        assertThat(noContent.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(noContent.hasBody()).isFalse();
        assertThat(badRequest.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(badRequest.getHeaders().getContentType()).hasToString("application/json");
        assertThat(badRequest.getBody()).isInstanceOf(byte[].class);
        assertThat(notFound.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    private void assertRequest(String method, String uri, String userId) {
        assertThat(lastRequest.method()).isEqualTo(method);
        assertThat(lastRequest.uri()).isEqualTo(uri);
        assertThat(lastRequest.userId()).isEqualTo(userId);
    }

    private void handleRequest(HttpExchange exchange) throws IOException {
        lastRequest = new RequestRecord(
                exchange.getRequestMethod(),
                exchange.getRequestURI().toString(),
                exchange.getRequestHeaders().getFirst("X-Sharer-User-Id")
        );

        String path = exchange.getRequestURI().getPath();
        if ("/no-content".equals(path)) {
            exchange.sendResponseHeaders(204, -1);
        } else if ("/bad-request".equals(path)) {
            sendJson(exchange, 400, "{\"error\":\"bad request\"}");
        } else if ("/not-found".equals(path)) {
            exchange.sendResponseHeaders(404, -1);
        } else {
            sendJson(exchange, 200, "{\"result\":\"ok\"}");
        }
        exchange.close();
    }

    private void sendJson(HttpExchange exchange, int status, String response) throws IOException {
        byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(status, bytes.length);
        exchange.getResponseBody().write(bytes);
    }

    private record RequestRecord(String method, String uri, String userId) {
    }

    private static class TestClient extends BaseClient {
        TestClient(String serverUrl) {
            super(RestClient.builder()
                    .baseUrl(serverUrl)
                    .requestFactory(new HttpComponentsClientHttpRequestFactory())
                    .build());
        }

        ResponseEntity<Object> getSimple(String path) {
            return get(path);
        }

        ResponseEntity<Object> getForUser(String path, Long userId) {
            return get(path, userId);
        }

        ResponseEntity<Object> getWithParameters(String path, Long userId, Map<String, Object> parameters) {
            return get(path, userId, parameters);
        }

        ResponseEntity<Object> postSimple(String path, Object body) {
            return post(path, body);
        }

        ResponseEntity<Object> postForUser(String path, Long userId, Object body) {
            return post(path, userId, body);
        }

        ResponseEntity<Object> postWithParameters(String path, Long userId,
                                                  Map<String, Object> parameters, Object body) {
            return post(path, userId, parameters, body);
        }

        ResponseEntity<Object> putForUser(String path, Long userId, Object body) {
            return put(path, userId, body);
        }

        ResponseEntity<Object> putWithParameters(String path, Long userId,
                                                 Map<String, Object> parameters, Object body) {
            return put(path, userId, parameters, body);
        }

        ResponseEntity<Object> patchSimple(String path, Object body) {
            return patch(path, body);
        }

        ResponseEntity<Object> patchForUser(String path, Long userId, Object body) {
            return patch(path, userId, body);
        }

        ResponseEntity<Object> patchWithParameters(String path, Long userId,
                                                   Map<String, Object> parameters, Object body) {
            return patch(path, userId, parameters, body);
        }

        ResponseEntity<Object> deleteSimple(String path) {
            return delete(path);
        }

        ResponseEntity<Object> deleteForUser(String path, Long userId) {
            return delete(path, userId);
        }

        ResponseEntity<Object> deleteWithParameters(String path, Long userId, Map<String, Object> parameters) {
            return delete(path, userId, parameters);
        }
    }
}
