package com.thang.roombooking.service;

import com.thang.roombooking.common.dto.request.AdminBookingSearchRequest;
import com.thang.roombooking.common.dto.request.BookingSearchRequest;
import com.thang.roombooking.common.dto.response.AdminBookingDetailResponse;
import com.thang.roombooking.common.dto.response.AdminBookingListResponse;
import com.thang.roombooking.common.dto.response.ApiResult;
import com.thang.roombooking.common.dto.response.BookingDetailResponse;
import com.thang.roombooking.common.dto.response.StudentDashboardResponse;
import com.thang.roombooking.entity.UserAccount;

import java.util.List;

public interface BookingQueryService {

    /**
     * Returns the full detail of a single booking.
     * Validates that {@code currentUser} owns the booking (or has ADMIN/STAFF role).
     *
     * @param id          booking primary key
     * @param currentUser authenticated user from the security context
     */
    BookingDetailResponse getBookingDetail(Long id, UserAccount currentUser);

    /**
     * Public paginated search for bookings owned by the current user.
     * Returns items + pagination metadata wrapped in {@link ApiResult}.
     *
     * @param request     filter + pagination parameters
     * @param currentUser authenticated user (bookings are scoped to this user)
     */
    ApiResult<List<BookingDetailResponse>> searchPublic(BookingSearchRequest request, UserAccount currentUser);

    StudentDashboardResponse getStudentDashboard(Long userId);

    /**
     * Admin paginated search across all bookings (ADMIN/STAFF only at controller layer).
     * Returns items + pagination metadata wrapped in {@link ApiResult}.
     */
    ApiResult<List<AdminBookingListResponse>> searchAdmin(AdminBookingSearchRequest request, UserAccount currentUser);

    /**
     * Full admin view of a booking, including user and room context.
     * Caller must enforce ADMIN/STAFF authorization.
     */
    AdminBookingDetailResponse getAdminBookingDetail(Long id, UserAccount currentUser);

}
