package com.gmail.merikbest2015.ecommerce.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring5.SpringTemplateEngine;

import javax.annotation.PreDestroy;
import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Service
@RequiredArgsConstructor
public class MailService {

    private final JavaMailSender mailSender;
    private final SpringTemplateEngine thymeleafTemplateEngine;

    @Value("${spring.mail.username}")
    private String username;

    @Value("${hostname}")
    private String hostname;

    private final ExecutorService mailExecutor = Executors.newFixedThreadPool(
            Math.max(1, Runtime.getRuntime().availableProcessors()));

    public void sendMessageHtml(String to, String subject, String template, Map<String, Object> attributes) {
        mailExecutor.execute(() -> {
            attributes.put("url", "http://" + hostname);
            Context thymeleafContext = new Context();
            thymeleafContext.setVariables(attributes);
            String htmlBody = thymeleafTemplateEngine.process("email/" + template, thymeleafContext);
            MimeMessage message = mailSender.createMimeMessage();
            try {
                MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
                helper.setFrom(username);
                helper.setTo(to);
                helper.setSubject(subject);
                helper.setText(htmlBody, true);
                mailSender.send(message);
            } catch (MessagingException e) {
                throw new RuntimeException(e);
            }
        });
    }

    @PreDestroy
    public void cleanup() {
        if (mailExecutor != null) {
            mailExecutor.shutdown();
        }
    }
}
