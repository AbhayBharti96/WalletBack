package com.loyaltyService.user_service.service.impl;

import com.loyaltyService.user_service.client.WalletServiceClient;
import com.loyaltyService.user_service.dto.KycStatusResponse;
import com.loyaltyService.user_service.entity.AuditLog;
import com.loyaltyService.user_service.entity.KycDetail;
import com.loyaltyService.user_service.entity.User;
import com.loyaltyService.user_service.exception.DuplicateKycException;
import com.loyaltyService.user_service.exception.ResourceNotFoundException;
import com.loyaltyService.user_service.mapper.KycMapper;
import com.loyaltyService.user_service.repository.AuditLogRepository;
import com.loyaltyService.user_service.repository.KycRepository;
import com.loyaltyService.user_service.repository.UserRepository;
import com.loyaltyService.user_service.service.CloudinaryService;
import com.loyaltyService.user_service.service.KafkaProducerService;
import com.loyaltyService.user_service.service.KycStatusResolver;
import com.loyaltyService.user_service.service.KycService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.nio.file.*;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class KycServiceImpl implements KycService {

    private final UserRepository      userRepo;
    private final KycRepository       kycRepo;
    private final AuditLogRepository  auditRepo;
    private final WalletServiceClient walletServiceClient;
    private final KafkaProducerService kafkaProducer;
    private final KycMapper kycMapper;
    private final CloudinaryService cloudinaryService;
    private final KycStatusResolver kycStatusResolver;


    @Value("${kyc.upload-dir:uploads/kyc}")
        private String uploadDir;

        // ── SUBMIT ───────────────────────────────────────────────────────────────
        @Override
        @Transactional
        public KycStatusResponse submitKyc(Long userId, KycDetail.DocType docType,
                String docNumber, MultipartFile docFile) {
            User user = findUser(userId);

            if (kycRepo.existsByUserIdAndStatus(userId, KycDetail.KycStatus.APPROVED))
                throw new DuplicateKycException("KYC already approved for this user");

//            String filePath = null;
//            if (docFile != null && !docFile.isEmpty()) {
//                try {
//                    Path dir = Paths.get(uploadDir, userId.toString());
//                    Files.createDirectories(dir);
//                    String fname = docType.name() + "_" + System.currentTimeMillis()
//                            + "_" + docFile.getOriginalFilename();
//                    filePath = dir.resolve(fname).toString();
//                    Files.copy(docFile.getInputStream(), Paths.get(filePath),
//                            StandardCopyOption.REPLACE_EXISTING);
//                } catch (IOException e) {
//                    throw new RuntimeException("Failed to store KYC document", e);
//                }
//            }

            String filePath = null;

            if (docFile != null && !docFile.isEmpty()) {
                filePath = cloudinaryService.uploadFile(
                        docFile,
                        userId,
                        docType.name()
                );
            }

            KycDetail kyc = KycDetail.builder()
                    .user(user).docType(docType)
                    .docNumber(docNumber).docFilePath(filePath)
                    .status(KycDetail.KycStatus.PENDING)
                    .build();

            KycDetail saved = kycRepo.save(kyc);

            auditRepo.save(AuditLog.builder()
                    .userId(userId).action("KYC_SUBMITTED")
                    .entityType("KycDetail").entityId(saved.getId().toString())
                .performedBy(user.getEmail())
                .details("DocType: " + docType + ", DocNumber: " + docNumber)
                .build());

        log.info("KYC submitted: userId={}, docType={}, kycId={}", userId, docType, saved.getId());
        return kycMapper.toResponse(saved);
    }

    // ── STATUS ────────────────────────────────────────────────────────────────
    @Override
    public KycStatusResponse getStatus(Long userId) {
        User user = findUser(userId);
        if (user.getRole() == User.Role.ADMIN) {
            return KycStatusResponse.builder()
                    .userId(userId)
                    .userName(user.getName())
                    .userEmail(user.getEmail())
                    .status(KycStatusResolver.APPROVED)
                    .build();
        }

        return kycRepo.findFirstByUserIdOrderBySubmittedAtDesc(userId)
                .map(kycMapper::toResponse)
                .orElse(KycStatusResponse.builder()
                        .userId(userId)
                        .status(kycStatusResolver.resolve(user))
                        .build());
    }

    // ── PENDING LIST (admin) ──────────────────────────────────────────────────
    @Override
    public Page<KycStatusResponse> getPendingKyc(Pageable pageable) {
        return kycRepo.findByStatusOrderBySubmittedAtDesc(KycDetail.KycStatus.PENDING, pageable)
                .map(kycMapper::toResponse);
    }

    // ── APPROVE by KYC id (admin) ─────────────────────────────────────────────
    @Override
    @Transactional
    public KycStatusResponse approve(Long kycId, String adminEmail) {
        KycDetail kyc = findKyc(kycId);
        return doApprove(kyc, adminEmail);
    }

    // ── APPROVE by USER id (admin) ────────────────────────────────────────────
    @Override
    @Transactional
    public KycStatusResponse approveByUserId(Long userId, String adminEmail) {
        KycDetail kyc = kycRepo
                .findFirstByUserIdAndStatusOrderBySubmittedAtDesc(userId, KycDetail.KycStatus.PENDING)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No pending KYC found for userId: " + userId));
        return doApprove(kyc, adminEmail);
    }

    // ── REJECT by KYC id (admin) ──────────────────────────────────────────────
    @Override
    @Transactional
    public KycStatusResponse reject(Long kycId, String reason, String adminEmail) {
        KycDetail kyc = findKyc(kycId);
        return doReject(kyc, reason, adminEmail);
    }

    // ── REJECT by USER id (admin) ─────────────────────────────────────────────
    @Override
    @Transactional
    public KycStatusResponse rejectByUserId(Long userId, String reason, String adminEmail) {
        KycDetail kyc = kycRepo
                .findFirstByUserIdAndStatusOrderBySubmittedAtDesc(userId, KycDetail.KycStatus.PENDING)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No pending KYC found for userId: " + userId));
        return doReject(kyc, reason, adminEmail);
    }

    // ── DASHBOARD COUNTS ──────────────────────────────────────────────────────
    @Override
    public long countPending() {
        return kycRepo.countByStatus(KycDetail.KycStatus.PENDING);
    }

    @Override
    public long countApprovedToday() {
        return kycRepo.countByStatusSince(KycDetail.KycStatus.APPROVED,
                Instant.now().truncatedTo(ChronoUnit.DAYS));
    }

    @Override
    public long countRejectedToday() {
        return kycRepo.countByStatusSince(KycDetail.KycStatus.REJECTED,
                Instant.now().truncatedTo(ChronoUnit.DAYS));
    }

    // ── PRIVATE HELPERS ───────────────────────────────────────────────────────

    /**
     * Core approve logic — shared by approve(kycId) and approveByUserId(userId).
     * Wallet and reward accounts are created HERE on approval, not on submission.
     */
    private KycStatusResponse doApprove(KycDetail kyc, String adminEmail) {

        if (kyc.getStatus() == KycDetail.KycStatus.APPROVED)
            throw new DuplicateKycException("KYC already approved");

        if (kyc.getStatus() != KycDetail.KycStatus.PENDING)
            throw new IllegalStateException("Only pending KYC can be approved");

        kyc.setStatus(KycDetail.KycStatus.APPROVED);
        kyc.setReviewedBy(adminEmail);
        KycDetail saved = kycRepo.save(kyc);

        Long userId = kyc.getUser().getId();

        auditRepo.save(AuditLog.builder()
                .userId(userId).action("KYC_APPROVED")
                .entityType("KycDetail").entityId(kyc.getId().toString())
                .performedBy(adminEmail).build());

        Map<String, Object> event = Map.of(
                "event", "KYC_APPROVED",
                "userId", userId,
                "reference", "KYC_" + kyc.getId(),
                "status", "APPROVED"
        );

        kafkaProducer.send("kyc-events", event);

        try {
            walletServiceClient.createWallet(userId);
            log.info("Wallet created for userId={} after KYC approval", userId);
        } catch (Exception e) {
            log.error("Failed to create wallet for userId={}", userId, e);
        }

        log.info("KYC approved: kycId={}, userId={}, by={}", kyc.getId(), userId, adminEmail);
        return kycMapper.toResponse(saved);
    }

    private KycStatusResponse doReject(KycDetail kyc, String reason, String adminEmail) {
        if (kyc.getStatus() != KycDetail.KycStatus.PENDING)
            throw new IllegalStateException("Only pending KYC can be rejected");
        kyc.setStatus(KycDetail.KycStatus.REJECTED);
        kyc.setRejectionReason(reason);
        kyc.setReviewedBy(adminEmail);
        KycDetail saved = kycRepo.save(kyc);

        auditRepo.save(AuditLog.builder()
                .userId(kyc.getUser().getId()).action("KYC_REJECTED")
                .entityType("KycDetail").entityId(kyc.getId().toString())
                .performedBy(adminEmail).details("Reason: " + reason).build());

        log.info("KYC rejected: kycId={}, userId={}, by={}", kyc.getId(), kyc.getUser().getId(), adminEmail);
        Long userId = kyc.getUser().getId();
        Map<String, Object> event = Map.of(
                "event", "KYC_REJECTED",
                "userId", userId,
                "reference", "KYC_" + kyc.getId(),
                "status", "REJECTED",
                "reason", reason
        );

        kafkaProducer.send("kyc-events", event);
        return kycMapper.toResponse(saved);
    }

    private KycDetail findKyc(Long id) {
        return kycRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("KYC record not found: " + id));
    }

    private User findUser(Long userId) {
        return userRepo.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));
    }

}
