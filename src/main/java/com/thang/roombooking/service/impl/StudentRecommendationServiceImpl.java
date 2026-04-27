package com.thang.roombooking.service.impl;

import com.thang.roombooking.common.dto.response.RoomRecommendationResponse;
import com.thang.roombooking.common.enums.BookingStatus;
import com.thang.roombooking.common.enums.TranslatableEntityType;
import com.thang.roombooking.entity.Classroom;
import com.thang.roombooking.entity.UserRoomPreferenceScore;
import com.thang.roombooking.repository.BookingRepository;
import com.thang.roombooking.repository.ClassroomRepository;
import com.thang.roombooking.repository.UserRoomPreferenceScoreRepository;
import com.thang.roombooking.service.StudentRecommendationService;
import com.thang.roombooking.service.TranslationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StudentRecommendationServiceImpl implements StudentRecommendationService {

    private final BookingRepository bookingRepository;
    private final ClassroomRepository classroomRepository;
    private final UserRoomPreferenceScoreRepository preferenceScoreRepository;
    private final TranslationService translationService;

    private static final List<BookingStatus> COMPLETED_STATUSES =
            List.of(BookingStatus.COMPLETED, BookingStatus.CHECKED_IN);

    @Override
    public List<RoomRecommendationResponse> getRecommendations(Long userId, Integer attendees, LocalDate date) {
        // 1. Determine minimum capacity — use request param or fall back to user's historical avg
        int minCapacity;
        if (attendees != null && attendees > 0) {
            minCapacity = attendees;
        } else {
            Double avg = bookingRepository.avgActualAttendeesByUser(userId);
            minCapacity = avg != null ? (int) Math.ceil(avg) : 1;
        }
        int effectiveAttendees = attendees != null && attendees > 0 ? attendees : minCapacity;

        // 2. Fetch user's booking history grouped by classroom (top 10)
        List<Object[]> historyRows = bookingRepository.findTopClassroomsByUser(
                userId, COMPLETED_STATUSES, PageRequest.of(0, 10));

        Map<Long, long[]> historyMap = new HashMap<>();
        Map<Long, Double> avgAttendeesMap = new HashMap<>();
        for (Object[] row : historyRows) {
            Long classroomId = (Long) row[0];
            long count = (Long) row[1];
            Double avgAtt = (Double) row[2];
            historyMap.put(classroomId, new long[]{count});
            if (avgAtt != null) avgAttendeesMap.put(classroomId, avgAtt);
        }

        // 3. Fetch available classrooms matching minimum capacity (JOIN FETCH building + roomType)
        List<Classroom> candidates = classroomRepository.findAvailableWithMinCapacity(minCapacity);

        // 4. Fetch pre-computed behavior preference scores for this user
        Map<Long, UserRoomPreferenceScore> behaviorScores = preferenceScoreRepository
                .findByUserIdWithClassroom(userId)
                .stream()
                .collect(Collectors.toMap(s -> s.getClassroom().getId(), s -> s));

        // 5. Score every candidate room, then return top 5
        record ScoredRoom(double score, RoomRecommendationResponse response) {}

        List<ScoredRoom> scored = new ArrayList<>();
        for (Classroom room : candidates) {
            long[] history = historyMap.get(room.getId());
            int bookingCount = history != null ? (int) history[0] : 0;
            Double avgAtt = avgAttendeesMap.get(room.getId());

            double score = bookingCount * 3.0;

            // Capacity match bonus: room fits the group without wasting more than 50% extra space
            if (room.getCapacity() >= effectiveAttendees
                    && room.getCapacity() <= effectiveAttendees * 1.5) {
                score += 5;
            }

            // Attendance rate bonus: user actually fills the room well
            if (avgAtt != null && room.getCapacity() > 0
                    && avgAtt / room.getCapacity() > 0.5) {
                score += 2;
            }

            // Behavior signal bonus (clicks, views) minus dismissal/cancellation penalties
            UserRoomPreferenceScore pref = behaviorScores.get(room.getId());
            if (pref != null && pref.getBehaviorScore() != null) {
                score += pref.getBehaviorScore().doubleValue();
            }

            String reasonKey = historyMap.containsKey(room.getId())
                    ? "recommendation.reason.frequent"
                    : "recommendation.reason.capacity_match";

            String buildingName = room.getBuilding() != null
                    ? translationService.getTranslation(
                            TranslatableEntityType.BUILDING, room.getBuilding().getId(), "name")
                    : null;
            String roomTypeName = room.getRoomType() != null
                    ? translationService.getTranslation(
                            TranslatableEntityType.ROOM_TYPE, room.getRoomType().getId(), "name")
                    : null;

            scored.add(new ScoredRoom(score, RoomRecommendationResponse.builder()
                    .classroomId(room.getId())
                    .roomName(room.getRoomName())
                    .buildingName(buildingName)
                    .roomTypeName(roomTypeName)
                    .capacity(room.getCapacity())
                    .status(room.getStatus())
                    .bookingCount(bookingCount)
                    .avgActualAttendees(avgAtt != null ? BigDecimal.valueOf(avgAtt) : null)
                    .reasonKey(reasonKey)
                    .build()));
        }

        return scored.stream()
                .sorted(Comparator.comparingDouble(ScoredRoom::score).reversed())
                .limit(5)
                .map(ScoredRoom::response)
                .toList();
    }
}
