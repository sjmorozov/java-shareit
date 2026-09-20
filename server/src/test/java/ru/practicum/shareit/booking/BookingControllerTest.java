package ru.practicum.shareit.booking;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import ru.practicum.shareit.booking.dto.BookingCreateDto;
import ru.practicum.shareit.booking.dto.BookingDto;
import ru.practicum.shareit.booking.service.BookingService;
import ru.practicum.shareit.exception.ErrorHandler;
import ru.practicum.shareit.item.dto.ItemDto;
import ru.practicum.shareit.user.dto.UserDto;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class BookingControllerTest {
    private static final String USER_ID_HEADER = "X-Sharer-User-Id";

    @Mock
    private BookingService bookingService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new BookingController(bookingService))
                .setControllerAdvice(new ErrorHandler())
                .build();
    }

    @Test
    void createShouldDelegateToService() throws Exception {
        LocalDateTime start = LocalDateTime.now().plusDays(1).withNano(0);
        LocalDateTime end = start.plusHours(2);
        when(bookingService.create(eq(3L), any(BookingCreateDto.class)))
                .thenReturn(bookingDto(1L, start, end, BookingStatus.WAITING));

        mockMvc.perform(post("/bookings")
                        .header(USER_ID_HEADER, 3)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"itemId\":1,\"start\":\"%s\",\"end\":\"%s\"}"
                                .formatted(start, end)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("WAITING"));

        verify(bookingService).create(eq(3L), argThat(booking ->
                Long.valueOf(1L).equals(booking.getItemId()) && start.equals(booking.getStart())));
    }

    @Test
    void updateStatusShouldDelegateToService() throws Exception {
        LocalDateTime start = LocalDateTime.now().plusDays(1);
        when(bookingService.updateStatus(2L, 1L, true))
                .thenReturn(bookingDto(1L, start, start.plusHours(1), BookingStatus.APPROVED));

        mockMvc.perform(patch("/bookings/1")
                        .header(USER_ID_HEADER, 2)
                        .param("approved", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"));

        verify(bookingService).updateStatus(2L, 1L, true);
    }

    @Test
    void getByIdShouldDelegateToService() throws Exception {
        LocalDateTime start = LocalDateTime.now().plusDays(1);
        when(bookingService.getById(3L, 1L))
                .thenReturn(bookingDto(1L, start, start.plusHours(1), BookingStatus.WAITING));

        mockMvc.perform(get("/bookings/1").header(USER_ID_HEADER, 3))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));

        verify(bookingService).getById(3L, 1L);
    }

    @Test
    void getAllByBookerShouldDelegateStateToService() throws Exception {
        when(bookingService.getAllByBooker(3L, BookingState.FUTURE)).thenReturn(List.of());

        mockMvc.perform(get("/bookings")
                        .header(USER_ID_HEADER, 3)
                        .param("state", "FUTURE"))
                .andExpect(status().isOk());

        verify(bookingService).getAllByBooker(3L, BookingState.FUTURE);
    }

    @Test
    void getAllByOwnerShouldDelegateStateToService() throws Exception {
        when(bookingService.getAllByOwner(2L, BookingState.ALL)).thenReturn(List.of());

        mockMvc.perform(get("/bookings/owner").header(USER_ID_HEADER, 2))
                .andExpect(status().isOk());

        verify(bookingService).getAllByOwner(2L, BookingState.ALL);
    }

    private BookingDto bookingDto(Long id, LocalDateTime start, LocalDateTime end,
                                  BookingStatus status) {
        ItemDto item = new ItemDto(1L, "Drill", "Cordless", true,
                null, null, null, List.of());
        UserDto booker = new UserDto(3L, "Booker", "booker@example.com");
        return new BookingDto(id, start, end, item, booker, status);
    }
}
