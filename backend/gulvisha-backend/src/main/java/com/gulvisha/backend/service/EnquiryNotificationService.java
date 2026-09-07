package com.gulvisha.backend.service;

import com.gulvisha.backend.entity.QuoteRequestEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EnquiryNotificationService {

    private static final Logger logger = LoggerFactory.getLogger(EnquiryNotificationService.class);

    private final JavaMailSender mailSender;
    private final boolean enabled;
    private final String recipient;
    private final String fromAddress;

    public EnquiryNotificationService(
            ObjectProvider<JavaMailSender> mailSenderProvider,
            @Value("${MAIL_ENABLED:false}") boolean enabled,
            @Value("${MAIL_NOTIFICATION_TO:}") String recipient,
            @Value("${MAIL_FROM:no-reply@gulvisha.local}") String fromAddress
    ) {
        this.mailSender = mailSenderProvider.getIfAvailable();
        this.enabled = enabled;
        this.recipient = recipient;
        this.fromAddress = fromAddress;
    }

    public void notifyNewEnquiry(QuoteRequestEntity enquiry) {
        if (!enabled || mailSender == null || recipient.isBlank()) {
            return;
        }

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromAddress);
        message.setTo(recipient);
        message.setSubject("New Gulvisha enquiry from " + enquiry.getName());
        message.setText("A new enquiry was received.\n\n"
                + "Name: " + enquiry.getName() + "\n"
                + "Email: " + enquiry.getEmail() + "\n"
                + "Company: " + (enquiry.getCompany() == null ? "-" : enquiry.getCompany()) + "\n"
                + "Service: " + enquiry.getService() + "\n\n"
                + enquiry.getDetails());

        try {
            mailSender.send(message);
        } catch (RuntimeException exception) {
            logger.warn("Unable to send new enquiry notification for {}", enquiry.getId(), exception);
        }
    }
}