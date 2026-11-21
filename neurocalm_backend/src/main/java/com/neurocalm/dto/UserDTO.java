package com.neurocalm.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import java.time.Instant;

/** Used to create a user (id/createdAt ignored) and to return one. */
public record UserDTO(Long id, @NotBlank String name, @NotBlank @Email String email, Instant createdAt) { }
