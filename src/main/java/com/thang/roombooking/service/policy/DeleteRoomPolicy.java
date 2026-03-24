package com.thang.roombooking.service.policy;

public interface DeleteRoomPolicy {
    default void validateCanDelete(Long roomId) {}
}
