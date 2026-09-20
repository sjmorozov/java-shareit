package ru.practicum.shareit.booking.service;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.shareit.booking.Booking;
import ru.practicum.shareit.booking.BookingState;
import ru.practicum.shareit.booking.BookingStatus;
import ru.practicum.shareit.booking.dto.BookingCreateDto;
import ru.practicum.shareit.booking.dto.BookingDto;
import ru.practicum.shareit.exception.ForbiddenException;
import ru.practicum.shareit.exception.NotFoundException;
import ru.practicum.shareit.item.model.Item;
import ru.practicum.shareit.user.User;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class BookingServiceIntegrationTest {
    @Autowired
    private BookingService bookingService;

    @Autowired
    private EntityManager entityManager;

    private User owner;
    private User booker;
    private Item item;

    @BeforeEach
    void setUp() {
        owner = persist(new User(null, "Owner", "owner@example.com"));
        booker = persist(new User(null, "Booker", "booker@example.com"));
        item = new Item();
        item.setName("Drill");
        item.setDescription("Cordless drill");
        item.setAvailable(true);
        item.setOwner(owner);
        item = persist(item);
    }

    @Test
    void createShouldPersistWaitingBooking() {
        LocalDateTime start = LocalDateTime.now().plusDays(1);
        BookingCreateDto input = new BookingCreateDto(item.getId(), start, start.plusHours(2));

        BookingDto created = bookingService.create(booker.getId(), input);

        assertThat(created.getId()).isNotNull();
        assertThat(created.getStatus()).isEqualTo(BookingStatus.WAITING);
        assertThat(findBooking(created.getId())).isNotNull();
    }

    @Test
    void updateStatusShouldApproveWaitingBookingByOwner() {
        Booking booking = saveBooking(
                BookingStatus.WAITING,
                LocalDateTime.now().plusDays(1),
                LocalDateTime.now().plusDays(2)
        );

        BookingDto updated = bookingService.updateStatus(owner.getId(), booking.getId(), true);

        assertThat(updated.getStatus()).isEqualTo(BookingStatus.APPROVED);
        assertThat(findBooking(booking.getId()).getStatus())
                .isEqualTo(BookingStatus.APPROVED);
    }

    @Test
    void getByIdShouldReturnBookingToBooker() {
        Booking booking = saveBooking(
                BookingStatus.WAITING,
                LocalDateTime.now().plusDays(1),
                LocalDateTime.now().plusDays(2)
        );

        BookingDto result = bookingService.getById(booker.getId(), booking.getId());

        assertThat(result.getId()).isEqualTo(booking.getId());
        assertThat(result.getBooker().getId()).isEqualTo(booker.getId());
        assertThat(result.getItem().getId()).isEqualTo(item.getId());
    }

    @Test
    void getAllByBookerShouldReturnBookersBookingsNewestFirst() {
        LocalDateTime now = LocalDateTime.now();
        Booking older = saveBooking(BookingStatus.WAITING, now.plusDays(1), now.plusDays(2));
        Booking newer = saveBooking(BookingStatus.WAITING, now.plusDays(3), now.plusDays(4));

        List<BookingDto> bookings = bookingService.getAllByBooker(booker.getId(), BookingState.ALL);

        assertThat(bookings)
                .extracting(BookingDto::getId)
                .containsExactly(newer.getId(), older.getId());
    }

    @Test
    void getAllByOwnerShouldReturnBookingsForOwnedItems() {
        Booking booking = saveBooking(
                BookingStatus.WAITING,
                LocalDateTime.now().plusDays(1),
                LocalDateTime.now().plusDays(2)
        );

        List<BookingDto> bookings = bookingService.getAllByOwner(owner.getId(), BookingState.ALL);

        assertThat(bookings)
                .extracting(BookingDto::getId)
                .containsExactly(booking.getId());
    }

    @Test
    void createShouldRejectInvalidInterval() {
        LocalDateTime start = LocalDateTime.now().plusDays(2);
        BookingCreateDto input = new BookingCreateDto(item.getId(), start, start.minusHours(1));

        assertThatThrownBy(() -> bookingService.create(booker.getId(), input))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void createShouldRejectUnavailableItem() {
        item.setAvailable(false);
        entityManager.flush();
        LocalDateTime start = LocalDateTime.now().plusDays(1);

        assertThatThrownBy(() -> bookingService.create(
                booker.getId(),
                new BookingCreateDto(item.getId(), start, start.plusHours(1))
        )).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void createShouldRejectBookingByItemOwner() {
        LocalDateTime start = LocalDateTime.now().plusDays(1);

        assertThatThrownBy(() -> bookingService.create(
                owner.getId(),
                new BookingCreateDto(item.getId(), start, start.plusHours(1))
        )).isInstanceOf(NotFoundException.class);
    }

    @Test
    void createShouldRejectOverlappingBooking() {
        LocalDateTime start = LocalDateTime.now().plusDays(1);
        saveBooking(BookingStatus.WAITING, start, start.plusHours(2));

        assertThatThrownBy(() -> bookingService.create(
                booker.getId(),
                new BookingCreateDto(item.getId(), start.plusMinutes(30), start.plusHours(3))
        )).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void createShouldRejectMissingUserOrItem() {
        LocalDateTime start = LocalDateTime.now().plusDays(1);
        BookingCreateDto missingItem = new BookingCreateDto(Long.MAX_VALUE, start, start.plusHours(1));
        BookingCreateDto existingItem = new BookingCreateDto(item.getId(), start, start.plusHours(1));

        assertThatThrownBy(() -> bookingService.create(Long.MAX_VALUE, existingItem))
                .isInstanceOf(NotFoundException.class);
        assertThatThrownBy(() -> bookingService.create(booker.getId(), missingItem))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void updateStatusShouldRejectBookingWhenRequested() {
        Booking booking = saveBooking(
                BookingStatus.WAITING,
                LocalDateTime.now().plusDays(1),
                LocalDateTime.now().plusDays(2)
        );

        BookingDto updated = bookingService.updateStatus(owner.getId(), booking.getId(), false);

        assertThat(updated.getStatus()).isEqualTo(BookingStatus.REJECTED);
    }

    @Test
    void updateStatusShouldRejectNonOwnerAndRepeatedDecision() {
        Booking waiting = saveBooking(
                BookingStatus.WAITING,
                LocalDateTime.now().plusDays(1),
                LocalDateTime.now().plusDays(2)
        );
        Booking approved = saveBooking(
                BookingStatus.APPROVED,
                LocalDateTime.now().plusDays(3),
                LocalDateTime.now().plusDays(4)
        );

        assertThatThrownBy(() -> bookingService.updateStatus(booker.getId(), waiting.getId(), true))
                .isInstanceOf(ForbiddenException.class);
        assertThatThrownBy(() -> bookingService.updateStatus(owner.getId(), approved.getId(), false))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> bookingService.updateStatus(owner.getId(), Long.MAX_VALUE, true))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void getByIdShouldAllowOwnerAndRejectUnrelatedUser() {
        Booking booking = saveBooking(
                BookingStatus.WAITING,
                LocalDateTime.now().plusDays(1),
                LocalDateTime.now().plusDays(2)
        );
        User outsider = persist(new User(null, "Outsider", "outsider@example.com"));

        assertThat(bookingService.getById(owner.getId(), booking.getId()).getId())
                .isEqualTo(booking.getId());
        assertThatThrownBy(() -> bookingService.getById(outsider.getId(), booking.getId()))
                .isInstanceOf(NotFoundException.class);
        assertThatThrownBy(() -> bookingService.getById(booker.getId(), Long.MAX_VALUE))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void listMethodsShouldSupportEveryBookingState() {
        LocalDateTime now = LocalDateTime.now();
        saveBooking(BookingStatus.APPROVED, now.minusHours(2), now.minusHours(1));
        saveBooking(BookingStatus.APPROVED, now.minusHours(1), now.plusHours(1));
        saveBooking(BookingStatus.APPROVED, now.plusHours(2), now.plusHours(3));
        saveBooking(BookingStatus.WAITING, now.plusHours(4), now.plusHours(5));
        saveBooking(BookingStatus.REJECTED, now.plusHours(6), now.plusHours(7));

        for (BookingState state : BookingState.values()) {
            assertThat(bookingService.getAllByBooker(booker.getId(), state)).isNotNull();
            assertThat(bookingService.getAllByOwner(owner.getId(), state)).isNotNull();
        }
    }

    private Booking saveBooking(BookingStatus status, LocalDateTime start, LocalDateTime end) {
        return persist(new Booking(null, start, end, item, booker, status));
    }

    private Booking findBooking(Long bookingId) {
        entityManager.flush();
        entityManager.clear();
        return entityManager.find(Booking.class, bookingId);
    }

    private <T> T persist(T entity) {
        entityManager.persist(entity);
        return entity;
    }
}
