package com.example.library;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

@Component

public class EmailTestRunner implements CommandLineRunner {

    @Autowired
    private JavaMailSender mailSender;

    @Override
    public void run(String... args) {
        try {
            SimpleMailMessage msg = new SimpleMailMessage();
            msg.setTo("nominhooh247@gmail.com");
            msg.setSubject("[Library Test] Gửi thử Gmail SMTP");
            msg.setText("Xin chào!\n\nEmail này được gửi từ Spring Boot qua Gmail SMTP.\n\nThân ái!");

            mailSender.send(msg);
            System.out.println("✅ Đã gửi email test thành công!");
        } catch (Exception e) {
            System.err.println("❌ Lỗi gửi mail: " + e.getMessage());
        }
    }
}
