package com.example.library.config;

import java.nio.file.Path;
import java.nio.file.Paths;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {
    private final String coversDir;

    public WebConfig(@Value("${library.covers.dir:uploads/covers}") String coversDir) {
        this.coversDir = coversDir;
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        Path coverPath = Paths.get(coversDir).toAbsolutePath().normalize();
        registry.addResourceHandler("/covers/**")
                .addResourceLocations(coverPath.toUri().toString());
    }
}
