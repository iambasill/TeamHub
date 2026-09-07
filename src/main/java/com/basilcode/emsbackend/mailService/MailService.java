package com.basilcode.emsbackend.mailService;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class MailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String mailUsername;

    @Value("${app.mail.from-name}")
    private String fromName;

    /**
     * Sends an HTML email. Templates rendered via {@link #renderTemplate} are HTML fragments.
     * <p>
     * Runs on a dedicated executor and never throws back to the caller — an SMTP outage is
     * routine and must never block, slow down, or roll back the operation that triggered the
     * email (e.g. creating an employee, changing a password).
     */
    @Async("mailTaskExecutor")
    public void sendMail(String to, String subject, String htmlBody) {
        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, StandardCharsets.UTF_8.name());
            helper.setFrom(fromName + " <" + mailUsername + ">");
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlBody, true);
            mailSender.send(mimeMessage);
            log.info("Sent email to {} with subject '{}'", to, subject);
        } catch (MessagingException | RuntimeException e) {
            log.warn("Failed to send email to {} with subject '{}': {}", to, subject, e.getMessage());
        }
    }

    /**
     * Loads a template colocated with the calling module (e.g. "com/basilcode/emsbackend/employee/template/foo.txt")
     * and substitutes {{key}} placeholders with the given values.
     */
    public String renderTemplate(String classpathLocation, Map<String, String> variables) {
        String template;
        try {
            template = new String(
                    new ClassPathResource(classpathLocation).getInputStream().readAllBytes(),
                    StandardCharsets.UTF_8
            );
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to load mail template: " + classpathLocation, e);
        }

        for (Map.Entry<String, String> variable : variables.entrySet()) {
            template = template.replace("{{" + variable.getKey() + "}}", variable.getValue());
        }
        return template;
    }
}
