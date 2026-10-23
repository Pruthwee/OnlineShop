package com.gmail.merikbest2015.ecommerce.configuration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueRequest;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueResponse;

import javax.annotation.PostConstruct;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Configuration
@Slf4j
public class SecretsManagerConfig {

    @Value("${DB_SECRET_NAME:}")
    private String dbSecretName;

    @Value("${MAIL_SECRET_NAME:}")
    private String mailSecretName;

    @Value("${RECAPTCHA_SECRET_NAME:}")
    private String recaptchaSecretName;

    private final Map<String, CachedSecret> secretCache = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper = new ObjectMapper();

    private SecretsManagerClient secretsClient;

    @PostConstruct
    public void init() {
        // Client built lazily on first use, not at class load
    }

    private SecretsManagerClient getSecretsClient() {
        if (secretsClient == null) {
            secretsClient = SecretsManagerClient.create();
        }
        return secretsClient;
    }

    public Map<String, String> getDatabaseCredentials() {
        if (dbSecretName == null || dbSecretName.isEmpty()) {
            return Collections.emptyMap();
        }
        try {
            return parseSecretAsMap(getSecretValue(dbSecretName));
        } catch (Exception e) {
            log.warn("Failed to retrieve database credentials from Secrets Manager", e);
            return Collections.emptyMap();
        }
    }

    public String getMailPassword() {
        if (mailSecretName == null || mailSecretName.isEmpty()) {
            return null;
        }
        try {
            return getSecretValue(mailSecretName);
        } catch (Exception e) {
            log.warn("Failed to retrieve mail password from Secrets Manager", e);
            return null;
        }
    }

    public String getRecaptchaSecret() {
        if (recaptchaSecretName == null || recaptchaSecretName.isEmpty()) {
            return null;
        }
        try {
            return getSecretValue(recaptchaSecretName);
        } catch (Exception e) {
            log.warn("Failed to retrieve reCAPTCHA secret from Secrets Manager", e);
            return null;
        }
    }

    private String getSecretValue(String secretName) {
        CachedSecret cached = secretCache.get(secretName);
        if (cached != null && !cached.isExpired(300)) {
            return cached.getValue();
        }

        try {
            GetSecretValueRequest request = GetSecretValueRequest.builder()
                    .secretId(secretName)
                    .build();
            GetSecretValueResponse response = getSecretsClient().getSecretValue(request);
            String secretString = response.secretString();
            secretCache.put(secretName, new CachedSecret(secretString));
            return secretString;
        } catch (SdkException e) {
            log.warn("AWS SDK error retrieving secret: " + secretName, e);
            throw e;
        }
    }

    private Map<String, String> parseSecretAsMap(String secretString) {
        try {
            JsonNode node = objectMapper.readTree(secretString);
            Map<String, String> result = new ConcurrentHashMap<>();
            node.fieldNames().forEachRemaining(field -> result.put(field, node.get(field).asText()));
            return result;
        } catch (Exception e) {
            log.warn("Failed to parse secret as JSON map", e);
            return Collections.emptyMap();
        }
    }

    private static class CachedSecret {
        private final String value;
        private final long timestamp;

        CachedSecret(String value) {
            this.value = value;
            this.timestamp = System.currentTimeMillis();
        }

        boolean isExpired(int ttlSeconds) {
            return (System.currentTimeMillis() - timestamp) > (ttlSeconds * 1000L);
        }

        String getValue() {
            return value;
        }
    }
}
