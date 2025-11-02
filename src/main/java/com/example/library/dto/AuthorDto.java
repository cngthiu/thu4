package com.example.library.dto;

import jakarta.validation.constraints.NotBlank;

public record AuthorDto(
        Long id,
        @NotBlank String name,
        String nationality
) {}
