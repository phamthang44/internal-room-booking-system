package com.thang.roombooking.infrastructure.security;

import com.thang.roombooking.common.enums.UserStatus;
import com.thang.roombooking.entity.UserAccount;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

@RequiredArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@Slf4j
public class SecurityUserDetails implements UserDetails {

    private Collection<? extends GrantedAuthority> authorities;
    private final UserAccount user;

    public static SecurityUserDetails build(UserAccount user, List<GrantedAuthority> authorityList) {
        return SecurityUserDetails.builder()
                .authorities(authorityList)
                .user(user)
                .build();
    }

    @Override
    public String getUsername() {
        return user.getUsername();
    }

    @Override
    public String getPassword() {
        return user.getPassword();
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return this.authorities == null ? List.of() : this.authorities;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return user.getStatus() == UserStatus.ACTIVE;
    }

    @Override
    public boolean isAccountNonLocked() {
        return user.getStatus() != UserStatus.BANNED;
    }
}
