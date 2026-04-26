package com.thang.roombooking.common.dto.response;

import com.thang.roombooking.common.enums.RoomStatus;
import lombok.*;

import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class ClassroomListResponse {
    private Long classroomId;
    private String buildingName;
    private String roomName;
    private int capacity;
    private RoomStatus status;
    private List<EquipmentResponse> equipments;
    private String roomType;

    // ─── Availability metrics for the queried date ─────────────────────────────
    /** True when ALL queried time-slots are free on the given bookingDate. */
    private boolean isAvailableForQuery;

    /**
     * Per-slot breakdown for only the slots the user selected.
     * Empty when no timeSlotIds filter was applied (show full dailySchedule instead).
     */
    private List<SlotStatus> queriedSlotsStatus;

    /** How many of the queried slots are still available (quick badge helper). */
    private int availableSlotCount;

    /** Total number of slots queried (= timeSlotIds.size(), 0 when not filtered). */
    private int totalQueriedSlots;

    /** Full daily schedule for all slots on the bookingDate. */
    private DateAvailability dailySchedule;

    private String imageUrl;
}
