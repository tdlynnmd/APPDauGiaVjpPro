#!/bin/bash

# Di chuyển về thư mục gốc của dự án
cd "$(dirname "$0")/.."

echo "=========================================="
echo "  Khởi chạy Auction System Server (Linux/macOS)"
echo "=========================================="

# 1. Kiểm tra và tự động tạo file .env nếu thiếu
if [ ! -f .env ]; then
    echo "[CẢNH BÁO] Không tìm thấy file .env ở thư mục gốc."
    echo "Đang tự động tạo file .env từ .env.example..."
    cp .env.example .env
    echo ""
    echo "======================================================================"
    echo "  ĐÃ TẠO FILE .env THÀNH CÔNG!"
    echo "  Vui lòng mở file .env ở thư mục gốc và kiểm tra cấu hình tài khoản"
    echo "  MySQL (DB_USER, DB_PASSWORD) của bạn trước khi chạy lại."
    echo "======================================================================"
    echo ""
    exit 0
fi

# 2. Kiểm tra cổng 3306 (MySQL local) có đang chạy hay không
(echo > /dev/tcp/127.0.0.1/3306) >/dev/null 2>&1
if [ $? -ne 0 ]; then
    echo "[LỖI] Không tìm thấy dịch vụ MySQL đang hoạt động trên cổng 3306."
    echo "Vui lòng bật MySQL Server của bạn lên trước khi chạy Server!"
    echo ""
    exit 1
fi

# 3. Thiết lập UTF-8 cho Maven để log tiếng Việt hiển thị đẹp
export MAVEN_OPTS="-Dfile.encoding=UTF-8"

# 4. Khởi chạy Server qua Maven
echo "Đang biên dịch và khởi chạy Server..."
mvn compile exec:java -pl Server
