package com.thang.roombooking.infrastructure.idempotency.config;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Idempotent {
    String keyPrefix() default "";
    long expirationInSeconds() default 3600; // Mặc định lưu 1 tiếng
}
