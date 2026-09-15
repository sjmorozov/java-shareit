package ru.practicum.shareit.client;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.practicum.shareit.booking.BookingClient;
import ru.practicum.shareit.booking.dto.BookingCreateDto;
import ru.practicum.shareit.booking.dto.BookingState;
import ru.practicum.shareit.item.ItemClient;
import ru.practicum.shareit.item.dto.CommentCreateDto;
import ru.practicum.shareit.item.dto.ItemDto;
import ru.practicum.shareit.request.RequestClient;
import ru.practicum.shareit.request.dto.ItemRequestCreateDto;
import ru.practicum.shareit.user.UserClient;
import ru.practicum.shareit.user.dto.UserDto;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class GatewayClientsTest {
    private HttpServer server;
    private String serverUrl;
    private final List<RequestRecord> requests = new ArrayList<>();

    @BeforeEach
    void setUp() throws IOException {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/", this::handleRequest);
        server.start();
        serverUrl = "http://localhost:" + server.getAddress().getPort();
    }

    @AfterEach
    void tearDown() {
        server.stop(0);
    }

    @Test
    void userClientShouldSendEveryRequestToUsersApi() {
        UserClient client = new UserClient(serverUrl);
        UserDto user = new UserDto(null, "User", "user@example.com");

        client.create(user);
        client.update(1L, user);
        client.getById(1L);
        client.getAll();
        client.delete(1L);

        assertThat(requests).extracting(RequestRecord::method)
                .containsExactly("POST", "PATCH", "GET", "GET", "DELETE");
        assertThat(requests).extracting(RequestRecord::uri)
                .containsExactly("/users", "/users/1", "/users/1", "/users", "/users/1");
        assertThat(requests.getFirst().body()).contains("user@example.com");
    }

    @Test
    void itemClientShouldSendEveryRequestToItemsApi() {
        ItemClient client = new ItemClient(serverUrl);
        ItemDto item = new ItemDto(null, "Drill", "Electric drill", true, 4L);
        CommentCreateDto comment = new CommentCreateDto("Useful item");

        client.create(2L, item);
        client.update(2L, 3L, item);
        client.getById(2L, 3L);
        client.getAllByOwnerId(2L);
        client.search(2L, "drill");
        client.addComment(2L, 3L, comment);

        assertThat(requests).extracting(RequestRecord::method)
                .containsExactly("POST", "PATCH", "GET", "GET", "GET", "POST");
        assertThat(requests).extracting(RequestRecord::uri)
                .containsExactly("/items", "/items/3", "/items/3", "/items",
                        "/items/search?text=drill", "/items/3/comment");
        assertThat(requests).extracting(RequestRecord::userId).containsOnly("2");
        assertThat(requests.getLast().body()).contains("Useful item");
    }

    @Test
    void bookingClientShouldSendEveryRequestToBookingsApi() {
        BookingClient client = new BookingClient(serverUrl);
        LocalDateTime start = LocalDateTime.now().plusDays(1);
        BookingCreateDto booking = new BookingCreateDto(3L, start, start.plusHours(1));

        client.create(2L, booking);
        client.updateStatus(2L, 4L, true);
        client.getById(2L, 4L);
        client.getAllByBooker(2L, BookingState.ALL);
        client.getAllByOwner(2L, BookingState.WAITING);

        assertThat(requests).extracting(RequestRecord::method)
                .containsExactly("POST", "PATCH", "GET", "GET", "GET");
        assertThat(requests).extracting(RequestRecord::uri)
                .containsExactly("/bookings", "/bookings/4?approved=true", "/bookings/4",
                        "/bookings?state=ALL", "/bookings/owner?state=WAITING");
        assertThat(requests).extracting(RequestRecord::userId).containsOnly("2");
    }

    @Test
    void requestClientShouldSendEveryRequestToRequestsApi() {
        RequestClient client = new RequestClient(serverUrl);
        ItemRequestCreateDto request = new ItemRequestCreateDto("Need a drill");

        client.create(2L, request);
        client.getOwn(2L);
        client.getAll(2L);
        client.getById(2L, 5L);

        assertThat(requests).extracting(RequestRecord::method)
                .containsExactly("POST", "GET", "GET", "GET");
        assertThat(requests).extracting(RequestRecord::uri)
                .containsExactly("/requests", "/requests", "/requests/all", "/requests/5");
        assertThat(requests).extracting(RequestRecord::userId).containsOnly("2");
        assertThat(requests.getFirst().body()).contains("Need a drill");
    }

    private void handleRequest(HttpExchange exchange) throws IOException {
        String requestBody = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        requests.add(new RequestRecord(
                exchange.getRequestMethod(),
                exchange.getRequestURI().toString(),
                exchange.getRequestHeaders().getFirst("X-Sharer-User-Id"),
                requestBody
        ));

        byte[] response = "{\"result\":\"ok\"}".getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(200, response.length);
        exchange.getResponseBody().write(response);
        exchange.close();
    }

    private record RequestRecord(String method, String uri, String userId, String body) {
    }
}
