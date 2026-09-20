package ru.practicum.shareit.request.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class ItemRequestItemDto {
    private Long id;
    private String name;
    private Long ownerId;
}
