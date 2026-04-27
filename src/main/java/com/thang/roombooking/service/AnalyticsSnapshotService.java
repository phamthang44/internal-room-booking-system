package com.thang.roombooking.service;

import com.thang.roombooking.common.dto.response.DailyBookingTrendResponse;
import com.thang.roombooking.common.dto.response.RoomUtilizationResponse;
import com.thang.roombooking.common.dto.response.ViolationTrendResponse;

import java.time.LocalDate;
import java.util.List;

public interface AnalyticsSnapshotService {

    void refreshDailySnapshots(LocalDate from, LocalDate to);

    void refreshRoomUtilizationSnapshots(LocalDate weekStart);

    void refreshViolationDailySnapshots(LocalDate from, LocalDate to);

    List<DailyBookingTrendResponse> getDailyTrend(int days);

    List<RoomUtilizationResponse> getRoomStats(int weeks);

    List<ViolationTrendResponse> getViolationTrend(int weeks);
}
