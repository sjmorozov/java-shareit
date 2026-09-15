package ru.practicum.shareit.item.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.shareit.booking.Booking;
import ru.practicum.shareit.booking.BookingStatus;
import ru.practicum.shareit.booking.repository.BookingRepository;
import ru.practicum.shareit.exception.NotFoundException;
import ru.practicum.shareit.item.dto.CommentCreateDto;
import ru.practicum.shareit.item.dto.CommentDto;
import ru.practicum.shareit.item.dto.ItemDto;
import ru.practicum.shareit.item.model.Item;
import ru.practicum.shareit.item.repository.CommentRepository;
import ru.practicum.shareit.item.repository.ItemRepository;
import ru.practicum.shareit.request.ItemRequest;
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
class ItemServiceIntegrationTest {
    @Autowired
    private ItemService itemService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ItemRepository itemRepository;

    @Autowired
    private ItemRequestRepository requestRepository;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private CommentRepository commentRepository;

    private User owner;
    private User booker;
    private Item item;

    @BeforeEach
    void setUp() {
        owner = userRepository.save(new User(null, "Owner", "owner@example.com"));
        booker = userRepository.save(new User(null, "Booker", "booker@example.com"));
        item = saveItem("Cordless drill", "Battery powered", true);
    }

    @Test
    void createShouldPersistItemWithRequest() {
        ItemRequest request = requestRepository.save(new ItemRequest(
                null,
                "Need a saw",
                booker,
                LocalDateTime.now()
        ));
        ItemDto input = new ItemDto(null, "Saw", "Circular saw", true,
                request.getId(), null, null, null);

        ItemDto created = itemService.create(owner.getId(), input);

        Item persisted = itemRepository.findById(created.getId()).orElseThrow();
        assertThat(created.getRequestId()).isEqualTo(request.getId());
        assertThat(persisted.getOwner().getId()).isEqualTo(owner.getId());
        assertThat(persisted.getRequest().getId()).isEqualTo(request.getId());
    }

    @Test
    void updateShouldChangeOnlyProvidedFields() {
        ItemDto patch = new ItemDto(null, "Updated drill", null, null,
                null, null, null, null);

        ItemDto updated = itemService.update(owner.getId(), item.getId(), patch);

        assertThat(updated.getName()).isEqualTo("Updated drill");
        assertThat(updated.getDescription()).isEqualTo("Battery powered");
        assertThat(updated.getAvailable()).isTrue();
    }

    @Test
    void getByIdShouldIncludeLastAndNextBookingsForOwner() {
        LocalDateTime now = LocalDateTime.now();
        Booking last = saveBooking(now.minusHours(2), now.minusHours(1));
        Booking next = saveBooking(now.plusHours(1), now.plusHours(2));

        ItemDto result = itemService.getById(owner.getId(), item.getId());

        assertThat(result.getLastBooking().getId()).isEqualTo(last.getId());
        assertThat(result.getNextBooking().getId()).isEqualTo(next.getId());
    }

    @Test
    void getAllByOwnerIdShouldReturnAllOwnedItems() {
        Item second = saveItem("Hammer", "Steel hammer", true);

        List<ItemDto> items = itemService.getAllByOwnerId(owner.getId());

        assertThat(items)
                .extracting(ItemDto::getId)
                .containsExactlyInAnyOrder(item.getId(), second.getId());
    }

    @Test
    void searchShouldReturnOnlyAvailableMatchingItems() {
        saveItem("Another drill", "Spare tool", false);
        saveItem("Hammer", "Steel hammer", true);

        List<ItemDto> result = itemService.search(booker.getId(), "DRILL");

        assertThat(result)
                .extracting(ItemDto::getId)
                .containsExactly(item.getId());
    }

    @Test
    void addCommentShouldPersistCommentAfterCompletedBooking() {
        LocalDateTime now = LocalDateTime.now();
        saveBooking(now.minusDays(2), now.minusDays(1));

        CommentDto created = itemService.addComment(
                booker.getId(),
                item.getId(),
                new CommentCreateDto("Works well")
        );

        assertThat(created.getText()).isEqualTo("Works well");
        assertThat(created.getAuthorName()).isEqualTo("Booker");
        assertThat(commentRepository.findById(created.getId())).isPresent();
    }

    @Test
    void createShouldWorkWithoutRequestAndRejectMissingRequest() {
        ItemDto input = new ItemDto(null, "Saw", "Circular saw", true,
                null, null, null, null);

        ItemDto created = itemService.create(owner.getId(), input);

        assertThat(created.getRequestId()).isNull();
        input.setRequestId(Long.MAX_VALUE);
        assertThatThrownBy(() -> itemService.create(owner.getId(), input))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void updateShouldChangeAllFieldsAndRejectNonOwner() {
        ItemDto patch = new ItemDto(null, "New name", "New description", false,
                null, null, null, null);

        ItemDto updated = itemService.update(owner.getId(), item.getId(), patch);

        assertThat(updated.getName()).isEqualTo("New name");
        assertThat(updated.getDescription()).isEqualTo("New description");
        assertThat(updated.getAvailable()).isFalse();
        assertThatThrownBy(() -> itemService.update(booker.getId(), item.getId(), patch))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void getByIdShouldHideBookingsFromNonOwner() {
        ItemDto result = itemService.getById(booker.getId(), item.getId());

        assertThat(result.getLastBooking()).isNull();
        assertThat(result.getNextBooking()).isNull();
    }

    @Test
    void getAllShouldReturnEmptyListWhenOwnerHasNoItems() {
        User emptyOwner = userRepository.save(new User(null, "Empty", "empty@example.com"));

        assertThat(itemService.getAllByOwnerId(emptyOwner.getId())).isEmpty();
    }

    @Test
    void searchShouldReturnEmptyListForBlankText() {
        assertThat(itemService.search(booker.getId(), null)).isEmpty();
        assertThat(itemService.search(booker.getId(), "   ")).isEmpty();
    }

    @Test
    void addCommentShouldRejectUserWithoutCompletedBooking() {
        assertThatThrownBy(() -> itemService.addComment(
                booker.getId(),
                item.getId(),
                new CommentCreateDto("Too early")
        )).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void operationsShouldRejectMissingUserAndItem() {
        ItemDto input = new ItemDto(null, "Saw", "Circular saw", true,
                null, null, null, null);

        assertThatThrownBy(() -> itemService.create(Long.MAX_VALUE, input))
                .isInstanceOf(NotFoundException.class);
        assertThatThrownBy(() -> itemService.getById(owner.getId(), Long.MAX_VALUE))
                .isInstanceOf(NotFoundException.class);
    }

    private Item saveItem(String name, String description, boolean available) {
        Item newItem = new Item();
        newItem.setName(name);
        newItem.setDescription(description);
        newItem.setAvailable(available);
        newItem.setOwner(owner);
        return itemRepository.save(newItem);
    }

    private Booking saveBooking(LocalDateTime start, LocalDateTime end) {
        return bookingRepository.save(new Booking(
                null,
                start,
                end,
                item,
                booker,
                BookingStatus.APPROVED
        ));
    }
}
