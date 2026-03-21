package com.thang.roombooking.repository;


import com.thang.roombooking.entity.RefreshToken;
import com.thang.roombooking.entity.UserAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    // 1. Đây là hàm quan trọng nhất (thay thế cho việc loop check hash)
    // Spring Data JPA sẽ tự tạo câu lệnh: SELECT * FROM ... WHERE token = ?
    Optional<RefreshToken> findByToken(String token);

    // 2. Xóa token của user (Dùng khi user muốn đăng xuất khỏi tất cả thiết bị)
    @Modifying
    void deleteByUser(UserAccount user);

    // Hoặc xóa cụ thể 1 token (khi logout 1 thiết bị)
    @Modifying
    void deleteByToken(String token);

    // 3. Hàm revoke (logout) tất cả token cũ của user
    @Modifying
    @Query("UPDATE RefreshToken r SET r.revoked = true WHERE r.user = :user AND r.revoked = false")
    void revokeAllByUser(@Param("user") UserAccount user);

    @Modifying
    @Transactional
    @Query("DELETE FROM RefreshToken r WHERE r.user.id = :userId")
    void deleteByUserId(@Param("userId") Long userId);

    // --- Các hàm dưới đây CÓ THỂ GIỮ LẠI nếu logic nghiệp vụ cần ---

    // Tìm xem user có token nào còn sống không (Lưu ý trả về List để tránh lỗi NonUnique)
    // List<RefreshToken> findAllByUserAndRevokedFalse(UserAccount user);
}
