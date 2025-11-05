package com.example.library.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

@Service
public class CoverStorageService {
    private final Path rootDirectory;

    public CoverStorageService(@Value("${library.covers.dir:uploads/covers}") String directory) throws IOException {
        this.rootDirectory = Paths.get(directory).toAbsolutePath().normalize();
        Files.createDirectories(this.rootDirectory);
    }

    public String store(MultipartFile file, String existingPath) {
        if (file == null || file.isEmpty()) {
            return existingPath;
        }
        String extension = extractExtension(file.getOriginalFilename());
        String filename = UUID.randomUUID().toString() + (extension.isEmpty() ? "" : "." + extension);
        Path target = rootDirectory.resolve(filename);
        try {
            Files.createDirectories(rootDirectory);
            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
            if (existingPath != null && !existingPath.isBlank() && !existingPath.equals(filename)) {
                delete(existingPath);
            }
            return filename;
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to store cover image", ex);
        }
    }

    public void delete(String path) {
        if (path == null || path.isBlank()) {
            return;
        }
        Path target = rootDirectory.resolve(path).normalize();
        if (!target.startsWith(rootDirectory)) {
            return;
        }
        try {
            Files.deleteIfExists(target);
        } catch (IOException ignored) {
        }
    }

    private String extractExtension(String filename) {
        if (!StringUtils.hasText(filename)) {
            return "";
        }
        String name = filename.trim();
        int dot = name.lastIndexOf('.');
        if (dot <= 0 || dot == name.length() - 1) {
            return "";
        }
        return name.substring(dot + 1);
    }
}
