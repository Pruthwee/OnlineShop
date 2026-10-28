package com.gmail.merikbest2015.ecommerce.service.impl;

import com.gmail.merikbest2015.ecommerce.constants.ErrorMessage;
import com.gmail.merikbest2015.ecommerce.constants.SuccessMessage;
import com.gmail.merikbest2015.ecommerce.domain.Role;
import com.gmail.merikbest2015.ecommerce.domain.User;
import com.gmail.merikbest2015.ecommerce.dto.response.CaptchaResponse;
import com.gmail.merikbest2015.ecommerce.dto.response.MessageResponse;
import com.gmail.merikbest2015.ecommerce.dto.request.UserRequest;
import com.gmail.merikbest2015.ecommerce.repository.UserRepository;
import com.gmail.merikbest2015.ecommerce.service.RegistrationService;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import javax.annotation.PreDestroy;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Service
@RequiredArgsConstructor
public class RegistrationServiceImpl implements RegistrationService {

    private final UserRepository userRepository;
    private final MailService mailService;
    private final PasswordEncoder passwordEncoder;
    private final RestTemplate restTemplate;
    private final ModelMapper modelMapper;

    private ExecutorService captchaExecutor;

    @Value("${recaptcha.url}")
    private String captchaUrl;

    @Value("${recaptcha.secret}")
    private String secret;

    @Override
    @Transactional
    public MessageResponse registration(String captchaResponse, UserRequest userRequest) {
        if (userRequest.getPassword() != null && !userRequest.getPassword().equals(userRequest.getPassword2())) {
            return new MessageResponse("passwordError", ErrorMessage.PASSWORDS_DO_NOT_MATCH);
        }
        if (userRepository.findByEmail(userRequest.getEmail()) != null) {
            return new MessageResponse("emailError", ErrorMessage.EMAIL_IN_USE);
        }

        String url = String.format(captchaUrl, secret, captchaResponse);
        CaptchaResponse response;
        try {
            response = verifyCaptchaAsync(url).join();
        } catch (CompletionException e) {
            if (e.getCause() instanceof RuntimeException) {
                throw (RuntimeException) e.getCause();
            }
            throw e;
        }
        if (!response.isSuccess()) {
            return new MessageResponse("captchaError", ErrorMessage.CAPTCHA_ERROR);
        }
        User user = modelMapper.map(userRequest, User.class);
        user.setActive(false);
        user.setRoles(Collections.singleton(Role.USER));
        user.setActivationCode(UUID.randomUUID().toString());
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        userRepository.save(user);
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("firstName", user.getFirstName());
        attributes.put("activationCode", "/registration/activate/" + user.getActivationCode());
        mailService.sendMessageHtml(user.getEmail(), "Activation code", "registration-template", attributes);
        return new MessageResponse("alert-success", SuccessMessage.ACTIVATION_CODE_SEND);
    }

    @Override
    @Transactional
    public MessageResponse activateEmailCode(String code) {
        User user = userRepository.findByActivationCode(code);

        if (user == null) {
            return new MessageResponse("alert-danger", ErrorMessage.ACTIVATION_CODE_NOT_FOUND);
        }
        user.setActivationCode(null);
        user.setActive(true);
        userRepository.save(user);
        return new MessageResponse("alert-success", SuccessMessage.USER_ACTIVATED);
    }

    private CompletableFuture<CaptchaResponse> verifyCaptchaAsync(String url) {
        return CompletableFuture
                .supplyAsync(() -> restTemplate.postForObject(url, Collections.emptyList(), CaptchaResponse.class),
                        getCaptchaExecutor());
    }

    private synchronized ExecutorService getCaptchaExecutor() {
        if (captchaExecutor == null) {
            captchaExecutor = Executors.newFixedThreadPool(Math.max(1, Runtime.getRuntime().availableProcessors()));
        }
        return captchaExecutor;
    }

    @PreDestroy
    public synchronized void shutdownCaptchaExecutor() {
        if (captchaExecutor != null) {
            captchaExecutor.shutdown();
            captchaExecutor = null;
        }
    }
}
