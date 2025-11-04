package com.example.library;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

@SpringBootApplication
public class MailTestApplication {
    public static void main(String[] args) {
        SpringApplication.run(MailTestApplication.class, args);
    }
}

@Component
class MailTestRunner implements CommandLineRunner {

    @Autowired
    private JavaMailSender mailSender;

    @Override
    public void run(String... args) throws Exception {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo("nominhooh247@gmail.com"); // 👈 Thay bằng email thật của bạn để kiểm tra nhận
        message.setSubject("[Test] Gửi email từ Spring Boot");
        message.setText("Xin chào,\n\nĐây là email test gửi qua Outlook SMTP.\n\nTrân trọng!");

        try {
            mailSender.send(message);
            System.out.println("✅ Gửi email thành công!");
        } catch (Exception e) {
            System.out.println("❌ Lỗi khi gửi email: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
