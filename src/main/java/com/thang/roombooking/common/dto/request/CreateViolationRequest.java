package com.thang.roombooking.common.dto.request;
 
import com.thang.roombooking.common.enums.ViolationType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
 
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateViolationRequest {
 
    @NotNull(message = "User ID is required")
    private Long userId;
 
    private Long bookingId; // Optional, can be null for general misconduct
 
    @NotNull(message = "Violation type is required")
    private ViolationType type;
 
    @NotNull(message = "Reason is required")
    @Size(max = 500, message = "Reason cannot exceed 500 characters")
    private String reason;
 
    private Integer severityPoints; // If null, use system default for the type
}
