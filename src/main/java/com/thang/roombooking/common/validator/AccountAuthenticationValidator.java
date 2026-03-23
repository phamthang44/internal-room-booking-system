package com.thang.roombooking.common.validator;


import com.thang.roombooking.common.constant.BlockWords;
import com.thang.roombooking.common.constant.CommonConfig;
import org.springframework.util.StringUtils;

import java.util.Locale;
import java.util.regex.Pattern;

public class AccountAuthenticationValidator {

    private AccountAuthenticationValidator() {
    }

    private static final Pattern PASSWORD_PATTERN =
            Pattern.compile("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z0-9]).{10,}$");


    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$");

    public static boolean isValidPassword(String password) {
        return StringUtils.hasText(password) && PASSWORD_PATTERN.matcher(password).matches();
    }

    public static boolean isValidFullName(String fullName) {
        if (!StringUtils.hasText(fullName)) {
            return false;
        }
        String lowerName = fullName.toLowerCase(Locale.ROOT);
        for (String badWord : BlockWords.getBadWords()) {
            if (lowerName.contains(badWord)) {
                return false;
            }
        }
        return fullName.length() <= CommonConfig.MAX_LENGTH_FULLNAME;
    }

    public static boolean isValidUsername(String username) {
        return isValidFullName(username);
    }

    public static boolean isValidEmail(String email) {
        return StringUtils.hasText(email) && EMAIL_PATTERN.matcher(email).matches();
    }

    public static boolean validate(String email, String password, String fullName, String username, String confirmPassword) {
        return isValidEmail(email) && isValidPassword(password) && isValidFullName(fullName) && isValidUsername(username) && validatePassword(password, confirmPassword);
    }

    public static boolean validatePassword(String password, String confirmPassword) {
        return password.equals(confirmPassword);
    }

}
