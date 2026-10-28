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

    private static final int MAIL_THREADS_PER_PROCESSOR = 2;

    private final JavaMailSender mailSender;
    private final SpringTemplateEngine thymeleafTemplateEngine;

    private ExecutorService executor;

    @Value("${spring.mail.username}")
    private String username;

    @Value("${hostname}")
    private String hostname;

    public void sendMessageHtml(String to, String subject, String template, Map<String, Object> attributes) {
        getExecutor().execute(() -> {
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

    private synchronized ExecutorService getExecutor() {
        if (executor == null) {
            // Pool size follows the CPUs the JVM sees (it honours the container's CPU limit); the OS schedules the threads
            int poolSize = Math.max(1, Runtime.getRuntime().availableProcessors()) * MAIL_THREADS_PER_PROCESSOR;
            executor = Executors.newFixedThreadPool(poolSize);
        }
        return executor;
    }

    @PreDestroy
    public synchronized void shutdownExecutor() {
        if (executor != null) {
            executor.shutdown();
            executor = null;
        }
    }
}
