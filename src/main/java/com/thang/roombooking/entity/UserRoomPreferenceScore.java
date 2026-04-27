package com.thang.roombooking.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(
        name = "user_room_preference_scores",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_user_classroom_pref",
                columnNames = {"user_id", "classroom_id"}
        )
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserRoomPreferenceScore {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private UserAccount user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "classroom_id", nullable = false)
    private Classroom classroom;

    @Column(name = "view_count", nullable = false)
    private int viewCount;

    @Column(name = "click_count", nullable = false)
    private int clickCount;

    @Column(name = "dismiss_count", nullable = false)
    private int dismissCount;

    @Column(name = "cancel_count", nullable = false)
    private int cancelCount;

    @Column(name = "avg_rating", precision = 3, scale = 2)
    private BigDecimal avgRating;

    @Column(name = "behavior_score", precision = 8, scale = 2, nullable = false)
    private BigDecimal behaviorScore;

    @Column(name = "computed_at", nullable = false)
    private Instant computedAt;
}
