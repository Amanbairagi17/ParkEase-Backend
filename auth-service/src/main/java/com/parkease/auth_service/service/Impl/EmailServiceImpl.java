package com.parkease.auth_service.service.Impl;

import com.parkease.auth_service.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailServiceImpl implements EmailService {

    @Value("${brevo.api-key}")
    private String apiKey;

    @Value("${brevo.sender.email}")
    private String senderEmail;

    @Value("${brevo.sender.name}")
    private String senderName;

    private final RestTemplate restTemplate;

    // COMMON METHOD (used internally)
    private void send(String toEmail, String subject, String htmlContent) {
      //  log.info("Brevo API Key: {}", apiKey);
        String url = "https://api.brevo.com/v3/smtp/email";

        Map<String, Object> body = new HashMap<>();

        body.put("sender", Map.of(
                "email", senderEmail,
                "name", senderName
        ));

        body.put("to", List.of(
                Map.of("email", toEmail)
        ));

        body.put("subject", subject);
        body.put("htmlContent", htmlContent);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("api-key", apiKey);

        HttpEntity<Map<String, Object>> request =
                new HttpEntity<>(body, headers);

        try {
            ResponseEntity<String> response =
                    restTemplate.postForEntity(url, request, String.class);

            log.info("Brevo Status: {}", response.getStatusCode());
            log.info("Brevo Response: {}", response.getBody());

        } catch (Exception e) {
            log.error("Brevo Email Error: {}", e.getMessage());
        }
    }

    // SEND OTP EMAIL
    @Override
    public void sendOtp(String toEmail, String otp) {

        String htmlContent = """
        <html>
          <body style="font-family: Arial, sans-serif;">
            <h2>Password Reset OTP</h2>
            <p>Your OTP is:</p>
            <h3 style="color:blue;">%s</h3>
            <p>This OTP is valid for 5 minutes.</p>
          </body>
        </html>
        """.formatted(otp);

        send(toEmail, "Your OTP Code", htmlContent);
    }

    // SEND VERIFICATION EMAIL
    @Override
    public void sendVerificationEmail(String toEmail, String verificationLink) {

        String htmlContent = """
        <html>
          <body style="font-family: Arial, sans-serif;">
            <h2>Email Verification</h2>
            <p>Click the button below to verify your account:</p>
            
            <a href="%s" 
               style="display:inline-block;padding:10px 20px;
                      background-color:#007bff;color:white;
                      text-decoration:none;border-radius:5px;">
                Verify Account
            </a>

            <p>If the button doesn't work, use this link:</p>
            <p>%s</p>
          </body>
        </html>
        """.formatted(verificationLink, verificationLink);

        send(toEmail, "Verify Your Email", htmlContent);
    }

}