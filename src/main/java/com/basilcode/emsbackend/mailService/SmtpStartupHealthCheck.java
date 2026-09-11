package com.basilcode.emsbackend.mailService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.stereotype.Component;

/**
 * {@code MailService.sendMail} runs async and only logs a warning on failure, so a bad SMTP
 * config (wrong host/port/credentials) otherwise surfaces as silently-unsent onboarding emails
 * rather than at boot. This checks the connection once at startup so it's loud immediately.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SmtpStartupHealthCheck implements ApplicationRunner {

    private final JavaMailSender mailSender;

    @Override
    public void run(ApplicationArguments args) {
        if (!(mailSender instanceof JavaMailSenderImpl impl)) {
            return;
        }
        try {
            impl.testConnection();
            log.info("SMTP connection check passed ({}:{})", impl.getHost(), impl.getPort());
        } catch (Exception e) {
            log.error("SMTP connection check FAILED ({}:{}) — emails (onboarding, password reset, "
                    + "notifications) will silently fail to send until this is fixed: {}",
                    impl.getHost(), impl.getPort(), e.getMessage());
        }
    }
}
