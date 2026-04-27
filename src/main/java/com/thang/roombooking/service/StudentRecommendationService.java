package com.thang.roombooking.service;

import com.thang.roombooking.common.dto.response.RoomRecommendationResponse;

import java.time.LocalDate;
import java.util.List;

public interface StudentRecommendationService {

    List<RoomRecommendationResponse> getRecommendations(Long userId, Integer attendees, LocalDate date);
}
