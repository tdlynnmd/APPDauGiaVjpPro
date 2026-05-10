package com.auction.dto;

import com.auction.enums.UserRole;
import com.auction.enums.UserStatus;

public class AdminDTO extends UserDTO {

    // Đã xóa actionLogs. Chỉ giữ lại Constructor kế thừa chuẩn từ UserDTO
    public AdminDTO(String id, String username, String email, UserRole role, UserStatus status) {
        super(id, username, email, role, status);
    }

    // Lớp này hiện tại trống, nhưng vẫn nên giữ để sau này có thêm các field
    // đặc thù của Admin (ví dụ: cấp độ quyền hạn - level, bộ phận quản lý - department)
}