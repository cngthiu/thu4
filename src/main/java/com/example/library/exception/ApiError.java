package com.example.library.exception;
import java.time.Instant;
public record ApiError(String path, int status, String error, String message, Instant timestamp) {}
