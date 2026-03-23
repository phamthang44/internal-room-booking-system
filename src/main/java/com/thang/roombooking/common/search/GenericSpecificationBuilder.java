package com.thang.roombooking.common.search;

import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

import static com.thang.roombooking.common.search.SearchOperation.ZERO_OR_MORE_REGEX;

public class GenericSpecificationBuilder<T> {

    private final List<SpecSearchCriteria> params;
    private final List<Specification<T>> specifications;

    public GenericSpecificationBuilder() {
        this.params = new ArrayList<>();
        this.specifications = new ArrayList<>();
    }

    public GenericSpecificationBuilder<T> with(String key, String operation, Object value, String prefix, String suffix) {
        return with(null, key, operation, value, prefix, suffix);
    }

    public GenericSpecificationBuilder<T> with(String orPredicate, String key, String operation, Object value, String prefix, String suffix) {
        SearchOperation searchOperation;
        
        // Handle multi-char operations first (in, not_in, is_null, is_not_null)
        String opLower = operation.toLowerCase();
        if (opLower.equals("in")) {
            searchOperation = SearchOperation.IN;
        } else if (opLower.equals("not_in") || opLower.equals("notin")) {
            searchOperation = SearchOperation.NOT_IN;
        } else {
            // Single-char operations
            searchOperation = SearchOperation.getSimpleOperation(operation.charAt(0));
        }

        if (searchOperation == SearchOperation.EQUALITY) {
            boolean startWithAsterisk = prefix != null && prefix.contains(ZERO_OR_MORE_REGEX);
            boolean endWithAsterisk = suffix != null && suffix.contains(ZERO_OR_MORE_REGEX);
            if (startWithAsterisk && endWithAsterisk) {
                searchOperation = SearchOperation.CONTAINS;
            } else if (startWithAsterisk) {
                searchOperation = SearchOperation.ENDS_WITH;
            } else if (endWithAsterisk) {
                searchOperation = SearchOperation.STARTS_WITH;
            }
        }
        params.add(new SpecSearchCriteria(orPredicate, key, searchOperation, value));
        return this;
    }

    public GenericSpecificationBuilder<T> addSpecification(Specification<T> specification) {
        this.specifications.add(specification);
        return this;
    }

    public GenericSpecificationBuilder<T> with(
            String key,
            SearchOperation operation,
            Object value,
            String prefix,
            String suffix
    ) {
        params.add(new SpecSearchCriteria(null, key, operation, value));
        return this;
    }

    public Specification<T> build() {
        if (params.isEmpty() && specifications.isEmpty()) return null;

        Specification<T> result = null;

        if (!params.isEmpty()) {
            result = new GenericSpecification<>(params.get(0));
            for (int i = 1; i < params.size(); i++) {
                result = Boolean.TRUE.equals(params.get(i).getOrPredicate())
                        ? Specification.where(result).or(new GenericSpecification<>(params.get(i)))
                        : Specification.where(result).and(new GenericSpecification<>(params.get(i)));
            }
        }

        if (!specifications.isEmpty()) {
            for (Specification<T> spec : specifications) {
                result = (result == null) ? spec : Specification.where(result).and(spec);
            }
        }

        return result;
    }
}