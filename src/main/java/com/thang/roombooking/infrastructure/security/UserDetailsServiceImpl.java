package com.thang.roombooking.infrastructure.security;

import com.thang.roombooking.entity.UserAccount;
import com.thang.roombooking.infrastructure.i18n.I18nUtils;
import com.thang.roombooking.repository.UserAccountRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserAccountRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String identifier) throws UsernameNotFoundException {
        UserAccount user;

        if (isNumericId(identifier)) {
            long userId = Long.parseLong(identifier);
            user = userRepository.findById(userId)
                    .orElseThrow(() -> new UsernameNotFoundException(I18nUtils.get("error.user_not_found", String.valueOf(userId))));
        } else {
            user = userRepository.findByIdentifier(identifier)
                    .orElseThrow(() -> new UsernameNotFoundException(I18nUtils.get("error.user_not_found", identifier)));
        }

        // Single role from entity → Spring Security authority
        String roleName = user.getRole().getName();
        List<SimpleGrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_" + roleName));

        log.debug("User: {}, Role: {}", identifier, roleName);

        return SecurityUserDetails.build(user, List.copyOf(authorities));
    }

    private boolean isNumericId(String str) {
        return str != null && str.matches("\\d+");
    }

    public Optional<UserAccount> getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) return Optional.empty();

        Object principal = authentication.getPrincipal();
        if (principal instanceof SecurityUserDetails sud) {
            return Optional.of(sud.getUser());
        }
        return Optional.empty();
    }
}
