package com.gmail.merikbest2015.ecommerce.service.impl;

import com.gmail.merikbest2015.ecommerce.constants.ErrorMessage;
import com.gmail.merikbest2015.ecommerce.constants.SuccessMessage;
import com.gmail.merikbest2015.ecommerce.domain.Order;
import com.gmail.merikbest2015.ecommerce.domain.Perfume;
import com.gmail.merikbest2015.ecommerce.domain.User;
import com.gmail.merikbest2015.ecommerce.dto.request.PerfumeRequest;
import com.gmail.merikbest2015.ecommerce.dto.request.SearchRequest;
import com.gmail.merikbest2015.ecommerce.dto.response.MessageResponse;
import com.gmail.merikbest2015.ecommerce.dto.response.UserInfoResponse;
import com.gmail.merikbest2015.ecommerce.repository.OrderRepository;
import com.gmail.merikbest2015.ecommerce.repository.PerfumeRepository;
import com.gmail.merikbest2015.ecommerce.repository.UserRepository;
import com.gmail.merikbest2015.ecommerce.service.AdminService;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import javax.annotation.PreDestroy;
import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AdminServiceImpl implements AdminService {

    private static final String UPLOAD_KEY_PREFIX = "uploads/";

    @Value("${upload.s3.bucket:}")
    private String uploadBucket;

    private S3Client s3Client;

    private final UserRepository userRepository;
    private final PerfumeRepository perfumeRepository;
    private final OrderRepository orderRepository;
    private final ModelMapper modelMapper;

    @Override
    public Page<Perfume> getPerfumes(Pageable pageable) {
        return perfumeRepository.findAllByOrderByPriceAsc(pageable);
    }

    @Override
    public Page<Perfume> searchPerfumes(SearchRequest request, Pageable pageable) {
        return perfumeRepository.searchPerfumes(request.getSearchType(), request.getText(), pageable);
    }

    @Override
    public Page<User> getUsers(Pageable pageable) {
        return userRepository.findAll(pageable);
    }

    @Override
    public Page<User> searchUsers(SearchRequest request, Pageable pageable) {
        return userRepository.searchUsers(request.getSearchType(), request.getText(), pageable);
    }

    @Override
    public Order getOrder(Long orderId) {
        return orderRepository.getById(orderId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, ErrorMessage.ORDER_NOT_FOUND));
    }

    @Override
    public Page<Order> getOrders(Pageable pageable) {
        return orderRepository.findAll(pageable);

    }

    @Override
    public Page<Order> searchOrders(SearchRequest request, Pageable pageable) {
        return orderRepository.searchOrders(request.getSearchType(), request.getText(), pageable);
    }

    @Override
    public Perfume getPerfumeById(Long perfumeId) {
        return perfumeRepository.findById(perfumeId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, ErrorMessage.PERFUME_NOT_FOUND));
    }

    @Override
    @SneakyThrows
    @Transactional
    public MessageResponse editPerfume(PerfumeRequest perfumeRequest, MultipartFile file) {
        return savePerfume(perfumeRequest, file, SuccessMessage.PERFUME_EDITED);
    }

    @Override
    @SneakyThrows
    @Transactional
    public MessageResponse addPerfume(PerfumeRequest perfumeRequest, MultipartFile file) {
        return savePerfume(perfumeRequest, file, SuccessMessage.PERFUME_ADDED);
    }

    @Override
    public UserInfoResponse getUserById(Long userId, Pageable pageable) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, ErrorMessage.USER_NOT_FOUND));
        Page<Order> orders = orderRepository.findOrderByUserId(userId, pageable);
        return new UserInfoResponse(user, orders);
    }

    private MessageResponse savePerfume(PerfumeRequest perfumeRequest, MultipartFile file, String message) throws IOException {
        Perfume perfume = modelMapper.map(perfumeRequest, Perfume.class);
        if (file != null && !file.getOriginalFilename().isEmpty()) {
            String uuidFile = UUID.randomUUID().toString();
            String resultFilename = uuidFile + "." + file.getOriginalFilename();
            uploadToS3(file, resultFilename);
            perfume.setFilename(resultFilename);
        }
        perfumeRepository.save(perfume);
        return new MessageResponse("alert-success", message);
    }

    private void uploadToS3(MultipartFile file, String filename) throws IOException {
        if (uploadBucket == null || uploadBucket.isEmpty()) {
            throw new IOException("APP_S3_BUCKET is not set");
        }
        PutObjectRequest.Builder putRequest = PutObjectRequest.builder()
                .bucket(uploadBucket)
                .key(UPLOAD_KEY_PREFIX + filename);
        if (file.getContentType() != null) {
            putRequest.contentType(file.getContentType());
        }
        try (InputStream inputStream = file.getInputStream()) {
            getS3Client().putObject(putRequest.build(), RequestBody.fromInputStream(inputStream, file.getSize()));
        } catch (SdkException e) {
            throw new IOException("Failed to upload " + filename + " to S3", e);
        }
    }

    private synchronized S3Client getS3Client() {
        if (s3Client == null) {
            s3Client = S3Client.create();
        }
        return s3Client;
    }

    @PreDestroy
    public synchronized void closeS3Client() {
        if (s3Client != null) {
            s3Client.close();
            s3Client = null;
        }
    }
}
