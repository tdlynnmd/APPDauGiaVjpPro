package com.auction.dto;

import com.auction.enums.UserRole;
import com.auction.enums.UserStatus;

public class AdminDTO extends UserDTO {
    private static final long serialVersionUID = 1L;

    public AdminDTO(String id, String username, String email, UserRole role, UserStatus status) {
        // Admin không cần quản lý tiền, truyền mặc định 0.0 cho cả 2 loại số dư
        super(id, username, email, role, status, 0.0, 0.0);
    }

    // Lớp này hiện tại trống, nhưng vẫn nên giữ để sau này có thêm các field
    // đặc thù của Admin (ví dụ: cấp độ quyền hạn - level, bộ phận quản lý - department)
}