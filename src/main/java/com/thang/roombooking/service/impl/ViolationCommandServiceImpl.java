package com.thang.roombooking.service.impl;
 
import com.thang.roombooking.common.constant.LogConstant;
import com.thang.roombooking.common.dto.request.CreateViolationRequest;
import com.thang.roombooking.common.dto.response.ViolationResponse;
import com.thang.roombooking.common.enums.ViolationSource;
import com.thang.roombooking.common.event.ViolationCreatedEvent;
import com.thang.roombooking.common.exception.AppException;
import com.thang.roombooking.common.exception.errorcode.AuthErrorCode;
import com.thang.roombooking.common.exception.errorcode.BookingErrorCode;
import com.thang.roombooking.entity.Booking;
import com.thang.roombooking.entity.BookingViolation;
import com.thang.roombooking.entity.UserAccount;
import com.thang.roombooking.infrastructure.configuration.PenaltyProperties;
import com.thang.roombooking.repository.BookingRepository;
import com.thang.roombooking.repository.BookingViolationRepository;
import com.thang.roombooking.repository.UserAccountRepository;
import com.thang.roombooking.service.ViolationCommandService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
 
import java.time.Instant;
 
@Service
@Slf4j
@RequiredArgsConstructor
public class ViolationCommandServiceImpl implements ViolationCommandService {
 
    private final BookingViolationRepository violationRepository;
    private final UserAccountRepository userRepository;
    private final BookingRepository bookingRepository;
    private final PenaltyProperties penaltyProperties;
    private final ApplicationEventPublisher eventPublisher;
 
    @Override
    @Transactional
    public ViolationResponse createManualViolation(CreateViolationRequest request, UserAccount admin) {
        log.info("{} | Admin {} creating manual violation for user {}", LogConstant.ACTION_START, admin.getUsername(), request.getUserId());
        
        UserAccount targetUser = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new AppException(AuthErrorCode.USER_NOT_FOUND, request.getUserId()));

        Booking booking = null;
        if (request.getBookingId() != null) {
            booking = bookingRepository.findById(request.getBookingId())
                    .orElseThrow(() -> new AppException(BookingErrorCode.BOOKING_NOT_FOUND));
        }

        int points = request.getSeverityPoints() != null ? request.getSeverityPoints() :
                penaltyProperties.getPoints().getOrDefault(request.getType(), 0);

        BookingViolation violation = BookingViolation.builder()
                .user(targetUser)
                .booking(booking)
                .type(request.getType())
                .reason(request.getReason())
                .severityPoints(points)
                .source(ViolationSource.ADMIN)
                .resolvedAt(Instant.now())
                .build();

        violationRepository.save(violation);

        // Trigger penalty evaluation
        eventPublisher.publishEvent(new ViolationCreatedEvent(violation.getId(), targetUser.getId()));

        log.info("Manual violation created successfully: ID {}", violation.getId());

        return ViolationResponse.builder()
                .id(violation.getId())
                .userId(targetUser.getId())
                .bookingId(booking != null ? booking.getId() : null)
                .type(violation.getType())
                .source(violation.getSource())
                .severityPoints(violation.getSeverityPoints())
                .reason(violation.getReason())
                .createdAt(violation.getCreatedAt())
                .build();
    }
}
