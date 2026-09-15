package ru.practicum.shareit.request.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.shareit.item.model.Item;
import ru.practicum.shareit.item.repository.ItemRepository;
import ru.practicum.shareit.exception.NotFoundException;
import ru.practicum.shareit.request.ItemRequest;
import ru.practicum.shareit.request.dto.ItemRequestCreateDto;
import ru.practicum.shareit.request.dto.ItemRequestDto;
import ru.practicum.shareit.request.repository.ItemRequestRepository;
import ru.practicum.shareit.user.User;
import ru.practicum.shareit.user.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ItemRequestServiceIntegrationTest {
    @Autowired
    private ItemRequestService requestService;

    @Autowired
    private ItemRequestRepository requestRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ItemRepository itemRepository;

    private User requester;
    private User responder;

    @BeforeEach
    void setUp() {
        requester = userRepository.save(new User(null, "Requester", "requester@example.com"));
        responder = userRepository.save(new User(null, "Responder", "responder@example.com"));
    }

    @Test
    void createShouldPersistRequestForUser() {
        ItemRequestDto created = requestService.create(
                requester.getId(),
                new ItemRequestCreateDto("Need a drill")
        );

        ItemRequest persisted = requestRepository.findById(created.getId()).orElseThrow();

        assertThat(created.getId()).isNotNull();
        assertThat(created.getDescription()).isEqualTo("Need a drill");
        assertThat(created.getCreated()).isNotNull();
        assertThat(created.getItems()).isEmpty();
        assertThat(persisted.getRequestor().getId()).isEqualTo(requester.getId());
    }

    @Test
    void getOwnShouldReturnOnlyOwnRequestsNewestFirst() {
        LocalDateTime now = LocalDateTime.now();
        saveRequest(requester, "Older request", now.minusHours(2));
        saveRequest(responder, "Someone else's request", now.minusHours(1));
        saveRequest(requester, "Newer request", now);

        List<ItemRequestDto> requests = requestService.getOwn(requester.getId());

        assertThat(requests)
                .extracting(ItemRequestDto::getDescription)
                .containsExactly("Newer request", "Older request");
    }

    @Test
    void getAllShouldExcludeOwnRequestsAndSortRemainingNewestFirst() {
        LocalDateTime now = LocalDateTime.now();
        saveRequest(requester, "Own request", now.plusHours(1));
        saveRequest(responder, "Older external request", now.minusHours(1));
        saveRequest(responder, "Newer external request", now);

        List<ItemRequestDto> requests = requestService.getAll(requester.getId());

        assertThat(requests)
                .extracting(ItemRequestDto::getDescription)
                .containsExactly("Newer external request", "Older external request");
    }

    @Test
    void getByIdShouldBeAvailableToAnotherUserAndContainResponseItems() {
        ItemRequest request = saveRequest(
                requester,
                "Need a drill",
                LocalDateTime.now()
        );
        Item item = new Item();
        item.setName("Drill");
        item.setDescription("Cordless drill");
        item.setAvailable(true);
        item.setOwner(responder);
        item.setRequest(request);
        itemRepository.save(item);

        ItemRequestDto result = requestService.getById(responder.getId(), request.getId());

        assertThat(result.getId()).isEqualTo(request.getId());
        assertThat(result.getItems()).singleElement().satisfies(responseItem -> {
            assertThat(responseItem.getId()).isEqualTo(item.getId());
            assertThat(responseItem.getName()).isEqualTo("Drill");
            assertThat(responseItem.getOwnerId()).isEqualTo(responder.getId());
        });
    }

    @Test
    void getOwnShouldReturnEmptyListAndGetByIdShouldRejectMissingRequest() {
        assertThat(requestService.getOwn(requester.getId())).isEmpty();
        assertThatThrownBy(() -> requestService.getById(requester.getId(), Long.MAX_VALUE))
                .isInstanceOf(NotFoundException.class);
        assertThatThrownBy(() -> requestService.getAll(Long.MAX_VALUE))
                .isInstanceOf(NotFoundException.class);
    }

    private ItemRequest saveRequest(User user, String description, LocalDateTime created) {
        return requestRepository.save(new ItemRequest(null, description, user, created));
    }
}
