package ru.practicum.shareit.request.mapper;

import ru.practicum.shareit.item.model.Item;
import ru.practicum.shareit.request.ItemRequest;
import ru.practicum.shareit.request.dto.ItemRequestCreateDto;
import ru.practicum.shareit.request.dto.ItemRequestDto;
import ru.practicum.shareit.request.dto.ItemRequestItemDto;
import ru.practicum.shareit.user.User;

import java.time.LocalDateTime;
import java.util.List;

public final class ItemRequestMapper {
    private ItemRequestMapper() {
    }

    public static ItemRequest toItemRequest(ItemRequestCreateDto requestDto,
                                            User requestor, LocalDateTime created) {
        ItemRequest request = new ItemRequest();
        request.setDescription(requestDto.getDescription());
        request.setRequestor(requestor);
        request.setCreated(created);
        return request;
    }

    public static ItemRequestDto toItemRequestDto(ItemRequest request, List<Item> items) {
        List<ItemRequestItemDto> itemDtos = items.stream()
                .map(item -> new ItemRequestItemDto(
                        item.getId(),
                        item.getName(),
                        item.getOwner().getId()
                ))
                .toList();

        return new ItemRequestDto(
                request.getId(),
                request.getDescription(),
                request.getCreated(),
                itemDtos
        );
    }
}
