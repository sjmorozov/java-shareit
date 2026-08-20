package ru.practicum.shareit.user.repository;

import ru.practicum.shareit.user.User;

import java.util.List;
import java.util.Optional;

public interface UserRepository {
    User save(User user) throws IllegalAccessException;

    Optional<User> findById(Long userId);

    List<User> findAll();

    void deleteById(Long userId);

    boolean existsByEmail(String email);
}
