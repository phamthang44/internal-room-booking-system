package com.thang.roombooking.repository;

import com.thang.roombooking.entity.UserRoomPreferenceScore;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserRoomPreferenceScoreRepository extends JpaRepository<UserRoomPreferenceScore, Long> {

    @Query("SELECT s FROM UserRoomPreferenceScore s JOIN FETCH s.classroom WHERE s.user.id = :userId")
    List<UserRoomPreferenceScore> findByUserIdWithClassroom(@Param("userId") Long userId);

    @Modifying
    @Query(value = "DELETE FROM user_room_preference_scores", nativeQuery = true)
    void deleteAllScores();

    @Modifying
    @Query(
            value = """
                    INSERT INTO user_room_preference_scores
                        (user_id, classroom_id, view_count, click_count, dismiss_count, cancel_count, behavior_score, computed_at)
                    VALUES (:userId, :classroomId, :viewCount, :clickCount, :dismissCount, 0, :behaviorScore, NOW())
                    """,
            nativeQuery = true
    )
    void insertScore(
            @Param("userId") long userId,
            @Param("classroomId") long classroomId,
            @Param("viewCount") int viewCount,
            @Param("clickCount") int clickCount,
            @Param("dismissCount") int dismissCount,
            @Param("behaviorScore") double behaviorScore
    );
}
