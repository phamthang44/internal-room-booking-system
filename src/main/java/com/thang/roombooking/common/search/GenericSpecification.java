package com.thang.roombooking.common.search;

import jakarta.persistence.criteria.*;
import org.springframework.data.jpa.domain.Specification;

public class GenericSpecification<T> implements Specification<T> {

    private final SpecSearchCriteria specSearchCriteria;

    public GenericSpecification(SpecSearchCriteria criteria) {
        this.specSearchCriteria = criteria;
    }

    @Override
    public Predicate toPredicate(Root<T> root, CriteriaQuery<?> query, CriteriaBuilder criteriaBuilder) {
        // 1. Lấy Path (Hỗ trợ nested như category.id)
        Path<?> path = getPath(root, specSearchCriteria.getKey());

        // 2. Lấy Value
        Object value = specSearchCriteria.getValue();

        return switch (specSearchCriteria.getOperation()) {
            // Các phép so sánh bằng/số
            case EQUALITY -> criteriaBuilder.equal(path, value);
            case NEGATION -> criteriaBuilder.notEqual(path, value);
            case GREATER_THAN -> criteriaBuilder.greaterThan(path.as(String.class), value.toString());
            case LESS_THAN -> criteriaBuilder.lessThan(path.as(String.class), value.toString());
            case GREATER_THAN_OR_EQUAL -> criteriaBuilder.greaterThanOrEqualTo(path.as(String.class), value.toString());
            case LESS_THAN_OR_EQUAL -> criteriaBuilder.lessThanOrEqualTo(path.as(String.class), value.toString());

            // 🔥 CÁC PHÉP SEARCH STRING (Fix Case-Insensitive & Nested Path)
            // Phải ép path về lower và value về lower
            case LIKE -> criteriaBuilder.like(
                    criteriaBuilder.lower(path.as(String.class)),
                    value.toString().toLowerCase()
            );
            case STARTS_WITH -> criteriaBuilder.like(
                    criteriaBuilder.lower(path.as(String.class)),
                    value.toString().toLowerCase() + "%"
            );
            case ENDS_WITH -> criteriaBuilder.like(
                    criteriaBuilder.lower(path.as(String.class)),
                    "%" + value.toString().toLowerCase()
            );
            case CONTAINS -> criteriaBuilder.like(
                    criteriaBuilder.lower(path.as(String.class)),
                    "%" + value.toString().toLowerCase() + "%"
            );

            // Fix nested cho IN clause & Fix lỗi Unsupported Types value khi value là List
            // Sử dụng explicit loop để tránh lỗi binding List as single param
            // Fix nested cho IN clause & Fix lỗi Unsupported Types value khi value là List
            // Sử dụng explicit loop để tránh lỗi binding List as single param
            case IN -> {
                // Determine the type from the value to give Hibernate a hint
                if (value instanceof java.util.Collection<?> collection && !collection.isEmpty()) {
                    Object firstItem = collection.iterator().next();
                    if (firstItem instanceof Long) {
                        // Hint type as Long to fix UNKNOWN type binding
                        CriteriaBuilder.In<Long> inClause = criteriaBuilder.in(path.as(Long.class));
                        for (Object item : collection) {
                            inClause.value((Long) item);
                        }
                        yield inClause;
                    } else if (firstItem instanceof String) {
                        CriteriaBuilder.In<String> inClause = criteriaBuilder.in(path.as(String.class));
                         for (Object item : collection) {
                            inClause.value((String) item);
                        }
                        yield inClause;
                    }
                }
                
                // Fallback for other types or single value
                CriteriaBuilder.In<Object> inClause = criteriaBuilder.in(path);
                if (value instanceof java.util.Collection<?> collection) {
                    for (Object item : collection) {
                        inClause.value(item);
                    }
                } else {
                    inClause.value(value);
                }
                yield inClause;
            }

            default -> null;
        };
    }

    // 🔥 HÀM QUAN TRỌNG: Hỗ trợ Nested Path (category.id)
    private Path<?> getPath(Root<T> root, String attributeName) {
        Path<?> path = root; // Khởi tạo path từ root

        // Nếu key là "category.id" -> Split ra và đi sâu vào từng cấp
        if (attributeName.contains(".")) {
            for (String part : attributeName.split("\\.")) {
                path = path.get(part);
            }
        } else {
            path = root.get(attributeName);
        }
        // Không ép kiểu bừa bãi về Path<String> nữa
        return path;
    }
}