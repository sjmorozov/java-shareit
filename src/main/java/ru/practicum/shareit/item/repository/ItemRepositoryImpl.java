package ru.practicum.shareit.item.repository;

import org.springframework.stereotype.Repository;
import ru.practicum.shareit.item.model.Item;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

@Repository
public class ItemRepositoryImpl implements ItemRepository {
    private final Map<Long, Item> items = new LinkedHashMap<>();
    private Long currentId = 1L;

    @Override
    public Item save(Item item) {
        if (item.getId() == null) {
            item.setId(currentId++);
        }

        items.put(item.getId(), item);
        return item;
    }

    @Override
    public Optional<Item> findById(Long itemId) {
        return Optional.ofNullable(items.get(itemId));
    }

    @Override
    public List<Item> findAllByOwnerId(Long ownerId) {
        if (ownerId == null) {
            return List.of();
        }

        return items.values().stream()
                .filter(item -> item.getOwner() != null)
                .filter(item -> ownerId.equals(item.getOwner().getId()))
                .toList();
    }

    @Override
    public List<Item> search(String text) {
        if (text == null || text.isBlank()) {
            return List.of();
        }

        String normalizedText = text.toLowerCase(Locale.ROOT);
        return items.values().stream()
                .filter(item -> Boolean.TRUE.equals(item.getAvailable()))
                .filter(item -> containsIgnoreCase(item.getName(), normalizedText)
                        || containsIgnoreCase(item.getDescription(), normalizedText))
                .toList();
    }

    private boolean containsIgnoreCase(String value, String normalizedText) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(normalizedText);
    }
}
