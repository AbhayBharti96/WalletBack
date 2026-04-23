package com.loyaltyService.user_service.entity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;
@Entity
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AuditLog {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "user_id") private Long userId;
    @Column(name = "action", nullable = false, length = 100) private String action;
    @Column(name = "entity_type", length = 50) private String entityType;
    @Column(name = "entity_id", length = 100) private String entityId;
    @Column(name = "performed_by", length = 150) private String performedBy;
    @Column(name = "details", length = 1000) private String details;
    @CreationTimestamp @Column(name = "created_at", updatable = false) private LocalDateTime createdAt;
}
