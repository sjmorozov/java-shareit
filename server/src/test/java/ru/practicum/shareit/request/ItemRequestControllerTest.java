package ru.practicum.shareit.request;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import ru.practicum.shareit.exception.ErrorHandler;
import ru.practicum.shareit.request.dto.ItemRequestCreateDto;
import ru.practicum.shareit.request.dto.ItemRequestDto;
import ru.practicum.shareit.request.service.ItemRequestService;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class ItemRequestControllerTest {
    private static final String USER_ID_HEADER = "X-Sharer-User-Id";

    @Mock
    private ItemRequestService requestService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new ItemRequestController(requestService))
                .setControllerAdvice(new ErrorHandler())
                .build();
    }

    @Test
    void createShouldDelegateToService() throws Exception {
        when(requestService.create(eq(3L), any(ItemRequestCreateDto.class)))
                .thenReturn(requestDto(1L));

        mockMvc.perform(post("/requests")
                        .header(USER_ID_HEADER, 3)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"description\":\"Need a drill\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));

        verify(requestService).create(eq(3L), argThat(request ->
                "Need a drill".equals(request.getDescription())));
    }

    @Test
    void getOwnShouldDelegateToService() throws Exception {
        when(requestService.getOwn(3L)).thenReturn(List.of(requestDto(1L)));

        mockMvc.perform(get("/requests").header(USER_ID_HEADER, 3))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1));

        verify(requestService).getOwn(3L);
    }

    @Test
    void getAllShouldDelegateToService() throws Exception {
        when(requestService.getAll(3L)).thenReturn(List.of(requestDto(1L)));

        mockMvc.perform(get("/requests/all").header(USER_ID_HEADER, 3))
                .andExpect(status().isOk());

        verify(requestService).getAll(3L);
    }

    @Test
    void getByIdShouldDelegateToService() throws Exception {
        when(requestService.getById(3L, 1L)).thenReturn(requestDto(1L));

        mockMvc.perform(get("/requests/1").header(USER_ID_HEADER, 3))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.description").value("Need a drill"));

        verify(requestService).getById(3L, 1L);
    }

    private ItemRequestDto requestDto(Long id) {
        return new ItemRequestDto(id, "Need a drill", LocalDateTime.now(), List.of());
    }
}
