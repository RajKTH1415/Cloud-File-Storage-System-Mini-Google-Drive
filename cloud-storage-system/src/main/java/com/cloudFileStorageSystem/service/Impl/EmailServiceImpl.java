package com.cloudFileStorageSystem.service.Impl;

import com.cloudFileStorageSystem.service.EmailService;
import lombok.RequiredArgsConstructor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender mailSender;

    @Override
    public void sendOtpEmail(String to, String otp) {

        SimpleMailMessage message = new SimpleMailMessage();

        message.setTo(to);
        message.setSubject("Password Reset OTP");

        message.setText(
                """
                Hello,

                Your password reset OTP is: %s

                This OTP is valid for 10 minutes.

                If you did not request this password reset, please ignore this email.

                Regards,
                Cloud File Storage Team
                """.formatted(otp)
        );

        mailSender.send(message);
    }
}