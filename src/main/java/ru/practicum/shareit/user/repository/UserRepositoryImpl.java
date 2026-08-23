package ru.practicum.shareit.user.repository;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;
import ru.practicum.shareit.user.User;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Repository
public class UserRepositoryImpl implements UserRepository {
    private final Map<Long, User> users = new LinkedHashMap<>();
    private Long currentId = 1L;

    @Override
    public User save(User user) {
        Long id = user.getId();
        if (id != null) {
            users.put(id, user);
            log.info("Пользователь с id = {} обновлён", id);
            return user;
        }

        user.setId(currentId);
        users.put(currentId, user);
        log.info("Пользователь с id = {} сохранён", user.getId());
        currentId++;
        return user;
    }

    @Override
    public Optional<User> findById(Long userId) {
        return Optional.ofNullable(users.get(userId));
    }

    @Override
    public List<User> findAll() {
        return users.values().stream().toList();
    }

    @Override
    public void deleteById(Long userId) {
        if (users.remove(userId) == null) {
            log.info("Пользователь с id = {} не найден", userId);
        }
    }

    @Override
    public boolean existsByEmail(String email) {
        if (email == null) {
            return false;
        }
        return users.values().stream()
                .anyMatch(user -> email.equals(user.getEmail()));
    }
}
