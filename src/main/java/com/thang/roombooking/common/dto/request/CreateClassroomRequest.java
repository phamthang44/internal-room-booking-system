package com.thang.roombooking.common.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import org.hibernate.validator.constraints.URL;

import java.util.List;

public record CreateClassroomRequest(
        @Schema(description = "Classroom name", example = "Athena-204")
        @NotBlank(message = "{validation.classroom.room.name.required}")
        @Size(min = 1, max = 50, message = "{validation.classroom.room.name.size}")
        String roomName,

        @NotNull(message = "{validation.classroom.room.building.id.required}")
        @Schema(description = "Building ID", example = "1")
        @Positive(message = "{validation.id.must_be_positive}")
        Long buildingId,

        @Schema(description = "Classroom capacity", example = "20")
        @NotNull(message = "{validation.classroom.room.capacity.required}")
        @Min(value = 10, message = "{validation.classroom.room.capacity.min}")
        @Max(value = 200, message = "{validation.classroom.room.capacity.max}")
        Integer capacity,

        @Positive(message = "{validation.classroom.capacity.positive}")
        @NotNull(message = "{validation.classroom.room.type.id.required}")
        Integer roomTypeId,

        @NotEmpty(message = "{validation.classroom.equipments.not_empty}")
        List<EquipmentRequest> equipments,

        @Size(max = 5, message = "{validation.classroom.images.max_limit}")
        @NotEmpty(message = "{validation.classroom.image_urls.not_empty}")
        List<@NotBlank(message= "{validation.classroom.image_urls.not_blank}")
             @URL(message = "{validation.classroom.image_urls.invalid}") String> imageUrls,

        @NotNull(message = "{validation.classroom.room.status.required}")
        Boolean isActive
) implements BaseClassroomRequest {
}
