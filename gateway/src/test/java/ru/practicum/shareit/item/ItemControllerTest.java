package ru.practicum.shareit.item;

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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class ItemControllerTest {
    private static final String USER_ID_HEADER = "X-Sharer-User-Id";

    @Mock
    private ItemClient itemClient;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new ItemController(itemClient))
                .setControllerAdvice(new ErrorHandler())
                .build();
    }

    @Test
    void createShouldForwardUserIdAndValidItem() throws Exception {
        when(itemClient.create(eq(7L), argThat(item -> "Drill".equals(item.getName()))))
                .thenReturn(ResponseEntity.ok(Map.of(
                        "id", 1,
                        "name", "Drill",
                        "description", "Cordless drill",
                        "available", true
                )));

        mockMvc.perform(post("/items")
                        .header(USER_ID_HEADER, 7)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Drill\",\"description\":\"Cordless drill\"," +
                                "\"available\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));

        verify(itemClient).create(eq(7L), argThat(item ->
                "Drill".equals(item.getName()) && Boolean.TRUE.equals(item.getAvailable())));
    }

    @Test
    void createShouldRejectIncompleteItemBeforeCallingClient() throws Exception {
        mockMvc.perform(post("/items")
                        .header(USER_ID_HEADER, 7)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Drill\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists())
                .andExpect(jsonPath("$.errors.description").exists())
                .andExpect(jsonPath("$.errors.available").exists());

        verifyNoInteractions(itemClient);
    }

    @Test
    void searchShouldReturnEmptyListWithoutServerCallForBlankText() throws Exception {
        mockMvc.perform(get("/items/search")
                        .header(USER_ID_HEADER, 7)
                        .param("text", "   "))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));

        verifyNoInteractions(itemClient);
    }

    @Test
    void updateShouldForwardUserItemAndBody() throws Exception {
        when(itemClient.update(eq(7L), eq(1L), any()))
                .thenReturn(ResponseEntity.ok(Map.of("id", 1, "name", "Updated")));

        mockMvc.perform(patch("/items/1")
                        .header(USER_ID_HEADER, 7)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Updated\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Updated"));

        verify(itemClient).update(eq(7L), eq(1L),
                argThat(item -> "Updated".equals(item.getName())));
    }

    @Test
    void getByIdShouldForwardUserAndItemIds() throws Exception {
        when(itemClient.getById(7L, 1L)).thenReturn(ResponseEntity.ok(Map.of("id", 1)));

        mockMvc.perform(get("/items/1").header(USER_ID_HEADER, 7))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));

        verify(itemClient).getById(7L, 1L);
    }

    @Test
    void getAllShouldForwardOwnerId() throws Exception {
        when(itemClient.getAllByOwnerId(7L)).thenReturn(ResponseEntity.ok(List.of()));

        mockMvc.perform(get("/items").header(USER_ID_HEADER, 7))
                .andExpect(status().isOk());

        verify(itemClient).getAllByOwnerId(7L);
    }

    @Test
    void searchShouldForwardNonBlankText() throws Exception {
        when(itemClient.search(7L, "drill")).thenReturn(ResponseEntity.ok(List.of()));

        mockMvc.perform(get("/items/search")
                        .header(USER_ID_HEADER, 7)
                        .param("text", "drill"))
                .andExpect(status().isOk());

        verify(itemClient).search(7L, "drill");
    }

    @Test
    void addCommentShouldForwardValidComment() throws Exception {
        when(itemClient.addComment(eq(7L), eq(1L), any()))
                .thenReturn(ResponseEntity.ok(Map.of("id", 4, "text", "Works well")));

        mockMvc.perform(post("/items/1/comment")
                        .header(USER_ID_HEADER, 7)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"text\":\"Works well\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.text").value("Works well"));

        verify(itemClient).addComment(eq(7L), eq(1L),
                argThat(comment -> "Works well".equals(comment.getText())));
    }
}
