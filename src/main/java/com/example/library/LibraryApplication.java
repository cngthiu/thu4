package com.example.library;

import com.example.library.util.Timezones;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;
import java.util.TimeZone;

@SpringBootApplication
@EnableScheduling
public class LibraryApplication {
    public static void main(String[] args) {
        TimeZone.setDefault(TimeZone.getTimeZone(Timezones.ASIA_HO_CHI_MINH));
        SpringApplication.run(LibraryApplication.class, args);
    }
}
