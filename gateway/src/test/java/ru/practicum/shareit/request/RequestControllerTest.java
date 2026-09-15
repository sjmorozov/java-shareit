package ru.practicum.shareit.request;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import ru.practicum.shareit.exception.ErrorHandler;

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class RequestControllerTest {
    private static final String USER_ID_HEADER = "X-Sharer-User-Id";

    @Mock
    private RequestClient requestClient;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new RequestController(requestClient))
                .setControllerAdvice(new ErrorHandler())
                .build();
    }

    @Test
    void createShouldForwardValidRequest() throws Exception {
        when(requestClient.create(eq(3L), argThat(request ->
                "Need a drill".equals(request.getDescription()))))
                .thenReturn(ResponseEntity.ok(Map.of(
                        "id", 1,
                        "description", "Need a drill"
                )));

        mockMvc.perform(post("/requests")
                        .header(USER_ID_HEADER, 3)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"description\":\"Need a drill\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.description").value("Need a drill"));

        verify(requestClient).create(eq(3L), argThat(request ->
                "Need a drill".equals(request.getDescription())));
    }

    @Test
    void createShouldRejectBlankDescriptionBeforeCallingClient() throws Exception {
        mockMvc.perform(post("/requests")
                        .header(USER_ID_HEADER, 3)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"description\":\"   \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.description").exists());

        verifyNoInteractions(requestClient);
    }

    @Test
    void getOwnShouldForwardUserId() throws Exception {
        when(requestClient.getOwn(3L))
                .thenReturn(ResponseEntity.ok(List.of(Map.of("id", 1))));

        mockMvc.perform(get("/requests")
                        .header(USER_ID_HEADER, 3))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1));

        verify(requestClient).getOwn(3L);
    }

    @Test
    void getAllShouldForwardUserId() throws Exception {
        when(requestClient.getAll(3L))
                .thenReturn(ResponseEntity.ok(List.of()));

        mockMvc.perform(get("/requests/all")
                        .header(USER_ID_HEADER, 3))
                .andExpect(status().isOk());

        verify(requestClient).getAll(3L);
    }

    @Test
    void getByIdShouldForwardUserAndRequestIds() throws Exception {
        when(requestClient.getById(3L, 8L))
                .thenReturn(ResponseEntity.ok(Map.of("id", 8)));

        mockMvc.perform(get("/requests/8")
                        .header(USER_ID_HEADER, 3))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(8));

        verify(requestClient).getById(3L, 8L);
    }
}
