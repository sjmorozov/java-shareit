package ru.practicum.shareit.item.service;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.shareit.booking.Booking;
import ru.practicum.shareit.booking.BookingStatus;
import ru.practicum.shareit.booking.dto.BookingShortDto;
import ru.practicum.shareit.booking.mapper.BookingMapper;
import ru.practicum.shareit.booking.repository.BookingRepository;
import ru.practicum.shareit.exception.NotFoundException;
import ru.practicum.shareit.item.dto.CommentCreateDto;
import ru.practicum.shareit.item.dto.CommentDto;
import ru.practicum.shareit.item.dto.ItemDto;
import ru.practicum.shareit.item.mapper.CommentMapper;
import ru.practicum.shareit.item.mapper.ItemMapper;
import ru.practicum.shareit.item.model.Comment;
import ru.practicum.shareit.item.model.Item;
import ru.practicum.shareit.item.repository.CommentRepository;
import ru.practicum.shareit.item.repository.ItemRepository;
import ru.practicum.shareit.user.User;
import ru.practicum.shareit.user.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
public class ItemServiceImpl implements ItemService {
    private final ItemRepository itemRepository;
    private final UserRepository userRepository;
    private final BookingRepository bookingRepository;
    private final CommentRepository commentRepository;

    @Override
    public ItemDto create(Long userId, ItemDto itemDto) {
        User owner = findUserById(userId);

        Item item = ItemMapper.toItem(itemDto, owner);
        return ItemMapper.toItemDto(itemRepository.save(item));
    }

    @Override
    @Transactional
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
            return ItemMapper.toItemDto(item, null, null, getComments(itemId));
        }

        return toItemDtoWithBookings(item, LocalDateTime.now());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ItemDto> getAllByOwnerId(Long userId) {
        findUserById(userId);
        List<Item> items = itemRepository.findAllByOwnerId(userId);
        if (items.isEmpty()) {
            return List.of();
        }

        LocalDateTime now = LocalDateTime.now();
        Map<Long, List<Booking>> bookingsByItemId = bookingRepository
                .findAllByItemOwnerIdAndStatusOrderByStartDesc(userId, BookingStatus.APPROVED)
                .stream()
                .collect(Collectors.groupingBy(booking -> booking.getItem().getId()));

        List<Long> itemIds = items.stream()
                .map(Item::getId)
                .toList();
        Map<Long, List<CommentDto>> commentsByItemId = commentRepository
                .findAllByItemIdInOrderByCreatedAsc(itemIds)
                .stream()
                .collect(Collectors.groupingBy(
                        comment -> comment.getItem().getId(),
                        Collectors.mapping(CommentMapper::toCommentDto, Collectors.toList())
                ));

        return items.stream()
                .map(item -> toItemDtoWithBookings(
                        item,
                        bookingsByItemId.getOrDefault(item.getId(), List.of()),
                        commentsByItemId.getOrDefault(item.getId(), List.of()),
                        now
                ))
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

    @Override
    @Transactional
    public CommentDto addComment(Long userId, Long itemId, CommentCreateDto commentDto) {
        User author = findUserById(userId);
        Item item = findItemById(itemId);

        boolean hasCompletedBooking = bookingRepository
                .existsByItemIdAndBookerIdAndStatusAndEndBefore(
                        itemId, userId, BookingStatus.APPROVED, LocalDateTime.now());
        if (!hasCompletedBooking) {
            throw new IllegalArgumentException(
                    "Оставить комментарий можно только после завершённого бронирования"
            );
        }

        Comment comment = CommentMapper.toComment(commentDto, item, author);
        return CommentMapper.toCommentDto(commentRepository.save(comment));
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

        return ItemMapper.toItemDto(item, lastBooking, nextBooking, getComments(item.getId()));
    }

    private ItemDto toItemDtoWithBookings(Item item, List<Booking> bookings,
                                          List<CommentDto> comments, LocalDateTime now) {
        BookingShortDto lastBooking = bookings.stream()
                .filter(booking -> booking.getStart().isBefore(now))
                .max(Comparator.comparing(Booking::getStart))
                .map(BookingMapper::toBookingShortDto)
                .orElse(null);

        BookingShortDto nextBooking = bookings.stream()
                .filter(booking -> booking.getStart().isAfter(now))
                .min(Comparator.comparing(Booking::getStart))
                .map(BookingMapper::toBookingShortDto)
                .orElse(null);

        return ItemMapper.toItemDto(item, lastBooking, nextBooking, comments);
    }

    private List<CommentDto> getComments(Long itemId) {
        return commentRepository.findAllByItemIdOrderByCreatedAsc(itemId).stream()
                .map(CommentMapper::toCommentDto)
                .toList();
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
