package ru.practicum.shareit.booking;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import ru.practicum.shareit.booking.dto.BookingCreateDto;
import ru.practicum.shareit.booking.dto.BookingState;
import ru.practicum.shareit.exception.ErrorHandler;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class BookingControllerTest {
    private static final String USER_ID_HEADER = "X-Sharer-User-Id";

    @Mock
    private BookingClient bookingClient;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new BookingController(bookingClient))
                .setControllerAdvice(new ErrorHandler())
                .build();
    }

    @Test
    void createShouldForwardValidBooking() throws Exception {
        LocalDateTime start = LocalDateTime.now().plusDays(1).withNano(0);
        LocalDateTime end = start.plusHours(2);
        when(bookingClient.create(eq(2L), any(BookingCreateDto.class)))
                .thenReturn(ResponseEntity.ok(Map.of("id", 1, "status", "WAITING")));

        mockMvc.perform(post("/bookings")
                        .header(USER_ID_HEADER, 2)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"itemId\":1,\"start\":\"%s\",\"end\":\"%s\"}"
                                .formatted(start, end)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("WAITING"));

        verify(bookingClient).create(eq(2L), any(BookingCreateDto.class));
    }

    @Test
    void createShouldRejectReversedDateRangeBeforeCallingClient() throws Exception {
        LocalDateTime start = LocalDateTime.now().plusDays(2).withNano(0);
        LocalDateTime end = start.minusHours(1);

        mockMvc.perform(post("/bookings")
                        .header(USER_ID_HEADER, 2)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"itemId\":1,\"start\":\"%s\",\"end\":\"%s\"}"
                                .formatted(start, end)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Invalid request"));

        verifyNoInteractions(bookingClient);
    }

    @Test
    void getAllShouldAcceptCaseInsensitiveState() throws Exception {
        when(bookingClient.getAllByBooker(2L, BookingState.CURRENT))
                .thenReturn(ResponseEntity.ok(List.of()));

        mockMvc.perform(get("/bookings")
                        .header(USER_ID_HEADER, 2)
                        .param("state", "current"))
                .andExpect(status().isOk());

        verify(bookingClient).getAllByBooker(2L, BookingState.CURRENT);
    }

    @Test
    void getAllShouldRejectUnknownStateBeforeCallingClient() throws Exception {
        mockMvc.perform(get("/bookings")
                        .header(USER_ID_HEADER, 2)
                        .param("state", "SOMETHING"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Unknown state: SOMETHING"));

        verifyNoInteractions(bookingClient);
    }
}
