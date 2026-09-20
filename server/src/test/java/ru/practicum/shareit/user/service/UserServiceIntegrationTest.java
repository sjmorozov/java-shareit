package ru.practicum.shareit.user.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.shareit.user.User;
import ru.practicum.shareit.user.dto.UserDto;
import ru.practicum.shareit.user.repository.UserRepository;
import ru.practicum.shareit.exception.EmailAlreadyExistsException;
import ru.practicum.shareit.exception.NotFoundException;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class UserServiceIntegrationTest {
    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    private User user;

    @BeforeEach
    void setUp() {
        user = userRepository.save(new User(null, "User", "user@example.com"));
    }

    @Test
    void createShouldPersistUser() {
        UserDto created = userService.create(
                new UserDto(null, "New user", "new-user@example.com")
        );

        assertThat(created.getId()).isNotNull();
        assertThat(userRepository.findById(created.getId()))
                .get()
                .extracting(User::getEmail)
                .isEqualTo("new-user@example.com");
    }

    @Test
    void updateShouldChangeOnlyProvidedFields() {
        UserDto updated = userService.update(
                user.getId(),
                new UserDto(null, "Updated", null)
        );

        assertThat(updated.getName()).isEqualTo("Updated");
        assertThat(updated.getEmail()).isEqualTo("user@example.com");
    }

    @Test
    void getByIdShouldReturnPersistedUser() {
        UserDto result = userService.getById(user.getId());

        assertThat(result.getId()).isEqualTo(user.getId());
        assertThat(result.getEmail()).isEqualTo(user.getEmail());
    }

    @Test
    void getAllShouldReturnPersistedUsers() {
        User second = userRepository.save(new User(null, "Second", "second@example.com"));

        List<UserDto> users = userService.getAll();

        assertThat(users)
                .extracting(UserDto::getId)
                .containsExactlyInAnyOrder(user.getId(), second.getId());
    }

    @Test
    void deleteShouldRemoveUser() {
        userService.delete(user.getId());

        assertThat(userRepository.existsById(user.getId())).isFalse();
    }

    @Test
    void createShouldRejectNullAndDuplicateEmail() {
        assertThatThrownBy(() -> userService.create(null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> userService.create(
                new UserDto(null, "Duplicate", user.getEmail())
        )).isInstanceOf(EmailAlreadyExistsException.class);
    }

    @Test
    void updateShouldChangeEmailAndRejectAnotherUsersEmail() {
        User second = userRepository.save(new User(null, "Second", "second@example.com"));

        UserDto updated = userService.update(
                user.getId(),
                new UserDto(null, null, "updated@example.com")
        );

        assertThat(updated.getName()).isEqualTo(user.getName());
        assertThat(updated.getEmail()).isEqualTo("updated@example.com");
        assertThatThrownBy(() -> userService.update(
                user.getId(),
                new UserDto(null, null, second.getEmail())
        )).isInstanceOf(EmailAlreadyExistsException.class);
    }

    @Test
    void operationsShouldRejectMissingUser() {
        assertThatThrownBy(() -> userService.getById(Long.MAX_VALUE))
                .isInstanceOf(NotFoundException.class);
        assertThatThrownBy(() -> userService.delete(Long.MAX_VALUE))
                .isInstanceOf(NotFoundException.class);
        assertThatThrownBy(() -> userService.update(Long.MAX_VALUE, new UserDto()))
                .isInstanceOf(NotFoundException.class);
    }
}
