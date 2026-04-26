package com.thang.roombooking.common.utils;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.thang.roombooking.infrastructure.i18n.I18nUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Utility for handling dynamic, localizable penalty reasons.
 * Reasons are stored in the database as a JSON array of {@link ReasonPart} objects.
 */
@Slf4j
@Component
public class PenaltyReasonUtils {

    private final ObjectMapper objectMapper;

    public PenaltyReasonUtils(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * Creates an initial structured reason.
     */
    public String createInitialReason(String key, Object... args) {
        try {
            List<ReasonPart> parts = new ArrayList<>();
            parts.add(new ReasonPart(key, List.of(args), null));
            return objectMapper.writeValueAsString(parts);
        } catch (Exception e) {
            log.error("Failed to create initial penalty reason", e);
            return key; // Fallback to raw key
        }
    }

    /**
     * Appends a new structured part to an existing reason.
     */
    public String appendReason(String existingReason, String key, Object... args) {
        try {
            List<ReasonPart> parts = parseToParts(existingReason);
            parts.add(new ReasonPart(key, List.of(args), null));
            return objectMapper.writeValueAsString(parts);
        } catch (Exception e) {
            log.error("Failed to append penalty reason", e);
            return existingReason; // Return original on failure
        }
    }

    /**
     * Translates the structured reason into a single localized string.
     */
    public String translate(String reason) {
        if (reason == null || reason.isBlank()) {
            return "";
        }

        // 1. Try parsing as JSON array (Standard format)
        if (reason.startsWith("[")) {
            try {
                List<ReasonPart> parts = objectMapper.readValue(reason, new TypeReference<List<ReasonPart>>() {});
                return parts.stream()
                        .map(this::translatePart)
                        .filter(s -> !s.isEmpty())
                        .collect(Collectors.joining(""));
            } catch (Exception e) {
                log.warn("Failed to parse penalty reason array: {}", reason);
            }
        }

        // 2. Try parsing as single JSON object (Legacy/Intermediate format)
        if (reason.startsWith("{")) {
            try {
                ReasonPart part = objectMapper.readValue(reason, ReasonPart.class);
                return translatePart(part);
            } catch (Exception e) {
                log.warn("Failed to parse penalty reason object: {}", reason);
            }
        }

        // 3. Fallback: try translating as a plain key or return as is (Legacy raw strings)
        try {
            return I18nUtils.get(reason);
        } catch (Exception e) {
            return reason;
        }
    }

    private String translatePart(ReasonPart part) {
        if (part.key() != null) {
            Object[] args = part.args() != null ? part.args().toArray() : new Object[0];
            return I18nUtils.get(part.key(), args);
        }
        return part.raw() != null ? part.raw() : "";
    }

    private List<ReasonPart> parseToParts(String reason) {
        if (reason == null || reason.isBlank()) {
            return new ArrayList<>();
        }

        if (reason.startsWith("[")) {
            try {
                return objectMapper.readValue(reason, new TypeReference<List<ReasonPart>>() {});
            } catch (Exception e) {
                log.warn("Could not parse reason array, treating as raw");
            }
        } else if (reason.startsWith("{")) {
            try {
                ReasonPart part = objectMapper.readValue(reason, ReasonPart.class);
                List<ReasonPart> parts = new ArrayList<>();
                parts.add(part);
                return parts;
            } catch (Exception e) {
                log.warn("Could not parse reason object, treating as raw");
            }
        }

        // Treat as raw legacy string
        List<ReasonPart> parts = new ArrayList<>();
        parts.add(new ReasonPart(null, null, reason));
        return parts;
    }

    /**
     * Represents a single part of a penalty reason.
     */
    public record ReasonPart(String key, List<Object> args, String raw) {
        // Constructor for Jackson
        public ReasonPart {
        }
    }
}
