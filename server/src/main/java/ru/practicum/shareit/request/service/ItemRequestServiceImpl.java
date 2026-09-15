package ru.practicum.shareit.request.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.shareit.exception.NotFoundException;
import ru.practicum.shareit.item.model.Item;
import ru.practicum.shareit.item.repository.ItemRepository;
import ru.practicum.shareit.request.ItemRequest;
import ru.practicum.shareit.request.dto.ItemRequestCreateDto;
import ru.practicum.shareit.request.dto.ItemRequestDto;
import ru.practicum.shareit.request.mapper.ItemRequestMapper;
import ru.practicum.shareit.request.repository.ItemRequestRepository;
import ru.practicum.shareit.user.User;
import ru.practicum.shareit.user.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ItemRequestServiceImpl implements ItemRequestService {
    private final ItemRequestRepository requestRepository;
    private final UserRepository userRepository;
    private final ItemRepository itemRepository;

    @Override
    @Transactional
    public ItemRequestDto create(Long userId, ItemRequestCreateDto requestDto) {
        User requestor = findUserById(userId);
        ItemRequest request = ItemRequestMapper.toItemRequest(
                requestDto,
                requestor,
                LocalDateTime.now()
        );
        return ItemRequestMapper.toItemRequestDto(requestRepository.save(request), List.of());
    }

    @Override
    public List<ItemRequestDto> getOwn(Long userId) {
        findUserById(userId);
        return toItemRequestDtos(requestRepository.findAllByRequestorIdOrderByCreatedDesc(userId));
    }

    @Override
    public List<ItemRequestDto> getAll(Long userId) {
        findUserById(userId);
        return toItemRequestDtos(requestRepository.findAllByRequestorIdNotOrderByCreatedDesc(userId));
    }

    @Override
    public ItemRequestDto getById(Long userId, Long requestId) {
        findUserById(userId);
        ItemRequest request = requestRepository.findById(requestId)
                .orElseThrow(() -> new NotFoundException(
                        "Запроса вещи с id = " + requestId + " не существует"
                ));
        List<Item> items = itemRepository.findAllByRequestIdIn(List.of(requestId));
        return ItemRequestMapper.toItemRequestDto(request, items);
    }

    private List<ItemRequestDto> toItemRequestDtos(List<ItemRequest> requests) {
        if (requests.isEmpty()) {
            return List.of();
        }

        List<Long> requestIds = requests.stream()
                .map(ItemRequest::getId)
                .toList();
        Map<Long, List<Item>> itemsByRequestId = itemRepository.findAllByRequestIdIn(requestIds)
                .stream()
                .collect(Collectors.groupingBy(item -> item.getRequest().getId()));

        return requests.stream()
                .map(request -> ItemRequestMapper.toItemRequestDto(
                        request,
                        itemsByRequestId.getOrDefault(request.getId(), List.of())
                ))
                .toList();
    }

    private User findUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException(
                        "Пользователя с id = " + userId + " не существует"
                ));
    }
}
