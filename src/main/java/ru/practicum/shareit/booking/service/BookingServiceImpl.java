package ru.practicum.shareit.booking.service;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.shareit.booking.Booking;
import ru.practicum.shareit.booking.BookingStatus;
import ru.practicum.shareit.booking.dto.BookingCreateDto;
import ru.practicum.shareit.booking.dto.BookingDto;
import ru.practicum.shareit.booking.mapper.BookingMapper;
import ru.practicum.shareit.booking.repository.BookingRepository;
import ru.practicum.shareit.exception.NotFoundException;
import ru.practicum.shareit.item.model.Item;
import ru.practicum.shareit.item.repository.ItemRepository;
import ru.practicum.shareit.user.User;
import ru.practicum.shareit.user.repository.UserRepository;

import java.time.LocalDateTime;

@Service
@AllArgsConstructor
public class BookingServiceImpl implements BookingService {
    private final BookingRepository bookingRepository;
    private final ItemRepository itemRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public BookingDto create(Long userId, BookingCreateDto bookingCreateDto) {
        validateBookingCreateDto(bookingCreateDto);

        User booker = findUserById(userId);
        Item item = findItemById(bookingCreateDto.getItemId());

        if (!Boolean.TRUE.equals(item.getAvailable())) {
            throw new IllegalArgumentException(
                    "Вещь с id = " + item.getId() + " недоступна для бронирования"
            );
        }

        if (booker.getId().equals(item.getOwner().getId())) {
            throw new NotFoundException("Владелец вещи не может забронировать её у самого себя");
        }

        Booking booking = BookingMapper.toBooking(bookingCreateDto, item, booker);
        return BookingMapper.toBookingDto(bookingRepository.save(booking));
    }

    @Override
    @Transactional
    public BookingDto updateStatus(Long userId, Long bookingId, boolean approved) {
        findUserById(userId);

        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new NotFoundException(
                        "Бронирования с id = " + bookingId + " не существует"
                ));

        if (!booking.getItem().getOwner().getId().equals(userId)) {
            throw new NotFoundException("Изменить статус бронирования может только владелец вещи");
        }

        if (booking.getStatus() != BookingStatus.WAITING) {
            throw new IllegalArgumentException("Статус бронирования уже был изменён");
        }

        booking.setStatus(approved ? BookingStatus.APPROVED : BookingStatus.REJECTED);
        return BookingMapper.toBookingDto(bookingRepository.save(booking));
    }

    @Override
    @Transactional(readOnly = true)
    public BookingDto getById(Long userId, Long bookingId) {
        findUserById(userId);

        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new NotFoundException(
                        "Бронирования с id = " + bookingId + " не существует"
                ));

        Long bookerId = booking.getBooker().getId();
        Long ownerId = booking.getItem().getOwner().getId();
        if (!userId.equals(bookerId) && !userId.equals(ownerId)) {
            throw new NotFoundException(
                    "Просматривать бронирование может только его автор или владелец вещи"
            );
        }

        return BookingMapper.toBookingDto(booking);
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

    private void validateBookingCreateDto(BookingCreateDto bookingCreateDto) {
        if (bookingCreateDto == null) {
            throw new IllegalArgumentException("Данные бронирования не должны быть null");
        }
        if (bookingCreateDto.getItemId() == null) {
            throw new IllegalArgumentException("Идентификатор вещи должен быть указан");
        }

        LocalDateTime start = bookingCreateDto.getStart();
        LocalDateTime end = bookingCreateDto.getEnd();
        if (start == null || end == null) {
            throw new IllegalArgumentException("Даты начала и окончания бронирования должны быть указаны");
        }

        LocalDateTime now = LocalDateTime.now();
        if (!start.isAfter(now)) {
            throw new IllegalArgumentException("Дата начала бронирования должна быть в будущем");
        }
        if (!end.isAfter(now)) {
            throw new IllegalArgumentException("Дата окончания бронирования должна быть в будущем");
        }
        if (!start.isBefore(end)) {
            throw new IllegalArgumentException(
                    "Дата начала бронирования должна быть раньше даты окончания"
            );
        }
    }
}
