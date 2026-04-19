package com.thang.roombooking.service.impl;

import com.thang.roombooking.common.dto.request.ApprovalSearchRequest;
import com.thang.roombooking.common.dto.response.ApiResult;
import com.thang.roombooking.common.dto.response.ApprovalSummaryResponse;
import com.thang.roombooking.common.mapper.BookingMapper;
import com.thang.roombooking.entity.BookingApproval;
import com.thang.roombooking.repository.BookingApprovalRepository;
import com.thang.roombooking.service.BookingApprovalQueryService;
import jakarta.persistence.criteria.JoinType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class BookingApprovalQueryServiceImpl implements BookingApprovalQueryService {

    private final BookingApprovalRepository bookingApprovalRepository;
    private final BookingMapper bookingMapper;

    @Override
    @Transactional(readOnly = true)
    public ApiResult<List<ApprovalSummaryResponse>> searchApprovals(ApprovalSearchRequest request, com.thang.roombooking.entity.UserAccount currentUser) {
        log.info("searchApprovals | userId={} | keyword={} | status={} | page={} | size={}",
                currentUser.getId(), request.getKeyword(), request.getStatus(), request.getPage(), request.getSize());

        Specification<BookingApproval> spec = buildSpecification(request, currentUser);
        Pageable pageable = buildPageable(request);

        Page<BookingApproval> approvalsPage = bookingApprovalRepository.findAll(spec, pageable);

        List<ApprovalSummaryResponse> items = approvalsPage.getContent().stream()
                .map(bookingMapper::toApprovalSummaryResponse)
                .toList();

        return ApiResult.success(
                items,
                approvalsPage.getNumber() + 1,
                approvalsPage.getSize(),
                approvalsPage.getTotalElements()
        );
    }

    private Specification<BookingApproval> buildSpecification(ApprovalSearchRequest request, com.thang.roombooking.entity.UserAccount currentUser) {
        return (root, query, cb) -> {
            // Eagerly fetch relationships to avoid N+1
            if (query != null && query.getResultType() != Long.class && query.getResultType() != long.class) {
                var bookingJoin = root.fetch("booking", JoinType.LEFT);
                bookingJoin.fetch("user", JoinType.LEFT);
                bookingJoin.fetch("classroom", JoinType.LEFT);
                root.fetch("approver", JoinType.LEFT);
            }

            var predicates = new ArrayList<jakarta.persistence.criteria.Predicate>();

            // 1. Scoping to student's own requests
            var bookingJoin = root.join("booking", JoinType.LEFT);
            predicates.add(cb.equal(bookingJoin.get("user").get("id"), currentUser.getId()));

            // 2. Filter by keyword: room name (student name is always current user)
            if (hasText(request.getKeyword())) {
                String pattern = "%" + request.getKeyword().toLowerCase() + "%";
                predicates.add(cb.like(cb.lower(bookingJoin.get("classroom").get("roomName")), pattern));
            }

            // Filter by status (converts BookingStatus enum's name to String comparison if entity field is String)
            if (request.getStatus() != null) {
                predicates.add(cb.equal(root.get("approvalStatus"), request.getStatus().name()));
            }

            // Filter by date range
            if (request.getFromDate() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt").as(java.time.LocalDate.class), request.getFromDate()));
            }
            if (request.getToDate() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("createdAt").as(java.time.LocalDate.class), request.getToDate()));
            }

            return cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        };
    }

    private Pageable buildPageable(ApprovalSearchRequest request) {
        int page = Math.max(request.getPage() - 1, 0);
        return PageRequest.of(page, request.getSize(), Sort.by("createdAt").descending());
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
