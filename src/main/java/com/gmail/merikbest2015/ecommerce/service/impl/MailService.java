package com.gmail.merikbest2015.ecommerce.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring5.SpringTemplateEngine;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class MailService {

    private final JavaMailSender mailSender;
    private final SpringTemplateEngine thymeleafTemplateEngine;

    @Value("${spring.mail.username}")
    private String username;

    @Value("${hostname}")
    private String hostname;

    private ExecutorService executor;

    private synchronized ExecutorService getExecutor() {
        if (executor == null) {
            // Dynamic thread pool sizing based on available processors
            int availableProcessors = Runtime.getRuntime().availableProcessors();
            int corePoolSize = Math.max(2, availableProcessors);
            int maxPoolSize = corePoolSize * 2;
            int queueCapacity = corePoolSize * 10;

            executor = new ThreadPoolExecutor(
                    corePoolSize,
                    maxPoolSize,
                    60L, TimeUnit.SECONDS,
                    new LinkedBlockingQueue<>(queueCapacity),
                    new ThreadPoolExecutor.CallerRunsPolicy()
            );
        }
        return executor;
    }

    public void sendMessageHtml(String to, String subject, String template, Map<String, Object> attributes) {
        ExecutorService executor = getExecutor();
        executor.execute(() -> {
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
}
