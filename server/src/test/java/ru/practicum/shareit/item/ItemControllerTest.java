package ru.practicum.shareit.item;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import ru.practicum.shareit.exception.ErrorHandler;
import ru.practicum.shareit.item.dto.CommentCreateDto;
import ru.practicum.shareit.item.dto.CommentDto;
import ru.practicum.shareit.item.dto.ItemDto;
import ru.practicum.shareit.item.service.ItemService;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class ItemControllerTest {
    private static final String USER_ID_HEADER = "X-Sharer-User-Id";

    @Mock
    private ItemService itemService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new ItemController(itemService))
                .setControllerAdvice(new ErrorHandler())
                .build();
    }

    @Test
    void createShouldDelegateToService() throws Exception {
        when(itemService.create(org.mockito.ArgumentMatchers.eq(2L), any(ItemDto.class)))
                .thenReturn(itemDto(1L));

        mockMvc.perform(post("/items")
                        .header(USER_ID_HEADER, 2)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Drill\",\"description\":\"Cordless\"," +
                                "\"available\":true,\"requestId\":7}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));

        verify(itemService).create(org.mockito.ArgumentMatchers.eq(2L), argThat(item ->
                "Drill".equals(item.getName()) && Long.valueOf(7L).equals(item.getRequestId())));
    }

    @Test
    void updateShouldDelegateToService() throws Exception {
        when(itemService.update(org.mockito.ArgumentMatchers.eq(2L),
                org.mockito.ArgumentMatchers.eq(1L), any(ItemDto.class)))
                .thenReturn(itemDto(1L));

        mockMvc.perform(patch("/items/1")
                        .header(USER_ID_HEADER, 2)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Drill\"}"))
                .andExpect(status().isOk());

        verify(itemService).update(org.mockito.ArgumentMatchers.eq(2L),
                org.mockito.ArgumentMatchers.eq(1L), argThat(item -> "Drill".equals(item.getName())));
    }

    @Test
    void getByIdShouldDelegateToService() throws Exception {
        when(itemService.getById(2L, 1L)).thenReturn(itemDto(1L));

        mockMvc.perform(get("/items/1").header(USER_ID_HEADER, 2))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Drill"));

        verify(itemService).getById(2L, 1L);
    }

    @Test
    void getAllShouldDelegateToService() throws Exception {
        when(itemService.getAllByOwnerId(2L)).thenReturn(List.of(itemDto(1L)));

        mockMvc.perform(get("/items").header(USER_ID_HEADER, 2))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1));

        verify(itemService).getAllByOwnerId(2L);
    }

    @Test
    void searchShouldDelegateToService() throws Exception {
        when(itemService.search(2L, "drill")).thenReturn(List.of(itemDto(1L)));

        mockMvc.perform(get("/items/search")
                        .header(USER_ID_HEADER, 2)
                        .param("text", "drill"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Drill"));

        verify(itemService).search(2L, "drill");
    }

    @Test
    void addCommentShouldDelegateToService() throws Exception {
        CommentDto comment = new CommentDto(4L, "Works well", "Booker", LocalDateTime.now());
        when(itemService.addComment(org.mockito.ArgumentMatchers.eq(3L),
                org.mockito.ArgumentMatchers.eq(1L), any(CommentCreateDto.class)))
                .thenReturn(comment);

        mockMvc.perform(post("/items/1/comment")
                        .header(USER_ID_HEADER, 3)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"text\":\"Works well\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.text").value("Works well"));

        verify(itemService).addComment(org.mockito.ArgumentMatchers.eq(3L),
                org.mockito.ArgumentMatchers.eq(1L),
                argThat(commentDto -> "Works well".equals(commentDto.getText())));
    }

    private ItemDto itemDto(Long id) {
        return new ItemDto(id, "Drill", "Cordless", true, 7L, null, null, List.of());
    }
}
