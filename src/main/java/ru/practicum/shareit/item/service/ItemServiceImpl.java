package ru.practicum.shareit.item.service;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.shareit.booking.BookingStatus;
import ru.practicum.shareit.booking.dto.BookingShortDto;
import ru.practicum.shareit.booking.mapper.BookingMapper;
import ru.practicum.shareit.booking.repository.BookingRepository;
import ru.practicum.shareit.exception.NotFoundException;
import ru.practicum.shareit.item.dto.ItemDto;
import ru.practicum.shareit.item.mapper.ItemMapper;
import ru.practicum.shareit.item.model.Item;
import ru.practicum.shareit.item.repository.ItemRepository;
import ru.practicum.shareit.user.User;
import ru.practicum.shareit.user.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.List;

@Service
@AllArgsConstructor
public class ItemServiceImpl implements ItemService {
    private final ItemRepository itemRepository;
    private final UserRepository userRepository;
    private final BookingRepository bookingRepository;

    @Override
    public ItemDto create(Long userId, ItemDto itemDto) {
        User owner = findUserById(userId);

        Item item = ItemMapper.toItem(itemDto, owner);
        return ItemMapper.toItemDto(itemRepository.save(item));
    }

    @Override
    public ItemDto update(Long userId, Long itemId, ItemDto itemDto) {
        User owner = findUserById(userId);
        Item item = findItemById(itemId);

        if (!owner.getId().equals(item.getOwner().getId())) {
            throw new NotFoundException("Вещь с id = " + itemId + " не принадлежит пользователю с id = " + userId);
        }

        if (itemDto.getName() != null) {
            item.setName(itemDto.getName());
        }
        if (itemDto.getDescription() != null) {
            item.setDescription(itemDto.getDescription());
        }
        if (itemDto.getAvailable() != null) {
            item.setAvailable(itemDto.getAvailable());
        }

        return ItemMapper.toItemDto(itemRepository.save(item));
    }

    @Override
    @Transactional(readOnly = true)
    public ItemDto getById(Long userId, Long itemId) {
        findUserById(userId);
        Item item = findItemById(itemId);

        if (!item.getOwner().getId().equals(userId)) {
            return ItemMapper.toItemDto(item);
        }

        return toItemDtoWithBookings(item, LocalDateTime.now());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ItemDto> getAllByOwnerId(Long userId) {
        findUserById(userId);
        LocalDateTime now = LocalDateTime.now();

        return itemRepository.findAllByOwnerId(userId).stream()
                .map(item -> toItemDtoWithBookings(item, now))
                .toList();
    }

    @Override
    public List<ItemDto> search(Long userId, String text) {
        findUserById(userId);

        if (text == null || text.isBlank()) {
            return List.of();
        }

        return itemRepository.search(text).stream()
                .map(ItemMapper::toItemDto)
                .toList();
    }

    private ItemDto toItemDtoWithBookings(Item item, LocalDateTime now) {
        BookingShortDto lastBooking = bookingRepository
                .findFirstByItemIdAndStatusAndStartBeforeOrderByStartDesc(
                        item.getId(), BookingStatus.APPROVED, now)
                .map(BookingMapper::toBookingShortDto)
                .orElse(null);

        BookingShortDto nextBooking = bookingRepository
                .findFirstByItemIdAndStatusAndStartAfterOrderByStartAsc(
                        item.getId(), BookingStatus.APPROVED, now)
                .map(BookingMapper::toBookingShortDto)
                .orElse(null);

        return ItemMapper.toItemDto(item, lastBooking, nextBooking);
    }

    private User findUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException(
                        "Пользователя с id = " + userId + " не существует"
                ));
    }

    private Item findItemById(Long itemId) {
        return itemRepository.findById(itemId)
                .orElseThrow(() -> new NotFoundException(
                        "Вещи с id = " + itemId + " не существует"
                ));
    }
}
