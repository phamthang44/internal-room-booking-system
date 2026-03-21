package com.thang.roombooking.repository;



import com.thang.roombooking.entity.UserAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
public interface UserAccountRepository extends JpaRepository<UserAccount, Long> {
    Optional<UserAccount> findByUsername(String username);
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);

    @Query("SELECT u FROM UserAccount u WHERE u.email = :identifier OR u.username = :identifier")
    Optional<UserAccount> findByIdentifier(@Param("identifier") String identifier);

    Optional<UserAccount> findByEmail(String email);

}
