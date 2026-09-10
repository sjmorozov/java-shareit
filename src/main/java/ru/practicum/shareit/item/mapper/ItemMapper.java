package ru.practicum.shareit.item.mapper;

import ru.practicum.shareit.booking.dto.BookingShortDto;
import ru.practicum.shareit.item.dto.ItemDto;
import ru.practicum.shareit.item.model.Item;
import ru.practicum.shareit.user.User;

public final class ItemMapper {

    private ItemMapper() {
    }

    public static ItemDto toItemDto(Item item) {
        return toItemDto(item, null, null);
    }

    public static ItemDto toItemDto(Item item, BookingShortDto lastBooking, BookingShortDto nextBooking) {
        return new ItemDto(
                item.getId(),
                item.getName(),
                item.getDescription(),
                item.getAvailable(),
                lastBooking,
                nextBooking
        );
    }

    public static Item toItem(ItemDto itemDto, User owner) {
        Item item = new Item();
        item.setName(itemDto.getName());
        item.setDescription(itemDto.getDescription());
        item.setAvailable(itemDto.getAvailable());
        item.setOwner(owner);
        return item;
    }
}
