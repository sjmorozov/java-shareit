package ru.practicum.shareit.request.service;

import ru.practicum.shareit.request.dto.ItemRequestCreateDto;
import ru.practicum.shareit.request.dto.ItemRequestDto;

import java.util.List;

public interface ItemRequestService {
    ItemRequestDto create(Long userId, ItemRequestCreateDto requestDto);

    List<ItemRequestDto> getOwn(Long userId);

    List<ItemRequestDto> getAll(Long userId);

    ItemRequestDto getById(Long userId, Long requestId);
}
