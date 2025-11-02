package com.example.library.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MailConfig {
    @Value("${spring.mail.host:}")
    private String mailHost;
    public boolean isMailEnabled() {
        return mailHost != null && !mailHost.isBlank();
    }
}
