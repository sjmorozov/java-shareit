package ru.practicum.shareit.booking.dto;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.JsonTest;
import org.springframework.boot.test.json.JacksonTester;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@JsonTest
class BookingCreateDtoJsonTest {
    @Autowired
    private JacksonTester<BookingCreateDto> json;

    @Test
    void shouldSerializeLocalDateTimeInIsoFormat() throws Exception {
        LocalDateTime start = LocalDateTime.of(2030, 1, 2, 3, 4, 5);
        LocalDateTime end = LocalDateTime.of(2030, 1, 2, 5, 4, 5);
        BookingCreateDto booking = new BookingCreateDto(42L, start, end);

        assertThat(json.write(booking))
                .extractingJsonPathNumberValue("@.itemId")
                .isEqualTo(42);
        assertThat(json.write(booking))
                .extractingJsonPathStringValue("@.start")
                .isEqualTo("2030-01-02T03:04:05");
        assertThat(json.write(booking))
                .extractingJsonPathStringValue("@.end")
                .isEqualTo("2030-01-02T05:04:05");
    }

    @Test
    void shouldDeserializeIsoDates() throws Exception {
        String content = "{" +
                "\"itemId\":42," +
                "\"start\":\"2030-01-02T03:04:05\"," +
                "\"end\":\"2030-01-02T05:04:05\"" +
                "}";

        BookingCreateDto booking = json.parseObject(content);

        assertThat(booking.getItemId()).isEqualTo(42L);
        assertThat(booking.getStart()).isEqualTo(LocalDateTime.of(2030, 1, 2, 3, 4, 5));
        assertThat(booking.getEnd()).isEqualTo(LocalDateTime.of(2030, 1, 2, 5, 4, 5));
    }
}
