package ru.practicum.shareit.item.service;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import ru.practicum.shareit.exception.NotFoundException;
import ru.practicum.shareit.item.dto.ItemDto;
import ru.practicum.shareit.item.mapper.ItemMapper;
import ru.practicum.shareit.item.model.Item;
import ru.practicum.shareit.item.repository.ItemRepository;
import ru.practicum.shareit.user.User;
import ru.practicum.shareit.user.repository.UserRepository;

import java.util.List;

@Service
@AllArgsConstructor
public class ItemServiceImpl implements ItemService {
    private final ItemRepository itemRepository;
    private final UserRepository userRepository;

    @Override
    public ItemDto create(Long userId, ItemDto itemDto) {
        validateItemDto(itemDto);
        User owner = findUserById(userId);

        Item item = ItemMapper.toItem(itemDto, owner);
        item.setId(null);
        return ItemMapper.toItemDto(itemRepository.save(item));
    }

    @Override
    public ItemDto update(Long userId, Long itemId, ItemDto itemDto) {
        validateItemDto(itemDto);
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
    public ItemDto getById(Long userId, Long itemId) {
        findUserById(userId);
        return ItemMapper.toItemDto(findItemById(itemId));
    }

    @Override
    public List<ItemDto> getAllByOwnerId(Long userId) {
        findUserById(userId);
        return itemRepository.findAllByOwnerId(userId).stream()
                .map(ItemMapper::toItemDto)
                .toList();
    }

    @Override
    public List<ItemDto> search(Long userId, String text) {
        findUserById(userId);
        return itemRepository.search(text).stream()
                .map(ItemMapper::toItemDto)
                .toList();
    }

    private User findUserById(Long userId) {
        if (userId == null) {
            throw new IllegalArgumentException("id пользователя не должен быть null");
        }

        return userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException(
                        "Пользователя с id = " + userId + " не существует"
                ));
    }

    private Item findItemById(Long itemId) {
        if (itemId == null) {
            throw new IllegalArgumentException("id вещи не должен быть null");
        }

        return itemRepository.findById(itemId)
                .orElseThrow(() -> new NotFoundException(
                        "Вещи с id = " + itemId + " не существует"
                ));
    }

    private void validateItemDto(ItemDto itemDto) {
        if (itemDto == null) {
            throw new IllegalArgumentException("Данные вещи не должны быть null");
        }
    }
}
