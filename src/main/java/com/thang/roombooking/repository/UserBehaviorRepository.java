package com.thang.roombooking.repository;

import com.thang.roombooking.entity.UserBehaviorEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface UserBehaviorRepository extends JpaRepository<UserBehaviorEvent, Long> {

    @Modifying
    @Query(
            value = """
                    INSERT INTO user_behavior_events (user_id, event_type, entity_type, entity_id, metadata, occurred_at)
                    VALUES (:userId, :eventType, :entityType, :entityId, :metadata, NOW())
                    """,
            nativeQuery = true
    )
    void insertEvent(
            @Param("userId") Long userId,
            @Param("eventType") String eventType,
            @Param("entityType") String entityType,
            @Param("entityId") Long entityId,
            @Param("metadata") String metadata
    );

    /**
     * Aggregates classroom-related events per (user, classroom, event type) for the rollup job.
     * Returns rows of [userId, classroomId, eventType, count].
     */
    @Query("""
            SELECT e.user.id, e.entityId, e.eventType, COUNT(e.id)
            FROM UserBehaviorEvent e
            WHERE e.entityType = 'CLASSROOM'
              AND e.entityId IS NOT NULL
              AND e.occurredAt >= :cutoff
            GROUP BY e.user.id, e.entityId, e.eventType
            """)
    List<Object[]> aggregateClassroomEvents(@Param("cutoff") Instant cutoff);

    @Modifying
    @Query("DELETE FROM UserBehaviorEvent e WHERE e.occurredAt < :cutoff")
    int deleteEventsBefore(@Param("cutoff") Instant cutoff);
}
