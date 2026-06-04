-- =============================================================================
-- KỊCH BẢN KHỞI TẠO CƠ SỞ DỮ LIỆU TỐI ƯU (REAL-TIME ONLINE AUCTION SYSTEM)
-- Cơ sở dữ liệu: vnu_auction_system (MySQL 8.0+)
-- Hướng dẫn: Khởi tạo database và chạy toàn bộ tệp SQL này để thiết lập cấu trúc bảng tối ưu.
-- =============================================================================

CREATE DATABASE IF NOT EXISTS `vnu_auction_system` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE `vnu_auction_system`;

-- 1. BẢNG USERS (Lưu trữ thông tin tài khoản, sử dụng UUID Binary, Unique Soft Delete)
DROP TABLE IF EXISTS `users`;
CREATE TABLE `users` (
  `id` binary(16) NOT NULL,
  `user_type` enum('ADMIN','SELLER','BIDDER') NOT NULL,
  `username` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL,
  `email` varchar(255) NOT NULL,
  `password_hash` varchar(255) NOT NULL,
  `status` enum('ACTIVE','INACTIVE','BANNED','UNVERIFIED') DEFAULT 'ACTIVE',
  `rating` decimal(3,1) DEFAULT NULL,
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted_at` timestamp NULL DEFAULT NULL,
  `available_balance` decimal(15,2) NOT NULL DEFAULT '0.00',
  `frozen_balance` decimal(15,2) NOT NULL DEFAULT '0.00',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_username_deleted` (`username`, `deleted_at`),
  UNIQUE KEY `uq_email_deleted` (`email`, `deleted_at`),
  KEY `idx_user_type` (`user_type`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 2. BẢNG ITEMS (Lưu trữ tài sản đấu giá đa hình, hỗ trợ Single Table Inheritance & CHECK constraint)
DROP TABLE IF EXISTS `items`;
CREATE TABLE `items` (
  `id` binary(16) NOT NULL,
  `item_type` enum('ART','ELECTRONICS','VEHICLES') NOT NULL,
  `seller_id` binary(16) NOT NULL,
  `name` varchar(255) NOT NULL,
  `description` text,
  `starting_price` decimal(15,2) NOT NULL,
  `year_created` int DEFAULT NULL,
  `image_url` varchar(512) DEFAULT NULL,
  `status` varchar(10) NOT NULL DEFAULT 'ACTIVE',
  `painter` varchar(255) DEFAULT NULL,
  `art_style` varchar(255) DEFAULT NULL,
  `brand` varchar(100) DEFAULT NULL,
  `warranty_months` int DEFAULT NULL,
  `model` varchar(100) DEFAULT NULL,
  `km_age` decimal(10,2) DEFAULT NULL,
  `license_plate` varchar(20) DEFAULT NULL,
  `engine_type` varchar(50) DEFAULT NULL,
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted_at` timestamp NULL DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_seller_id` (`seller_id`),
  KEY `idx_item_type` (`item_type`),
  KEY `idx_status` (`status`),
  CONSTRAINT `fk_items_seller` FOREIGN KEY (`seller_id`) REFERENCES `users` (`id`),
  -- Check constraints bảo vệ dữ liệu lớp con khỏi dữ liệu rác
  CONSTRAINT `chk_art_properties` CHECK (
    (item_type = 'ART' AND painter IS NOT NULL AND art_style IS NOT NULL) OR (item_type != 'ART')
  ),
  CONSTRAINT `chk_electronics_properties` CHECK (
    (item_type = 'ELECTRONICS' AND brand IS NOT NULL AND warranty_months IS NOT NULL) OR (item_type != 'ELECTRONICS')
  ),
  CONSTRAINT `chk_vehicle_properties` CHECK (
    (item_type = 'VEHICLES' AND model IS NOT NULL AND license_plate IS NOT NULL) OR (item_type != 'VEHICLES')
  )
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 3. BẢNG AUCTIONS (Lưu trữ phiên đấu giá, Composite Index tối ưu luồng daemon monitor)
DROP TABLE IF EXISTS `auctions`;
CREATE TABLE `auctions` (
  `id` binary(16) NOT NULL,
  `item_id` binary(16) NOT NULL,
  `seller_id` binary(16) NOT NULL,
  `highest_bidder_id` binary(16) DEFAULT NULL,
  `current_winning_bid_id` binary(16) DEFAULT NULL,
  `current_price` decimal(15,2) NOT NULL,
  `step_price` decimal(15,2) NOT NULL,
  `start_time` timestamp NOT NULL,
  `end_time` timestamp NOT NULL,
  `status` enum('OPEN','RUNNING','FINISHED','PAID','CANCELED') DEFAULT 'OPEN',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_item_id` (`item_id`),
  KEY `idx_seller_id` (`seller_id`),
  KEY `idx_highest_bidder_id` (`highest_bidder_id`),
  -- Composite Index quan trọng cho daemon monitor quét phiên hết hạn liên tục mỗi giây
  KEY `idx_status_endtime` (`status`, `end_time`),
  CONSTRAINT `fk_auctions_item` FOREIGN KEY (`item_id`) REFERENCES `items` (`id`),
  CONSTRAINT `fk_auctions_seller` FOREIGN KEY (`seller_id`) REFERENCES `users` (`id`),
  CONSTRAINT `fk_auctions_bidder` FOREIGN KEY (`highest_bidder_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 4. BẢNG AUTO_BIDS (Cấu hình tự động thầu, sử dụng Decimal cho tiền tệ & bổ sung Khóa ngoại)
DROP TABLE IF EXISTS `auto_bids`;
CREATE TABLE `auto_bids` (
  `id` binary(16) NOT NULL,
  `user_id` binary(16) NOT NULL,
  `auction_id` binary(16) NOT NULL,
  `max_bid` decimal(15,2) NOT NULL,
  `increment_amount` decimal(15,2) NOT NULL,
  `is_active` tinyint(1) DEFAULT '1',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `unique_user_auction` (`user_id`,`auction_id`),
  CONSTRAINT `fk_autobids_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_autobids_auction` FOREIGN KEY (`auction_id`) REFERENCES `auctions` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 5. BẢNG BID_TRANSACTIONS (Lịch sử đặt giá, lưu vết giao dịch tài chính ví)
DROP TABLE IF EXISTS `bid_transactions`;
CREATE TABLE `bid_transactions` (
  `id` binary(16) NOT NULL,
  `bidder_id` binary(16) NOT NULL,
  `auction_id` binary(16) NOT NULL,
  `amount` decimal(15,2) NOT NULL,
  `status` enum('ACCEPTED','REJECTED','REFUNDED') DEFAULT 'ACCEPTED',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_bidder_id` (`bidder_id`),
  KEY `idx_status` (`status`),
  KEY `idx_auction_time` (`auction_id`,`created_at` DESC),
  CONSTRAINT `fk_bids_bidder` FOREIGN KEY (`bidder_id`) REFERENCES `users` (`id`),
  CONSTRAINT `fk_bids_auction` FOREIGN KEY (`auction_id`) REFERENCES `auctions` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 6. BẢNG BIDDER_JOINED_AUCTIONS (Theo dõi các bidder đang tham gia phòng xem live)
DROP TABLE IF EXISTS `bidder_joined_auctions`;
CREATE TABLE `bidder_joined_auctions` (
  `user_id` binary(16) NOT NULL,
  `auction_id` binary(16) NOT NULL,
  `joined_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`user_id`,`auction_id`),
  KEY `idx_auction_id` (`auction_id`),
  CONSTRAINT `fk_joins_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`),
  CONSTRAINT `fk_joins_auction` FOREIGN KEY (`auction_id`) REFERENCES `auctions` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 7. BẢNG ACTION_LOGS (Nhật ký hành động kiểm toán của Admin)
DROP TABLE IF EXISTS `action_logs`;
CREATE TABLE `action_logs` (
  `id` binary(16) NOT NULL,
  `admin_id` binary(16) NOT NULL,
  `action_detail` text NOT NULL,
  `target_type` varchar(50) NOT NULL,
  `target_id` binary(16) NOT NULL,
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_admin_id` (`admin_id`),
  KEY `idx_target` (`target_type`,`target_id`),
  CONSTRAINT `fk_logs_admin` FOREIGN KEY (`admin_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
