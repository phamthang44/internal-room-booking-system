package com.thang.roombooking.entity;

import com.thang.roombooking.common.enums.BehaviorEventType;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(
        name = "user_behavior_events",
        indexes = {
                @Index(name = "idx_behavior_user_type", columnList = "user_id, event_type"),
                @Index(name = "idx_behavior_entity", columnList = "entity_type, entity_id")
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserBehaviorEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private UserAccount user;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 50)
    private BehaviorEventType eventType;

    @Column(name = "entity_type", length = 30)
    private String entityType;

    @Column(name = "entity_id")
    private Long entityId;

    @Column(name = "metadata", columnDefinition = "text")
    private String metadata;

    @Column(name = "session_id", length = 36)
    private String sessionId;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    @PrePersist
    protected void onPersist() {
        if (occurredAt == null) {
            occurredAt = Instant.now();
        }
    }
}
