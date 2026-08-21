package ru.practicum.shareit.user.service;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import ru.practicum.shareit.exception.EmailAlreadyExistsException;
import ru.practicum.shareit.exception.NotFoundException;
import ru.practicum.shareit.user.User;
import ru.practicum.shareit.user.dto.UserDto;
import ru.practicum.shareit.user.mapper.UserMapper;
import ru.practicum.shareit.user.repository.UserRepository;

import java.util.List;

@Service
@AllArgsConstructor
public class UserServiceImpl implements UserService {
    private final UserRepository userRepository;

    @Override
    public UserDto create(UserDto userDto) {
        validateUserDto(userDto);

        if (userRepository.existsByEmail(userDto.getEmail())) {
            throw new EmailAlreadyExistsException(
                    "Пользователь с email = " + userDto.getEmail() + " уже существует"
            );
        }

        User user = UserMapper.toUser(userDto);
        user.setId(null);
        User savedUser = userRepository.save(user);
        return UserMapper.toUserDto(savedUser);
    }

    @Override
    public UserDto update(Long userId, UserDto userDto) {
        validateUserDto(userDto);
        User user = findUserById(userId);

        String email = userDto.getEmail();
        if (email != null && !email.equals(user.getEmail()) && userRepository.existsByEmail(email)) {
            throw new EmailAlreadyExistsException(
                    "Пользователь с email = " + email + " уже существует"
            );
        }

        if (userDto.getName() != null) {
            user.setName(userDto.getName());
        }
        if (email != null) {
            user.setEmail(email);
        }

        User updatedUser = userRepository.save(user);
        return UserMapper.toUserDto(updatedUser);
    }

    @Override
    public UserDto getById(Long userId) {
        return UserMapper.toUserDto(findUserById(userId));
    }

    @Override
    public List<UserDto> getAll() {
        return userRepository.findAll().stream()
                .map(UserMapper::toUserDto)
                .toList();
    }

    @Override
    public void delete(Long userId) {
        findUserById(userId);
        userRepository.deleteById(userId);
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

    private void validateUserDto(UserDto userDto) {
        if (userDto == null) {
            throw new IllegalArgumentException("Данные пользователя не должны быть null");
        }
    }
}
