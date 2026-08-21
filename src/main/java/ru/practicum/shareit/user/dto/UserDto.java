package ru.practicum.shareit.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import ru.practicum.shareit.validation.OnCreate;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class UserDto {
    private Long id;

    @NotBlank(groups = OnCreate.class, message = "Имя пользователя не должно быть пустым")
    @Pattern(regexp = ".*\\S.*", message = "Имя пользователя не должно быть пустым")
    private String name;

    @NotBlank(groups = OnCreate.class, message = "Email пользователя не должен быть пустым")
    @Email(message = "Email пользователя имеет неверный формат")
    @Pattern(regexp = ".*\\S.*", message = "Email пользователя не должен быть пустым")
    private String email;
}
