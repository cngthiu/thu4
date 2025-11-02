package com.example.library.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record MemberDto(
        Long id,
        @NotBlank String fullName,
        @Email @NotBlank String email,
        String phone,
        String status
) {}
