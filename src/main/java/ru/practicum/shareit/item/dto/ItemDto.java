package ru.practicum.shareit.item.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import ru.practicum.shareit.booking.dto.BookingShortDto;
import ru.practicum.shareit.validation.OnCreate;

import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class ItemDto {
    private Long id;

    @NotBlank(groups = OnCreate.class, message = "Название вещи не должно быть пустым")
    @Pattern(regexp = ".*\\S.*", message = "Название вещи не должно быть пустым")
    private String name;

    @NotBlank(groups = OnCreate.class, message = "Описание вещи не должно быть пустым")
    @Pattern(regexp = ".*\\S.*", message = "Описание вещи не должно быть пустым")
    private String description;

    @NotNull(groups = OnCreate.class, message = "Статус доступности вещи должен быть указан")
    private Boolean available;

    private BookingShortDto lastBooking;
    private BookingShortDto nextBooking;
    private List<CommentDto> comments;
}
