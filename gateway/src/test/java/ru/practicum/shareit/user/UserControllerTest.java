package ru.practicum.shareit.user;

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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class UserControllerTest {
    @Mock
    private UserClient userClient;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new UserController(userClient))
                .setControllerAdvice(new ErrorHandler())
                .build();
    }

    @Test
    void createShouldForwardValidUserToClient() throws Exception {
        when(userClient.create(argThat(user -> "user@example.com".equals(user.getEmail()))))
                .thenReturn(ResponseEntity.ok(Map.of(
                        "id", 1,
                        "name", "User",
                        "email", "user@example.com"
                )));

        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"User\",\"email\":\"user@example.com\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.email").value("user@example.com"));

        verify(userClient).create(argThat(user ->
                "User".equals(user.getName()) && "user@example.com".equals(user.getEmail())));
    }

    @Test
    void createShouldRejectInvalidEmailBeforeCallingClient() throws Exception {
        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"User\",\"email\":\"not-an-email\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.email").exists());

        verifyNoInteractions(userClient);
    }

    @Test
    void updateShouldForwardValidPartialUser() throws Exception {
        when(userClient.update(eq(1L), argThat(user -> "Updated".equals(user.getName()))))
                .thenReturn(ResponseEntity.ok(Map.of("id", 1, "name", "Updated")));

        mockMvc.perform(patch("/users/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Updated\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Updated"));

        verify(userClient).update(eq(1L), argThat(user -> "Updated".equals(user.getName())));
    }

    @Test
    void getByIdShouldForwardUserId() throws Exception {
        when(userClient.getById(1L)).thenReturn(ResponseEntity.ok(Map.of("id", 1)));

        mockMvc.perform(get("/users/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));

        verify(userClient).getById(1L);
    }

    @Test
    void getAllShouldCallClient() throws Exception {
        when(userClient.getAll()).thenReturn(ResponseEntity.ok(List.of(Map.of("id", 1))));

        mockMvc.perform(get("/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1));

        verify(userClient).getAll();
    }

    @Test
    void deleteShouldForwardUserId() throws Exception {
        when(userClient.delete(1L)).thenReturn(ResponseEntity.ok().build());

        mockMvc.perform(delete("/users/1"))
                .andExpect(status().isOk());

        verify(userClient).delete(1L);
    }
}
