package su.foxogram.services;

import lombok.extern.slf4j.Slf4j;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import su.foxogram.configs.EmailConfig;
import su.foxogram.models.Code;
import su.foxogram.constants.EmailConstants;
import su.foxogram.repositories.CodeRepository;
import su.foxogram.util.Algorithm;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

@Slf4j
@Service
public class EmailService {

    private final CodeRepository codeRepository;
    private final ResourceLoader resourceLoader;
    private final JavaMailSender javaMailSender;
    private final EmailConfig emailConfig;

    @Autowired
    public EmailService(JavaMailSender javaMailSender, ResourceLoader resourceLoader, CodeRepository codeRepository, EmailConfig emailConfig) {
        this.javaMailSender = javaMailSender;
        this.resourceLoader = resourceLoader;
        this.codeRepository = codeRepository;
        this.emailConfig = emailConfig;
    }

    @Async
    public void sendEmail(String to, long id, String type, String username, String digitCode, long issuedAt, long expiresAt, String token) {
        MimeMessage mimeMessage = javaMailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, StandardCharsets.UTF_8.name());

        try {
            helper.setTo(to);
            helper.setFrom(emailConfig.getEmail());

            String subject = getSubjectByType(type);
            String htmlContent = getEmailContentByType(username, digitCode, token);

            helper.setSubject(subject);
            helper.setText(htmlContent, true);

            javaMailSender.send(mimeMessage);

            saveCode(id, type, digitCode, issuedAt, expiresAt);
        } catch (IllegalArgumentException | MessagingException | IOException e) {
            log.error("Error occurred while sending email to {}: {}", to, e.getMessage(), e);
        }
    }

    private String getSubjectByType(String type) {
        type = type.toUpperCase();

        return switch (EmailConstants.Type.valueOf(type)) {
            case DELETE -> "Confirm Your Account Deletion";
            case CONFIRM -> "Confirm Your Email Address";
            default -> throw new IllegalArgumentException("Invalid email type: " + type);
        };
    }

    private String getEmailContentByType(String username, String digitCode, String token) throws IOException {
        return readHTML().replace("{0}", username).replace("{1}", digitCode);//.replace("{2}", token);
    }

    private String readHTML() throws IOException {
        String templateName = "email";
        Resource resource = resourceLoader.getResource("classpath:email/templates/" + templateName + ".html");

        if (!resource.exists()) {
            log.error("Template not found: {}", templateName);
            throw new IOException("Template file not found: " + templateName);
        }

        try (InputStream inputStream = resource.getInputStream()) {
            return Algorithm.inputStreamToString(inputStream, StandardCharsets.UTF_8);
        }
    }

    private void saveCode(long id, String type, String digitCode, long issuedAt, long expiresAt) {
        Code code = new Code(id, type, digitCode, issuedAt, expiresAt);
        codeRepository.save(code);
    }
}
