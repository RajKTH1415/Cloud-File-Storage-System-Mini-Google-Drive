package com.cloudFileStorageSystem.service.Impl;

import com.cloudFileStorageSystem.service.EmailService;
import lombok.RequiredArgsConstructor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
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

    @Async
    @Override
    public void sendVerificationEmail(String to, String token) {

        String verificationUrl =
                "http://localhost:8080/api/v1/auth/verify-email?token="
                        + token;

        String subject = "Verify Your Email Address";

        String body = """
                Hello,
                
                Welcome to Cloud File Storage System.
                
                Thank you for creating your account.
                
                To activate your account and start using the platform,
                please verify your email address by clicking the link below:
                
                %s
                
                This verification link is valid for 15 minutes.
                
                If you did not create this account, please ignore this email.
                
                Regards,
                Cloud File Storage Team
                """.formatted(verificationUrl);

        SimpleMailMessage message = new SimpleMailMessage();

        message.setTo(to);
        message.setSubject(subject);
        message.setText(body);

        mailSender.send(message);
    }
}