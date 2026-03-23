package com.thang.roombooking.entity;

import com.thang.roombooking.common.constant.CommonConfig;
import com.thang.roombooking.common.entity.BaseSoftDeleteEntity;
import com.thang.roombooking.common.enums.IdentityProvider;
import com.thang.roombooking.common.enums.UserStatus;
import jakarta.persistence.*;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "users")
public class UserAccount extends BaseSoftDeleteEntity<Long> {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "username", nullable = false, unique = true, length = CommonConfig.MAX_LENGTH_USERNAME)
    private String username;

    @Column(name = "full_name", nullable = false, length = CommonConfig.MAX_LENGTH_FULLNAME)
    private String fullName;

    @Column(name = "email", nullable = false, unique = true, length = 255)
    private String email;

    @Column(name = "password", nullable = false, length = 255)
    private String password;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "role_id", nullable = false)
    private Role role;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private UserStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "provider", length = 20, nullable = true)
    private IdentityProvider provider;

    @Column(name = "provider_id", nullable = true)
    private String providerId;

    @Version
    @Column(name = "version")
    private Integer version;

    @Override
    public Long getId() {
        return id;
    }
}