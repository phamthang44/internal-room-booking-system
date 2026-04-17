package com.thang.roombooking.infrastructure.mail.core;

import java.util.Map;

/**
 * Template rendering abstraction so we can swap Thymeleaf out later if needed.
 */
public interface TemplateRenderer {

    String render(String templateName, Map<String, Object> variables);
}

