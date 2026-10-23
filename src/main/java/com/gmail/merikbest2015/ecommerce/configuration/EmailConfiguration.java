package com.gmail.merikbest2015.ecommerce.configuration;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import software.amazon.awssdk.core.exception.SdkException;

import java.util.Properties;

@Configuration
@Slf4j
public class EmailConfiguration {

    @Value("${spring.mail.host}")
    private String host;

    @Value("${spring.mail.username}")
    private String username;

    @Value("${spring.mail.port}")
    private int port;

    @Value("${spring.mail.protocol}")
    private String protocol;

    @Value("${spring.mail.properties.mail.smtp.auth}")
    private String auth;

    @Value("${spring.mail.properties.mail.smtp.starttls.enable}")
    private String enable;

    @Value("${mail.debug}")
    private String debug;

    private final SecretsManagerConfig secretsManagerConfig;

    public EmailConfiguration(SecretsManagerConfig secretsManagerConfig) {
        this.secretsManagerConfig = secretsManagerConfig;
    }

    @Bean
    public JavaMailSender getMailSender() {
        JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
        mailSender.setHost(host);
        mailSender.setPort(port);
        mailSender.setUsername(username);

        // Read mail password from AWS Secrets Manager instead of hardcoded value
        String mailPassword = null;
        try {
            mailPassword = secretsManagerConfig.getMailPassword();
        } catch (SdkException e) {
            log.warn("Failed to retrieve mail password from Secrets Manager; mail sending may fail", e);
        }
        if (mailPassword != null) {
            mailSender.setPassword(mailPassword);
        }

        Properties mailProperties = mailSender.getJavaMailProperties();
        mailProperties.setProperty("mail.transport.protocol", protocol);
        mailProperties.setProperty("mail.debug", debug);
        mailProperties.setProperty("mail.smtp.auth", auth);
        mailProperties.setProperty("mail.smtp.starttls.enable", enable);
        return mailSender;
    }
}
