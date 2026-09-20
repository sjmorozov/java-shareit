package ru.practicum.shareit.item.mapper;

import ru.practicum.shareit.booking.dto.BookingShortDto;
import ru.practicum.shareit.item.dto.CommentDto;
import ru.practicum.shareit.item.dto.ItemDto;
import ru.practicum.shareit.item.model.Item;
import ru.practicum.shareit.request.ItemRequest;
import ru.practicum.shareit.user.User;

import java.util.List;

public final class ItemMapper {

    private ItemMapper() {
    }

    public static ItemDto toItemDto(Item item) {
        return toItemDto(item, null, null, List.of());
    }

    public static ItemDto toItemDto(Item item, BookingShortDto lastBooking, BookingShortDto nextBooking) {
        return toItemDto(item, lastBooking, nextBooking, List.of());
    }

    public static ItemDto toItemDto(Item item, BookingShortDto lastBooking, BookingShortDto nextBooking,
                                    List<CommentDto> comments) {
        return new ItemDto(
                item.getId(),
                item.getName(),
                item.getDescription(),
                item.getAvailable(),
                item.getRequest() == null ? null : item.getRequest().getId(),
                lastBooking,
                nextBooking,
                comments
        );
    }

    public static Item toItem(ItemDto itemDto, User owner) {
        return toItem(itemDto, owner, null);
    }

    public static Item toItem(ItemDto itemDto, User owner, ItemRequest request) {
        Item item = new Item();
        item.setName(itemDto.getName());
        item.setDescription(itemDto.getDescription());
        item.setAvailable(itemDto.getAvailable());
        item.setOwner(owner);
        item.setRequest(request);
        return item;
    }
}
