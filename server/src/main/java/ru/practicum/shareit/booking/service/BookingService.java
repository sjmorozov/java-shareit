package ru.practicum.shareit.booking.service;

import ru.practicum.shareit.booking.BookingState;
import ru.practicum.shareit.booking.dto.BookingCreateDto;
import ru.practicum.shareit.booking.dto.BookingDto;

import java.util.List;

public interface BookingService {
    BookingDto create(Long userId, BookingCreateDto bookingCreateDto);

    BookingDto updateStatus(Long userId, Long bookingId, boolean approved);

    BookingDto getById(Long userId, Long bookingId);

    List<BookingDto> getAllByBooker(Long userId, BookingState state);

    List<BookingDto> getAllByOwner(Long userId, BookingState state);
}
