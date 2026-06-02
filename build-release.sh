#!/bin/bash
cd "$(dirname "$0")"

echo "=================================================="
echo "  Đóng gói dự án Auction System và tạo Release (Linux/macOS)"
echo "=================================================="

# 1. Biên dịch và đóng gói dự án
echo "Đang clean và package dự án (bỏ qua test)..."
mvn clean package -DskipTests
if [ $? -ne 0 ]; then
    echo "[LỖI] Đóng gói dự án thất bại!"
    exit 1
fi

# 2. Tạo thư mục release và các thư mục con
if [ -d release ]; then
    echo "Đang dọn dẹp thư mục release cũ..."
    rm -rf release
fi
mkdir -p release/windows
mkdir -p release/linux_macos

# 3. Sao chép các file JAR
echo "Đang sao chép các file JAR..."
cp Server/target/Server-1.0-SNAPSHOT-jar-with-dependencies.jar release/Server.jar
cp Client/target/Client-1.0-SNAPSHOT-jar-with-dependencies.jar release/Client.jar

# 4. Sao chép file cấu hình ví dụ
cp .env.example release/.env.example
cp .env.example release/.env

# 5. Sao chép các file script chạy bản build sẵn từ template
echo "Đang sao chép script Windows chạy bản build sẵn..."
cp .templates/windows/run-server.bat release/windows/run-server.bat
cp .templates/windows/run-client.bat release/windows/run-client.bat
cp .templates/windows/stop.bat release/windows/stop.bat

echo "Đang sao chép script Linux/macOS chạy bản build sẵn..."
cp .templates/linux_macos/run-server.sh release/linux_macos/run-server.sh
cp .templates/linux_macos/run-client.sh release/linux_macos/run-client.sh
cp .templates/linux_macos/stop.sh release/linux_macos/stop.sh


# Cấp quyền thực thi cho các file shell
chmod +x release/linux_macos/*.sh

echo "=================================================="
echo "  ĐÓNG GÓI HOÀN TẤT!"
echo "  Thư mục release/ đã sẵn sàng tại thư mục gốc."
echo "=================================================="
