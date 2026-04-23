package com.loyaltyService.user_service.entity;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import java.time.Instant;
import java.util.UUID;
@Entity
@Table(name = "kyc_details",
        indexes = {
                @Index(name = "idx_kyc_user_id", columnList = "user_id"),
                @Index(name = "idx_kyc_status",  columnList = "status")
        })
@EntityListeners(AuditingEntityListener.class)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class KycDetail {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // Denormalized for direct admin queries without JOIN
    @Column(name = "user_id", insertable = false, updatable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "doc_type", nullable = false, length = 30)
    private DocType docType;

    @Column(name = "doc_number", nullable = false, length = 50)
    private String docNumber;

    @Column(name = "doc_file_path", length = 500)
    private String docFilePath;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
//    @Builder.Default
    private KycStatus status = KycStatus.NOT_SUBMITTED;

    @Column(name = "rejection_reason", length = 500)
    private String rejectionReason;

    @Column(name = "reviewed_by", length = 150)
    private String reviewedBy;

    @CreatedDate
    @Column(name = "submitted_at", updatable = false)
    private Instant submittedAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private Instant updatedAt;

    public enum DocType   { AADHAAR, PAN, PASSPORT, DRIVING_LICENSE }
    public enum KycStatus { NOT_SUBMITTED, PENDING, APPROVED, REJECTED }
}
