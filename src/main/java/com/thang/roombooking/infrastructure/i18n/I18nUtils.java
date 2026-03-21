package com.thang.roombooking.infrastructure.i18n;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.Locale;

public class I18nUtils {

    private I18nUtils() {}

    public static String get(String key) {
        return Translator.toLocale(key);
    }
    
    public static String get(String key, Object... args) {
        return Translator.toLocale(key, args);
    }

    public static String formatCurrency(BigDecimal amount, Locale locale) {
        // Tự động detect currency dựa trên Locale
        // Ví dụ Locale.US -> USD, Locale("vi", "VN") -> VND
        NumberFormat formatter = NumberFormat.getCurrencyInstance(locale);
        return formatter.format(amount);
    }
}
